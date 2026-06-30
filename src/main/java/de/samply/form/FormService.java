package de.samply.form;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectForm;
import de.samply.db.model.ProjectFormField;
import de.samply.db.repository.ProjectFormFieldRepository;
import de.samply.db.repository.ProjectFormRepository;
import de.samply.frontend.dto.FormField;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.security.SessionUser;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service class responsible for handling operations related to forms and their associated configurations,
 * labels, and values within a project context.
 * <p>
 * This class provides methods to fetch form titles, retrieve form field configurations, and manage label-value
 * pairs for specific project forms. It uses repositories and services to interact with underlying
 * storage and notification systems.
 */
@Service
public class FormService {

    private final FormConfig formConfig;
    private final ProjectFormFieldRepository projectFormFieldRepository;
    private final ProjectFormRepository projectFormRepository;
    private final NotificationService notificationService;
    private final SessionUser sessionUser;


    public FormService(FormConfig formConfig,
                       ProjectFormFieldRepository projectFormFieldRepository,
                       ProjectFormRepository projectFormRepository,
                       NotificationService notificationService,
                       SessionUser sessionUser) {
        this.formConfig = formConfig;
        this.projectFormFieldRepository = projectFormFieldRepository;
        this.projectFormRepository = projectFormRepository;
        this.notificationService = notificationService;
        this.sessionUser = sessionUser;
    }

    protected List<ProjectFormField> fetchProjectFormFieldsWithValues(@NotNull String formTitle, @NotNull Project project) {
        return projectFormFieldRepository.findByProjectAndFormTitle(project, formTitle);
    }

    @Transactional
    public void editProjectFormFieldValues(Optional<FormField[]> formFields, @NotNull Project project) {
        if (formFields.isEmpty() || formFields.get().length == 0) {
            return;
        }
        Map<String, ProjectFormField> labelFormMap = projectFormFieldRepository.findByProject(project).stream()
                .collect(Collectors.toMap(ProjectFormField::getLabel, Function.identity()));
        Arrays.stream(formFields.get()).forEach(formField -> {
            ProjectFormField projectFormField = labelFormMap.get(formField.label());
            boolean isModified = false;
            String details = "title: " + formField.title() + "label: " + formField.label() + " - value: " + formField.value();
            if (projectFormField == null) {
                projectFormField = new ProjectFormField();
                projectFormField.setLabel(formField.label());
                projectFormField.setFormTitle(formField.title());
                projectFormField.setValue(formField.value());
                projectFormField.setProject(project);
                isModified = true;
                notificationService.createNotification(project, null,
                        sessionUser.getEmail(), OperationType.ADD_PROJECT_FORM_LABEL,
                        details, null, null);
            } else {
                if (!projectFormField.getValue().equals(formField.value())) {
                    projectFormField.setValue(formField.value());
                    notificationService.createNotification(project, null,
                            sessionUser.getEmail(), OperationType.EDIT_PROJECT_FORM_LABEL,
                            details, null, null);
                    isModified = true;
                }
            }
            if (isModified) {
                projectFormField.setModifiedAt(Instant.now());
                projectFormFieldRepository.save(projectFormField);
            }
        });
    }

    protected List<ProjectForm> fetchSelectedForms(@NotNull Project project) {
        return projectFormRepository.findByProject(project);
    }

    @Transactional
    public void addSelectedForm(@NotNull Project project, @NotNull String formTitle) {
        projectFormRepository
                .findByProjectAndFormTitle(project, formTitle)
                .ifPresentOrElse(
                        _ -> { /* already exists → do nothing */ },
                        () -> {
                            if (!formConfig.getFormTitleLabelFieldMap().containsKey(formTitle)) {
                                throw new IllegalArgumentException("Form title not found: " + formTitle);
                            }

                            ProjectForm projectForm = new ProjectForm();
                            projectForm.setFormTitle(formTitle);
                            projectForm.setProject(project);
                            projectForm.setCreatedAt(Instant.now());

                            projectFormRepository.save(projectForm);
                        }
                );
    }

    @Transactional
    public void removeSelectedForm(@NotNull Project project, @NotNull String formTitle) {
        projectFormRepository
                .findByProjectAndFormTitle(project, formTitle)
                .ifPresent(projectForm -> {
                            if (!formConfig.getFormTitleLabelFieldMap().containsKey(formTitle)) {
                                throw new IllegalArgumentException("Form title not found: " + formTitle);
                            }
                            this.projectFormRepository.delete(projectForm);
                        }
                );
    }

    @Transactional
    public void syncSelectedForms(@NotNull Project project,
                                  @NotNull Collection<String> formTitles) {

        // Validate input titles first
        Set<String> allowedTitles = formConfig.getFormTitleLabelFieldMap().keySet();

        for (String title : formTitles) {
            if (!allowedTitles.contains(title)) {
                throw new IllegalArgumentException("Form title not found: " + title);
            }
        }

        // Load existing forms from DB
        List<ProjectForm> existingForms =
                projectFormRepository.findByProject(project);

        Set<String> existingTitles = existingForms.stream()
                .map(ProjectForm::getFormTitle)
                .collect(Collectors.toSet());

        Set<String> newTitles = new HashSet<>(formTitles);

        // Determine what to ADD
        Set<String> toAdd = new HashSet<>(newTitles);
        toAdd.removeAll(existingTitles);

        // Determine what to REMOVE
        Set<String> toRemove = new HashSet<>(existingTitles);
        toRemove.removeAll(newTitles);

        // Add missing forms
        for (String title : toAdd) {
            ProjectForm projectForm = new ProjectForm();
            projectForm.setProject(project);
            projectForm.setFormTitle(title);
            projectForm.setCreatedAt(Instant.now());
            projectFormRepository.save(projectForm);
        }

        // Remove obsolete forms
        existingForms.stream()
                .filter(pf -> toRemove.contains(pf.getFormTitle()))
                .forEach(projectFormRepository::delete);
    }

    public List<ProjectForm> fetchProjectForms(Project project) {
        return projectFormRepository.findByProject(project);
    }

    public List<ProjectFormField> fetchProjectFormFields(Project project) {
        return projectFormFieldRepository.findByProject(project);
    }


}
