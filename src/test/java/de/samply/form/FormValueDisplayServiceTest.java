package de.samply.form;

import de.samply.display.DisplayFormatKey;
import de.samply.display.DisplayFormatService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormValueDisplayServiceTest {

    private final DisplayFormatService displayFormatService = mock(DisplayFormatService.class);
    private final FormValueDisplayService service = new FormValueDisplayService(displayFormatService);

    FormValueDisplayServiceTest() {
        when(displayFormatService.getDefaultDateDisplayFormat()).thenReturn(DisplayFormatKey.DATE_FORMAT);
        when(displayFormatService.getDefaultTimestampDisplayFormat()).thenReturn(DisplayFormatKey.DATE_TIME_FORMAT);
    }

    @Test
    void formatsCanonicalDateForPresentation() {
        when(displayFormatService.format(
                DisplayFormatKey.DATE_FORMAT, LocalDate.of(2026, 9, 11), "de"))
                .thenReturn("11.09.2026");

        assertThat(service.format(DataType.DATE, "2026-09-11", "de"))
                .isEqualTo("11.09.2026");
        verify(displayFormatService).format(
                DisplayFormatKey.DATE_FORMAT, LocalDate.of(2026, 9, 11), "de");
    }

    @Test
    void preservesUnknownLegacyDateShape() {
        assertThat(service.format(DataType.DATE, "11.09.2026", "de"))
                .isEqualTo("11.09.2026");
        assertThat(service.format(DataType.DATE, "2023-02-29", "de"))
                .isEqualTo("2023-02-29");
        assertThat(service.format(DataType.DATE, null, "de")).isNull();
        assertThat(service.format(DataType.STRING, "2026-09-11", "de"))
                .isEqualTo("2026-09-11");
    }

    @Test
    void formatsUtcInstantInUtcForPresentation() {
        when(displayFormatService.format(
                DisplayFormatKey.DATE_TIME_FORMAT,
                Instant.parse("2026-09-11T08:30:00Z"), "de", ZoneOffset.UTC))
                .thenReturn("11.09.2026 08:30");

        assertThat(service.format(DataType.TIMESTAMP, "2026-09-11T08:30:00Z", "de"))
                .isEqualTo("11.09.2026 08:30");
    }

    @Test
    void formatsLocalDateTimeWithoutApplyingAZone() {
        LocalDateTime local = LocalDateTime.of(2026, 9, 11, 10, 30);
        when(displayFormatService.format(DisplayFormatKey.DATE_TIME_FORMAT, local, "de"))
                .thenReturn("11.09.2026 10:30");

        assertThat(service.format(DataType.LOCAL_DATE_TIME, "2026-09-11T10:30", "de"))
                .isEqualTo("11.09.2026 10:30");
    }
}
