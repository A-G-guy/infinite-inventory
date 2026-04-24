package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.service.search.DatabaseSearchFilterType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DatabaseSearchFilterTypeTest {

    @Test
    void shouldHaveFourTypes() {
        assertEquals(4, DatabaseSearchFilterType.values().length);
    }

    @Test
    void modShouldHaveAtPrefix() {
        assertEquals('@', DatabaseSearchFilterType.MOD.prefix());
    }

    @Test
    void tagShouldHaveHashPrefix() {
        assertEquals('#', DatabaseSearchFilterType.TAG.prefix());
    }

    @Test
    void itemIdShouldHaveAmpersandPrefix() {
        assertEquals('&', DatabaseSearchFilterType.ITEM_ID.prefix());
    }

    @Test
    void creativeTabShouldHavePercentPrefix() {
        assertEquals('%', DatabaseSearchFilterType.CREATIVE_TAB.prefix());
    }

    @Test
    void fromPrefixShouldReturnCorrectType() {
        assertEquals(DatabaseSearchFilterType.MOD, DatabaseSearchFilterType.fromPrefix('@'));
        assertEquals(DatabaseSearchFilterType.TAG, DatabaseSearchFilterType.fromPrefix('#'));
        assertEquals(DatabaseSearchFilterType.ITEM_ID, DatabaseSearchFilterType.fromPrefix('&'));
        assertEquals(DatabaseSearchFilterType.CREATIVE_TAB, DatabaseSearchFilterType.fromPrefix('%'));
    }

    @Test
    void fromPrefixShouldReturnNullForUnknownPrefix() {
        assertNull(DatabaseSearchFilterType.fromPrefix('!'));
        assertNull(DatabaseSearchFilterType.fromPrefix('$'));
        assertNull(DatabaseSearchFilterType.fromPrefix('^'));
    }
}
