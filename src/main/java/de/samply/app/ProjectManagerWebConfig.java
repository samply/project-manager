package de.samply.app;

import de.samply.resolvers.LanguageArgumentResolver;
import de.samply.resolvers.RequestVariableAndParameterMethodArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class ProjectManagerWebConfig implements WebMvcConfigurer {

    private final RequestVariableAndParameterMethodArgumentResolver requestVariableResolver;
    private final LanguageArgumentResolver languageArgumentResolver;

    public ProjectManagerWebConfig(RequestVariableAndParameterMethodArgumentResolver resolver,
                                   LanguageArgumentResolver languageArgumentResolver) {
        this.requestVariableResolver = resolver;
        this.languageArgumentResolver = languageArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(requestVariableResolver);
        resolvers.add(languageArgumentResolver);
    }

}
