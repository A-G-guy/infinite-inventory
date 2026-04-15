package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTabDirectoryTest {
    @Test
    void defaultConcreteTabShouldKeepBuiltinDefaultName() {
        DatabaseTabDirectory directory = new DatabaseTabDirectory();

        DatabaseTab defaultTab = directory.defaultConcreteTab();

        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, defaultTab.id());
        assertEquals(DatabaseTabs.DEFAULT_TAB_TRANSLATION_KEY, defaultTab.translationKey());
        assertTrue(defaultTab.customName().isBlank());
        assertFalse(defaultTab.systemTab());
        assertTrue(defaultTab.protectedTab());
    }

    @Test
    void addCustomTabShouldUseNewCategoryTranslationWhenNameIsBlank() {
        DatabaseTabDirectory directory = new DatabaseTabDirectory();

        DatabaseTab newTab = directory.addCustomTab("   ", DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID);

        assertFalse(newTab.systemTab());
        assertFalse(newTab.protectedTab());
        assertTrue(newTab.customName().isBlank());
        assertEquals(DatabaseTabs.NEW_CUSTOM_TAB_TRANSLATION_KEY, newTab.translationKey());
    }

    @Test
    void addCustomTabShouldKeepExplicitCustomName() {
        DatabaseTabDirectory directory = new DatabaseTabDirectory();

        DatabaseTab newTab = directory.addCustomTab("矿物", DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID);

        assertEquals("矿物", newTab.customName());
        assertTrue(newTab.translationKey().isBlank());
    }
}
