package de.samply.utils;

import java.util.Map;
import java.util.stream.Collectors;

public class KeyTransformer {

    public static Map<String, String> transformMapKeys(Map<String, String> inputMap) {
        return inputMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> transformKey(entry.getKey()),  // transform the key
                        Map.Entry::getValue  // keep the value unchanged
                ));
    }

    private static String transformKey(String key) {
        StringBuilder transformedKey = new StringBuilder();
        boolean toUpperCase = false;

        for (char c : key.toCharArray()) {
            if (c == '-') {
                toUpperCase = true;  // next character should be uppercase
            } else {
                if (toUpperCase) {
                    transformedKey.append(Character.toUpperCase(c));
                    toUpperCase = false;
                } else {
                    transformedKey.append(c);
                }
            }
        }
        return transformedKey.toString();
    }

}
