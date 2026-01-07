package de.samply.form;

import de.samply.db.model.ProjectForm;
import de.samply.db.repository.ProjectFormRepository;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.FormField;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.security.SessionUser;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service class responsible for handling operations related to forms and their associated configurations,
 * labels, and values within a project context.
 * <p>
 * This class provides methods to fetch form titles, retrieve form field configurations, and manage label-value
 * pairs for specific project forms. It utilizes repositories and services to interact with underlying
 * storage and notification systems.
 */
@Service
public class FormService {

    private final FormConfig formConfig;
    private final ProjectFormRepository projectFormRepository;
    private final NotificationService notificationService;
    private final SessionUser sessionUser;
    private final DtoFactory dtoFactory;


    public FormService(FormConfig formConfig,
                       ProjectFormRepository projectFormRepository,
                       NotificationService notificationService,
                       SessionUser sessionUser,
                       DtoFactory dtoFactory) {
        this.formConfig = formConfig;
        this.projectFormRepository = projectFormRepository;
        this.notificationService = notificationService;
        this.sessionUser = sessionUser;
        this.dtoFactory = dtoFactory;
    }

    public List<FormField> fetchProjectFormTitles(Optional<String> language) {
        return formConfig.getFormTitleLabelFieldMap().keySet().stream()
                .map(title -> dtoFactory.convert(
                        title,
                        Optional.empty(),
                        Optional.empty(),
                        language
                ))
                .collect(Collectors.toList());
    }

    public List<FormField> fetchProjectFormFields(@NotNull String formTitle, Optional<String> language) {
        return formConfig.getFormTitleLabelFieldMap()
                .getOrDefault(formTitle, Map.of())
                .values()
                .stream()
                .map(field -> dtoFactory.convert(formTitle, field, Optional.empty(), language))
                .toList();
    }

    public List<FormField> fetchAllProjectFormFields(Optional<String> language) {
        return formConfig.getFormTitleLabelFieldMap().entrySet().stream()
                .flatMap(e1 -> e1.getValue().values().stream()
                        .map(formFieldConfig -> Map.entry(e1.getKey(), formFieldConfig)))
                .map(titleFormFieldConfig ->
                        dtoFactory.convert(
                                titleFormFieldConfig.getKey(),
                                titleFormFieldConfig.getValue(),
                                Optional.empty(),
                                language))
                .toList();
    }

    public List<FormField> fetchProjectFormLabelAndValues(@NotNull String formTitle, @NotNull String projectCode, Optional<String> language) {
        return projectFormRepository.findByProject_CodeAndFormTitle(projectCode, formTitle).stream()
                .map(projectForm -> dtoFactory.convert(projectForm, language))
                .toList();
    }

    public void editProjectFormLabelAndValues(Optional<List<FormField>> formFields, @NotNull String projectCode) {
        if (formFields.isEmpty() || formFields.get().isEmpty()) {
            return;
        }
        Map<String, ProjectForm> labelFormMap = projectFormRepository.findByProject_Code(projectCode).stream()
                .collect(Collectors.toMap(ProjectForm::getLabel, Function.identity()));
        formFields.get().forEach(formField -> {
            ProjectForm projectForm = labelFormMap.get(formField.label());
            boolean isModified = false;
            if (projectForm == null) {
                projectForm = new ProjectForm();
                projectForm.setLabel(formField.label());
                projectForm.setFormTitle(formField.title());
                projectForm.setValue(formField.value());
                isModified = true;
                notificationService.createNotification(projectCode, null,
                        sessionUser.getEmail(), OperationType.ADD_PROJECT_FORM_LABEL,
                        "title: " + formField.title() + "label: " + formField.label() + " - value: " + formField.value(), null, null);
            } else {
                if (!projectForm.getValue().equals(formField.value())) {
                    projectForm.setValue(formField.value());
                    notificationService.createNotification(projectCode, null,
                            sessionUser.getEmail(), OperationType.EDIT_PROJECT_FORM_LABEL,
                            "title: " + formField.title() + "label: " + formField.label() + " - value: " + formField.value(), null, null);
                    isModified = true;
                }
            }
            if (isModified) {
                projectForm.setModifiedAt(Instant.now());
                projectFormRepository.save(projectForm);
            }
        });
    }


}
