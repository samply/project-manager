package de.samply.display;

import java.util.EnumMap;
import java.util.Map;

public record DisplayFormatsResponse(
        DisplayFormatKey defaultDateDisplayFormat,
        DisplayFormatKey defaultTimestampDisplayFormat,
        Map<DisplayFormatKey, ResolvedDisplayFormat> formats
) {
    public static DisplayFormatsResponse from(DisplayFormatService service, String requestedLanguage) {
        Map<DisplayFormatKey, ResolvedDisplayFormat> resolved = new EnumMap<>(DisplayFormatKey.class);
        for (DisplayFormatKey key : DisplayFormatKey.values()) {
            resolved.put(key, service.resolve(key, requestedLanguage));
        }
        return new DisplayFormatsResponse(
                service.getDefaultDateDisplayFormat(),
                service.getDefaultTimestampDisplayFormat(),
                resolved
        );
    }
}
