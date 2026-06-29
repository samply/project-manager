package de.samply.form;

import de.samply.db.model.Project;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.FormField;
import de.samply.project.DtoProjectService;
import de.samply.utils.FormFieldUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DtoFormService {

    // Services
    private final FormService formService;
    private final DtoProjectService dtoProjectService;

    private final DtoFactory dtoFactory;
    private final FormConfig formConfig;


    public DtoFormService(
            FormService formService,
            DtoFactory dtoFactory,
            FormConfig formConfig,
            DtoProjectService dtoProjectService) {
        this.formService = formService;
        this.dtoFactory = dtoFactory;
        this.formConfig = formConfig;
        this.dtoProjectService = dtoProjectService;
    }

    public List<FormField> fetchProjectFormTitles(Optional<String> language) {
        return formConfig
                .getFormTitleLabelFieldMap()
                .keySet()
                .stream()
                .map(title -> dtoFactory.convert(
                        title,
                        Optional.empty(),
                        Optional.empty(),
                        language
                ))
                .collect(Collectors.toList());
    }

    public List<FormField> fetchProjectFormFieldsDefinedInConfigWithoutValues(@NotNull String formTitle, Optional<String> language) {
        return formConfig
                .getFormTitleLabelFieldMap()
                .getOrDefault(formTitle, Map.of())
                .values()
                .stream()
                .filter(field -> !field.isArchived())
                .map(field ->
                        dtoFactory.convert(formTitle, field, Optional.empty(), language))
                .toList();
    }

    public List<FormField> fetchProjectFormFieldsWithValues(@NotNull String formTitle, @NotNull Project project, Optional<String> language) {
        return formService
                .fetchProjectFormFieldsWithValues(formTitle, project)
                .stream()
                .map(projectFormField -> dtoFactory.convert(projectFormField, language))
                .toList();
    }

    public List<Form> fetchSelectedForms(@NotNull Project project, Optional<String> language) {
        return Stream.concat(
                        // Fetch forms selected explicitly
                        formService.fetchSelectedForms(project)
                                .stream()
                                .map(projectForm -> dtoFactory.convert(projectForm, language)),
                        // Fetch forms that should be selected according to the current configuration.
                        // This is particularly important if the current configuration is CUSTOM
                        dtoProjectService.fetchCurrentProjectConfiguration(project)
                                .values()
                                .stream()
                                .flatMap(projectAndForms -> Arrays.stream(projectAndForms.forms()))
                )
                // Remove duplicates
                .collect(Collectors.toMap(
                        Form::title,
                        Function.identity(),
                        (existing, _) -> existing
                ))
                .values()
                .stream()
                .toList();
    }

    public Stream<FormField> fetchBaseAndOverrideFormFields(@NotNull String formTitle, @NotNull Project project, Optional<String> language
    ) {
        return Stream.concat(
                fetchProjectFormFieldsDefinedInConfigWithoutValues(formTitle, language).stream(),
                fetchProjectFormFieldsWithValues(formTitle, project, language).stream()
        );
    }

    public List<FormField> fetchProjectFormFields(Optional<String> formTitle, @NotNull Project project, Optional<String> language) {
        return formTitle
                .map(Stream::of)
                .orElseGet(() -> formConfig.getFormTitleLabelFieldMap().keySet().stream())
                .flatMap(title -> fetchBaseAndOverrideFormFields(title, project, language))
                .sorted(FormFieldUtils.FORM_FIELD_COMPARATOR)
                .collect(FormFieldUtils.formFieldMapCollector())
                .values()
                .stream()
                .filter(field -> !isArchivedInConfig(field.title(), field.label()))
                .toList();
    }

    private boolean isArchivedInConfig(String title, String label) {
        return Optional.ofNullable(formConfig.getFormTitleLabelFieldMap().get(title))
                .map(m -> m.get(label))
                .map(FormFieldConfig::isArchived)
                .orElse(false);
    }


}
