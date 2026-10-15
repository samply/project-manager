package de.samply.form.core.history;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.db.model.FormHistory;
import de.samply.db.repository.FormHistoryRepository;
import de.samply.db.repository.ProjectFormFieldRepository;
import de.samply.db.repository.ProjectFormRepository;
import de.samply.form.core.history.FormDefinitionConflict.Kind;
import de.samply.form.core.history.FormDefinitionFactory.CanonicalFormDefinition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormHistoryValidatorTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final FormDefinitionFactory factory = mock(FormDefinitionFactory.class);
    private final FormHistoryRepository repository = mock(FormHistoryRepository.class);
    private final ProjectFormFieldRepository values = mock(ProjectFormFieldRepository.class);
    private final ProjectFormRepository selections = mock(ProjectFormRepository.class);
    private final FormHistoryValidator validator = new FormHistoryValidator(factory, repository, values, selections);

    @Test
    void recordsEveryFormSeenForTheFirstTimeAsVersionOne() {
        configure(definition("project", "STRING"), definition("ethics", "STRING"));

        validator.validate();

        ArgumentCaptor<FormHistory> saved = ArgumentCaptor.forClass(FormHistory.class);
        verify(repository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(FormHistory::getFormTitle, FormHistory::getVersion)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("project", 1), org.assertj.core.groups.Tuple.tuple("ethics", 1));
    }

    @Test
    void leavesAnUnchangedFormAsItIs() {
        CanonicalFormDefinition project = definition("project", "STRING");
        configure(project);
        recorded(project, 3);

        validator.validate();

        verify(repository, never()).save(any());
    }

    @Test
    void recordsTheNextVersionOfACompatiblyChangedForm() {
        CanonicalFormDefinition before = definition("project", "STRING");
        configure(definition("project", "LONG_STRING"));
        recorded(before, 3);

        validator.validate();

        ArgumentCaptor<FormHistory> saved = ArgumentCaptor.forClass(FormHistory.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getVersion()).isEqualTo(4);
    }

    @Test
    void stopsOnConflictsOfAllFormsAndRecordsNothing() {
        configure(definition("project", "INTEGER"), definition("ethics", "BOOLEAN"), definition("funding", "LONG_STRING"));
        recorded(definition("project", "STRING"), 1);
        recorded(definition("ethics", "STRING"), 2);
        recorded(definition("funding", "STRING"), 1);
        when(values.existsValue(anyString(), anyString())).thenReturn(true);

        FormDefinitionConflictException exception =
                catchThrowableOfType(FormDefinitionConflictException.class, validator::validate);

        assertThat(exception.getConflicts()).extracting(FormDefinitionConflict::formTitle).containsExactly("project", "ethics");
        verify(repository, never()).save(any());
    }

    @Test
    void aRecordedFormMissingFromTheConfigurationIsAConflict() {
        configure(definition("project", "STRING"));
        recorded(definition("samples", "STRING"), 2);
        when(repository.findAllFormTitles()).thenReturn(List.of("project", "samples"));
        when(selections.existsByFormTitle("samples")).thenReturn(true);

        FormDefinitionConflictException exception =
                catchThrowableOfType(FormDefinitionConflictException.class, validator::validate);

        assertThat(exception.getConflicts()).singleElement()
                .satisfies(conflict -> {
                    assertThat(conflict.kind()).isEqualTo(Kind.FORM_REMOVED);
                    assertThat(conflict.describe()).isEqualTo("Form \"samples\": form removed or renamed");
                });
    }

    @Test
    void aVersionRecordedByAnotherInstanceAtTheSameTimeIsNoError() {
        configure(definition("project", "STRING"));
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void acceptsAConflictOnAFieldNoProjectHasAValueFor() {
        configure(definition("project", "INTEGER"));
        recorded(definition("project", "STRING"), 2);

        validator.validate();

        ArgumentCaptor<FormHistory> saved = ArgumentCaptor.forClass(FormHistory.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getVersion()).isEqualTo(3);
    }

    @Test
    void onlyConflictsOnFieldsWithStoredValuesBlock() throws Exception {
        configure(twoFields("project", "INTEGER", "INTEGER"));
        recorded(twoFields("project", "STRING", "STRING"), 1);
        when(values.existsValue("project", "used")).thenReturn(true);

        FormDefinitionConflictException exception =
                catchThrowableOfType(FormDefinitionConflictException.class, validator::validate);

        assertThat(exception.getConflicts()).extracting(FormDefinitionConflict::label).containsExactly("used");
        verify(repository, never()).save(any());
    }

    @Test
    void anUnusedRemovedFormIsRecordedAsRemovedOnce() {
        configure(definition("project", "STRING"));
        recorded(definition("samples", "STRING"), 4);
        when(repository.findAllFormTitles()).thenReturn(List.of("project", "samples"));

        validator.validate();

        ArgumentCaptor<FormHistory> saved = ArgumentCaptor.forClass(FormHistory.class);
        verify(repository, times(2)).save(saved.capture());
        FormHistory marker = saved.getAllValues().stream()
                .filter(row -> row.getFormTitle().equals("samples")).findFirst().orElseThrow();
        assertThat(marker.getVersion()).isEqualTo(5);
        assertThat(marker.getDefinition()).isEqualTo("null");
    }

    @Test
    void aFormRecordedAsRemovedIsNotReportedAgain() {
        CanonicalFormDefinition project = definition("project", "STRING");
        configure(project);
        recorded(project, 1);
        recordedAsRemoved("samples", 5);
        when(repository.findAllFormTitles()).thenReturn(List.of("project", "samples"));

        validator.validate();

        verify(repository, never()).save(any());
    }

    @Test
    void aFormRecordedAsRemovedIsRecordedAgainWhenItComesBack() {
        configure(definition("samples", "STRING"));
        recordedAsRemoved("samples", 5);

        validator.validate();

        ArgumentCaptor<FormHistory> saved = ArgumentCaptor.forClass(FormHistory.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getVersion()).isEqualTo(6);
        assertThat(saved.getValue().getDefinition()).contains("\"samples\"");
    }

    private void configure(CanonicalFormDefinition... definitions) {
        Map<String, CanonicalFormDefinition> byTitle = new LinkedHashMap<>();
        for (CanonicalFormDefinition definition : definitions) {
            byTitle.put(definition.formTitle(), definition);
        }
        when(factory.fetchDefinitions()).thenReturn(byTitle);
    }

    private void recorded(CanonicalFormDefinition definition, int version) {
        FormHistory row = new FormHistory();
        row.setFormTitle(definition.formTitle());
        row.setVersion(version);
        row.setChecksum(definition.checksum());
        row.setDefinition(definition.definition());
        when(repository.findFirstByFormTitleOrderByVersionDesc(definition.formTitle())).thenReturn(Optional.of(row));
    }

    private void recordedAsRemoved(String formTitle, int version) {
        FormHistory row = new FormHistory();
        row.setFormTitle(formTitle);
        row.setVersion(version);
        row.setChecksum(FormDefinitionFactory.checksum("null"));
        row.setDefinition("null");
        when(repository.findFirstByFormTitleOrderByVersionDesc(formTitle)).thenReturn(Optional.of(row));
    }

    private static CanonicalFormDefinition twoFields(String title, String usedType, String unusedType) throws Exception {
        JsonNode file = OBJECT_MAPPER.readTree("{\"title\": \"" + title + "\", \"fields\": ["
                + "{\"label\": \"used\", \"data_type\": \"" + usedType + "\"},"
                + "{\"label\": \"unused\", \"data_type\": \"" + unusedType + "\"}]}");
        return FormDefinitionFactory.createDefinition(title, file);
    }

    private static CanonicalFormDefinition definition(String title, String dataType) {
        try {
            JsonNode file = OBJECT_MAPPER.readTree("{\"title\": \"" + title + "\", \"fields\": ["
                    + "{\"label\": \"field\", \"data_type\": \"" + dataType + "\"}]}");
            return FormDefinitionFactory.createDefinition(title, file);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
