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
    void numericFieldsKeepCanonicalApiValuesAndRenderLocalizedPdf(@TempDir Path directory) throws Exception {
        FormConfig config = formConfig(directory);
        DtoFactory factory = dtoFactory(config, displayFormats("yyyy-MM-dd", "dd.MM.yyyy"), directory);
        StringBuilder rows = new StringBuilder();
        String[][] cases = {
                {"count", "1000", "1,000", "1.000"},
                {"measurement", "1000.1", "1,000.1", "1.000,1"},
                {"measurement", "-1000.1234567", "-1,000.1234567", "-1.000,1234567"},
                {"measurement", "0", "0", "0"}
        };
        for (String[] sample : cases) {
            rows.append("<tr><td>").append(sample[1]).append("</td>");
            for (int index = 0; index < 2; index++) {
                String language = index == 0 ? "en" : "de-DE";
                FormField field = convert(factory, config, sample[0], sample[1], language);
                ProjectFormField persisted = new ProjectFormField();
                persisted.setFormTitle("request");
                persisted.setLabel(sample[0]);
                persisted.setFieldInstance(2);
                persisted.setValue(sample[1]);
                FormField repeated = factory.convert(persisted, Optional.of(language));
                assertThat(field.fetchDisplayValue()).isEqualTo(sample[index + 2]);
                assertThat(repeated.fetchDisplayValue()).isEqualTo(field.fetchDisplayValue());
                assertThat(repeated.fieldInstance()).isEqualTo(2);
                assertThat(field.value()).isEqualTo(sample[1]);
                assertThat(persisted.getValue()).isEqualTo(sample[1]);
                assertThat(new ObjectMapper().writeValueAsString(field))
                        .contains("\"value\":\"" + sample[1] + "\"").doesNotContain("displayValue");
                rows.append("<td>").append(field.fetchDisplayValue()).append("</td>");
            }
            rows.append("</tr>");
        }
        var converter = new de.samply.form.pdf.FormPdfConverter(new ExistingDirectory(directory));
        byte[] pdf = converter.convert("""
                <html><head><style>
                body { font-family: sans-serif; font-size: 12pt; }
                table { border-collapse: collapse; width: 100%%; }
                th, td { border: 1px solid #cccccc; padding: 10px; text-align: right; }
                th { background: #eeeeee; }
                </style></head><body><h1>Number display formats</h1>
                <p>Canonical values and localized form values</p>
                <table><tr><th>Canonical value</th><th>English</th><th>German</th></tr>
                %s</table></body></html>
                """.formatted(rows));
        try (var document = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            String text = new org.apache.pdfbox.text.PDFTextStripper().getText(document);
            assertThat(text).contains("1,000.1", "1.000,1", "-1,000.1234567", "-1.000,1234567");
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String previewDirectory = System.getProperty("numberFormatPreviewDirectory");
            if (previewDirectory != null) {
                Path preview = Files.createDirectories(Path.of(previewDirectory));
                Files.write(preview.resolve("number-formats.pdf"), pdf);
                javax.imageio.ImageIO.write(new org.apache.pdfbox.rendering.PDFRenderer(document)
                        .renderImageWithDPI(0, 120), "png", preview.resolve("number-formats.png").toFile());
            }
        }
    }

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
                    {"label": "count", "data_type": "INTEGER"},
                    {"label": "measurement", "data_type": "DOUBLE"},
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
