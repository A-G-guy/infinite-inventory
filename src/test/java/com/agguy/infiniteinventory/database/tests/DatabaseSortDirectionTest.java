package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseSortDirection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class DatabaseSortDirectionTest {

    @Test
    void shouldHaveTwoDirections() {
        assertEquals(2, DatabaseSortDirection.values().length);
    }

    @Test
    void eachDirectionShouldHaveTranslationKey() {
        assertNotNull(DatabaseSortDirection.ASC.translationKey());
        assertNotNull(DatabaseSortDirection.DESC.translationKey());
    }
}
