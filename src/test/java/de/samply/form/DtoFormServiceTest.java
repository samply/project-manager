package de.samply.form;

import de.samply.db.model.Project;
import de.samply.form.condition.FormFieldConditionEvaluator;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.FormField;
import de.samply.project.DtoProjectService;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DtoFormServiceTest {

    @Test
    void createsCompleteMinimumBlockInstancesWhenNoValuesArePersisted() {
        FormService formService = mock(FormService.class);
        DtoFactory dtoFactory = mock(DtoFactory.class);
        FormConfig formConfig = mock(FormConfig.class);
        DtoProjectService dtoProjectService = mock(DtoProjectService.class);
        FormFieldConditionEvaluator conditionEvaluator = mock(FormFieldConditionEvaluator.class);
        DtoFormService service = new DtoFormService(
                formService, dtoFactory, formConfig, dtoProjectService, conditionEvaluator);

        String title = "samples";
        Optional<String> language = Optional.empty();
        Project project = new Project();
        FormFieldConfig typeConfig = formFieldConfig("type", "liquid");
        FormFieldConfig volumeConfig = formFieldConfig("volume", "liquid");

        when(formConfig.getFormTitleLabelFieldMap()).thenReturn(
                Map.of(title, Map.of("type", typeConfig, "volume", volumeConfig)));
        when(formService.fetchProjectFormFields(title, project)).thenReturn(List.of());
        when(dtoFactory.convert(eq(title), eq(typeConfig), any(), any(), eq(language)))
                .thenReturn(formField(title, "type", 1));
        when(dtoFactory.convert(eq(title), eq(volumeConfig), any(), any(), eq(language)))
                .thenReturn(formField(title, "volume", 2));
        when(conditionEvaluator.filter(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Collection<FormField> result = service.fetchProjectFormFields(
                Optional.of(title), project, language);

        assertThat(result)
                .extracting(FormField::label, FormField::blockInstance)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("type", 1),
                        org.assertj.core.groups.Tuple.tuple("volume", 1));
    }

    private FormFieldConfig formFieldConfig(String label, @SuppressWarnings("SameParameterValue") String block) {
        FormFieldConfig config = new FormFieldConfig();
        config.setLabel(label);
        config.setBlock(block);
        return config;
    }

    private FormField formField(String title, String label, int order) {
        return FormField.builder()
                .title(title)
                .label(label)
                .block("liquid")
                .minBlockInstances(1)
                .order(order)
                .build();
    }
}
