package com.agguy.infiniteinventory.service.search;

public record DatabaseSearchRanking(
        boolean matched,
        boolean active,
        int exactMatches,
        int prefixMatches,
        int containsMatches,
        int fuzzyMatches,
        double textScore,
        double countBoostScore
) {
    private static final DatabaseSearchRanking UNFILTERED = new DatabaseSearchRanking(true, false, 0, 0, 0, 0, 0.0D, 0.0D);
    private static final DatabaseSearchRanking NO_MATCH = new DatabaseSearchRanking(false, true, 0, 0, 0, 0, 0.0D, 0.0D);

    public static DatabaseSearchRanking unfiltered() {
        return UNFILTERED;
    }

    public static DatabaseSearchRanking noMatch() {
        return NO_MATCH;
    }
}
