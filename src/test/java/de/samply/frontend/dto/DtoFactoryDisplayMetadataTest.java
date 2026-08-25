package de.samply.frontend.dto;

import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.form.FormConfig;
import de.samply.form.FormFieldConfig;
import de.samply.form.FormService;
import de.samply.form.template.FormTemplateConfig;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.project.state.ProjectState;
import de.samply.user.UserService;
import de.samply.utils.directory.ExistingDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DtoFactoryDisplayMetadataTest {

    @Test
    void mapsLocalizedDisplayMetadataAndStateRestrictedInformation(@TempDir Path temporaryDirectory)
            throws Exception {
        Path configDirectory = Files.createDirectory(temporaryDirectory.resolve("form-fields"));
        Path templateMetadataDirectory = Files.createDirectory(temporaryDirectory.resolve("template-metadata"));
        Files.writeString(configDirectory.resolve("patient.json"), """
                {
                  "title": "patient",
                  "display_name": {"en": "Patient"},
                  "description": {"en": "Patient description"},
                  "short_description": {"en": "Patient short"},
                  "pre_info": {
                    "content": {"en": "Patient draft introduction"},
                    "project_states": ["DRAFT", "REVIEW"]
                  },
                  "post_info": {
                    "content": {"EN": "Patient information in every state"}
                  },
                  "groups": {
                    "identity": {
                      "display_name": {"en": "Identity"},
                      "description": {"en": "Identity description"},
                      "short_description": {"en": "Identity short"}
                    }
                  },
                  "blocks": [{
                    "label": "details",
                    "display_name": {"en": "Details"},
                    "description": {"en": "Details description"},
                    "short_description": {"en": "Details short"},
                    "pre_info": {
                      "content": {"en": "Details draft introduction"},
                      "project_states": ["DRAFT"]
                    },
                    "post_info": {
                      "content": {"en": "Details information in every state"}
                    }
                  }],
                  "fields": [{
                    "label": "status",
                    "data_type": "ENUM",
                    "display_name": {"en": "Status"},
                    "description": {"en": "Status description"},
                    "short_description": {"en": "Status short"},
                    "pre_info": {
                      "content": {"en": "Status draft introduction"},
                      "project_states": ["DRAFT"]
                    },
                    "post_info": {
                      "content": {"en": "Status review ending"},
                      "project_states": ["REVIEW"]
                    },
                    "groups": ["identity"],
                    "block": "details",
                    "allowed_values": [{
                      "label": "active",
                      "display_name": {"en": "Active"},
                      "description": {"en": "Active description"},
                      "short_description": {"en": "Active short"}
                    }]
                  }]
                }
                """);

        FormConfig formConfig = new FormConfig(new ExistingDirectory(configDirectory));
        DtoFactory factory = new DtoFactory(
                mock(BridgeheadsConfiguration.class),
                mock(FormService.class),
                mock(UserService.class),
                formConfig,
                new FormTemplateConfig(new ExistingDirectory(templateMetadataDirectory), "en"),
                "en",
                mock(ProjectBridgeheadUserService.class));
        FormFieldConfig fieldConfig = formConfig.fetchFormFieldConfig("patient", "status");

        Form draftForm = factory.convertForm("patient", Optional.of("en"), ProjectState.DRAFT);
        Form finalForm = factory.convertForm("patient", Optional.of("en"), ProjectState.FINAL);
        FormField draftField = factory.convert(
                "patient", fieldConfig, Optional.empty(), Optional.empty(), Optional.of("active"),
                Optional.of("en"), ProjectState.DRAFT);
        FormField reviewField = factory.convert(
                "patient", fieldConfig, Optional.empty(), Optional.empty(), Optional.of("active"),
                Optional.of("en"), ProjectState.REVIEW);
        FormField finalField = factory.convert(
                "patient", fieldConfig, Optional.empty(), Optional.empty(), Optional.of("active"),
                Optional.of("en"), ProjectState.FINAL);

        assertThat(draftForm.titleShortDescription()).isEqualTo("Patient short");
        assertThat(draftForm.titlePreInfo()).isEqualTo("Patient draft introduction");
        assertThat(draftForm.titlePostInfo()).isEqualTo("Patient information in every state");
        assertThat(finalForm.titlePreInfo()).isNull();
        assertThat(finalForm.titlePostInfo()).isEqualTo("Patient information in every state");

        assertThat(draftField.titleShortDescription()).isEqualTo("Patient short");
        assertThat(draftField.titlePreInfo()).isEqualTo("Patient draft introduction");
        assertThat(draftField.labelShortDescription()).isEqualTo("Status short");
        assertThat(draftField.labelPreInfo()).isEqualTo("Status draft introduction");
        assertThat(draftField.labelPostInfo()).isNull();
        assertThat(reviewField.labelPreInfo()).isNull();
        assertThat(reviewField.labelPostInfo()).isEqualTo("Status review ending");
        assertThat(finalField.titlePreInfo()).isNull();
        assertThat(finalField.labelPreInfo()).isNull();
        assertThat(finalField.labelPostInfo()).isNull();
        assertThat(draftField.blockShortDescription()).isEqualTo("Details short");
        assertThat(draftField.blockPreInfo()).isEqualTo("Details draft introduction");
        assertThat(finalField.blockPreInfo()).isNull();
        assertThat(finalField.blockPostInfo()).isEqualTo("Details information in every state");
        assertThat(draftField.groups()).singleElement()
                .extracting(FormFieldGroup::shortDescription)
                .isEqualTo("Identity short");
        assertThat(draftField.allowedValues()).singleElement()
                .extracting(FormFieldValue::shortDescription)
                .isEqualTo("Active short");

        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(draftField);
        assertThat(json)
                .contains("\"titlePreInfo\":\"Patient draft introduction\"")
                .contains("\"titlePostInfo\":\"Patient information in every state\"")
                .contains("\"labelPreInfo\":\"Status draft introduction\"")
                .contains("\"blockPreInfo\":\"Details draft introduction\"");
    }
}
