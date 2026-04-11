package com.agguy.infiniteinventory.service.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SearchTextNormalizer {
    private SearchTextNormalizer() {
    }

    public static String normalizeQueryText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length());
        boolean previousWhitespace = true;
        for (int index = 0; index < text.length(); index++) {
            char character = Character.toLowerCase(text.charAt(index));
            if (Character.isWhitespace(character)) {
                if (!previousWhitespace) {
                    builder.append(' ');
                }
                previousWhitespace = true;
                continue;
            }
            builder.append(character);
            previousWhitespace = false;
        }
        int length = builder.length();
        if (length > 0 && builder.charAt(length - 1) == ' ') {
            builder.setLength(length - 1);
        }
        return builder.toString();
    }

    public static List<String> splitTerms(String text) {
        String normalizedText = normalizeQueryText(text);
        if (normalizedText.isEmpty()) {
            return List.of();
        }
        return List.of(normalizedText.split(" "));
    }

    public static String normalizeNaturalText(String text) {
        return normalizeText(text, true);
    }

    public static String compactNaturalText(String text) {
        return compact(normalizeNaturalText(text));
    }

    public static List<String> tokenizeNaturalText(String text) {
        return tokenize(normalizeNaturalText(text));
    }

    public static String normalizeIdentifierText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT);
    }

    public static String compactIdentifierText(String text) {
        return compact(normalizeText(text, false));
    }

    public static List<String> tokenizeIdentifierText(String text) {
        return tokenize(normalizeText(text, false));
    }

    public static boolean isSubsequence(String text, String term) {
        if (term.isEmpty() || text.isEmpty() || term.length() > text.length()) {
            return false;
        }
        int termIndex = 0;
        for (int index = 0; index < text.length() && termIndex < term.length(); index++) {
            if (text.charAt(index) == term.charAt(termIndex)) {
                termIndex++;
            }
        }
        return termIndex == term.length();
    }

    private static String normalizeText(String text, boolean keepChineseCharacters) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length());
        boolean previousWhitespace = true;
        for (int index = 0; index < text.length(); index++) {
            char character = Character.toLowerCase(text.charAt(index));
            if (Character.isLetterOrDigit(character) || keepChineseCharacters && isChineseCharacter(character)) {
                builder.append(character);
                previousWhitespace = false;
                continue;
            }
            if (!previousWhitespace) {
                builder.append(' ');
                previousWhitespace = true;
            }
        }
        int length = builder.length();
        if (length > 0 && builder.charAt(length - 1) == ' ') {
            builder.setLength(length - 1);
        }
        return builder.toString();
    }

    private static List<String> tokenize(String normalizedText) {
        if (normalizedText.isEmpty()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String token : normalizedText.split(" ")) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return List.copyOf(tokens);
    }

    private static String compact(String normalizedText) {
        if (normalizedText.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(normalizedText.length());
        for (int index = 0; index < normalizedText.length(); index++) {
            char character = normalizedText.charAt(index);
            if (character != ' ') {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private static boolean isChineseCharacter(char character) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(character);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }
}
