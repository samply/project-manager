package de.samply.form;

import de.samply.db.model.Project;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.FormField;
import de.samply.utils.FormFieldUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DtoFormService {

    private final FormService formService;
    private final DtoFactory dtoFactory;
    private final FormConfig formConfig;

    public DtoFormService(
            FormService formService,
            DtoFactory dtoFactory,
            FormConfig formConfig) {
        this.formService = formService;
        this.dtoFactory = dtoFactory;
        this.formConfig = formConfig;
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
        return formService
                .fetchSelectedForms(project)
                .stream()
                .map(projectForm -> dtoFactory.convert(projectForm, language))
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
                .toList();
    }


}
