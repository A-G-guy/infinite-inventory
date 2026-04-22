package com.agguy.infiniteinventory.service.search;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.database.DatabaseTabQueryState;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

public final class DatabaseSearchEvaluator {
    private static final int EXACT_BASE_SCORE = 40_000;
    private static final int PREFIX_BASE_SCORE = 30_000;
    private static final int CONTAINS_BASE_SCORE = 20_000;
    private static final int FUZZY_BASE_SCORE = 10_000;
    private static final double PHRASE_EXACT_BONUS = 1_200.0D;
    private static final double PHRASE_PREFIX_BONUS = 800.0D;
    private static final double TOKEN_SEQUENCE_BONUS = 400.0D;
    private static final double MULTI_FIELD_BONUS = 180.0D;
    private static final SequentialFuzzyScore FUZZY_SCORE = new SequentialFuzzyScore(Locale.ROOT);

    public DatabaseSearchRanking evaluate(DatabaseQuery query, DatabaseSearchIndex index, long amount) {
        DatabaseQuery normalizedQuery = query == null ? DatabaseQuery.defaultQuery() : query;
        return this.evaluate(normalizedQuery.tabStateFor(normalizedQuery.focusedTab()), index, amount);
    }

    public DatabaseSearchRanking evaluate(DatabaseTabQueryState tabQueryState, DatabaseSearchIndex index, long amount) {
        DatabaseTabQueryState normalizedState = tabQueryState == null ? DatabaseTabQueryState.defaultState() : tabQueryState;
        List<String> terms = SearchTextNormalizer.splitTerms(normalizedState.searchText());
        if (terms.isEmpty()) {
            return DatabaseSearchRanking.unfiltered();
        }

        DatabaseSearchConfig config = normalizedState.searchConfig();
        EnumSet<DatabaseSearchField> matchedFields = EnumSet.noneOf(DatabaseSearchField.class);
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

            if (bestMatch.field() != null) {
                matchedFields.add(bestMatch.field());
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

        textScore += this.fieldCoverageBonus(matchedFields);
        textScore += this.phraseBonus(normalizedState, index, config, terms);
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
        TokenMatch matched = switch (field) {
            case DISPLAY_NAME -> this.matchNaturalField(
                    term,
                    weight,
                    index.displayNameTokens(),
                    index.displayNameSearchNormalizedTexts(),
                    index.displayNameSearchCompactTexts()
            );
            case ITEM_ID -> this.matchIdentifierField(term, weight, index.registryNameNormalized(), index.registryNameCompact(), index.registryPathTokens(), index.registryPathNormalized(), index.registryPathCompact());
            case PINYIN -> this.matchPinyinField(term, weight, index.pinyinTokens(), index.pinyinFull(), index.pinyinInitials());
            case MOD_NAMESPACE -> this.matchCompactField(term, weight, index.modNamespace());
            case NOTE -> this.matchNaturalField(
                    term,
                    weight,
                    index.noteTokens(),
                    index.noteSearchNormalizedTexts(),
                    index.noteSearchCompactTexts()
            );
            case COUNT_BOOST -> TokenMatch.noMatch();
        };
        return matched.withField(field);
    }

    private TokenMatch matchNaturalField(
            String term,
            DatabaseSearchWeight weight,
            List<String> tokens,
            List<String> normalizedTexts,
            List<String> compactTexts
    ) {
        String compactTerm = SearchTextNormalizer.compactNaturalText(term);
        if (compactTerm.isEmpty()) {
            return TokenMatch.noMatch();
        }
        return this.matchNormalizedCandidates(compactTerm, weight, tokens, normalizedTexts, compactTexts);
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

        bestMatch = bestMatch.betterOf(this.matchNormalizedCandidates(
                compactIdentifier,
                weight,
                pathTokens,
                List.of(registryPath),
                List.of(registryPathCompact)
        ));
        bestMatch = bestMatch.betterOf(this.matchCompactField(compactIdentifier, weight, registryNameCompact));
        return bestMatch;
    }

    private TokenMatch matchPinyinField(String term, DatabaseSearchWeight weight, List<String> tokens, String fullPinyin, String initials) {
        String compactTerm = SearchTextNormalizer.compactIdentifierText(term);
        if (compactTerm.isEmpty()) {
            return TokenMatch.noMatch();
        }
        TokenMatch bestMatch = this.matchNormalizedCandidates(compactTerm, weight, tokens, List.of(fullPinyin), List.of(fullPinyin));
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
            List<String> normalizedTexts,
            List<String> compactTexts
    ) {
        TokenMatch exactMatch = TokenMatch.noMatch();
        for (String normalizedText : normalizedTexts) {
            exactMatch = exactMatch.betterOf(this.matchExact(term, weight, normalizedText, EXACT_BASE_SCORE));
        }
        for (String compactText : compactTexts) {
            exactMatch = exactMatch.betterOf(this.matchExact(term, weight, compactText, EXACT_BASE_SCORE));
        }
        for (String token : tokens) {
            exactMatch = exactMatch.betterOf(this.matchExact(term, weight, token, EXACT_BASE_SCORE));
        }
        if (exactMatch.matched()) {
            return exactMatch;
        }

        TokenMatch prefixMatch = TokenMatch.noMatch();
        for (String normalizedText : normalizedTexts) {
            prefixMatch = prefixMatch.betterOf(this.matchPrefix(term, weight, normalizedText, PREFIX_BASE_SCORE));
        }
        for (String compactText : compactTexts) {
            prefixMatch = prefixMatch.betterOf(this.matchPrefix(term, weight, compactText, PREFIX_BASE_SCORE));
        }
        for (String token : tokens) {
            prefixMatch = prefixMatch.betterOf(this.matchPrefix(term, weight, token, PREFIX_BASE_SCORE));
        }
        if (prefixMatch.matched()) {
            return prefixMatch;
        }

        TokenMatch containsMatch = TokenMatch.noMatch();
        for (String compactText : compactTexts) {
            containsMatch = containsMatch.betterOf(this.matchContains(term, weight, compactText, CONTAINS_BASE_SCORE));
        }
        for (String normalizedText : normalizedTexts) {
            containsMatch = containsMatch.betterOf(this.matchContains(term, weight, normalizedText, CONTAINS_BASE_SCORE));
        }
        if (containsMatch.matched()) {
            return containsMatch;
        }

        TokenMatch fuzzyMatch = TokenMatch.noMatch();
        for (String compactText : compactTexts) {
            fuzzyMatch = fuzzyMatch.betterOf(this.matchFuzzy(term, weight, compactText));
        }
        for (String normalizedText : normalizedTexts) {
            fuzzyMatch = fuzzyMatch.betterOf(this.matchFuzzy(term, weight, normalizedText));
        }
        return fuzzyMatch;
    }

    private TokenMatch matchExact(String term, DatabaseSearchWeight weight, String candidate, int baseScore) {
        if (candidate == null || candidate.isEmpty()) {
            return TokenMatch.noMatch();
        }
        if (!term.equals(candidate)) {
            return TokenMatch.noMatch();
        }
        return new TokenMatch(null, MatchLevel.EXACT, score(baseScore, weight, 0, candidate.length()));
    }

    private TokenMatch matchPrefix(String term, DatabaseSearchWeight weight, String candidate, int baseScore) {
        if (candidate == null || candidate.isEmpty()) {
            return TokenMatch.noMatch();
        }
        if (!candidate.startsWith(term) || term.equals(candidate)) {
            return TokenMatch.noMatch();
        }
        return new TokenMatch(null, MatchLevel.PREFIX, score(baseScore, weight, 0, candidate.length() - term.length()));
    }

    private TokenMatch matchContains(String term, DatabaseSearchWeight weight, String candidate, int baseScore) {
        if (candidate == null || candidate.isEmpty()) {
            return TokenMatch.noMatch();
        }
        int position = candidate.indexOf(term);
        if (position < 0 || position == 0 && term.length() == candidate.length()) {
            return TokenMatch.noMatch();
        }
        return new TokenMatch(null, MatchLevel.CONTAINS, score(baseScore, weight, position, candidate.length() - term.length()));
    }

    private TokenMatch matchFuzzy(String term, DatabaseSearchWeight weight, String candidate) {
        if (candidate == null || candidate.isEmpty() || term.length() < 3 || !SearchTextNormalizer.isSubsequence(candidate, term)) {
            return TokenMatch.noMatch();
        }
        int fuzzyScore = FUZZY_SCORE.score(candidate, term);
        if (fuzzyScore <= 0) {
            return TokenMatch.noMatch();
        }
        int subsequenceSpan = this.subsequenceSpan(candidate, term);
        int spanPenalty = Math.max(0, subsequenceSpan - term.length()) * 3;
        int candidatePenalty = Math.max(0, candidate.length() - term.length());
        return new TokenMatch(null, MatchLevel.FUZZY, score(FUZZY_BASE_SCORE + fuzzyScore, weight, 0, candidatePenalty + spanPenalty));
    }

    private double phraseBonus(DatabaseTabQueryState tabQueryState, DatabaseSearchIndex index, DatabaseSearchConfig config, List<String> terms) {
        if (terms.size() < 2) {
            return 0.0D;
        }
        double bestBonus = 0.0D;
        String normalizedPhrase = SearchTextNormalizer.normalizeQueryText(tabQueryState.searchText());
        String compactPhrase = SearchTextNormalizer.compactIdentifierText(tabQueryState.searchText());
        for (DatabaseSearchField field : DatabaseSearchField.values()) {
            if (!field.isTextField()) {
                continue;
            }
            DatabaseSearchWeight weight = config.weightFor(field);
            if (weight == DatabaseSearchWeight.OFF) {
                continue;
            }
            bestBonus = Math.max(bestBonus, this.phraseBonusForField(field, weight, index, normalizedPhrase, compactPhrase, terms));
        }
        return bestBonus;
    }

    private double phraseBonusForField(
            DatabaseSearchField field,
            DatabaseSearchWeight weight,
            DatabaseSearchIndex index,
            String normalizedPhrase,
            String compactPhrase,
            List<String> terms
    ) {
        return switch (field) {
            case DISPLAY_NAME -> this.phraseBonusForTexts(
                    weight,
                    normalizedPhrase,
                    compactPhrase,
                    index.displayNameSearchNormalizedTexts(),
                    index.displayNameSearchCompactTexts(),
                    index.displayNameTokens()
            );
            case ITEM_ID -> this.phraseBonusForText(weight, normalizedPhrase, compactPhrase, index.registryPathNormalized(), index.registryPathCompact(), index.registryPathTokens());
            case PINYIN -> this.phraseBonusForText(weight, normalizedPhrase, compactPhrase, index.pinyinFull(), index.pinyinFull(), index.pinyinTokens());
            case MOD_NAMESPACE -> this.phraseBonusForCompact(weight, compactPhrase, index.modNamespace());
            case NOTE -> this.phraseBonusForTexts(
                    weight,
                    normalizedPhrase,
                    compactPhrase,
                    index.noteSearchNormalizedTexts(),
                    index.noteSearchCompactTexts(),
                    index.noteTokens()
            );
            case COUNT_BOOST -> 0.0D;
        };
    }

    private double phraseBonusForTexts(
            DatabaseSearchWeight weight,
            String normalizedPhrase,
            String compactPhrase,
            List<String> normalizedTexts,
            List<String> compactTexts,
            List<String> tokens
    ) {
        double bestBonus = 0.0D;
        for (String normalizedText : normalizedTexts) {
            bestBonus = Math.max(bestBonus, this.phraseBonusForText(weight, normalizedPhrase, compactPhrase, normalizedText, "", tokens));
        }
        for (String compactText : compactTexts) {
            bestBonus = Math.max(bestBonus, this.phraseBonusForText(weight, normalizedPhrase, compactPhrase, "", compactText, tokens));
        }
        return bestBonus;
    }

    private double phraseBonusForText(
            DatabaseSearchWeight weight,
            String normalizedPhrase,
            String compactPhrase,
            String normalizedText,
            String compactText,
            List<String> tokens
    ) {
        if (!normalizedPhrase.isEmpty() && normalizedPhrase.equals(normalizedText)) {
            return PHRASE_EXACT_BONUS * weight.multiplier();
        }
        if (!compactPhrase.isEmpty() && compactPhrase.equals(compactText)) {
            return PHRASE_EXACT_BONUS * weight.multiplier();
        }
        if (!normalizedPhrase.isEmpty() && !normalizedText.isEmpty() && normalizedText.startsWith(normalizedPhrase)) {
            return PHRASE_PREFIX_BONUS * weight.multiplier();
        }
        if (!compactPhrase.isEmpty() && !compactText.isEmpty() && compactText.startsWith(compactPhrase)) {
            return PHRASE_PREFIX_BONUS * weight.multiplier();
        }
        if (this.containsAdjacentTokens(tokens, SearchTextNormalizer.splitTerms(normalizedPhrase))) {
            return TOKEN_SEQUENCE_BONUS * weight.multiplier();
        }
        return 0.0D;
    }

    private double phraseBonusForCompact(DatabaseSearchWeight weight, String compactPhrase, String compactText) {
        if (compactPhrase.isEmpty() || compactText.isEmpty()) {
            return 0.0D;
        }
        if (compactPhrase.equals(compactText)) {
            return PHRASE_EXACT_BONUS * weight.multiplier();
        }
        if (compactText.startsWith(compactPhrase)) {
            return PHRASE_PREFIX_BONUS * weight.multiplier();
        }
        return 0.0D;
    }

    private double fieldCoverageBonus(EnumSet<DatabaseSearchField> matchedFields) {
        if (matchedFields.size() < 2) {
            return 0.0D;
        }
        return (matchedFields.size() - 1L) * MULTI_FIELD_BONUS;
    }

    private boolean containsAdjacentTokens(List<String> tokens, List<String> queryTokens) {
        if (tokens.isEmpty() || queryTokens.size() < 2 || tokens.size() < queryTokens.size()) {
            return false;
        }
        for (int startIndex = 0; startIndex <= tokens.size() - queryTokens.size(); startIndex++) {
            boolean matched = true;
            for (int offset = 0; offset < queryTokens.size(); offset++) {
                if (!tokens.get(startIndex + offset).startsWith(queryTokens.get(offset))) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private int subsequenceSpan(String candidate, String term) {
        int firstMatch = -1;
        int lastMatch = -1;
        int termIndex = 0;
        for (int index = 0; index < candidate.length() && termIndex < term.length(); index++) {
            if (candidate.charAt(index) != term.charAt(termIndex)) {
                continue;
            }
            if (firstMatch < 0) {
                firstMatch = index;
            }
            lastMatch = index;
            termIndex++;
        }
        if (firstMatch < 0 || lastMatch < firstMatch) {
            return Integer.MAX_VALUE;
        }
        return lastMatch - firstMatch + 1;
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
}
