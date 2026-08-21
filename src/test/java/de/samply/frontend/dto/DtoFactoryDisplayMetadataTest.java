package de.samply.frontend.dto;

import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.form.FormConfig;
import de.samply.form.FormFieldConfig;
import de.samply.form.FormService;
import de.samply.form.template.FormTemplateConfig;
import de.samply.project.ProjectBridgeheadUserService;
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
    void mapsLocalizedShortDescriptionsToDependentDtos(@TempDir Path temporaryDirectory)
            throws Exception {
        Path configDirectory = Files.createDirectory(temporaryDirectory.resolve("form-fields"));
        Path templateMetadataDirectory = Files.createDirectory(temporaryDirectory.resolve("template-metadata"));
        Files.writeString(configDirectory.resolve("patient.json"), """
                {
                  "title": "patient",
                  "display_name": {"en": "Patient"},
                  "description": {"en": "Patient description"},
                  "short_description": {"en": "Patient short"},
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
                    "short_description": {"en": "Details short"}
                  }],
                  "fields": [{
                    "label": "status",
                    "data_type": "ENUM",
                    "display_name": {"en": "Status"},
                    "description": {"en": "Status description"},
                    "short_description": {"en": "Status short"},
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

        Form form = factory.convertForm("patient", Optional.of("en"));
        FormField field = factory.convert(
                "patient", fieldConfig, Optional.empty(), Optional.empty(), Optional.of("active"), Optional.of("en"));

        assertThat(form.titleShortDescription()).isEqualTo("Patient short");
        assertThat(field.titleShortDescription()).isEqualTo("Patient short");
        assertThat(field.labelShortDescription()).isEqualTo("Status short");
        assertThat(field.blockShortDescription()).isEqualTo("Details short");
        assertThat(field.groups()).singleElement()
                .extracting(FormFieldGroup::shortDescription)
                .isEqualTo("Identity short");
        assertThat(field.allowedValues()).singleElement()
                .extracting(FormFieldValue::shortDescription)
                .isEqualTo("Active short");
    }
}
