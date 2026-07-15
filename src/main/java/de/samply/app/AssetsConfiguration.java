package de.samply.app;

import de.samply.utils.directory.ExistingDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Duration;

@Configuration
public class AssetsConfiguration implements WebMvcConfigurer {

    private final int cacheDurationInHours;
    private final Path assetsDirectory;

    public AssetsConfiguration(
            @Value(ProjectManagerConst.ASSETS_DIRECTORY_SV) ExistingDirectory assetsDirectory,
            @Value(ProjectManagerConst.ASSETS_CACHE_DURATION_IN_HOURS_SV) int cacheDurationInHours) {
        this.cacheDurationInHours = cacheDurationInHours;
        this.assetsDirectory = assetsDirectory.path();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(ProjectManagerConst.ASSETS)
                .addResourceLocations(assetsDirectory.toUri().toString())
                .setCacheControl(CacheControl.maxAge(Duration.ofHours(cacheDurationInHours)));
    }

}
