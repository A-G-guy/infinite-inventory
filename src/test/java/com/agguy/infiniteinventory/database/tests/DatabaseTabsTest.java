package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DatabaseTabs} 工具类的常量、规范化方法与默认页签构造测试。
 */
class DatabaseTabsTest {

    @Test
    void allTabShouldHaveCorrectProperties() {
        DatabaseTab tab = DatabaseTabs.allTab();

        assertEquals(DatabaseTabs.ALL_TAB_ID, tab.id());
        assertEquals(DatabaseTabs.ALL_TAB_TRANSLATION_KEY, tab.translationKey());
        assertEquals(DatabaseTabs.DEFAULT_ALL_ICON_ITEM_ID, tab.iconItemId());
        assertTrue(tab.systemTab());
        assertTrue(tab.protectedTab());
        assertTrue(tab.isAllTab());
        assertFalse(tab.isFavoritesTab());
    }

    @Test
    void favoritesTabShouldHaveCorrectProperties() {
        DatabaseTab tab = DatabaseTabs.favoritesTab();

        assertEquals(DatabaseTabs.FAVORITES_TAB_ID, tab.id());
        assertEquals(DatabaseTabs.FAVORITES_TAB_TRANSLATION_KEY, tab.translationKey());
        assertEquals(DatabaseTabs.DEFAULT_FAVORITES_ICON_ITEM_ID, tab.iconItemId());
        assertTrue(tab.systemTab());
        assertTrue(tab.protectedTab());
        assertTrue(tab.isFavoritesTab());
        assertFalse(tab.isAllTab());
    }

