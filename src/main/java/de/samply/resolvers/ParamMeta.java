package de.samply.resolvers;

import org.springframework.core.MethodParameter;

public record ParamMeta(MethodParameter parameter, String name, boolean required, boolean notEmpty,
                        String defaultValue, boolean bodyLookup) {
}