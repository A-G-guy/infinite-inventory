package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DatabasePageTest {
    @Test
    void emptyPageShouldNormalizeCountsAndRejectOutOfRangeAccess() {
        DatabasePage page = new DatabasePage(DatabaseQuery.defaultQuery(), -2, 0, -5L, List.of());

        assertEquals(0, page.totalEntries());
        assertEquals(1, page.totalPages());
        assertEquals(0L, page.totalItems());
        assertNull(page.entryAt(0));
        assertNull(page.entryAt(-1));
    }
}
