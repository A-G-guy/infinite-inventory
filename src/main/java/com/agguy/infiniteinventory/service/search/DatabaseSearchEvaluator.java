package com.agguy.infiniteinventory.service.search;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import java.util.List;
import java.util.Locale;
import org.apache.commons.text.similarity.FuzzyScore;

public final class DatabaseSearchEvaluator {
    private static final int EXACT_BASE_SCORE = 40_000;
    private static final int PREFIX_BASE_SCORE = 30_000;
    private static final int CONTAINS_BASE_SCORE = 20_000;
    private static final int FUZZY_BASE_SCORE = 10_000;
    private static final FuzzyScore FUZZY_SCORE = new FuzzyScore(Locale.ROOT);

    public DatabaseSearchRanking evaluate(DatabaseQuery query, DatabaseSearchIndex index, long amount) {
        List<String> terms = SearchTextNormalizer.splitTerms(query.searchText());
        if (terms.isEmpty()) {
            return DatabaseSearchRanking.unfiltered();
        }

        DatabaseSearchConfig config = query.searchConfig();
        int exactMatches = 0;
        int prefixMatches = 0;
        int containsMatches = 0;
        int fuzzyMatches = 0;
        double textScore = 0.0D;

        for (String term : terms) {
            TokenMatch bestMatch = this.bestMatch(term, index, config);
            if (!bestMatch.matched()) {
                return DatabaseSearchRanking.noMatch();
            }

            textScore += bestMatch.score();
            switch (bestMatch.level()) {
                case EXACT -> exactMatches++;
                case PREFIX -> prefixMatches++;
                case CONTAINS -> containsMatches++;
                case FUZZY -> fuzzyMatches++;
                case NONE -> {
                }
            }
        }

        double countBoostScore = this.countBoostScore(config.weightFor(DatabaseSearchField.COUNT_BOOST), amount);
        return new DatabaseSearchRanking(true, true, exactMatches, prefixMatches, containsMatches, fuzzyMatches, textScore, countBoostScore);
    }

    private TokenMatch bestMatch(String term, DatabaseSearchIndex index, DatabaseSearchConfig config) {
        TokenMatch bestMatch = TokenMatch.noMatch();
        for (DatabaseSearchField field : DatabaseSearchField.values()) {
            if (!field.isTextField()) {
                continue;
            }
            DatabaseSearchWeight weight = config.weightFor(field);
            if (weight == DatabaseSearchWeight.OFF) {
                continue;
            }
            TokenMatch candidateMatch = this.matchField(field, term, index, weight);
            if (candidateMatch.isBetterThan(bestMatch)) {
                bestMatch = candidateMatch;
            }
        }
        return bestMatch;
    }

    private TokenMatch matchField(DatabaseSearchField field, String term, DatabaseSearchIndex index, DatabaseSearchWeight weight) {
        return switch (field) {
            case DISPLAY_NAME -> this.matchNaturalField(term, weight, index.displayNameTokens(), index.displayNameNormalized(), index.displayNameCompact());
            case ITEM_ID -> this.matchIdentifierField(term, weight, index.registryNameNormalized(), index.registryNameCompact(), index.registryPathTokens(), index.registryPathNormalized(), index.registryPathCompact());
            case PINYIN -> this.matchPinyinField(term, weight, index.pinyinTokens(), index.pinyinFull(), index.pinyinInitials());
            case MOD_NAMESPACE -> this.matchCompactField(term, weight, index.modNamespace());
            case COUNT_BOOST -> TokenMatch.noMatch();
        };
    }

    private TokenMatch matchNaturalField(
            String term,
            DatabaseSearchWeight weight,
            List<String> tokens,
            String normalizedText,
            String compactText
    ) {
        String compactTerm = SearchTextNormalizer.compactNaturalText(term);
        if (compactTerm.isEmpty()) {
            return TokenMatch.noMatch();
        }
        return this.matchNormalizedCandidates(compactTerm, weight, tokens, normalizedText, compactText);
    }

    private TokenMatch matchIdentifierField(
            String term,
            DatabaseSearchWeight weight,
            String registryName,
            String registryNameCompact,
            List<String> pathTokens,
            String registryPath,
            String registryPathCompact
    ) {
        String normalizedIdentifier = SearchTextNormalizer.normalizeIdentifierText(term);
        if (normalizedIdentifier.isEmpty()) {
            return TokenMatch.noMatch();
        }

        TokenMatch bestMatch = TokenMatch.noMatch();
        bestMatch = bestMatch.betterOf(this.matchExact(normalizedIdentifier, weight, registryName, EXACT_BASE_SCORE));
        bestMatch = bestMatch.betterOf(this.matchPrefix(normalizedIdentifier, weight, registryName, PREFIX_BASE_SCORE));
        bestMatch = bestMatch.betterOf(this.matchContains(normalizedIdentifier, weight, registryName, CONTAINS_BASE_SCORE));

        String compactIdentifier = SearchTextNormalizer.compactIdentifierText(term);
        if (compactIdentifier.isEmpty()) {
            return bestMatch;
        }

        bestMatch = bestMatch.betterOf(this.matchNormalizedCandidates(compactIdentifier, weight, pathTokens, registryPath, registryPathCompact));
        bestMatch = bestMatch.betterOf(this.matchCompactField(compactIdentifier, weight, registryNameCompact));
        return bestMatch;
    }

