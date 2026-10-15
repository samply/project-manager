package de.samply.utils;

import de.samply.app.ProjectManagerConst;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateUtils {

    public static String fetchCurrentDate(String dateTimePattern) {
        return DateTimeFormatter
                .ofPattern(dateTimePattern)
                .format(Instant.now()
                        .atZone(ZoneId.of(ProjectManagerConst.FORM_FILENAME_TIMESTAMP_ZONE)));
    }

    public static String fetchCurrentDate(String dateTimePattern, String languageCode) {
        return fetchDate(Instant.now(), dateTimePattern, languageCode);
    }

    public static String fetchDate(Instant date, String dateTimePattern, String languageCode) {
        return DateTimeFormatter
                .ofPattern(dateTimePattern, Locale.forLanguageTag(languageCode))
                .format(date.atZone(ZoneId.of(ProjectManagerConst.FORM_FILENAME_TIMESTAMP_ZONE)));
    }


}
