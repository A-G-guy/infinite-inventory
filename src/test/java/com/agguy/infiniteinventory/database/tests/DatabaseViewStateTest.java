package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseViewStateTest {
    @Test
    void activeQueryShouldOverrideMatchingStoredScopeQuery() {
        DatabaseQuery personalQuery = new DatabaseQuery(DatabaseScope.PERSONAL, DatabaseCategory.BLOCKS, DatabaseSortOption.NAME_ASC, "stone", 2, 81);
        DatabaseQuery publicQuery = new DatabaseQuery(DatabaseScope.PUBLIC, DatabaseCategory.MATERIALS, DatabaseSortOption.COUNT_DESC, "iron", 1, 96);
        DatabaseQuery activePublicQuery = publicQuery.withSearchText("gold");

        DatabaseViewState viewState = new DatabaseViewState(3, activePublicQuery, personalQuery, publicQuery, 5, 2, 64L, List.of());

        assertEquals(activePublicQuery, viewState.query());
        assertEquals(personalQuery, viewState.queryForScope(DatabaseScope.PERSONAL));
        assertEquals(activePublicQuery, viewState.queryForScope(DatabaseScope.PUBLIC));
    }
}
