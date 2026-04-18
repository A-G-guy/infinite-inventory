package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.service.DatabaseEntrySorter;
import com.agguy.infiniteinventory.service.DatabaseSortSnapshot;
import com.agguy.infiniteinventory.service.search.DatabaseSearchRanking;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseEntrySorterTest {
    private final DatabaseEntrySorter sorter = DatabaseEntrySorter.INSTANCE;

    @Test
    void recentlyChangedSortShouldPreferNewerEntriesThenLargerAmounts() {
        List<DatabaseSortSnapshot> sorted = this.sort(
                this.query(DatabaseSortOption.RECENTLY_CHANGED, ""),
                this.snapshot("stone", "minecraft:stone", 16L, 1L, 10L, 1),
                this.snapshot("apple", "minecraft:apple", 64L, 2L, 10L, 2),
                this.snapshot("dirt", "minecraft:dirt", 1L, 3L, 20L, 3)
        );

        assertEquals(
                List.of("minecraft:dirt", "minecraft:apple", "minecraft:stone"),
                sorted.stream().map(DatabaseSortSnapshot::registryName).toList()
        );
    }

    @Test
    void recentlyChangedAscendingSortShouldPreferOlderEntriesFirst() {
        List<DatabaseSortSnapshot> sorted = this.sort(
                this.query(DatabaseSortOption.RECENTLY_CHANGED_ASC, ""),
                this.snapshot("stone", "minecraft:stone", 16L, 1L, 10L, 1),
                this.snapshot("apple", "minecraft:apple", 64L, 2L, 10L, 2),
                this.snapshot("dirt", "minecraft:dirt", 1L, 3L, 20L, 3)
        );

        assertEquals(
                List.of("minecraft:stone", "minecraft:apple", "minecraft:dirt"),
                sorted.stream().map(DatabaseSortSnapshot::registryName).toList()
        );
    }

    @Test
    void countSortShouldUseLastModifiedAsTieBreaker() {
        List<DatabaseSortSnapshot> sorted = this.sort(
                this.query(DatabaseSortOption.COUNT_DESC, ""),
                this.snapshot("stone", "minecraft:stone", 16L, 1L, 10L, 1),
                this.snapshot("dirt", "minecraft:dirt", 16L, 2L, 20L, 2),
                this.snapshot("apple", "minecraft:apple", 8L, 3L, 30L, 3)
        );

        assertEquals(
                List.of("minecraft:dirt", "minecraft:stone", "minecraft:apple"),
                sorted.stream().map(DatabaseSortSnapshot::registryName).toList()
        );
    }

    @Test
    void modNamespaceSortShouldGroupByNamespaceBeforeDisplayName() {
        List<DatabaseSortSnapshot> sorted = this.sort(
                this.query(DatabaseSortOption.MOD_NAMESPACE_ASC, ""),
                this.snapshot("stone", "beta:stone", 1L, 1L, 1L, 1),
                this.snapshot("apple", "alpha:apple", 1L, 1L, 1L, 2),
                this.snapshot("dirt", "alpha:dirt", 1L, 1L, 1L, 3)
        );

        assertEquals(
                List.of("alpha:apple", "alpha:dirt", "beta:stone"),
                sorted.stream().map(DatabaseSortSnapshot::registryName).toList()
        );
    }

    @Test
    void itemIdDescendingSortShouldReverseRegistryOrder() {
        List<DatabaseSortSnapshot> sorted = this.sort(
                this.query(DatabaseSortOption.ITEM_ID_DESC, ""),
                this.snapshot("apple", "alpha:apple", 1L, 1L, 1L, 1),
                this.snapshot("stone", "beta:stone", 1L, 1L, 1L, 2),
                this.snapshot("dirt", "alpha:dirt", 1L, 1L, 1L, 3)
        );

        assertEquals(
                List.of("beta:stone", "alpha:dirt", "alpha:apple"),
                sorted.stream().map(DatabaseSortSnapshot::registryName).toList()
        );
    }

    @Test
    void searchSortShouldKeepRelevanceFirstAndUseManualSortForTies() {
        List<DatabaseSortSnapshot> sorted = this.sort(
                this.query(DatabaseSortOption.COUNT_DESC, "stone"),
                this.snapshot("stone", "minecraft:stone", 1L, 1L, 1L, 1, this.ranking(1, 0, 0, 0, 40_600.0D, 0.0D)),
                this.snapshot("stone bricks", "minecraft:stone_bricks", 500L, 1L, 1L, 2, this.ranking(1, 0, 0, 0, 40_600.0D, 0.0D)),
                this.snapshot("stonelight", "minecraft:stonelight", 999L, 1L, 1L, 3, this.ranking(0, 1, 0, 0, 30_600.0D, 0.0D))
        );

        assertEquals(
                List.of("minecraft:stone_bricks", "minecraft:stone", "minecraft:stonelight"),
                sorted.stream().map(DatabaseSortSnapshot::registryName).toList()
        );
    }

    @Test
    void recentlyAddedSortShouldPreferNewerFirstInsertThenRecentChanges() {
        List<DatabaseSortSnapshot> sorted = this.sort(
                this.query(DatabaseSortOption.RECENTLY_ADDED, ""),
                this.snapshot("stone", "minecraft:stone", 32L, 5L, 10L, 1),
                this.snapshot("apple", "minecraft:apple", 16L, 9L, 10L, 2),
                this.snapshot("dirt", "minecraft:dirt", 8L, 7L, 7L, 3)
        );

        assertEquals(
                List.of("minecraft:apple", "minecraft:dirt", "minecraft:stone"),
                sorted.stream().map(DatabaseSortSnapshot::registryName).toList()
        );
    }

    @Test
    void recentlyAddedAscendingSortShouldPreferEarliestInsertFirst() {
        List<DatabaseSortSnapshot> sorted = this.sort(
                this.query(DatabaseSortOption.RECENTLY_ADDED_ASC, ""),
                this.snapshot("stone", "minecraft:stone", 32L, 5L, 10L, 1),
                this.snapshot("apple", "minecraft:apple", 16L, 9L, 10L, 2),
                this.snapshot("dirt", "minecraft:dirt", 8L, 7L, 7L, 3)
        );

        assertEquals(
                List.of("minecraft:stone", "minecraft:dirt", "minecraft:apple"),
                sorted.stream().map(DatabaseSortSnapshot::registryName).toList()
        );
    }

    private List<DatabaseSortSnapshot> sort(DatabaseQuery query, DatabaseSortSnapshot... snapshots) {
        List<DatabaseSortSnapshot> sorted = new ArrayList<>(List.of(snapshots));
        sorted.sort(this.sorter.comparatorFor(query));
        return sorted;
    }

    private DatabaseQuery query(DatabaseSortOption sortOption, String searchText) {
        return new DatabaseQuery(
                DatabaseScope.PERSONAL,
                DatabaseTabs.ALL_TAB_ID,
                List.of(DatabaseTabs.ALL_TAB_ID),
                Map.of(DatabaseTabs.ALL_TAB_ID, 0),
                Map.of(DatabaseTabs.ALL_TAB_ID, DatabaseQuery.DEFAULT_PAGE_SIZE),
                sortOption,
                searchText,
                com.agguy.infiniteinventory.database.DatabaseSearchConfig.defaultConfig()
        );
    }

    private DatabaseSortSnapshot snapshot(String displayName, String registryName, long amount, long firstAdded, long lastModified, int stackHash) {
        return this.snapshot(displayName, registryName, amount, firstAdded, lastModified, stackHash, DatabaseSearchRanking.unfiltered());
    }

    private DatabaseSortSnapshot snapshot(
            String displayName,
            String registryName,
            long amount,
            long firstAdded,
            long lastModified,
            int stackHash,
            DatabaseSearchRanking searchRanking
    ) {
        String[] idParts = registryName.split(":", 2);
        return new DatabaseSortSnapshot(
                displayName,
                registryName,
                idParts[0],
                idParts[1],
                amount,
                firstAdded,
                lastModified,
                stackHash,
                searchRanking
        );
    }

    private DatabaseSearchRanking ranking(
            int exactMatches,
            int prefixMatches,
            int containsMatches,
            int fuzzyMatches,
            double textScore,
            double countBoostScore
    ) {
        return new DatabaseSearchRanking(true, true, exactMatches, prefixMatches, containsMatches, fuzzyMatches, textScore, countBoostScore);
    }
}
