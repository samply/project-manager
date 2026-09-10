package de.samply.display;

import java.util.Map;

public record DisplayFormatsResponse(
        String defaultLanguage,
        DisplayFormatKey defaultDateDisplayFormat,
        DisplayFormatKey defaultTimestampDisplayFormat,
        Map<DisplayFormatKey, Map<String, String>> formats
) {
    public static DisplayFormatsResponse from(DisplayFormatService service) {
        return new DisplayFormatsResponse(
                service.getDefaultLanguage(),
                service.getDefaultDateDisplayFormat(),
                service.getDefaultTimestampDisplayFormat(),
                service.getDisplayFormats().getFormats()
        );
    }
}
