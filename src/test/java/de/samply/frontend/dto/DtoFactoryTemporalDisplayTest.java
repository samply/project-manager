package de.samply.frontend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.db.model.ProjectFormField;
import de.samply.display.DisplayFormatKey;
import de.samply.display.DisplayFormatService;
import de.samply.display.DisplayFormats;
import de.samply.form.DataType;
import de.samply.form.FormConfig;
import de.samply.form.FormService;
import de.samply.form.FormValueDisplayService;
import de.samply.form.template.FormTemplateConfig;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.user.UserService;
import de.samply.utils.directory.ExistingDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DtoFactoryTemporalDisplayTest {

    @Test
    void localizesDateForPdfWhileKeepingTheApiValueCanonical(@TempDir Path temporaryDirectory)
            throws Exception {
        FormConfig formConfig = formConfig(temporaryDirectory);
        DtoFactory factory = dtoFactory(
                formConfig, displayFormats("yyyy-MM-dd", "dd.MM.yyyy"), temporaryDirectory);

        FormField english = convertDate(factory, formConfig, "en");
        FormField german = convertDate(factory, formConfig, "de-DE");

        assertThat(english.value()).isEqualTo("2026-09-11");
        assertThat(english.fetchDisplayValue()).isEqualTo("2026-09-11");
        assertThat(german.value()).isEqualTo("2026-09-11");
        assertThat(german.fetchDisplayValue()).isEqualTo("11.09.2026");

        String json = new ObjectMapper().writeValueAsString(german);
        assertThat(json)
                .contains("\"value\":\"2026-09-11\"")
                .doesNotContain("11.09.2026")
                .doesNotContain("displayValue");
    }

    @Test
    void changingDisplayPatternDoesNotChangeDateInterpretation(@TempDir Path temporaryDirectory)
            throws Exception {
        FormConfig formConfig = formConfig(temporaryDirectory);
        DtoFactory compactFactory = dtoFactory(
                formConfig, displayFormats("yyyy-MM-dd", "dd.MM.yyyy"), temporaryDirectory);
        DtoFactory alternateFactory = dtoFactory(
                formConfig, displayFormats("dd/MM/yyyy", "yyyy.MM.dd"), temporaryDirectory);

        FormField compact = convertDate(compactFactory, formConfig, "en");
        FormField alternate = convertDate(alternateFactory, formConfig, "en");

        assertThat(compact.value()).isEqualTo(alternate.value()).isEqualTo("2026-09-11");
        assertThat(compact.fetchDisplayValue()).isEqualTo("2026-09-11");
        assertThat(alternate.fetchDisplayValue()).isEqualTo("11/09/2026");
    }

    @Test
    void localizesDistinctDateTimeTypesWithoutChangingApiValues(@TempDir Path temporaryDirectory)
            throws Exception {
        FormConfig formConfig = formConfig(temporaryDirectory);
        DtoFactory factory = dtoFactory(
                formConfig, displayFormats("yyyy-MM-dd", "dd.MM.yyyy"), temporaryDirectory);

        FormField timestamp = convert(
                factory, formConfig, "event-instant", "2026-09-11T08:30:00Z", "de");
        FormField localDateTime = convert(
                factory, formConfig, "local-appointment", "2026-09-11T10:30", "de");
        FormField legacyTimestamp = convert(
                factory, formConfig, "event-instant", "2026-09-11T10:30", "de");

        assertThat(timestamp.value()).isEqualTo("2026-09-11T08:30:00Z");
        assertThat(timestamp.fetchDisplayValue()).isEqualTo("11.09.2026 08:30");
        assertThat(localDateTime.value()).isEqualTo("2026-09-11T10:30");
        assertThat(localDateTime.fetchDisplayValue()).isEqualTo("11.09.2026 10:30");
        assertThat(legacyTimestamp.fetchDisplayValue()).isEqualTo("2026-09-11T10:30");

        assertThat(new ObjectMapper().writeValueAsString(timestamp))
                .contains("\"value\":\"2026-09-11T08:30:00Z\"")
                .doesNotContain("11.09.2026 08:30")
                .doesNotContain("displayValue");
    }

    @Test
    void formatsExistingDatabaseRowsUsingConfiguredType(@TempDir Path temporaryDirectory)
            throws Exception {
        FormConfig formConfig = formConfig(temporaryDirectory);
        DtoFactory factory = dtoFactory(
                formConfig, displayFormats("yyyy-MM-dd", "dd.MM.yyyy"), temporaryDirectory);
        ProjectFormField persisted = new ProjectFormField();
        persisted.setFormTitle("request");
        persisted.setLabel("planned-start-date");
        persisted.setValue("2026-09-11");

        FormField converted = factory.convert(persisted, Optional.of("de"));

        assertThat(converted.type()).isEqualTo(DataType.DATE);
        assertThat(converted.value()).isEqualTo("2026-09-11");
        assertThat(converted.fetchDisplayValue()).isEqualTo("11.09.2026");
        assertThat(persisted.getValue()).isEqualTo("2026-09-11");
        assertThat(new ObjectMapper().writeValueAsString(converted))
                .contains("\"value\":\"2026-09-11\"")
                .doesNotContain("11.09.2026");
    }

    @Test
    void optionalFormatReachesApiAndPdfForBothDtoPaths(@TempDir Path directory) throws Exception {
        FormConfig config = formConfig(directory, ",\"display_format\":\"LONG_DATE_FORMAT\"");
        DtoFactory factory = dtoFactory(config, displayFormats("yyyy-MM-dd", "dd.MM.yyyy"), directory);
        ProjectFormField row = new ProjectFormField();
        row.setFormTitle("request");
        row.setLabel("planned-start-date");
        row.setValue("2026-09-11");

        for (FormField field : new FormField[]{convertDate(factory, config, "en"),
                factory.convert(row, Optional.of("en"))}) {
            assertThat(field.displayFormat()).isEqualTo(DisplayFormatKey.LONG_DATE_FORMAT);
            assertThat(field.fetchDisplayValue()).isEqualTo("September 11, 2026");
            assertThat(field.value()).isEqualTo("2026-09-11");
            assertThat(new ObjectMapper().writeValueAsString(field))
                    .contains("\"displayFormat\":\"LONG_DATE_FORMAT\"")
                    .contains("\"value\":\"2026-09-11\"")
                    .doesNotContain("September 11");
        }
        assertThat(factory.convert(row, Optional.of("de")).fetchDisplayValue())
                .isEqualTo("11. September 2026");
    }

    @Test
    void rejectsTimeFormatForDateOnlyField(@TempDir Path directory) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                formConfig(directory, ",\"display_format\":\"DATE_TIME_FORMAT\""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("planned-start-date")
                .hasMessageContaining("incompatible");
    }

    @Test
    void timestampOverrideIncludesSecondsAndOmittedFormatKeepsDefault(@TempDir Path directory)
            throws Exception {
        FormConfig config = formConfig(directory);
        DtoFactory factory = dtoFactory(config, displayFormats("yyyy-MM-dd", "dd.MM.yyyy"), directory);
        FormField defaultField = convert(factory, config, "event-instant", "2026-09-11T08:30:45Z", "de");
        assertThat(defaultField.displayFormat()).isNull();
        assertThat(defaultField.fetchDisplayValue()).isEqualTo("11.09.2026 08:30");
        config.fetchFormFieldConfig("request", "event-instant")
                .setDisplayFormat(DisplayFormatKey.DATE_TIME_WITH_SECONDS_FORMAT);
        FormField explicit = convert(factory, config, "event-instant", "2026-09-11T08:30:45Z", "de");
        assertThat(explicit.fetchDisplayValue()).isEqualTo("11.09.2026 08:30:45");
        assertThat(explicit.value()).isEqualTo(defaultField.value());
    }

    private FormConfig formConfig(Path temporaryDirectory) throws Exception {
        return formConfig(temporaryDirectory, "");
    }

    private FormConfig formConfig(Path temporaryDirectory, String override) throws Exception {
        Path configDirectory = Files.createDirectory(temporaryDirectory.resolve("form-fields"));
        Files.writeString(configDirectory.resolve("request.json"), """
                {
                  "title": "request",
                  "fields": [
                    {
                      "label": "planned-start-date",
                      "data_type": "DATE"%s
                    },
                    {
                      "label": "event-instant",
                      "data_type": "TIMESTAMP"
                    },
                    {
                      "label": "local-appointment",
                      "data_type": "LOCAL_DATE_TIME"
                    }
                  ]
                }
                """.formatted(override));
        return new FormConfig(new ExistingDirectory(configDirectory));
    }

    private DtoFactory dtoFactory(
            FormConfig formConfig,
            DisplayFormats displayFormats,
            Path temporaryDirectory
    ) throws Exception {
        Path templateDirectory = Files.createTempDirectory(
                temporaryDirectory, "form-template-metadata-");
        DisplayFormatService formatService = new DisplayFormatService(
                displayFormats, "en", DisplayFormatKey.DATE_FORMAT, DisplayFormatKey.DATE_TIME_FORMAT);
        return new DtoFactory(
                mock(BridgeheadsConfiguration.class),
                mock(FormService.class),
                mock(UserService.class),
                formConfig,
                new FormTemplateConfig(new ExistingDirectory(templateDirectory), "en"),
                "en",
                mock(ProjectBridgeheadUserService.class),
                new FormValueDisplayService(formatService));
    }

    private FormField convertDate(DtoFactory factory, FormConfig formConfig, String language) {
        return convert(factory, formConfig, "planned-start-date", "2026-09-11", language);
    }

    private FormField convert(
            DtoFactory factory,
            FormConfig formConfig,
            String label,
            String value,
            String language
    ) {
        return factory.convert(
                "request",
                formConfig.fetchFormFieldConfig("request", label),
                Optional.empty(),
                Optional.empty(),
                Optional.of(value),
                Optional.of(language));
    }

    private DisplayFormats displayFormats(String englishDate, String germanDate) {
        Map<DisplayFormatKey, Map<String, String>> formats = new EnumMap<>(DisplayFormatKey.class);
        formats.put(DisplayFormatKey.DATE_FORMAT, Map.of("en", englishDate, "de", germanDate));
        formats.put(DisplayFormatKey.LONG_DATE_FORMAT,
                Map.of("en", "MMMM d, yyyy", "de", "d. MMMM yyyy"));
        formats.put(DisplayFormatKey.DATE_TIME_FORMAT,
                Map.of("en", "yyyy-MM-dd HH:mm", "de", "dd.MM.yyyy HH:mm"));
        formats.put(DisplayFormatKey.DATE_TIME_WITH_SECONDS_FORMAT,
                Map.of("en", "yyyy-MM-dd HH:mm:ss", "de", "dd.MM.yyyy HH:mm:ss"));
        return DisplayFormats.of(formats);
    }
}
