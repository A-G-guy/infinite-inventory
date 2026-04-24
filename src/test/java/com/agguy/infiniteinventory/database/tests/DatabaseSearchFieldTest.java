package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSearchFieldTest {

    @Test
    void shouldHaveSixSearchFields() {
        assertEquals(6, DatabaseSearchField.values().length);
    }

    @Test
    void eachFieldShouldHaveTranslationKey() {
        for (DatabaseSearchField field : DatabaseSearchField.values()) {
            assertNotNull(field.translationKey());
            assertNotNull(field.defaultWeight());
        }
    }

    @Test
    void displayNameShouldBeTextField() {
        assertTrue(DatabaseSearchField.DISPLAY_NAME.isTextField());
    }

    @Test
    void itemIdShouldBeTextField() {
        assertTrue(DatabaseSearchField.ITEM_ID.isTextField());
    }

    @Test
    void countBoostShouldNotBeTextField() {
        assertFalse(DatabaseSearchField.COUNT_BOOST.isTextField());
    }

    @Test
    void displayNameShouldHaveHighWeight() {
        assertEquals(DatabaseSearchWeight.HIGH, DatabaseSearchField.DISPLAY_NAME.defaultWeight());
    }

    @Test
    void modNamespaceShouldHaveLowWeight() {
        assertEquals(DatabaseSearchWeight.LOW, DatabaseSearchField.MOD_NAMESPACE.defaultWeight());
    }

    @Test
    void countBoostShouldHaveLowWeight() {
        assertEquals(DatabaseSearchWeight.LOW, DatabaseSearchField.COUNT_BOOST.defaultWeight());
    }
}
