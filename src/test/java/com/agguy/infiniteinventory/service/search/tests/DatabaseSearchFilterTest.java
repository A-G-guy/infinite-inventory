package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.service.search.DatabaseSearchFilter;
import com.agguy.infiniteinventory.service.search.DatabaseSearchFilterType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DatabaseSearchFilter 单元测试。
 */
class DatabaseSearchFilterTest {

    @Test
    void nullTypeShouldDefaultToMod() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(null, "value");
        assertEquals(DatabaseSearchFilterType.MOD, filter.type());
    }

    @Test
    void nullValueShouldDefaultToEmptyString() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.TAG, null);
        assertEquals("", filter.value());
    }

    @Test
    void valueShouldBeTrimmed() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.MOD, "  applied energistics 2  ");
        assertEquals("applied energistics 2", filter.value());
    }

    @Test
    void meaningfulModFilterShouldReturnTrue() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.MOD, "minecraft");
        assertTrue(filter.isMeaningful());
    }

    @Test
    void emptyModFilterShouldReturnFalse() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.MOD, "");
        assertFalse(filter.isMeaningful());
    }

    @Test
    void whitespaceOnlyModFilterShouldReturnFalse() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.MOD, "   ");
        assertFalse(filter.isMeaningful());
    }

    @Test
    void meaningfulTagFilterShouldReturnTrue() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.TAG, "minecraft:logs");
        assertTrue(filter.isMeaningful());
    }

    @Test
    void emptyTagFilterShouldReturnFalse() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.TAG, "");
        assertFalse(filter.isMeaningful());
    }

    @Test
    void meaningfulItemIdFilterShouldReturnTrue() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.ITEM_ID, "minecraft:diamond");
        assertTrue(filter.isMeaningful());
    }

    @Test
    void meaningfulCreativeTabFilterShouldReturnTrue() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.CREATIVE_TAB, "building blocks");
        assertTrue(filter.isMeaningful());
    }

    @Test
    void canonicalTokenForModShouldIncludePrefix() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.MOD, "minecraft");
        assertEquals("@minecraft", filter.canonicalToken());
    }

    @Test
    void canonicalTokenForTagShouldIncludePrefix() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.TAG, "minecraft:logs");
        assertEquals("#minecraft:logs", filter.canonicalToken());
    }

    @Test
    void canonicalTokenForItemIdShouldIncludePrefix() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.ITEM_ID, "diamond_sword");
        assertEquals("&diamond_sword", filter.canonicalToken());
    }

    @Test
    void canonicalTokenForCreativeTabShouldIncludePrefix() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.CREATIVE_TAB, "building blocks");
        assertEquals("%\"building blocks\"", filter.canonicalToken());
    }

    @Test
    void canonicalTokenForEmptyValueShouldReturnEmptyString() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.MOD, "");
        assertEquals("", filter.canonicalToken());
    }

    @Test
    void canonicalTokenShouldNormalizeValue() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(DatabaseSearchFilterType.MOD, "  Applied Energistics 2  ");
        assertEquals("@\"applied energistics 2\"", filter.canonicalToken());
    }
}
