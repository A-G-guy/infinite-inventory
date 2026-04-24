package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseSortMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabaseSortMethodTest {

    @Test
    void shouldHaveSevenSortMethods() {
        assertEquals(7, DatabaseSortMethod.values().length);
    }

    @Test
    void eachSortMethodShouldHaveTranslationKeys() {
        for (DatabaseSortMethod method : DatabaseSortMethod.values()) {
            assertNotNull(method.translationKey());
            assertNotNull(method.buttonTranslationKey());
        }
    }

    @Test
    void orderedValuesShouldMatchDeclarationOrder() {
        assertEquals(DatabaseSortMethod.RECENTLY_CHANGED, DatabaseSortMethod.orderedValues().get(0));
        assertEquals(DatabaseSortMethod.RECENTLY_ADDED, DatabaseSortMethod.orderedValues().get(1));
        assertEquals(DatabaseSortMethod.NAME, DatabaseSortMethod.orderedValues().get(2));
    }
}