    @Test
    void defaultConcreteTabShouldHaveCorrectProperties() {
        DatabaseTab tab = DatabaseTabs.defaultConcreteTab();

        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, tab.id());
        assertEquals(DatabaseTabs.DEFAULT_TAB_TRANSLATION_KEY, tab.translationKey());
        assertEquals(DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID, tab.iconItemId());
        assertFalse(tab.systemTab());
        assertTrue(tab.protectedTab());
        assertTrue(tab.isConcreteTab());
    }

    @Test
    void isReservedIdShouldMatchAllDefaultAndFavorites() {
        assertTrue(DatabaseTabs.isReservedId(DatabaseTabs.ALL_TAB_ID));
        assertTrue(DatabaseTabs.isReservedId(DatabaseTabs.FAVORITES_TAB_ID));
        assertTrue(DatabaseTabs.isReservedId(DatabaseTabs.DEFAULT_TAB_ID));
        assertFalse(DatabaseTabs.isReservedId("custom_tab"));
        assertFalse(DatabaseTabs.isReservedId(""));
    }

    @Test
    void isAllTabIdShouldOnlyMatchAllTabId() {
        assertTrue(DatabaseTabs.isAllTabId(DatabaseTabs.ALL_TAB_ID));
        assertFalse(DatabaseTabs.isAllTabId(DatabaseTabs.FAVORITES_TAB_ID));
        assertFalse(DatabaseTabs.isAllTabId("other"));
    }

    @Test
    void isFavoritesTabIdShouldOnlyMatchFavoritesTabId() {
        assertTrue(DatabaseTabs.isFavoritesTabId(DatabaseTabs.FAVORITES_TAB_ID));
        assertFalse(DatabaseTabs.isFavoritesTabId(DatabaseTabs.ALL_TAB_ID));
        assertFalse(DatabaseTabs.isFavoritesTabId("other"));
    }

    @Test
    void isSystemTabIdShouldMatchAllAndFavorites() {
        assertTrue(DatabaseTabs.isSystemTabId(DatabaseTabs.ALL_TAB_ID));
        assertTrue(DatabaseTabs.isSystemTabId(DatabaseTabs.FAVORITES_TAB_ID));
        assertFalse(DatabaseTabs.isSystemTabId(DatabaseTabs.DEFAULT_TAB_ID));
        assertFalse(DatabaseTabs.isSystemTabId("custom"));
    }

    @Test
    void isConcreteTabIdShouldRejectSystemAndBlank() {
        assertFalse(DatabaseTabs.isConcreteTabId(DatabaseTabs.ALL_TAB_ID));
        assertFalse(DatabaseTabs.isConcreteTabId(DatabaseTabs.FAVORITES_TAB_ID));
        assertFalse(DatabaseTabs.isConcreteTabId(null));
        assertFalse(DatabaseTabs.isConcreteTabId(""));
        assertFalse(DatabaseTabs.isConcreteTabId("   "));
        assertTrue(DatabaseTabs.isConcreteTabId("custom_tab"));
    }

    @Test
    void normalizeTabIdShouldReturnFallbackWhenNull() {
        assertEquals("fallback", DatabaseTabs.normalizeTabId(null, "fallback"));
    }

    @Test
    void normalizeTabIdShouldReturnFallbackWhenBlank() {
        assertEquals("fallback", DatabaseTabs.normalizeTabId("   ", "fallback"));
        assertEquals("fallback", DatabaseTabs.normalizeTabId("", "fallback"));
    }

    @Test
    void normalizeTabIdShouldTrimAndPreserveNonBlank() {
        assertEquals("tab_abc", DatabaseTabs.normalizeTabId("  tab_abc  ", "fallback"));
    }

    @Test
    void normalizeConcreteTargetShouldReturnDefaultForInvalid() {
        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, DatabaseTabs.normalizeConcreteTarget(null));
        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, DatabaseTabs.normalizeConcreteTarget(""));
        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, DatabaseTabs.normalizeConcreteTarget(DatabaseTabs.ALL_TAB_ID));
        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, DatabaseTabs.normalizeConcreteTarget(DatabaseTabs.FAVORITES_TAB_ID));
    }

    @Test
    void normalizeConcreteTargetShouldTrimValidId() {
        assertEquals("custom", DatabaseTabs.normalizeConcreteTarget("  custom  "));
    }

    @Test
    void normalizeTabNameShouldReturnEmptyWhenNull() {
        assertEquals("", DatabaseTabs.normalizeTabName(null, "fallback"));
    }

    @Test
    void normalizeTabNameShouldReturnEmptyWhenBlank() {
        assertEquals("", DatabaseTabs.normalizeTabName("   ", "fallback"));
        assertEquals("", DatabaseTabs.normalizeTabName("", "fallback"));
    }

    @Test
    void normalizeTabNameShouldTrimInput() {
        assertEquals("名称", DatabaseTabs.normalizeTabName("  名称  ", "fallback"));
    }

    @Test
    void normalizeTabNameShouldTruncateOverlongName() {
        String longName = "a".repeat(DatabaseTabs.MAX_TAB_NAME_LENGTH + 5);
        String result = DatabaseTabs.normalizeTabName(longName, "fallback");

        assertEquals(DatabaseTabs.MAX_TAB_NAME_LENGTH, result.length());
    }

    @Test
    void normalizeTabNameShouldPreserveShortName() {
        assertEquals("短名", DatabaseTabs.normalizeTabName("短名", "fallback"));
    }

    @Test
    void normalizeIconItemIdShouldReturnDefaultForAllTabWhenBlank() {
        assertEquals(DatabaseTabs.DEFAULT_ALL_ICON_ITEM_ID, DatabaseTabs.normalizeIconItemId("", true, false));
        assertEquals(DatabaseTabs.DEFAULT_ALL_ICON_ITEM_ID, DatabaseTabs.normalizeIconItemId("   ", true, false));
    }

    @Test
    void normalizeIconItemIdShouldReturnDefaultForFavoritesTabWhenBlank() {
        assertEquals(DatabaseTabs.DEFAULT_FAVORITES_ICON_ITEM_ID, DatabaseTabs.normalizeIconItemId("", false, true));
        assertEquals(DatabaseTabs.DEFAULT_FAVORITES_ICON_ITEM_ID, DatabaseTabs.normalizeIconItemId("   ", false, true));
    }

    @Test
    void normalizeIconItemIdShouldReturnDefaultConcreteWhenBlank() {
        assertEquals(DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID, DatabaseTabs.normalizeIconItemId("", false, false));
        assertEquals(DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID, DatabaseTabs.normalizeIconItemId("   ", false, false));
    }

    @Test
    void normalizeIconItemIdShouldLowercaseInput() {
        assertEquals("minecraft:diamond", DatabaseTabs.normalizeIconItemId("MINECRAFT:DIAMOND", false, false));
    }

    @Test
    void normalizeIconItemIdShouldTrimInput() {
        assertEquals("minecraft:diamond", DatabaseTabs.normalizeIconItemId("  minecraft:diamond  ", false, false));
    }

    @Test
    void normalizeIconItemIdOverloadShouldDelegateCorrectly() {
        assertEquals(DatabaseTabs.DEFAULT_ALL_ICON_ITEM_ID, DatabaseTabs.normalizeIconItemId("", true));
        assertEquals(DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID, DatabaseTabs.normalizeIconItemId("", false));
    }

    @Test
    void newCustomTabIdShouldStartWithTabPrefix() {
        String id = DatabaseTabs.newCustomTabId();

        assertNotNull(id);
        assertTrue(id.startsWith("tab_"));
        assertTrue(id.length() > "tab_".length());
    }

    @Test
    void newCustomTabIdShouldNotContainHyphens() {
        String id = DatabaseTabs.newCustomTabId();

        assertFalse(id.contains("-"));
    }

    @Test
    void normalizeVisibleTabIdsShouldReturnFallbackWhenInputIsNull() {
        DatabaseTabDirectory directory = new DatabaseTabDirectory();
        List<String> result = DatabaseTabs.normalizeVisibleTabIds(null, directory, DatabaseTabs.ALL_TAB_ID);

        assertFalse(result.isEmpty());
        assertEquals(DatabaseTabs.ALL_TAB_ID, result.getFirst());
    }

    @Test
    void normalizeVisibleTabIdsShouldLimitToMaxCount() {
        DatabaseTabDirectory directory = new DatabaseTabDirectory();
        directory.addCustomTab("A", "minecraft:stone");
        directory.addCustomTab("B", "minecraft:stone");
        directory.addCustomTab("C", "minecraft:stone");
        directory.addCustomTab("D", "minecraft:stone");
        directory.addCustomTab("E", "minecraft:stone");

        List<String> requested = directory.orderedTabs().stream().map(DatabaseTab::id).toList();
        List<String> result = DatabaseTabs.normalizeVisibleTabIds(requested, directory, DatabaseTabs.ALL_TAB_ID);

        assertTrue(result.size() <= DatabaseTabs.MAX_VISIBLE_TAB_COUNT);
    }

    @Test
    void normalizeVisibleTabIdsShouldIgnoreUnknownTabIds() {
        DatabaseTabDirectory directory = new DatabaseTabDirectory();
        List<String> result = DatabaseTabs.normalizeVisibleTabIds(
                List.of("unknown_tab"), directory, DatabaseTabs.ALL_TAB_ID
        );

        assertFalse(result.isEmpty());
        assertEquals(DatabaseTabs.ALL_TAB_ID, result.getFirst());
    }

    @Test
    void normalizeVisibleTabIdsShouldPreserveOrderFromDirectory() {
        DatabaseTabDirectory directory = new DatabaseTabDirectory();
        DatabaseTab tab1 = directory.addCustomTab("A", "minecraft:stone");
        DatabaseTab tab2 = directory.addCustomTab("B", "minecraft:stone");

        List<String> requested = List.of(tab2.id(), tab1.id());
        List<String> result = DatabaseTabs.normalizeVisibleTabIds(requested, directory, DatabaseTabs.ALL_TAB_ID);

        // 结果顺序应与 directory.orderedTabs() 一致，而非请求顺序
        int index1 = result.indexOf(tab1.id());
        int index2 = result.indexOf(tab2.id());
        assertTrue(index1 < index2, "结果顺序应与目录顺序一致");
    }

    @Test
    void maxVisibleTabCountShouldBeFour() {
        assertEquals(4, DatabaseTabs.MAX_VISIBLE_TAB_COUNT);
    }

    @Test
    void maxTabNameLengthShouldBeThirtyTwo() {
        assertEquals(32, DatabaseTabs.MAX_TAB_NAME_LENGTH);
    }
}
