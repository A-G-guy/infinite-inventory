package com.agguy.infiniteinventory.service.search;

import java.util.Locale;
import java.util.Objects;

final class SequentialFuzzyScore {
    private final Locale locale;

    SequentialFuzzyScore(Locale locale) {
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    int score(String candidateText, String queryText) {
        if (candidateText == null || queryText == null || candidateText.isEmpty() || queryText.isEmpty()) {
            return 0;
        }

        String candidate = candidateText.toLowerCase(this.locale);
        String query = queryText.toLowerCase(this.locale);
        int score = 0;
        int candidateIndex = 0;
        int previousMatchIndex = -2;

        for (int queryIndex = 0; queryIndex < query.length(); queryIndex++) {
            int matchIndex = this.findNextMatch(candidate, candidateIndex, query.charAt(queryIndex));
            if (matchIndex < 0) {
                continue;
            }

            score++;
            if (matchIndex == previousMatchIndex + 1) {
                score += 2;
            }
            previousMatchIndex = matchIndex;
            candidateIndex = matchIndex + 1;
        }

        return score;
    }

    private int findNextMatch(String candidate, int startIndex, char expectedCharacter) {
        for (int index = startIndex; index < candidate.length(); index++) {
            if (candidate.charAt(index) == expectedCharacter) {
                return index;
            }
        }
        return -1;
    }
}
