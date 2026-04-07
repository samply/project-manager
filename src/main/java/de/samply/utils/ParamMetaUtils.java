package de.samply.utils;

import de.samply.annotations.RequestParameter;
import de.samply.annotations.RequestVariable;
import de.samply.resolvers.ParamMeta;
import org.springframework.core.MethodParameter;

public class ParamMetaUtils {

    public static ParamMeta extractParamMeta(MethodParameter parameter) {
        RequestVariable rv = parameter.getParameterAnnotation(RequestVariable.class);
        if (rv != null) {
            return new ParamMeta(parameter, rv.name(), rv.required(), rv.notEmpty(), rv.defaultValue(), true);
        }

        RequestParameter rp = parameter.getParameterAnnotation(RequestParameter.class);
        if (rp != null) {
            return new ParamMeta(parameter, rp.name(), rp.required(), rp.notEmpty(), rp.defaultValue(), false);
        }

        return null; // should not happen if supportsParameter() is used
    }

}
