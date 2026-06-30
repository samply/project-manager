package de.samply.resolvers;

import de.samply.annotations.Language;
import de.samply.app.ProjectManagerConst;
import de.samply.utils.LanguageUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

@Component
public class LanguageArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Language.class);
    }

    @Override
    public Object resolveArgument(
            @NonNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        return Optional.ofNullable(webRequest.getNativeRequest(HttpServletRequest.class))
                .map(r -> r.getParameter(ProjectManagerConst.LANGUAGE))
                .map(LanguageUtils::normalize)
                .orElse(null);
    }

}
