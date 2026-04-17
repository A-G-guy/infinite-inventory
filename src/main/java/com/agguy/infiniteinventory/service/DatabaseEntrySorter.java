package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseTabQueryState;
import com.agguy.infiniteinventory.service.search.SearchTextNormalizer;
import java.util.Comparator;

public final class DatabaseEntrySorter {
    public static final DatabaseEntrySorter INSTANCE = new DatabaseEntrySorter();

    private DatabaseEntrySorter() {
    }

    public Comparator<DatabaseSortSnapshot> comparatorFor(DatabaseQuery query) {
        DatabaseQuery normalizedQuery = query == null ? DatabaseQuery.defaultQuery() : query;
        return this.comparatorFor(normalizedQuery.tabStateFor(normalizedQuery.focusedTab()));
    }

    public Comparator<DatabaseSortSnapshot> comparatorFor(DatabaseTabQueryState tabQueryState) {
        DatabaseTabQueryState normalizedState = tabQueryState == null ? DatabaseTabQueryState.defaultState() : tabQueryState;
        Comparator<DatabaseSortSnapshot> manualComparator = this.manualComparatorFor(normalizedState.sortOption());
        if (SearchTextNormalizer.splitTerms(normalizedState.searchText()).isEmpty()) {
            return manualComparator;
        }
        return Comparator.comparingInt((DatabaseSortSnapshot snapshot) -> snapshot.searchRanking().exactMatches()).reversed()
                .thenComparing(Comparator.comparingInt((DatabaseSortSnapshot snapshot) -> snapshot.searchRanking().prefixMatches()).reversed())
                .thenComparing(Comparator.comparingInt((DatabaseSortSnapshot snapshot) -> snapshot.searchRanking().containsMatches()).reversed())
                .thenComparing(Comparator.comparingInt((DatabaseSortSnapshot snapshot) -> snapshot.searchRanking().fuzzyMatches()).reversed())
                .thenComparing(Comparator.comparingDouble((DatabaseSortSnapshot snapshot) -> snapshot.searchRanking().textScore()).reversed())
                .thenComparing(Comparator.comparingDouble((DatabaseSortSnapshot snapshot) -> snapshot.searchRanking().countBoostScore()).reversed())
                .thenComparing(manualComparator);
    }

    private Comparator<DatabaseSortSnapshot> manualComparatorFor(DatabaseSortOption sortOption) {
        return switch (sortOption) {
            case RECENTLY_ADDED -> Comparator.comparingLong(DatabaseSortSnapshot::firstAdded).reversed()
                    .thenComparing(Comparator.comparingLong(DatabaseSortSnapshot::lastModified).reversed())
                    .thenComparing(Comparator.comparingLong(DatabaseSortSnapshot::amount).reversed())
                    .thenComparing(this.nameAscendingComparator());
            case NAME_ASC -> this.nameAscendingComparator();
            case NAME_DESC -> this.nameDescendingComparator();
            case COUNT_ASC -> Comparator.comparingLong(DatabaseSortSnapshot::amount)
                    .thenComparing(Comparator.comparingLong(DatabaseSortSnapshot::lastModified).reversed())
                    .thenComparing(this.nameAscendingComparator());
            case COUNT_DESC -> Comparator.comparingLong(DatabaseSortSnapshot::amount).reversed()
                    .thenComparing(Comparator.comparingLong(DatabaseSortSnapshot::lastModified).reversed())
                    .thenComparing(this.nameAscendingComparator());
            case MOD_NAMESPACE_ASC -> Comparator.comparing(DatabaseSortSnapshot::registryNamespace)
                    .thenComparing(this.nameAscendingComparator());
            case MOD_NAMESPACE_DESC -> Comparator.comparing(DatabaseSortSnapshot::registryNamespace, Comparator.reverseOrder())
                    .thenComparing(this.nameAscendingComparator());
            case ITEM_ID_ASC -> Comparator.comparing(DatabaseSortSnapshot::registryName)
                    .thenComparing(DatabaseSortSnapshot::displayNameNormalized)
                    .thenComparingInt(DatabaseSortSnapshot::stackHash);
            case ITEM_ID_DESC -> Comparator.comparing(DatabaseSortSnapshot::registryName, Comparator.reverseOrder())
                    .thenComparing(DatabaseSortSnapshot::displayNameNormalized)
                    .thenComparingInt(DatabaseSortSnapshot::stackHash);
            case RECENTLY_CHANGED -> Comparator.comparingLong(DatabaseSortSnapshot::lastModified).reversed()
                    .thenComparing(Comparator.comparingLong(DatabaseSortSnapshot::amount).reversed())
                    .thenComparing(this.nameAscendingComparator());
        };
    }

    private Comparator<DatabaseSortSnapshot> nameAscendingComparator() {
        return Comparator.comparing(DatabaseSortSnapshot::displayNameNormalized)
                .thenComparing(DatabaseSortSnapshot::registryName)
                .thenComparingInt(DatabaseSortSnapshot::stackHash);
    }

    private Comparator<DatabaseSortSnapshot> nameDescendingComparator() {
        return Comparator.comparing(DatabaseSortSnapshot::displayNameNormalized, Comparator.reverseOrder())
                .thenComparing(DatabaseSortSnapshot::registryName)
                .thenComparingInt(DatabaseSortSnapshot::stackHash);
    }
}
