package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseQueryTest {
    @Test
    void queryShouldTrimAndClampInputs() {
        DatabaseQuery query = new DatabaseQuery(DatabaseScope.PUBLIC, DatabaseCategory.ALL, DatabaseSortOption.NAME_ASC, "  diamonds  ", -3, 0);

        assertEquals("diamonds", query.searchText());
        assertEquals(0, query.pageIndex());
        assertEquals(1, query.pageSize());
        assertEquals(DatabaseScope.PUBLIC, query.scope());
    }

    @Test
    void categoryAndSearchUpdatesShouldResetPageIndexButKeepPageSize() {
        DatabaseQuery query = new DatabaseQuery(DatabaseScope.PERSONAL, DatabaseCategory.MATERIALS, DatabaseSortOption.COUNT_DESC, "ore", 4, 72);

        assertEquals(0, query.withCategory(DatabaseCategory.BLOCKS).pageIndex());
        assertEquals(72, query.withCategory(DatabaseCategory.BLOCKS).pageSize());
        assertEquals(0, query.withSearchText("stone").pageIndex());
        assertEquals(72, query.withSearchText("stone").pageSize());
        assertEquals(4, query.withSortOption(DatabaseSortOption.NAME_DESC).pageIndex());
        assertEquals(72, query.withSortOption(DatabaseSortOption.NAME_DESC).pageSize());
        assertEquals(90, query.withPageSize(90).pageSize());
    }

    @Test
    void defaultQueryShouldUseDefaultPageSize() {
        assertEquals(DatabaseQuery.DEFAULT_PAGE_SIZE, DatabaseQuery.defaultQuery().pageSize());
        assertEquals(DatabaseScope.PERSONAL, DatabaseQuery.defaultQuery().scope());
    }

    @Test
    void normalizeForScopeShouldRetargetStoredQueryWithoutDroppingFilters() {
        DatabaseQuery query = new DatabaseQuery(DatabaseScope.PERSONAL, DatabaseCategory.MATERIALS, DatabaseSortOption.COUNT_DESC, "ore", 4, 72);
        DatabaseQuery normalized = DatabaseQuery.normalizeForScope(DatabaseScope.PUBLIC, query);

        assertEquals(DatabaseScope.PUBLIC, normalized.scope());
        assertEquals(DatabaseCategory.MATERIALS, normalized.category());
        assertEquals(DatabaseSortOption.COUNT_DESC, normalized.sortOption());
        assertEquals("ore", normalized.searchText());
        assertEquals(4, normalized.pageIndex());
        assertEquals(72, normalized.pageSize());
    }
}
