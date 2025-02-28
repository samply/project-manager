package de.samply.utils;

import java.util.Map;
import java.util.stream.Collectors;

public class KeyTransformer {

    public static Map<String, String> transformMapKeys(Map<String, String> inputMap) {
        return inputMap.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null) // Filter out null keys or values
                .collect(Collectors.toMap(
                        entry -> transformKey(entry.getKey()), // Transform the key
                        Map.Entry::getValue // Keep the value unchanged
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
