package de.samply.app;

import de.samply.cache.CacheConfiguration;
import de.samply.cache.CacheResource;
import de.samply.display.DisplayFormatKey;
import de.samply.display.DisplayFormatService;
import de.samply.display.DisplayFormatsResponse;
import de.samply.display.ResolvedDisplayFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
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
    void exposesDefaultFormatKeysResolvedPatternsAndCachePolicy() {
        when(displayFormatService.resolveLocale("de")).thenReturn("de-DE");
        when(displayFormatService.getDefaultDateDisplayFormat()).thenReturn(DisplayFormatKey.DATE_FORMAT);
        when(displayFormatService.getDefaultTimestampDisplayFormat()).thenReturn(DisplayFormatKey.DATE_TIME_FORMAT);
        when(displayFormatService.resolve(DisplayFormatKey.DATE_FORMAT, "de"))
                .thenReturn(new ResolvedDisplayFormat("de", "dd.MM.yyyy"));
        when(displayFormatService.resolve(DisplayFormatKey.LONG_DATE_FORMAT, "de"))
                .thenReturn(new ResolvedDisplayFormat("de", "d. MMMM yyyy"));
        when(displayFormatService.resolve(DisplayFormatKey.DATE_TIME_FORMAT, "de"))
                .thenReturn(new ResolvedDisplayFormat("de", "dd.MM.yyyy HH:mm"));
        when(displayFormatService.resolve(DisplayFormatKey.DATE_TIME_WITH_SECONDS_FORMAT, "de"))
                .thenReturn(new ResolvedDisplayFormat("de", "dd.MM.yyyy HH:mm:ss"));
        when(cacheConfiguration.cacheControl(CacheResource.DISPLAY_FORMATS))
                .thenReturn(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic());

        ResponseEntity<DisplayFormatsResponse> response = controller.fetchDisplayFormats("de");

        assertThat(response.getHeaders().getCacheControl()).isEqualTo("max-age=60, public");
        assertThat(response.getBody()).isEqualTo(new DisplayFormatsResponse(
                DisplayFormatKey.DATE_FORMAT,
                DisplayFormatKey.DATE_TIME_FORMAT,
                Map.of(
                        DisplayFormatKey.DATE_FORMAT, new ResolvedDisplayFormat("de", "dd.MM.yyyy"),
                        DisplayFormatKey.LONG_DATE_FORMAT, new ResolvedDisplayFormat("de", "d. MMMM yyyy"),
                        DisplayFormatKey.DATE_TIME_FORMAT, new ResolvedDisplayFormat("de", "dd.MM.yyyy HH:mm"),
                        DisplayFormatKey.DATE_TIME_WITH_SECONDS_FORMAT,
                        new ResolvedDisplayFormat("de", "dd.MM.yyyy HH:mm:ss")
                ), "de-DE"));
        verify(displayFormatService).resolve(DisplayFormatKey.DATE_FORMAT, "de");
    }

    @Test
    void resolvesWithTheBackendDefaultLanguageWhenNoneIsRequested() {
        when(displayFormatService.resolveLocale(null)).thenReturn("en");
        when(displayFormatService.getDefaultDateDisplayFormat()).thenReturn(DisplayFormatKey.DATE_FORMAT);
        when(displayFormatService.getDefaultTimestampDisplayFormat()).thenReturn(DisplayFormatKey.DATE_TIME_FORMAT);
        when(displayFormatService.resolve(DisplayFormatKey.DATE_FORMAT, null))
                .thenReturn(new ResolvedDisplayFormat("en", "yyyy-MM-dd"));
        when(displayFormatService.resolve(DisplayFormatKey.LONG_DATE_FORMAT, null))
                .thenReturn(new ResolvedDisplayFormat("en", "MMMM d, yyyy"));
        when(displayFormatService.resolve(DisplayFormatKey.DATE_TIME_FORMAT, null))
                .thenReturn(new ResolvedDisplayFormat("en", "yyyy-MM-dd HH:mm"));
        when(displayFormatService.resolve(DisplayFormatKey.DATE_TIME_WITH_SECONDS_FORMAT, null))
                .thenReturn(new ResolvedDisplayFormat("en", "yyyy-MM-dd HH:mm:ss"));
        when(cacheConfiguration.cacheControl(CacheResource.DISPLAY_FORMATS))
                .thenReturn(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic());

        // No @Language query parameter - the endpoint must not require one.
        ResponseEntity<DisplayFormatsResponse> response = controller.fetchDisplayFormats(null);

        assertThat(response.getBody().formats().get(DisplayFormatKey.DATE_FORMAT).language()).isEqualTo("en");
        assertThat(response.getBody().locale()).isEqualTo("en");
        verify(displayFormatService).resolve(DisplayFormatKey.DATE_FORMAT, null);
    }
}