    private TokenMatch matchPinyinField(String term, DatabaseSearchWeight weight, List<String> tokens, String fullPinyin, String initials) {
        String compactTerm = SearchTextNormalizer.compactIdentifierText(term);
        if (compactTerm.isEmpty()) {
            return TokenMatch.noMatch();
        }
        TokenMatch bestMatch = this.matchNormalizedCandidates(compactTerm, weight, tokens, fullPinyin, fullPinyin);
        return bestMatch.betterOf(this.matchCompactField(compactTerm, weight, initials));
    }

    private TokenMatch matchCompactField(String term, DatabaseSearchWeight weight, String compactText) {
        if (compactText.isEmpty()) {
            return TokenMatch.noMatch();
        }
        TokenMatch exactMatch = this.matchExact(term, weight, compactText, EXACT_BASE_SCORE);
        if (exactMatch.matched()) {
            return exactMatch;
        }
        TokenMatch prefixMatch = this.matchPrefix(term, weight, compactText, PREFIX_BASE_SCORE);
        if (prefixMatch.matched()) {
            return prefixMatch;
        }
        TokenMatch containsMatch = this.matchContains(term, weight, compactText, CONTAINS_BASE_SCORE);
        if (containsMatch.matched()) {
            return containsMatch;
        }
        return this.matchFuzzy(term, weight, compactText);
    }

    private TokenMatch matchNormalizedCandidates(
            String term,
            DatabaseSearchWeight weight,
            List<String> tokens,
            String normalizedText,
            String compactText
    ) {
        TokenMatch exactMatch = TokenMatch.noMatch();
        for (String token : tokens) {
            exactMatch = exactMatch.betterOf(this.matchExact(term, weight, token, EXACT_BASE_SCORE));
        }
        if (exactMatch.matched()) {
            return exactMatch;
        }

        TokenMatch prefixMatch = TokenMatch.noMatch();
        for (String token : tokens) {
            prefixMatch = prefixMatch.betterOf(this.matchPrefix(term, weight, token, PREFIX_BASE_SCORE));
        }
        if (prefixMatch.matched()) {
            return prefixMatch;
        }

        TokenMatch containsMatch = this.matchContains(term, weight, compactText.isEmpty() ? normalizedText : compactText, CONTAINS_BASE_SCORE);
        if (containsMatch.matched()) {
            return containsMatch;
        }

        return this.matchFuzzy(term, weight, compactText.isEmpty() ? normalizedText : compactText);
    }

    private TokenMatch matchExact(String term, DatabaseSearchWeight weight, String candidate, int baseScore) {
        if (!term.equals(candidate)) {
            return TokenMatch.noMatch();
        }
        return new TokenMatch(MatchLevel.EXACT, score(baseScore, weight, 0, candidate.length()));
    }

    private TokenMatch matchPrefix(String term, DatabaseSearchWeight weight, String candidate, int baseScore) {
        if (!candidate.startsWith(term) || term.equals(candidate)) {
            return TokenMatch.noMatch();
        }
        return new TokenMatch(MatchLevel.PREFIX, score(baseScore, weight, 0, candidate.length() - term.length()));
    }

    private TokenMatch matchContains(String term, DatabaseSearchWeight weight, String candidate, int baseScore) {
        int position = candidate.indexOf(term);
        if (position < 0 || position == 0 && term.length() == candidate.length()) {
            return TokenMatch.noMatch();
        }
        return new TokenMatch(MatchLevel.CONTAINS, score(baseScore, weight, position, candidate.length() - term.length()));
    }

    private TokenMatch matchFuzzy(String term, DatabaseSearchWeight weight, String candidate) {
        if (term.length() < 2 || candidate.isEmpty() || !SearchTextNormalizer.isSubsequence(candidate, term)) {
            return TokenMatch.noMatch();
        }
        int fuzzyScore = FUZZY_SCORE.fuzzyScore(candidate, term);
        if (fuzzyScore <= 0) {
            return TokenMatch.noMatch();
        }
        return new TokenMatch(MatchLevel.FUZZY, score(FUZZY_BASE_SCORE + fuzzyScore, weight, 0, candidate.length() - term.length()));
    }

    private double countBoostScore(DatabaseSearchWeight weight, long amount) {
        if (weight == DatabaseSearchWeight.OFF || amount <= 0L) {
            return 0.0D;
        }
        return Math.log10(amount + 1.0D) * weight.multiplier() * 100.0D;
    }

    private static double score(int baseScore, DatabaseSearchWeight weight, int positionPenalty, int lengthPenalty) {
        return baseScore
                + weight.multiplier() * 200.0D
                - positionPenalty * 5.0D
                - Math.max(0, lengthPenalty);
    }

    private enum MatchLevel {
        NONE(0),
        FUZZY(1),
        CONTAINS(2),
        PREFIX(3),
        EXACT(4);

        private final int rank;

        MatchLevel(int rank) {
            this.rank = rank;
        }
    }

    private record TokenMatch(MatchLevel level, double score) {
        private static final TokenMatch NO_MATCH = new TokenMatch(MatchLevel.NONE, Double.NEGATIVE_INFINITY);

        static TokenMatch noMatch() {
            return NO_MATCH;
        }

        boolean matched() {
            return this.level != MatchLevel.NONE;
        }

        boolean isBetterThan(TokenMatch other) {
            return this.betterOf(other) == this;
        }

        TokenMatch betterOf(TokenMatch other) {
            if (other == null || other.level.rank < this.level.rank) {
                return this;
            }
            if (other.level.rank > this.level.rank) {
                return other;
            }
            return other.score > this.score ? other : this;
        }
    }
}
