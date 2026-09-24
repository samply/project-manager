package de.samply.form.core.history;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.db.model.FormHistory;
import de.samply.db.repository.FormHistoryRepository;
import de.samply.db.repository.ProjectFormFieldRepository;
import de.samply.db.repository.ProjectFormRepository;
import de.samply.form.core.history.FormDefinitionConflict.Kind;
import de.samply.form.core.history.FormDefinitionFactory.CanonicalFormDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * At startup, checks every configured form against its latest recorded
 * definition ({@code samply.form_history}) and stops the project manager on
 * incompatible changes (see {@link FormDefinitionComparator}):
 * <ul>
 *   <li>a form seen for the first time is recorded as version 1;</li>
 *   <li>an unchanged form (same checksum) is left as it is;</li>
 *   <li>a changed form is compared: without conflicts, its next version is
 *   recorded;</li>
 *   <li>a recorded form missing from the configuration (removed or renamed) is
 *   a conflict: its stored values would be orphaned.</li>
 * </ul>
 * A conflict only blocks startup if data exists that it could break (see
 * {@link #hasStoredValue}); otherwise it is logged as a warning and accepted.
 * All conflicts of all forms are logged - per form, with the fix to paste into
 * its JSON file ({@link FormDefinitionFix}) - then startup fails; nothing is
 * recorded unless every form is free of conflicts.
 * <p>
 * Runs once all beans exist, before the web server starts
 * ({@link SmartInitializingSingleton}), so a conflicting configuration never
 * serves a request.
 */
@Slf4j
@Component
public class FormHistoryValidator implements SmartInitializingSingleton {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String REMOVED_DEFINITION = "null";

    private final FormDefinitionFactory formDefinitionFactory;
    private final FormHistoryRepository formHistoryRepository;
    private final ProjectFormFieldRepository projectFormFieldRepository;
    private final ProjectFormRepository projectFormRepository;

    public FormHistoryValidator(FormDefinitionFactory formDefinitionFactory,
                                FormHistoryRepository formHistoryRepository,
                                ProjectFormFieldRepository projectFormFieldRepository,
                                ProjectFormRepository projectFormRepository) {
        this.formDefinitionFactory = formDefinitionFactory;
        this.formHistoryRepository = formHistoryRepository;
        this.projectFormFieldRepository = projectFormFieldRepository;
        this.projectFormRepository = projectFormRepository;
    }

    @Override
    public void afterSingletonsInstantiated() {
        validate();
    }

    public void validate() {
        Map<String, CanonicalFormDefinition> definitions = formDefinitionFactory.fetchDefinitions();
        List<FormConflicts> conflictingForms = new ArrayList<>();
        List<FormHistory> newVersions = new ArrayList<>();

        definitions.forEach((formTitle, definition) -> {
            Optional<FormHistory> recorded = formHistoryRepository.findFirstByFormTitleOrderByVersionDesc(formTitle);
            if (recorded.isEmpty()) {
                newVersions.add(newVersion(definition, 1));
            } else if (!recorded.get().getChecksum().equals(definition.checksum())) {
                JsonNode recordedTree = readTree(recorded.get());
                JsonNode currentTree = readTree(definition.definition(), formTitle);
                List<FormDefinitionConflict> conflicts = new ArrayList<>();
                FormDefinitionComparator.compare(formTitle, recordedTree, currentTree).forEach(conflict -> {
                    if (hasStoredValue(conflict)) {
                        conflicts.add(conflict);
                    } else {
                        log.warn("Accepted form definition change - {} (no project has a value for it)",
                                conflict.describe());
                    }
                });
                if (conflicts.isEmpty()) {
                    newVersions.add(newVersion(definition, recorded.get().getVersion() + 1));
                } else {
                    conflictingForms.add(new FormConflicts(recorded.get(), recordedTree, currentTree, conflicts));
                }
            }
        });

        formHistoryRepository.findAllFormTitles().stream()
                .filter(formTitle -> !definitions.containsKey(formTitle))
                .sorted()
                .forEach(formTitle -> formHistoryRepository.findFirstByFormTitleOrderByVersionDesc(formTitle)
                        .filter(recorded -> !isRemovedMarker(recorded))
                        .ifPresent(recorded -> {
                            FormDefinitionConflict removed =
                                    new FormDefinitionConflict(formTitle, null, Kind.FORM_REMOVED, null, null);
                            if (isFormUsed(formTitle)) {
                                conflictingForms.add(new FormConflicts(recorded, readTree(recorded), null, List.of(removed)));
                            } else {
                                // Recorded once, so later starts do not report the form again.
                                log.warn("Accepted form definition change - {} (no project selected it or has a value in it)",
                                        removed.describe());
                                newVersions.add(removedMarker(formTitle, recorded.getVersion() + 1));
                            }
                        }));

        if (!conflictingForms.isEmpty()) {
            // One explanation per form, with the fix to paste into its JSON file.
            conflictingForms.forEach(form -> log.error(FormDefinitionFix.explain(
                    form.recorded().getFormTitle(), form.recorded().getVersion(),
                    form.recordedTree(), form.currentTree(), form.conflicts())));
            throw new FormDefinitionConflictException(
                    conflictingForms.stream().flatMap(form -> form.conflicts().stream()).toList());
        }
        newVersions.forEach(this::record);
    }

    // A conflict only blocks if data exists that it could break: a non-blank
    // stored value of the field (or, for a removed form, a project that
    // selected it or has a value in it). Changes nobody has used yet - e.g.
    // while a form is still being developed - are accepted with a warning.
    private boolean hasStoredValue(FormDefinitionConflict conflict) {
        return conflict.label() == null
                ? isFormUsed(conflict.formTitle())
                : projectFormFieldRepository.existsValue(conflict.formTitle(), conflict.label());
    }

    private boolean isFormUsed(String formTitle) {
        return projectFormRepository.existsByFormTitle(formTitle)
                || projectFormFieldRepository.existsValueInForm(formTitle);
    }

    /**
     * The version recorded when an unused form is removed from the
     * configuration: its definition is JSON {@code null}. A form whose latest
     * version is this marker is no longer reported as removed; if it comes
     * back, it is compared against an empty definition and recorded again.
     */
    private static FormHistory removedMarker(String formTitle, int version) {
        FormHistory result = new FormHistory();
        result.setFormTitle(formTitle);
        result.setVersion(version);
        result.setChecksum(FormDefinitionFactory.checksum(REMOVED_DEFINITION));
        result.setDefinition(REMOVED_DEFINITION);
        return result;
    }

    private static boolean isRemovedMarker(FormHistory recorded) {
        return REMOVED_DEFINITION.equals(recorded.getDefinition());
    }

    /** A form's conflicts with what they were found between (currentTree null: form no longer configured). */
    private record FormConflicts(FormHistory recorded, JsonNode recordedTree, JsonNode currentTree,
                                 List<FormDefinitionConflict> conflicts) {
    }

    private static JsonNode readTree(FormHistory recorded) {
        return readTree(recorded.getDefinition(), recorded.getFormTitle() + " version " + recorded.getVersion());
    }

    private static JsonNode readTree(String definition, String description) {
        try {
            return OBJECT_MAPPER.readTree(definition);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Definition of form " + description + " is not valid JSON", e);
        }
    }

    private static FormHistory newVersion(CanonicalFormDefinition definition, int version) {
        FormHistory result = new FormHistory();
        result.setFormTitle(definition.formTitle());
        result.setVersion(version);
        result.setChecksum(definition.checksum());
        result.setDefinition(definition.definition());
        return result;
    }

    // Another instance starting at the same time may have recorded the same
    // version already: the unique (form_title, version) constraint lets one win.
    private void record(FormHistory definition) {
        try {
            formHistoryRepository.save(definition);
            log.info("Recorded form definition {} version {}", definition.getFormTitle(), definition.getVersion());
        } catch (DataIntegrityViolationException e) {
            log.info("Form definition {} version {} was recorded by another instance",
                    definition.getFormTitle(), definition.getVersion());
        }
    }
}
