package de.samply.app;

import de.samply.cache.CacheConfiguration;
import de.samply.cache.CacheCategory;
import de.samply.cache.CacheResource;
import de.samply.resolvers.LanguageArgumentResolver;
import de.samply.resolvers.RequestVariableAndParameterMethodArgumentResolver;
import de.samply.utils.directory.ExistingDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.List;

@Configuration
@CacheCategory(CacheResource.BACKEND_ASSETS)
public class ProjectManagerWebConfig implements WebMvcConfigurer {

    private final RequestVariableAndParameterMethodArgumentResolver requestVariableResolver;
    private final LanguageArgumentResolver languageArgumentResolver;
    private final CacheConfiguration cacheConfiguration;
    private final Path assetsDirectory;


    public ProjectManagerWebConfig(
            RequestVariableAndParameterMethodArgumentResolver resolver,
            LanguageArgumentResolver languageArgumentResolver,
            @Value(ProjectManagerConst.ASSETS_DIRECTORY_SV) ExistingDirectory assetsDirectory,
            CacheConfiguration cacheConfiguration) {
        this.requestVariableResolver = resolver;
        this.languageArgumentResolver = languageArgumentResolver;
        this.cacheConfiguration = cacheConfiguration;
        this.assetsDirectory = assetsDirectory.path();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(requestVariableResolver);
        resolvers.add(languageArgumentResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(ProjectManagerConst.ASSETS)
                .addResourceLocations(assetsDirectory.toUri().toString())
                .setCacheControl(cacheConfiguration.cacheControl(CacheResource.BACKEND_ASSETS));
    }

}
