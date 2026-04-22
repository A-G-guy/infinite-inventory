package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import java.util.List;
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

    @Test
    void orderedTabsShouldIncludeSystemTabsAtFront() {
        DatabaseTabDirectory directory = new DatabaseTabDirectory();

        List<DatabaseTab> tabs = directory.orderedTabs();

        assertTrue(tabs.size() >= 3);
        assertEquals(DatabaseTabs.ALL_TAB_ID, tabs.get(0).id());
        assertEquals(DatabaseTabs.FAVORITES_TAB_ID, tabs.get(1).id());
        assertTrue(tabs.get(0).systemTab());
        assertTrue(tabs.get(1).systemTab());
    }

    @Test
    void systemTabShouldSupportMoveRight() {
        DatabaseTabDirectory directory = new DatabaseTabDirectory();

        boolean moved = directory.moveTab(DatabaseTabs.ALL_TAB_ID, 1);

        assertTrue(moved);
        List<DatabaseTab> tabs = directory.orderedTabs();
        assertEquals(DatabaseTabs.FAVORITES_TAB_ID, tabs.get(0).id());
        assertEquals(DatabaseTabs.ALL_TAB_ID, tabs.get(1).id());
    }

    @Test
    void systemTabShouldSupportMoveLeft() {
        DatabaseTabDirectory directory = new DatabaseTabDirectory();
        directory.moveTab(DatabaseTabs.ALL_TAB_ID, 1);

        boolean moved = directory.moveTab(DatabaseTabs.ALL_TAB_ID, -1);

        assertTrue(moved);
        List<DatabaseTab> tabs = directory.orderedTabs();
        assertEquals(DatabaseTabs.ALL_TAB_ID, tabs.get(0).id());
        assertEquals(DatabaseTabs.FAVORITES_TAB_ID, tabs.get(1).id());
    }

    @Test
    void systemTabShouldSupportIconUpdate() {
        DatabaseTabDirectory directory = new DatabaseTabDirectory();

        boolean updated = directory.updateTabIcon(DatabaseTabs.ALL_TAB_ID, "minecraft:diamond");

        assertTrue(updated);
        assertEquals("minecraft:diamond", directory.find(DatabaseTabs.ALL_TAB_ID).orElseThrow().iconItemId());
    }

    @Test
    void systemTabShouldNotBeDeletable() {
        DatabaseTabDirectory directory = new DatabaseTabDirectory();

        boolean deleted = directory.deleteTab(DatabaseTabs.ALL_TAB_ID);

        assertFalse(deleted);
        assertTrue(directory.contains(DatabaseTabs.ALL_TAB_ID));
    }

    @Test
    void fromTagShouldEnsureSystemTabsWhenMissing() {
        net.minecraft.nbt.CompoundTag legacyTag = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.ListTag tabsTag = new net.minecraft.nbt.ListTag();
        net.minecraft.nbt.CompoundTag defaultTabTag = new net.minecraft.nbt.CompoundTag();
        defaultTabTag.putString("id", DatabaseTabs.DEFAULT_TAB_ID);
        defaultTabTag.putString("custom_name", "");
        defaultTabTag.putString("translation_key", DatabaseTabs.DEFAULT_TAB_TRANSLATION_KEY);
        defaultTabTag.putString("icon_item_id", DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID);
        defaultTabTag.putBoolean("system_tab", false);
        defaultTabTag.putBoolean("protected_tab", true);
        tabsTag.add(defaultTabTag);
        legacyTag.put("tabs", tabsTag);

        DatabaseTabDirectory directory = DatabaseTabDirectory.fromTag(legacyTag);

        List<DatabaseTab> tabs = directory.orderedTabs();
        assertTrue(tabs.size() >= 3);
        assertEquals(DatabaseTabs.ALL_TAB_ID, tabs.get(0).id());
        assertEquals(DatabaseTabs.FAVORITES_TAB_ID, tabs.get(1).id());
        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, tabs.get(2).id());
    }

    @Test
    void fromTagShouldPreserveCustomSystemTabIcons() {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.ListTag tabsTag = new net.minecraft.nbt.ListTag();
        net.minecraft.nbt.CompoundTag allTabTag = new net.minecraft.nbt.CompoundTag();
        allTabTag.putString("id", DatabaseTabs.ALL_TAB_ID);
        allTabTag.putString("custom_name", "");
        allTabTag.putString("translation_key", DatabaseTabs.ALL_TAB_TRANSLATION_KEY);
        allTabTag.putString("icon_item_id", "minecraft:diamond");
        allTabTag.putBoolean("system_tab", true);
        allTabTag.putBoolean("protected_tab", true);
        tabsTag.add(allTabTag);
        net.minecraft.nbt.CompoundTag favTabTag = new net.minecraft.nbt.CompoundTag();
        favTabTag.putString("id", DatabaseTabs.FAVORITES_TAB_ID);
        favTabTag.putString("custom_name", "");
        favTabTag.putString("translation_key", DatabaseTabs.FAVORITES_TAB_TRANSLATION_KEY);
        favTabTag.putString("icon_item_id", "minecraft:emerald");
        favTabTag.putBoolean("system_tab", true);
        favTabTag.putBoolean("protected_tab", true);
        tabsTag.add(favTabTag);
        tag.put("tabs", tabsTag);

        DatabaseTabDirectory directory = DatabaseTabDirectory.fromTag(tag);

        assertEquals("minecraft:diamond", directory.find(DatabaseTabs.ALL_TAB_ID).orElseThrow().iconItemId());
        assertEquals("minecraft:emerald", directory.find(DatabaseTabs.FAVORITES_TAB_ID).orElseThrow().iconItemId());
    }
}
