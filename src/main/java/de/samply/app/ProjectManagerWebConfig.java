package de.samply.app;

import de.samply.resolvers.RequestVariableMethodArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class ProjectManagerWebConfig implements WebMvcConfigurer {

    private final RequestVariableMethodArgumentResolver resolver;

    public ProjectManagerWebConfig(RequestVariableMethodArgumentResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolver);
    }

}
