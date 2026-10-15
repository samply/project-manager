package de.samply.form.core;

import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectFormField;
import de.samply.form.core.condition.FormFieldConditionEvaluator;
import de.samply.form.template.config.FormTemplateConfig;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.FormField;
import de.samply.project.DtoProjectService;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.project.state.ProjectState;
import de.samply.user.UserService;
import de.samply.utils.directory.ExistingDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * pre_info/post_info with a condition, with the real form configuration, conversion and condition evaluation: shown
 * only when the condition holds for the project's saved values.
 */
class DtoFormServiceConditionalInfoTest {

    private static final String ETHICS = "ethics";
    private static final String STATUS = "status";
    private static final String SITE_NAME = "site_name";
    private static final String FORM_INFO = "While the ethics vote is pending";
    private static final String FIELD_INFO = "No data or material before the approval";
    private static final String BLOCK_INFO = "Heidelberg needs its own vote";
    private static final String ALWAYS_INFO = "Shown without condition";
    private static final Optional<String> LANGUAGE = Optional.of("en");

    @TempDir
    Path temporaryDirectory;

    private final FormService formService = mock(FormService.class);
    private final Project project = new Project();
    private DtoFormService service;

    @BeforeEach
    void setUp() throws Exception {
        Path configDirectory = Files.createDirectory(temporaryDirectory.resolve("form-fields"));
        Path templateMetadataDirectory = Files.createDirectory(temporaryDirectory.resolve("template-metadata"));
        Files.writeString(configDirectory.resolve("ethics.json"), """
                {
                  "title": "ethics",
                  "pre_info": {
                    "content": {"en": "While the ethics vote is pending"},
                    "condition": "['ethics']['status']['value'] == 'pending'"
                  },
                  "post_info": {"content": {"en": "Shown without condition"}},
                  "blocks": [{
                    "label": "site",
                    "multiple": true,
                    "post_info": {
                      "content": {"en": "Heidelberg needs its own vote"},
                      "condition": "['ethics']['site_name']['value'] == 'Heidelberg'"
                    }
                  }],
                  "fields": [{
                    "label": "status",
                    "data_type": "STRING",
                    "post_info": {
                      "content": {"en": "No data or material before the approval"},
                      "condition": "['ethics']['status']['value'] == 'pending'"
                    }
                  }, {
                    "label": "site_name",
                    "data_type": "STRING",
                    "block": "site"
                  }]
                }
                """);
        FormConfig formConfig = new FormConfig(new ExistingDirectory(configDirectory));
        DtoFactory dtoFactory = new DtoFactory(
                mock(BridgeheadsConfiguration.class),
                formService,
                mock(UserService.class),
                formConfig,
                new FormTemplateConfig(new ExistingDirectory(templateMetadataDirectory), "en"),
                "en",
                mock(ProjectBridgeheadUserService.class),
                mock(FormValueDisplayService.class));
        service = new DtoFormService(formService, dtoFactory, formConfig, mock(DtoProjectService.class),
                new FormFieldConditionEvaluator(formConfig));
        project.setState(ProjectState.DRAFT);
    }

    @Test
    void showsTheInformationWhenItsConditionHolds() {
        saveValues(value(STATUS, null, "pending"));

        FormField status = fetchField(STATUS, null);

        assertThat(status.labelPostInfo()).isEqualTo(FIELD_INFO);
        assertThat(status.titlePreInfo()).isEqualTo(FORM_INFO);
        assertThat(status.titlePostInfo()).isEqualTo(ALWAYS_INFO);
        assertThat(service.fetchProjectFormTitleCanonicalOrder(List.of(ETHICS), project, LANGUAGE))
                .extracting(Form::titlePreInfo, Form::titlePostInfo)
                .containsExactly(tuple(FORM_INFO, ALWAYS_INFO));
        assertThat(service.fetchProjectFormTitles(project, LANGUAGE))
                .extracting(FormField::titlePreInfo)
                .containsExactly(FORM_INFO);
    }

    @Test
    void hidesTheInformationWhenItsConditionDoesNotHold() {
        saveValues(value(STATUS, null, "approved"));

        FormField status = fetchField(STATUS, null);

        assertThat(status.labelPostInfo()).isNull();
        assertThat(status.titlePreInfo()).isNull();
        assertThat(status.titlePostInfo()).isEqualTo(ALWAYS_INFO);
        assertThat(service.fetchProjectFormTitleCanonicalOrder(List.of(ETHICS), project, LANGUAGE))
                .extracting(Form::titlePreInfo)
                .containsExactly((String) null);
    }

    @Test
    void hidesConditionalInformationWithoutSavedValues() {
        saveValues();

        FormField status = fetchField(STATUS, null);

        assertThat(status.labelPostInfo()).isNull();
        assertThat(status.titlePreInfo()).isNull();
        assertThat(status.titlePostInfo()).isEqualTo(ALWAYS_INFO);
    }

    @Test
    void decidesTheBlockInformationPerBlockInstance() {
        saveValues(value(SITE_NAME, 1, "Heidelberg"), value(SITE_NAME, 2, "Berlin"));

        assertThat(fetchFields().stream().filter(field -> SITE_NAME.equals(field.label())))
                .extracting(FormField::blockInstance, FormField::blockPostInfo)
                .containsExactlyInAnyOrder(tuple(1, BLOCK_INFO), tuple(2, null));
    }

    private void saveValues(ProjectFormField... values) {
        Arrays.stream(values).forEach(value -> value.setProject(project));
        when(formService.fetchProjectFormFields(ETHICS, project)).thenReturn(List.of(values));
    }

    private static ProjectFormField value(String label, Integer blockInstance, String value) {
        ProjectFormField field = new ProjectFormField();
        field.setFormTitle(ETHICS);
        field.setLabel(label);
        field.setBlockInstance(blockInstance);
        field.setFieldInstance(1);
        field.setValue(value);
        return field;
    }

    private Collection<FormField> fetchFields() {
        return service.fetchProjectFormFields(Optional.of(ETHICS), project, LANGUAGE);
    }

    private FormField fetchField(String label, Integer blockInstance) {
        return fetchFields().stream()
                .filter(field -> label.equals(field.label()))
                .filter(field -> blockInstance == null || blockInstance.equals(field.blockInstance()))
                .findFirst().orElseThrow();
    }

}
