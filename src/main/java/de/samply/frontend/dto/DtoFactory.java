package de.samply.frontend.dto;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.model.*;
import de.samply.form.*;
import de.samply.form.template.FormTemplateConfig;
import de.samply.form.template.FormTemplateMetadata;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.UserProjectState;
import de.samply.user.UserService;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.LanguageUtils;
import de.samply.utils.UserUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class DtoFactory {

    private final BridgeheadConfiguration bridgeheadConfiguration;
    private final FormConfig formConfig;
    private final FormTemplateConfig formTemplateConfig;
    private final String defaultLanguage;

    // Services
    private final FormService formService;
    private final UserService userService;
    private final ProjectBridgeheadUserService projectBridgeheadUserService;


    public DtoFactory(BridgeheadConfiguration bridgeheadConfiguration,
                      FormService formService, UserService userService,
                      FormConfig formConfig,
                      FormTemplateConfig formTemplateConfig,
                      @Value(ProjectManagerConst.DEFAULT_LANGUAGE_SV) String defaultLanguage,
                      ProjectBridgeheadUserService projectBridgeheadUserService) {
        this.bridgeheadConfiguration = bridgeheadConfiguration;
        this.userService = userService;
        this.projectBridgeheadUserService = projectBridgeheadUserService;
        this.formService = formService;
        this.formConfig = formConfig;
        this.formTemplateConfig = formTemplateConfig;
        this.defaultLanguage = LanguageUtils.normalize(defaultLanguage);
    }

    public Project convert(@NotNull de.samply.db.model.Project project) {
        Project result = new Project();
        result.setCode(project.getCode());
        result.setCreatorEmail(project.getCreatorEmail());
        result.setCreatorName(fetchEmailUserName(project.getCreatorEmail()));
        result.setCreatedAt(project.getCreatedAt());
        result.setExpiresAt(project.getExpiresAt());
        result.setArchivedAt(project.getArchivedAt());
        result.setModifiedAt(project.getModifiedAt());
        result.setState(project.getState());
        result.setIsCustomConfigSelected(project.getIsCustomConfigSelected());
        result.setQuery(project.getQuery().getQuery());
        result.setHumanReadable(project.getQuery().getHumanReadable());
        result.setQueryFormat(project.getQuery().getQueryFormat());
        result.setLabel(project.getQuery().getLabel());
        result.setQueryDetails(project.getQuery().getDetails());
        result.setDescription(project.getQuery().getDescription());
        result.setExplorerUrl(project.getQuery().getExplorerUrl());
        result.setQueryContext(project.getQuery().getContext());
        result.setCreatorState(project.getCreatorResultsState());
        result.setResultsUrl(project.getResultsUrl());
        result.setCohortDefinition(project.getQuery().getCohortDefinition());
        result.setOutputs(project.getQuery().getOutputs().stream().map(queryOutput ->
                new ProjectOutput(queryOutput.getProjectType(), queryOutput.getOutputFormat(), queryOutput.getTemplateId())
        ).toArray(ProjectOutput[]::new));
        return result;
    }

    private String fetchEmailUserName(String email) {
        return UserUtils.extractFullName(userService.fetchUser(email));
    }


    public static de.samply.db.model.Project merge(@NotNull Project dtoProject, @NotNull de.samply.db.model.Project dbProject) {
        if (dtoProject.getExpiresAt() != null) {
            dbProject.setExpiresAt(dtoProject.getExpiresAt());
        }
        if (dtoProject.getQuery() != null) {
            dbProject.getQuery().setQuery(dtoProject.getQuery());
        }
        if (dtoProject.getHumanReadable() != null) {
            dbProject.getQuery().setHumanReadable(dtoProject.getHumanReadable());
        }
        if (dtoProject.getQueryFormat() != null) {
            dbProject.getQuery().setQueryFormat(dtoProject.getQueryFormat());
        }
        if (dtoProject.getQueryContext() != null) {
            dbProject.getQuery().setContext(dtoProject.getQueryContext());
        }
        if (dtoProject.getOutputs() != null) {
            merge(dtoProject.getOutputs(), dbProject);
        }
        return dbProject;
    }

    private static void merge(@NotNull ProjectOutput[] dtoOutputs, @NotNull de.samply.db.model.Project project) {
        Map<ProjectType, QueryOutput> outputMap = project.getQuery().getOutputs().stream().collect(Collectors.toMap(QueryOutput::getProjectType, Function.identity()));
        Arrays.stream(dtoOutputs).forEach(dtoOutput ->
                outputMap.compute(dtoOutput.projectType(), (projectType, existingQueryOutput) -> {
                    if (existingQueryOutput == null) {
                        QueryOutput created = new QueryOutput();
                        created.setProjectType(projectType);
                        created.setOutputFormat(dtoOutput.outputFormat());
                        created.setTemplateId(dtoOutput.templateId());
                        project.addOutput(created);
                        return created;
                    }
                    Optional.ofNullable(dtoOutput.outputFormat()).ifPresent(existingQueryOutput::setOutputFormat);
                    Optional.ofNullable(dtoOutput.templateId()).ifPresent(existingQueryOutput::setTemplateId);
                    return existingQueryOutput;
                })
        );

        var dtoTypes = Arrays.stream(dtoOutputs)
                .map(ProjectOutput::projectType)
                .collect(Collectors.toSet());

        project.getQuery().getOutputs()
                .removeIf(o -> !dtoTypes.contains(o.getProjectType()));
    }

    public Notification convert(@NotNull de.samply.db.model.Notification notification, Supplier<NotificationUserAction> userActionSupplier) {
        return new Notification(
                notification.getId(),
                notification.getEmail(),
                notification.getTimestamp(),
                notification.getProject().getCode(),
                notification.getBridgehead(),
                fetchHumanReadableBridgehead(notification.getBridgehead()),
                notification.getOperationType(),
                notification.getDetails(),
                notification.getError(),
                notification.getHttpStatus(),
                userActionSupplier.get().isRead()
        );
    }

    public ProjectDocument convert(@NotNull de.samply.db.model.ProjectDocument projectDocument) {
        return new ProjectDocument(
                projectDocument.getProject().getCode(),
                projectDocument.getOriginalFilename(),
                projectDocument.getUrl(),
                projectDocument.getCreatedAt(),
                projectDocument.getBridgehead(),
                fetchHumanReadableBridgehead(projectDocument.getBridgehead()),
                projectDocument.getCreatorEmail(),
                fetchEmailUserName(projectDocument.getCreatorEmail()),
                projectDocument.getLabel(),
                projectDocument.getDocumentType()
        );
    }

    public ProjectBridgehead convert(@NotNull de.samply.db.model.ProjectBridgehead projectBridgehead) {
        return new ProjectBridgehead(
                projectBridgehead.getProject().getCode(),
                projectBridgehead.getBridgehead(),
                fetchHumanReadableBridgehead(projectBridgehead),
                projectBridgehead.getState(),
                projectBridgehead.getModifiedAt(),
                projectBridgehead.getExecutions().stream()
                        .map(execution -> new ProjectBridgeheadExecution(
                                execution.getQueryOutput().getProjectType(),
                                execution.getQueryState()))
                        .collect(Collectors.toSet()).toArray(ProjectBridgeheadExecution[]::new),
                projectBridgehead.getCreatorResultsState()
        );
    }

    public String fetchHumanReadableBridgehead(@NotNull de.samply.db.model.ProjectBridgehead projectBridgehead) {
        return fetchHumanReadableBridgehead(projectBridgehead.getBridgehead());
    }

    public String fetchHumanReadableBridgehead(@NotNull String bridgehead) {
        Optional<String> humanReadable = bridgeheadConfiguration.getHumanReadable(bridgehead);
        return humanReadable.orElse(bridgehead);
    }

    public User convert(@NotNull de.samply.db.model.ProjectBridgeheadUser projectBridgeheadUser) {
        Optional<de.samply.db.model.User> user = userService.fetchUser(projectBridgeheadUser.getEmail());
        return new User(
                projectBridgeheadUser.getEmail(),
                user.map(de.samply.db.model.User::getFirstName).orElse(null),
                user.map(de.samply.db.model.User::getLastName).orElse(null),
                projectBridgeheadUser.getProjectBridgehead().getBridgehead(),
                fetchHumanReadableBridgehead(projectBridgeheadUser.getProjectBridgehead()),
                projectBridgeheadUser.getProjectRole(),
                projectBridgeheadUser.getProjectState()
        );
    }

    public User convertFilteringProjectRoleAndState(@NotNull de.samply.db.model.ProjectBridgeheadUser projectBridgeheadUser) {
        Optional<de.samply.db.model.User> user = userService.fetchUser(projectBridgeheadUser.getEmail());
        return new User(
                projectBridgeheadUser.getEmail(),
                user.map(de.samply.db.model.User::getFirstName).orElse(null),
                user.map(de.samply.db.model.User::getLastName).orElse(null),
                projectBridgeheadUser.getProjectBridgehead().getBridgehead(),
                fetchHumanReadableBridgehead(projectBridgeheadUser.getProjectBridgehead()),
                null,
                null
        );
    }

    public Bridgehead convertToBridgehead(@NotNull String bridgehead) {
        Optional<String> humanReadable = bridgeheadConfiguration.getHumanReadable(bridgehead);
        return humanReadable.map(s -> new Bridgehead(bridgehead, s)).orElseGet(() -> new Bridgehead(bridgehead, null));
    }

    public static User convert(de.samply.db.model.User user) {
        return new User(user.getEmail(), user.getFirstName(), user.getLastName(), null, null, null, null);
    }

    public FormField convert(@NotNull String title, Optional<String> label, Optional<Integer> blockInstance, Optional<String> value, Optional<String> language) {
        return new FormField(
                title,
                Optional.ofNullable(formConfig.getFormTitleDisplaMetadataMap().get(title))
                        .map(DisplayMetadata::getDisplayName)
                        .map(m -> fetchValue(m, language))
                        .orElse(null),
                Optional.ofNullable(formConfig.getFormTitleDisplaMetadataMap().get(title))
                        .map(DisplayMetadata::getDescription)
                        .map(m -> fetchValue(m, language))
                        .orElse(null),
                label.orElse(null),
                label.map(_ -> formConfig.getFormTitleLabelFieldMap().get(title))
                        .map(tm -> tm.get(label.get()))
                        .map(DisplayMetadata::getDisplayName)
                        .map(m -> fetchValue(m, language))
                        .orElse(null),
                label.map(_ -> formConfig.getFormTitleLabelFieldMap().get(title))
                        .map(tm -> tm.get(label.get()))
                        .map(DisplayMetadata::getDescription)
                        .map(m -> fetchValue(m, language))
                        .orElse(null),
                label.map(_ -> formConfig.getFormTitleLabelFieldMap().get(title))
                        .map(tm -> tm.get(label.get()))
                        .map(FormFieldConfig::getGroups)
                        .map(groups -> convertGroups(groups, language))
                        .orElse(null),
                label.map(_ -> formConfig.getFormTitleLabelFieldMap().get(title))
                        .map(tm -> tm.get(label.get()))
                        .map(FormFieldConfig::getDataType)
                        .orElse(null),
                label.map(_ -> formConfig.getFormTitleLabelFieldMap().get(title))
                        .map(tm -> tm.get(label.get()))
                        .map(FormFieldConfig::getAllowedValues)
                        .map(values -> convert(values, language))
                        .orElse(null),
                label.map(_ -> formConfig.getFormTitleLabelFieldMap().get(title))
                        .map(tm -> tm.get(label.get()))
                        .map(FormFieldConfig::isMandatory)
                        .orElse(null),
                label.map(_ -> formConfig.getFormTitleLabelFieldMap().get(title))
                        .map(tm -> tm.get(label.get()))
                        .map(FormFieldConfig::getBlock)
                        .orElse(null),
                label.map(_ -> formConfig.getFormTitleLabelFieldMap().get(title))
                        .map(tm -> tm.get(label.get()))
                        .map(FormFieldConfig::getBlock)
                        .map(b -> formConfig.getBlockLabelformFieldBlockMap().get(b))
                        .map(DisplayMetadata::getDisplayName)
                        .map(m -> fetchValue(m, language))
                        .orElse(null),
                label.map(_ -> formConfig.getFormTitleLabelFieldMap().get(title))
                        .map(tm -> tm.get(label.get()))
                        .map(FormFieldConfig::getBlock)
                        .map(b -> formConfig.getBlockLabelformFieldBlockMap().get(b))
                        .map(DisplayMetadata::getDescription)
                        .map(m -> fetchValue(m, language))
                        .orElse(null),
                blockInstance.orElse(null),
                label.map(_ -> formConfig.getFormTitleLabelFieldMap().get(title))
                        .map(tm -> tm.get(label.get()))
                        .map(FormFieldConfig::getBlock)
                        .map(b -> formConfig.getBlockLabelformFieldBlockMap().get(b))
                        .map(FormFieldBlock::getMultiple)
                        .orElse(null),
                label.map(_ -> formConfig.getFormTitleLabelFieldMap().get(title))
                        .map(tm -> tm.get(label.get()))
                        .map(FormFieldConfig::getBlock)
                        .map(b -> formConfig.getBlockLabelformFieldBlockMap().get(b))
                        .map(FormFieldBlock::getMinInstances)
                        .orElse(null),
                label.map(l -> fetchFormFieldOrder(title, l)).orElse(null),
                value.orElse(null)
        );
    }

    private FormFieldValue[] convert(de.samply.form.FormFieldValue[] values, Optional<String> language) {
        return Optional.ofNullable(values)
                .map(Arrays::stream)
                .map(stream -> stream
                        .map(value -> convert(value, language))
                        .toArray(FormFieldValue[]::new))
                .orElse(null);
    }

    private FormFieldValue convert(de.samply.form.FormFieldValue value, Optional<String> language) {
        return Optional.ofNullable(value)
                .map(v -> new FormFieldValue(
                        v.getLabel(),
                        fetchValue(v.getDisplayName(), language),
                        fetchValue(v.getDescription(), language)
                ))
                .orElse(null);
    }

    private FormFieldGroup[] convertGroups(String[] groups, Optional<String> language) {
        if (groups == null) {
            return null;
        }
        return Arrays.stream(groups)
                .map(group -> Map.entry(
                        group,
                        formConfig.getGroupsDisplayMetadataMap().get(group)
                ))
                .filter(entry -> entry.getValue() != null)
                .map(entry -> {
                    var group = entry.getKey();
                    var metadata = entry.getValue();

                    return new FormFieldGroup(
                            group,
                            fetchValue(metadata.getDisplayName(), language),
                            fetchValue(metadata.getDescription(), language)
                    );
                })
                .toArray(FormFieldGroup[]::new);
    }

    private <T> T fetchValue(Map<String, T> languageMapper, Optional<String> languageCode) {
        return languageCode
                .map(languageMapper::get)  // returns the value or null
                .orElse(languageMapper.get(defaultLanguage));
    }

    public FormField convert(@NotNull String title, @NotNull FormFieldConfig formFieldConfig, Optional<Integer> blockInstance, Optional<String> value, Optional<String> language) {
        return new FormField(
                title,
                Optional.ofNullable(formConfig.getFormTitleDisplaMetadataMap().get(title))
                        .map(DisplayMetadata::getDisplayName)
                        .map(m -> fetchValue(m, language))
                        .orElse(null),
                Optional.ofNullable(formConfig.getFormTitleDisplaMetadataMap().get(title))
                        .map(DisplayMetadata::getDescription)
                        .map(m -> fetchValue(m, language))
                        .orElse(null),
                formFieldConfig.getLabel(),
                fetchValue(formFieldConfig.getDisplayName(), language),
                fetchValue(formFieldConfig.getDescription(), language),
                convertGroups(formFieldConfig.getGroups(), language),
                formFieldConfig.getDataType(),
                convert(formFieldConfig.getAllowedValues(), language),
                formFieldConfig.isMandatory(),
                formFieldConfig.getBlock(),
                Optional.ofNullable(formFieldConfig.getBlock())
                        .map(b -> formConfig.getBlockLabelformFieldBlockMap().get(b))
                        .map(DisplayMetadata::getDisplayName)
                        .map(m -> fetchValue(m, language))
                        .orElse(null),
                Optional.ofNullable(formFieldConfig.getBlock())
                        .map(b -> formConfig.getBlockLabelformFieldBlockMap().get(b))
                        .map(DisplayMetadata::getDescription)
                        .map(m -> fetchValue(m, language))
                        .orElse(null),
                blockInstance.orElse(null),
                Optional.ofNullable(formFieldConfig.getBlock())
                        .map(b -> formConfig.getBlockLabelformFieldBlockMap().get(b))
                        .map(FormFieldBlock::getMultiple)
                        .orElse(null),
                Optional.ofNullable(formFieldConfig.getBlock())
                        .map(b -> formConfig.getBlockLabelformFieldBlockMap().get(b))
                        .map(FormFieldBlock::getMinInstances)
                        .orElse(null),
                fetchFormFieldOrder(title, formFieldConfig.getLabel()),
                value.orElse(null)
        );
    }

    private int fetchFormFieldOrder(String title, String label) {
        return formTemplateConfig.isProjectFormFieldTitle(title) ?
                formTemplateConfig.fetchProjectFormFieldOrder(title, label) :
                formConfig.getFormTitleLabelOrderMap().get(title).get(label);
    }

    public FormField convert(@NotNull ProjectFormField projectFormField, Optional<String> language) {
        return convert(
                projectFormField.getFormTitle(),
                Optional.of(projectFormField.getLabel()),
                Optional.ofNullable(projectFormField.getBlockInstance()),
                Optional.ofNullable(projectFormField.getValue()),
                language);
    }

    public Form convert(@NotNull ProjectForm projectForm, Optional<String> language) {
        return convertForm(projectForm.getFormTitle(), language);
    }

    public Form convertForm(@NotNull String formTitle, Optional<String> language) {
        return new Form(
                formTitle,
                Optional.ofNullable(formConfig.getFormTitleDisplaMetadataMap().get(formTitle))
                        .map(DisplayMetadata::getDisplayName)
                        .map(m -> fetchValue(m, language))
                        .orElse(null),
                Optional.ofNullable(formConfig.getFormTitleDisplaMetadataMap().get(formTitle))
                        .map(DisplayMetadata::getDescription)
                        .map(m -> fetchValue(m, language))
                        .orElse(null)
        );
    }

    public Optional<Results> fetchResults(@NotNull de.samply.db.model.Project project) {
        Set<ProjectBridgeheadUser> finalUsers = projectBridgeheadUserService.fetchUsers(ProjectRole.FINAL, project);
        Optional<ProjectBridgeheadUser> finalUser = finalUsers.stream().filter(user -> user.getProjectState() == UserProjectState.ACCEPTED).findAny();
        AtomicReference<Optional<String>> email = new AtomicReference<>(Optional.empty());
        AtomicReference<Optional<String>> firstName = new AtomicReference<>(Optional.empty());
        AtomicReference<Optional<String>> lastName = new AtomicReference<>(Optional.empty());
        if (finalUser.isEmpty()) {
            if (finalUsers.isEmpty()) {
                return Optional.empty();
            }
            finalUser = finalUsers.stream().findAny();
        }
        finalUser.ifPresent(user -> email.set(Optional.of(user.getEmail())));
        email.get().flatMap(userService::fetchUser).ifPresent(tempUser -> {
            firstName.set(Optional.of(tempUser.getFirstName()));
            lastName.set(Optional.of(tempUser.getLastName()));
        });
        return Optional.of(new Results(null, null, fetchValue(email), fetchValue(firstName), fetchValue(lastName),
                fetchProjectResultsUrl(project, finalUser),
                project.getCreatorResultsState(),
                null,
                fetchValue(new AtomicReference<>(finalUser), ProjectBridgeheadUser::getProjectState)));
    }

    private String fetchProjectResultsUrl(@NotNull de.samply.db.model.Project project, Optional<ProjectBridgeheadUser> finalUser) {
        return (finalUser.isPresent() && finalUser.get().getProjectState() == UserProjectState.ACCEPTED) ? project.getResultsUrl() : ProjectManagerConst.NOT_AUTHORIZED;
    }

    public Results fetchResults(@NotNull de.samply.db.model.ProjectBridgehead projectBridgehead) {
        Optional<BridgeheadAdminUser> bridgeheadAdmin = userService.fetchBridgeheadAdmin(projectBridgehead).stream().findAny();
        AtomicReference<Optional<de.samply.db.model.User>> user = new AtomicReference<>(Optional.empty());
        bridgeheadAdmin.ifPresent(tempUser -> user.set(userService.fetchUser(tempUser.getEmail())));
        AtomicReference<Optional<String>> humanReadableBridgehead = new AtomicReference<>(bridgeheadConfiguration.getHumanReadable(projectBridgehead.getBridgehead()));
        return new Results(projectBridgehead.getBridgehead(),
                fetchValue(humanReadableBridgehead),
                fetchValue(user, de.samply.db.model.User::getEmail),
                fetchValue(user, de.samply.db.model.User::getFirstName),
                fetchValue(user, de.samply.db.model.User::getLastName),
                fetchProjectBridgeheadResults(projectBridgehead),
                projectBridgehead.getCreatorResultsState(),
                projectBridgehead.getState(),
                null
        );
    }

    public ProjectAndForms convertToProjectAndForms(@NotNull de.samply.db.model.Project project, Optional<String> language) {
        return new ProjectAndForms(
                convert(project),
                formService
                        .fetchProjectForms(project)
                        .stream()
                        .map(f -> convert(f, language))
                        .toArray(Form[]::new),
                formService
                        .fetchProjectFormFields(project)
                        .stream()
                        .map(f -> convert(f, language))
                        .toArray(FormField[]::new)
        );
    }

    public FormTemplate convert(@NotNull FormTemplateMetadata formTemplateMetadata, Optional<String> language) {
        return new FormTemplate(
                formTemplateMetadata.getTemplate(),
                Optional.ofNullable(formTemplateMetadata.getLanguageDisplayNameMap().get(language.orElse(defaultLanguage)))
                        .orElse(formTemplateMetadata.getLanguageDisplayNameMap().get(defaultLanguage)));
    }

    private String fetchProjectBridgeheadResults(@NotNull de.samply.db.model.ProjectBridgehead projectBridgehead) {
        return (projectBridgehead.getState() == ProjectBridgeheadState.ACCEPTED) ? projectBridgehead.getResultsUrl() : ProjectManagerConst.NOT_AUTHORIZED;
    }

    private <O> O fetchValue(AtomicReference<Optional<O>> value) {
        return value.get().isPresent() ? value.get().get() : null;
    }

    private <I, O> O fetchValue(AtomicReference<Optional<I>> value, Function<I, O> function) {
        return value.get().isPresent() ? function.apply(value.get().get()) : null;
    }

}
