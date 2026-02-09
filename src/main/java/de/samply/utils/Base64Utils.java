package de.samply.utils;

import java.util.Base64;
import java.util.Optional;

public class Base64Utils {

    public static Optional<String> decodeIfNecessary(String element) {
        try {
            return (element != null) ? Optional.of(new String(Base64.getDecoder().decode(element))) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.of(element);
        }
    }

    public static String encode(String element) {
        return Base64.getEncoder().encodeToString(element.getBytes());
    }


}
