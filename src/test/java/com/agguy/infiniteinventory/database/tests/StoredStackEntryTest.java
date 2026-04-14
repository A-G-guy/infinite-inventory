package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoredStackEntryTest {
    @Test
    void addAndRemoveShouldUpdateAmountAndSequence() {
        StoredStackEntry entry = new StoredStackEntry(DatabaseTabs.DEFAULT_TAB_ID, 10L, 2L);

        entry.add(5L, 4L);
        assertEquals(15L, entry.amount());
        assertEquals(4L, entry.lastModified());

        long removed = entry.remove(6L, 8L);
        assertEquals(6L, removed);
        assertEquals(9L, entry.amount());
        assertEquals(8L, entry.lastModified());
    }

    @Test
    void addShouldSaturateAtLongMaxValue() {
        StoredStackEntry entry = new StoredStackEntry(DatabaseTabs.DEFAULT_TAB_ID, Long.MAX_VALUE - 2L, 1L);

        entry.add(10L, 3L);

        assertEquals(Long.MAX_VALUE, entry.amount());
        assertEquals(3L, entry.lastModified());
    }

    @Test
    void removeShouldClampAndMarkEmpty() {
        StoredStackEntry entry = new StoredStackEntry(DatabaseTabs.DEFAULT_TAB_ID, 3L, 1L);

        long removed = entry.remove(99L, 5L);

        assertEquals(3L, removed);
        assertEquals(0L, entry.amount());
        assertEquals(5L, entry.lastModified());
        assertTrue(entry.isEmpty());
    }
}
