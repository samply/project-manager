package de.samply.form;

import de.samply.db.model.ProjectForm;
import de.samply.db.repository.ProjectFormRepository;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.security.SessionUser;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
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


    public FormService(FormConfig formConfig,
                       ProjectFormRepository projectFormRepository,
                       NotificationService notificationService,
                       SessionUser sessionUser) {
        this.formConfig = formConfig;
        this.projectFormRepository = projectFormRepository;
        this.notificationService = notificationService;
        this.sessionUser = sessionUser;
    }

    public Set<String> fetchProjectFormTitles() {
        return formConfig.getFormTitleLabelFieldMap().keySet();
    }

    public List<FormFieldConfig> fetchProjectFormFields(@NotNull String projectFormTitle) {
        return List.copyOf(formConfig.getFormTitleLabelFieldMap().getOrDefault(projectFormTitle, Map.of()).values());
    }

    public Map<String, String> fetchProjectFormLabelAndValues(@NotNull String projectFormTitle, @NotNull String projectCode) {
        return projectFormRepository.findByProject_CodeAndFormTitle(projectCode, projectFormTitle).stream()
                .collect(Collectors.toMap(ProjectForm::getLabel, ProjectForm::getValue));
    }

    public void addProjectFormLabelAndValues(@NotNull Map<String, String> labelAndValues, @NotNull String projectFormTitle, @NotNull String projectCode) {
        Map<String, ProjectForm> labelFormMap = projectFormRepository.findByProject_CodeAndFormTitle(projectCode, projectFormTitle).stream()
                .collect(Collectors.toMap(ProjectForm::getLabel, Function.identity()));
        labelAndValues.forEach((label, formValue) -> {
            ProjectForm projectForm = labelFormMap.get(label);
            boolean isModified = false;
            if (projectForm == null) {
                projectForm = new ProjectForm();
                projectForm.setLabel(label);
                projectForm.setFormTitle(projectFormTitle);
                projectForm.setValue(formValue);
                isModified = true;
                notificationService.createNotification(projectCode, null,
                        sessionUser.getEmail(), OperationType.ADD_PROJECT_FORM_LABEL,
                        "label: " + label + " - value: " + formValue, null, null);
            } else {
                if (!projectForm.getValue().equals(formValue)) {
                    projectForm.setValue(formValue);
                    notificationService.createNotification(projectCode, null,
                            sessionUser.getEmail(), OperationType.EDIT_PROJECT_FORM_LABEL,
                            "label: " + label + " - value: " + formValue, null, null);
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
