package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.service.DatabaseSortSnapshot;
import com.agguy.infiniteinventory.service.search.DatabaseSearchRanking;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabaseSortSnapshotTest {

    @Test
    void shouldHandleNullStrings() {
        DatabaseSortSnapshot snapshot = new DatabaseSortSnapshot(
                null, null, null, null, 0, 0, 0, 0, false, null);

        assertEquals("", snapshot.displayNameNormalized());
        assertEquals("", snapshot.registryName());
        assertEquals("", snapshot.registryNamespace());
        assertEquals("", snapshot.registryPath());
    }

    @Test
    void shouldClampNegativeValuesToZero() {
        DatabaseSortSnapshot snapshot = new DatabaseSortSnapshot(
                "", "", "", "", -100, -50, -1, 0, false, null);

        assertEquals(0, snapshot.amount());
        assertEquals(0, snapshot.firstAdded());
        assertEquals(0, snapshot.lastModified());
    }

    @Test
    void shouldDefaultNullSearchRankingToUnfiltered() {
        DatabaseSortSnapshot snapshot = new DatabaseSortSnapshot(
                "", "", "", "", 0, 0, 0, 0, false, null);

        assertNotNull(snapshot.searchRanking());
        assertEquals(DatabaseSearchRanking.unfiltered(), snapshot.searchRanking());
    }

    @Test
    void shouldPreserveValidValues() {
        DatabaseSearchRanking ranking = DatabaseSearchRanking.noMatch();
        DatabaseSortSnapshot snapshot = new DatabaseSortSnapshot(
                "diamond sword", "minecraft:diamond_sword", "minecraft",
                "diamond_sword", 64, 1000, 2000, 12345, true, ranking);

        assertEquals("diamond sword", snapshot.displayNameNormalized());
        assertEquals("minecraft:diamond_sword", snapshot.registryName());
        assertEquals("minecraft", snapshot.registryNamespace());
        assertEquals("diamond_sword", snapshot.registryPath());
        assertEquals(64, snapshot.amount());
        assertEquals(1000, snapshot.firstAdded());
        assertEquals(2000, snapshot.lastModified());
        assertEquals(12345, snapshot.stackHash());
        assertEquals(true, snapshot.starred());
        assertEquals(ranking, snapshot.searchRanking());
    }

    @Test
    void withSearchRankingShouldUpdateRanking() {
        DatabaseSortSnapshot snapshot = new DatabaseSortSnapshot(
                "", "", "", "", 0, 0, 0, 0, false, DatabaseSearchRanking.unfiltered());

        DatabaseSearchRanking newRanking = DatabaseSearchRanking.noMatch();
        DatabaseSortSnapshot updated = snapshot.withSearchRanking(newRanking);

        assertEquals(newRanking, updated.searchRanking());
        assertEquals(snapshot.displayNameNormalized(), updated.displayNameNormalized());
        assertEquals(snapshot.amount(), updated.amount());
    }
}
