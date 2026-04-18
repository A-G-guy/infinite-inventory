package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseSortDirection;
import com.agguy.infiniteinventory.database.DatabaseSortMethod;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseSortOptionTest {
    @Test
    void optionShouldExposeMethodAndDirection() {
        assertEquals(DatabaseSortMethod.RECENTLY_CHANGED, DatabaseSortOption.RECENTLY_CHANGED.method());
        assertEquals(DatabaseSortDirection.DESC, DatabaseSortOption.RECENTLY_CHANGED.direction());
        assertEquals(DatabaseSortMethod.COUNT, DatabaseSortOption.COUNT_ASC.method());
        assertEquals(DatabaseSortDirection.ASC, DatabaseSortOption.COUNT_ASC.direction());
    }

    @Test
    void optionFactoryShouldMapEveryFieldAndDirectionPair() {
        assertEquals(
                DatabaseSortOption.RECENTLY_CHANGED_ASC,
                DatabaseSortOption.of(DatabaseSortMethod.RECENTLY_CHANGED, DatabaseSortDirection.ASC)
        );
        assertEquals(
                DatabaseSortOption.RECENTLY_ADDED,
                DatabaseSortOption.of(DatabaseSortMethod.RECENTLY_ADDED, DatabaseSortDirection.DESC)
        );
        assertEquals(DatabaseSortOption.NAME_DESC, DatabaseSortOption.of(DatabaseSortMethod.NAME, DatabaseSortDirection.DESC));
        assertEquals(DatabaseSortOption.COUNT_ASC, DatabaseSortOption.of(DatabaseSortMethod.COUNT, DatabaseSortDirection.ASC));
        assertEquals(
                DatabaseSortOption.MOD_NAMESPACE_DESC,
                DatabaseSortOption.of(DatabaseSortMethod.MOD_NAMESPACE, DatabaseSortDirection.DESC)
        );
        assertEquals(DatabaseSortOption.ITEM_ID_ASC, DatabaseSortOption.of(DatabaseSortMethod.ITEM_ID, DatabaseSortDirection.ASC));
    }
}
