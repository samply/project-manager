package de.samply.app;

import de.samply.cache.CacheConfiguration;
import de.samply.cache.CacheResource;
import de.samply.display.DisplayFormatKey;
import de.samply.display.DisplayFormatService;
import de.samply.display.DisplayFormats;
import de.samply.display.DisplayFormatsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectManagerControllerDisplayFormatsTest {

    @Mock
    private DisplayFormatService displayFormatService;

    @Mock
    private CacheConfiguration cacheConfiguration;

    @InjectMocks
    private ProjectManagerController controller;

    @Test
    void exposesDefaultLanguageValidatedFormatsAndCachePolicy() {
        DisplayFormats formats = formats();
        when(displayFormatService.getDefaultLanguage()).thenReturn("en");
        when(displayFormatService.getDefaultDateDisplayFormat()).thenReturn(DisplayFormatKey.DATE_FORMAT);
        when(displayFormatService.getDefaultTimestampDisplayFormat()).thenReturn(DisplayFormatKey.DATE_TIME_FORMAT);
        when(displayFormatService.getDisplayFormats()).thenReturn(formats);
        when(cacheConfiguration.cacheControl(CacheResource.DISPLAY_FORMATS))
                .thenReturn(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic());

        ResponseEntity<DisplayFormatsResponse> response = controller.fetchDisplayFormats();

        assertThat(response.getHeaders().getCacheControl()).isEqualTo("max-age=60, public");
        assertThat(response.getBody()).isEqualTo(new DisplayFormatsResponse(
                "en", DisplayFormatKey.DATE_FORMAT, DisplayFormatKey.DATE_TIME_FORMAT, formats.getFormats()));
    }

    private DisplayFormats formats() {
        Map<DisplayFormatKey, Map<String, String>> formats = new EnumMap<>(DisplayFormatKey.class);
        formats.put(DisplayFormatKey.DATE_FORMAT, Map.of("en", "yyyy-MM-dd"));
        formats.put(DisplayFormatKey.LONG_DATE_FORMAT, Map.of("en", "MMMM d, yyyy"));
        formats.put(DisplayFormatKey.DATE_TIME_FORMAT, Map.of("en", "yyyy-MM-dd HH:mm"));
        formats.put(DisplayFormatKey.DATE_TIME_WITH_SECONDS_FORMAT,
                Map.of("en", "yyyy-MM-dd HH:mm:ss"));
        return DisplayFormats.of(formats);
    }
}
