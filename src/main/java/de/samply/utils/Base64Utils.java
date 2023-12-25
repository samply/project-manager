package de.samply.utils;

import java.util.Base64;

public class Base64Utils {

    public static String decode(String element) {
        return new String(Base64.getDecoder().decode(element));
    }

    public static String decodeIfNecessary(String element) {
        try {
            return Base64Utils.decode(element);
        } catch (IllegalArgumentException e) {
            return element;
        }
    }

    public static String decode(byte[] element) {
        return new String(Base64.getDecoder().decode(element));
    }

    public static String encode(String element) {
        return Base64.getEncoder().encodeToString(element.getBytes());
    }


}
