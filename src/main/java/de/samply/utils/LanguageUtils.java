package de.samply.utils;

import java.util.Locale;

public class LanguageUtils {

    public static String normalize(String languageCode) {
        return languageCode == null
                ? null
                : languageCode.strip().replace('_', '-').toLowerCase(Locale.ROOT);
    }

}
