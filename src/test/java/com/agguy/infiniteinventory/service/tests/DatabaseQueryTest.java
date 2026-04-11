package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseQueryTest {
    @Test
    void queryShouldTrimAndClampInputs() {
        DatabaseQuery query = new DatabaseQuery(DatabaseCategory.ALL, DatabaseSortOption.NAME_ASC, "  diamonds  ", -3);

        assertEquals("diamonds", query.searchText());
        assertEquals(0, query.pageIndex());
    }

    @Test
    void categoryAndSearchUpdatesShouldResetPageIndex() {
        DatabaseQuery query = new DatabaseQuery(DatabaseCategory.MATERIALS, DatabaseSortOption.COUNT_DESC, "ore", 4);

        assertEquals(0, query.withCategory(DatabaseCategory.BLOCKS).pageIndex());
        assertEquals(0, query.withSearchText("stone").pageIndex());
        assertEquals(4, query.withSortOption(DatabaseSortOption.NAME_DESC).pageIndex());
    }
}
