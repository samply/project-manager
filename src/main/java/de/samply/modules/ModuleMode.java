package de.samply.modules;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * How a deployment runs an optional module: the value of its ENABLE_* variable.
 */
public enum ModuleMode {

    /** Enabled: the module's real beans ({@link de.samply.annotations.ConditionalOnModule}). */
    TRUE,
    /** Disabled: the disabled implementations ({@link de.samply.annotations.ConditionalOnModuleDisabled}). */
    FALSE,
    /**
     * Test implementation instead of the real one ({@link de.samply.annotations.ConditionalOnModuleTest}), e.g. for development
     * without the external systems; only for modules that have one.
     */
    TEST;

    public static ModuleMode parse(String variable, String value) {
        return Arrays.stream(values())
                .filter(mode -> mode.toString().equals(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(variable + "=" + value + " is not valid; allowed: "
                        + Arrays.stream(values()).map(ModuleMode::toString).collect(Collectors.joining(", "))));
    }

    /** The value of the variable: "true", "false" or "test". */
    @Override
    public String toString() {
        return name().toLowerCase();
    }

}
