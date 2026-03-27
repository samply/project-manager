package de.samply.resolvers;

public record ParamMeta(String name, boolean required, boolean notEmpty, String defaultValue, boolean bodyLookup) {
}
