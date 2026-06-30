package de.samply.utils;

public class LanguageUtils {

    public static String normalize(String languageCode) {
        return languageCode == null ? null : languageCode.toLowerCase();
    }

}
