package de.samply.utils;

import java.util.Locale;
import java.util.regex.Pattern;

public class EmailUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    public static boolean isValid(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    // Addresses of the role lists are stored in one form: the login, the configuration and the configuration page may
    // write them with different case
    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

}
