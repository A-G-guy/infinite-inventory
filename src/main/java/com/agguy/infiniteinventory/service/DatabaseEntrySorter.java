package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSortDirection;
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
        DatabaseSortOption resolvedOption = sortOption == null ? DatabaseSortOption.RECENTLY_CHANGED : sortOption;
        return switch (resolvedOption.method()) {
            case RECENTLY_CHANGED -> this.recentlyChangedComparator(resolvedOption.direction());
            case RECENTLY_ADDED -> this.recentlyAddedComparator(resolvedOption.direction());
            case NAME -> resolvedOption.direction() == DatabaseSortDirection.ASC
                    ? this.nameAscendingComparator()
                    : this.nameDescendingComparator();
            case COUNT -> this.countComparator(resolvedOption.direction());
            case MOD_NAMESPACE -> this.modNamespaceComparator(resolvedOption.direction());
            case ITEM_ID -> this.itemIdComparator(resolvedOption.direction());
        };
    }

    private Comparator<DatabaseSortSnapshot> recentlyChangedComparator(DatabaseSortDirection direction) {
        return direction == DatabaseSortDirection.ASC
                ? Comparator.comparingLong(DatabaseSortSnapshot::lastModified)
                        .thenComparingLong(DatabaseSortSnapshot::amount)
                        .thenComparing(this.nameAscendingComparator())
                : Comparator.comparingLong(DatabaseSortSnapshot::lastModified).reversed()
                        .thenComparing(Comparator.comparingLong(DatabaseSortSnapshot::amount).reversed())
                        .thenComparing(this.nameAscendingComparator());
    }

    private Comparator<DatabaseSortSnapshot> recentlyAddedComparator(DatabaseSortDirection direction) {
        return direction == DatabaseSortDirection.ASC
                ? Comparator.comparingLong(DatabaseSortSnapshot::firstAdded)
                        .thenComparingLong(DatabaseSortSnapshot::lastModified)
                        .thenComparingLong(DatabaseSortSnapshot::amount)
                        .thenComparing(this.nameAscendingComparator())
                : Comparator.comparingLong(DatabaseSortSnapshot::firstAdded).reversed()
                        .thenComparing(Comparator.comparingLong(DatabaseSortSnapshot::lastModified).reversed())
                        .thenComparing(Comparator.comparingLong(DatabaseSortSnapshot::amount).reversed())
                        .thenComparing(this.nameAscendingComparator());
    }

    private Comparator<DatabaseSortSnapshot> countComparator(DatabaseSortDirection direction) {
        return direction == DatabaseSortDirection.ASC
                ? Comparator.comparingLong(DatabaseSortSnapshot::amount)
                        .thenComparing(Comparator.comparingLong(DatabaseSortSnapshot::lastModified).reversed())
                        .thenComparing(this.nameAscendingComparator())
                : Comparator.comparingLong(DatabaseSortSnapshot::amount).reversed()
                        .thenComparing(Comparator.comparingLong(DatabaseSortSnapshot::lastModified).reversed())
                        .thenComparing(this.nameAscendingComparator());
    }

    private Comparator<DatabaseSortSnapshot> modNamespaceComparator(DatabaseSortDirection direction) {
        return direction == DatabaseSortDirection.ASC
                ? Comparator.comparing(DatabaseSortSnapshot::registryNamespace)
                        .thenComparing(this.nameAscendingComparator())
                : Comparator.comparing(DatabaseSortSnapshot::registryNamespace, Comparator.reverseOrder())
                        .thenComparing(this.nameAscendingComparator());
    }

    private Comparator<DatabaseSortSnapshot> itemIdComparator(DatabaseSortDirection direction) {
        return direction == DatabaseSortDirection.ASC
                ? Comparator.comparing(DatabaseSortSnapshot::registryName)
                        .thenComparing(DatabaseSortSnapshot::displayNameNormalized)
                        .thenComparingInt(DatabaseSortSnapshot::stackHash)
                : Comparator.comparing(DatabaseSortSnapshot::registryName, Comparator.reverseOrder())
                        .thenComparing(DatabaseSortSnapshot::displayNameNormalized)
                        .thenComparingInt(DatabaseSortSnapshot::stackHash);
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
