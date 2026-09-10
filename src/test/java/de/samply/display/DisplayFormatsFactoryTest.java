package de.samply.display;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisplayFormatsFactoryTest {

    private final DisplayFormatsFactory factory = new DisplayFormatsFactory();

    @Test
    void loadsFileAndNormalizesLanguageTags(@TempDir Path directory) throws Exception {
        Path path = directory.resolve("display-formats.json");
        Files.writeString(path, """
                {
                  "formats": {
                    "DATE_FORMAT": {"EN_gb": "dd/MM/yyyy"},
                    "LONG_DATE_FORMAT": {"EN_gb": "d MMMM yyyy"},
                    "DATE_TIME_FORMAT": {"EN_gb": "dd/MM/yyyy HH:mm"},
                    "DATE_TIME_WITH_SECONDS_FORMAT": {"EN_gb": "dd/MM/yyyy HH:mm:ss"}
                  }
                }
                """);

        DisplayFormats result = factory.displayFormats(path);

        assertThat(result.getFormats().get(DisplayFormatKey.DATE_FORMAT))
                .containsEntry("en-gb", "dd/MM/yyyy");
    }

    @Test
    void failsWhenPathIsAbsent() {
        assertThatThrownBy(() -> factory.displayFormats(Path.of("")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DISPLAY_FORMATS_CONFIG_PATH");
    }

    @Test
    void failsWhenConfiguredFileCannotBeRead(@TempDir Path directory) {
        Path missing = directory.resolve("missing.json");

        assertThatThrownBy(() -> factory.displayFormats(missing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(missing.toString());
    }
}
