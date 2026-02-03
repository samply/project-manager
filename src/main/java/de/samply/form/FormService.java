package de.samply.form;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectForm;
import de.samply.db.model.ProjectFormField;
import de.samply.db.repository.ProjectFormFieldRepository;
import de.samply.db.repository.ProjectFormRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.FormField;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.security.SessionUser;
import de.samply.utils.FormFieldUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final ProjectRepository projectRepository;
    private final ProjectFormFieldRepository projectFormFieldRepository;
    private final ProjectFormRepository projectFormRepository;
    private final NotificationService notificationService;
    private final SessionUser sessionUser;
    private final DtoFactory dtoFactory;


    public FormService(FormConfig formConfig,
                       ProjectRepository projectRepository,
                       ProjectFormFieldRepository projectFormFieldRepository,
                       ProjectFormRepository projectFormRepository,
                       NotificationService notificationService,
                       SessionUser sessionUser,
                       DtoFactory dtoFactory) {
        this.formConfig = formConfig;
        this.projectRepository = projectRepository;
        this.projectFormFieldRepository = projectFormFieldRepository;
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

    public List<FormField> fetchProjectFormFieldsDefinedInConfigWithoutValues(@NotNull String formTitle, Optional<String> language) {
        return formConfig.getFormTitleLabelFieldMap()
                .getOrDefault(formTitle, Map.of())
                .values()
                .stream()
                .map(field ->
                        dtoFactory.convert(formTitle, field, Optional.empty(), language))
                .toList();
    }

    public List<FormField> fetchProjectFormFieldsWithValues(@NotNull String formTitle, @NotNull String projectCode, Optional<String> language) {
        return projectFormFieldRepository.findByProject_CodeAndFormTitle(projectCode, formTitle).stream()
                .map(projectFormField -> dtoFactory.convert(projectFormField, language))
                .toList();
    }

    public List<FormField> fetchProjectFormFields(Optional<String> formTitle, @NotNull String projectCode, Optional<String> language) {
        return formTitle.map(Stream::of)
                .orElseGet(() -> formConfig.getFormTitleLabelFieldMap().keySet().stream())
                .flatMap(title -> fetchBaseAndOverrideFormFields(title, projectCode, language))
                .sorted(FormFieldUtils.FORM_FIELD_COMPARATOR)
                .collect(FormFieldUtils.formFieldMapCollector())
                .values()
                .stream()
                .toList();
    }

    public void editProjectFormFieldValues(Optional<FormField[]> formFields, @NotNull String projectCode) {
        if (formFields.isEmpty() || formFields.get().length == 0) {
            return;
        }
        Map<String, ProjectFormField> labelFormMap = projectFormFieldRepository.findByProject_Code(projectCode).stream()
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
                projectFormField.setProject(fetchProject(projectCode));
                isModified = true;
                notificationService.createNotification(projectCode, null,
                        sessionUser.getEmail(), OperationType.ADD_PROJECT_FORM_LABEL,
                        details, null, null);
            } else {
                if (!projectFormField.getValue().equals(formField.value())) {
                    projectFormField.setValue(formField.value());
                    notificationService.createNotification(projectCode, null,
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

    private Project fetchProject(@NotNull String projectCode) throws FormServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new FormServiceException("Project " + projectCode + " not found");
        }
        return project.get();
    }


    public Stream<FormField> fetchBaseAndOverrideFormFields(@NotNull String formTitle, @NotNull String projectCode, Optional<String> language
    ) {
        return Stream.concat(
                fetchProjectFormFieldsDefinedInConfigWithoutValues(formTitle, language).stream(),
                fetchProjectFormFieldsWithValues(formTitle, projectCode, language).stream()
        );
    }

    public List<Form> fetchSelectedForms(@NotNull String projectCode, Optional<String> language) {
        return projectFormRepository.findByProject_Code(projectCode).stream()
                .map(projectForm -> dtoFactory.convert(projectForm, language))
                .toList();
    }

    public void addSelectedForm(@NotNull String projectCode, @NotNull String formTitle) {
        projectFormRepository
                .findByProject_CodeAndFormTitle(projectCode, formTitle)
                .ifPresentOrElse(
                        _ -> { /* already exists → do nothing */ },
                        () -> {
                            if (!formConfig.getFormTitleLabelFieldMap().containsKey(formTitle)) {
                                throw new IllegalArgumentException("Form title not found: " + formTitle);
                            }

                            ProjectForm projectForm = new ProjectForm();
                            projectForm.setFormTitle(formTitle);
                            projectForm.setProject(fetchProject(projectCode));
                            projectForm.setCreatedAt(Instant.now());

                            projectFormRepository.save(projectForm);
                        }
                );
    }


}
