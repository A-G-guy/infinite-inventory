package com.agguy.infiniteinventory.service.search;

import java.util.Set;

public record DatabaseSearchQueryParserContext(
        Set<String> modPhraseCandidates,
        Set<String> creativeTabPhraseCandidates
) {
    private static final DatabaseSearchQueryParserContext EMPTY = new DatabaseSearchQueryParserContext(Set.of(), Set.of());

    public DatabaseSearchQueryParserContext {
        modPhraseCandidates = Set.copyOf(modPhraseCandidates == null ? Set.of() : modPhraseCandidates);
        creativeTabPhraseCandidates = Set.copyOf(creativeTabPhraseCandidates == null ? Set.of() : creativeTabPhraseCandidates);
    }

    public static DatabaseSearchQueryParserContext empty() {
        return EMPTY;
    }

    public static DatabaseSearchQueryParserContext from(DatabaseSearchEnvironment searchEnvironment) {
        return fromQuery("", searchEnvironment);
    }

    public static DatabaseSearchQueryParserContext fromQuery(String searchText, DatabaseSearchEnvironment searchEnvironment) {
        DatabaseSearchEnvironment normalizedEnvironment = searchEnvironment == null
                ? DatabaseSearchEnvironment.defaultEnvironment()
                : searchEnvironment;
        return new DatabaseSearchQueryParserContext(
                requiresCandidates(searchText, DatabaseSearchFilterType.MOD)
                        ? DatabaseModMetadataResolver.INSTANCE.modDisplayNameCandidates()
                        : Set.of(),
                requiresCandidates(searchText, DatabaseSearchFilterType.CREATIVE_TAB)
                        ? DatabaseCreativeTabSearchResolver.INSTANCE.creativeTabDisplayNameCandidates(normalizedEnvironment)
                        : Set.of()
        );
    }

    private static boolean requiresCandidates(String searchText, DatabaseSearchFilterType filterType) {
        if (searchText == null || searchText.isBlank()) {
            return false;
        }
        return searchText.indexOf(filterType.prefix()) >= 0;
    }

    boolean canExtend(DatabaseSearchFilterType filterType, String candidatePhrase) {
        String normalizedPhrase = SearchTextNormalizer.normalizeNaturalText(candidatePhrase);
        if (normalizedPhrase.isEmpty()) {
            return false;
        }
        Set<String> candidates = switch (filterType) {
            case MOD -> this.modPhraseCandidates;
            case CREATIVE_TAB -> this.creativeTabPhraseCandidates;
            case TAG, ITEM_ID -> Set.of();
        };
        for (String candidate : candidates) {
            if (candidate.startsWith(normalizedPhrase)) {
                return true;
            }
        }
        return false;
    }
}
