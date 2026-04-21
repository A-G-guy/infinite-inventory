package com.agguy.infiniteinventory.service.search;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseTabQueryState;
import com.agguy.infiniteinventory.database.StoredStackKey;
import java.util.Comparator;

public final class DatabaseSearchExpressionEvaluator {
    public static final DatabaseSearchExpressionEvaluator INSTANCE = new DatabaseSearchExpressionEvaluator();

    private static final Comparator<DatabaseSearchRanking> RANKING_COMPARATOR =
            Comparator.comparingInt(DatabaseSearchRanking::exactMatches)
                    .thenComparingInt(DatabaseSearchRanking::prefixMatches)
                    .thenComparingInt(DatabaseSearchRanking::containsMatches)
                    .thenComparingInt(DatabaseSearchRanking::fuzzyMatches)
                    .thenComparingDouble(DatabaseSearchRanking::textScore)
                    .thenComparingDouble(DatabaseSearchRanking::countBoostScore);

    private final DatabaseSearchEvaluator textEvaluator = new DatabaseSearchEvaluator();

    private DatabaseSearchExpressionEvaluator() {
    }

    public DatabaseSearchRanking evaluate(
            DatabaseTabQueryState tabQueryState,
            DatabaseParsedSearchQuery parsedQuery,
            DatabaseSearchIndex searchIndex,
            DatabaseItemSearchMetadata searchMetadata,
            StoredStackKey key,
            long amount,
            DatabaseSearchEnvironment searchEnvironment
    ) {
        if (parsedQuery == null || !parsedQuery.active()) {
            return DatabaseSearchRanking.unfiltered();
        }
        DatabaseTabQueryState normalizedState = tabQueryState == null ? DatabaseTabQueryState.defaultState() : tabQueryState;
        DatabaseItemSearchMetadata normalizedMetadata = searchMetadata == null ? DatabaseItemSearchMetadata.of(key) : searchMetadata;
        DatabaseSearchEnvironment normalizedEnvironment = searchEnvironment == null
                ? DatabaseSearchEnvironment.defaultEnvironment()
                : searchEnvironment;

        DatabaseSearchRanking bestRanking = null;
        for (DatabaseSearchClause clause : parsedQuery.clauses()) {
            DatabaseSearchRanking clauseRanking = this.evaluateClause(
                    normalizedState,
                    clause,
                    searchIndex,
                    normalizedMetadata,
                    key,
                    amount,
                    normalizedEnvironment
            );
            if (!clauseRanking.matched()) {
                continue;
            }
            if (bestRanking == null || RANKING_COMPARATOR.compare(clauseRanking, bestRanking) > 0) {
                bestRanking = clauseRanking;
            }
        }
        return bestRanking == null ? DatabaseSearchRanking.noMatch() : bestRanking;
    }

    private DatabaseSearchRanking evaluateClause(
            DatabaseTabQueryState tabQueryState,
            DatabaseSearchClause clause,
            DatabaseSearchIndex searchIndex,
            DatabaseItemSearchMetadata searchMetadata,
            StoredStackKey key,
            long amount,
            DatabaseSearchEnvironment searchEnvironment
    ) {
        for (DatabaseSearchFilter filter : clause.negativeFilters()) {
            if (this.matchesFilter(filter, searchIndex, searchMetadata, key, searchEnvironment)) {
                return DatabaseSearchRanking.noMatch();
            }
        }
        for (String term : clause.negativePlainTerms()) {
            if (this.evaluatePlainText(term, tabQueryState.searchConfig(), searchIndex, amount).matched()) {
                return DatabaseSearchRanking.noMatch();
            }
        }
        for (DatabaseSearchFilter filter : clause.positiveFilters()) {
            if (!this.matchesFilter(filter, searchIndex, searchMetadata, key, searchEnvironment)) {
                return DatabaseSearchRanking.noMatch();
            }
        }
        if (clause.positivePlainTerms().isEmpty()) {
            return DatabaseSearchRanking.unfiltered();
        }
        return this.evaluatePlainText(clause.positivePlainQuery(), tabQueryState.searchConfig(), searchIndex, amount);
    }

    private DatabaseSearchRanking evaluatePlainText(
            String plainText,
            DatabaseSearchConfig searchConfig,
            DatabaseSearchIndex searchIndex,
            long amount
    ) {
        DatabaseTabQueryState temporaryState = new DatabaseTabQueryState(
                DatabaseSortOption.RECENTLY_CHANGED,
                plainText,
                searchConfig,
                0,
                DatabaseQuery.DEFAULT_PAGE_SIZE
        );
        return this.textEvaluator.evaluate(temporaryState, searchIndex, amount);
    }

    private boolean matchesFilter(
            DatabaseSearchFilter filter,
            DatabaseSearchIndex searchIndex,
            DatabaseItemSearchMetadata searchMetadata,
            StoredStackKey key,
            DatabaseSearchEnvironment searchEnvironment
    ) {
        return switch (filter.type()) {
            case MOD -> this.matchesModFilter(filter.value(), searchIndex, searchMetadata);
            case TAG -> this.matchesTagFilter(filter.value(), searchMetadata);
            case ITEM_ID -> this.matchesItemIdFilter(filter.value(), searchIndex);
            case CREATIVE_TAB -> this.matchesCreativeTabFilter(filter.value(), key, searchEnvironment);
        };
    }

    private boolean matchesModFilter(String queryText, DatabaseSearchIndex searchIndex, DatabaseItemSearchMetadata searchMetadata) {
        return this.matchesIdentifierQuery(queryText, searchIndex.modNamespace(), searchIndex.modNamespace())
                || this.matchesNaturalQuery(queryText, searchMetadata.modDisplayNameNormalized(), searchMetadata.modDisplayNameCompact());
    }

    private boolean matchesTagFilter(String queryText, DatabaseItemSearchMetadata searchMetadata) {
        String normalizedQuery = SearchTextNormalizer.normalizeIdentifierText(queryText);
        String compactQuery = SearchTextNormalizer.compactIdentifierText(queryText);
        if (normalizedQuery.isEmpty() && compactQuery.isEmpty()) {
            return false;
        }
        for (String tagId : searchMetadata.itemTagIdsNormalized()) {
            if (this.matchesIdentifierQuery(normalizedQuery, compactQuery, tagId, SearchTextNormalizer.compactIdentifierText(tagId))) {
                return true;
            }
        }
        for (String compactTagId : searchMetadata.itemTagIdsCompact()) {
            if (this.matchesIdentifierQuery(normalizedQuery, compactQuery, compactTagId, compactTagId)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesItemIdFilter(String queryText, DatabaseSearchIndex searchIndex) {
        return this.matchesIdentifierQuery(queryText, searchIndex.registryNameNormalized(), searchIndex.registryNameCompact())
                || this.matchesIdentifierQuery(queryText, searchIndex.registryPathNormalized(), searchIndex.registryPathCompact());
    }

    private boolean matchesCreativeTabFilter(
            String queryText,
            StoredStackKey key,
            DatabaseSearchEnvironment searchEnvironment
    ) {
        for (DatabaseCreativeTabSearchEntry tabEntry : DatabaseCreativeTabSearchResolver.INSTANCE.tabsForItem(key, searchEnvironment)) {
            if (this.matchesIdentifierQuery(queryText, tabEntry.registryNameNormalized(), tabEntry.registryNameCompact())
                    || this.matchesIdentifierQuery(queryText, tabEntry.registryPathNormalized(), tabEntry.registryPathCompact())
                    || this.matchesNaturalQuery(queryText, tabEntry.displayNameNormalized(), tabEntry.displayNameCompact())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesNaturalQuery(String queryText, String normalizedCandidate, String compactCandidate) {
        String normalizedQuery = SearchTextNormalizer.normalizeNaturalText(queryText);
        String compactQuery = SearchTextNormalizer.compactNaturalText(queryText);
        if (normalizedQuery.isEmpty() && compactQuery.isEmpty()) {
            return false;
        }
        return this.matchesNaturalQuery(normalizedQuery, compactQuery, normalizedCandidate, compactCandidate);
    }

    private boolean matchesNaturalQuery(
            String normalizedQuery,
            String compactQuery,
            String normalizedCandidate,
            String compactCandidate
    ) {
        return this.matchesCandidate(normalizedQuery, normalizedCandidate)
                || this.matchesCandidate(compactQuery, compactCandidate);
    }

    private boolean matchesIdentifierQuery(String queryText, String normalizedCandidate, String compactCandidate) {
        String normalizedQuery = SearchTextNormalizer.normalizeIdentifierText(queryText);
        String compactQuery = SearchTextNormalizer.compactIdentifierText(queryText);
        if (normalizedQuery.isEmpty() && compactQuery.isEmpty()) {
            return false;
        }
        return this.matchesIdentifierQuery(normalizedQuery, compactQuery, normalizedCandidate, compactCandidate);
    }

    private boolean matchesIdentifierQuery(
            String normalizedQuery,
            String compactQuery,
            String normalizedCandidate,
            String compactCandidate
    ) {
        return this.matchesCandidate(normalizedQuery, normalizedCandidate)
                || this.matchesCandidate(compactQuery, compactCandidate);
    }

    private boolean matchesCandidate(String query, String candidate) {
        if (query == null || query.isEmpty() || candidate == null || candidate.isEmpty()) {
            return false;
        }
        return candidate.equals(query)
                || candidate.startsWith(query)
                || candidate.contains(query);
    }
}
