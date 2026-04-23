package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DatabaseTab} 的构造、规范化、属性判断与变换方法测试。
 */
class DatabaseTabTest {

    @Test
    void shouldCreateConcreteTabWithCustomName() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "矿物", "", "minecraft:diamond", false, false);

        assertEquals("tab_abc", tab.id());
        assertEquals("矿物", tab.customName());
        assertEquals("minecraft:diamond", tab.iconItemId());
        assertFalse(tab.systemTab());
        assertFalse(tab.protectedTab());
    }

    @Test
    void shouldNormalizeBlankIdToFallback() {
        DatabaseTab tab = new DatabaseTab("   ", "名称", "", "minecraft:stone", false, false);

        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, tab.id());
    }

    @Test
    void shouldNormalizeNullIdToFallback() {
        DatabaseTab tab = new DatabaseTab(null, "名称", "", "minecraft:stone", false, false);

        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, tab.id());
    }

    @Test
    void shouldNormalizeAllTabIdToAllTab() {
        DatabaseTab tab = new DatabaseTab(DatabaseTabs.ALL_TAB_ID, "", DatabaseTabs.ALL_TAB_TRANSLATION_KEY, "", true, true);

        assertEquals(DatabaseTabs.ALL_TAB_ID, tab.id());
        assertTrue(tab.isAllTab());
        assertTrue(tab.isSystemTab());
        assertFalse(tab.isConcreteTab());
    }

    @Test
    void shouldNormalizeFavoritesTabIdToFavoritesTab() {
        DatabaseTab tab = new DatabaseTab(DatabaseTabs.FAVORITES_TAB_ID, "", DatabaseTabs.FAVORITES_TAB_TRANSLATION_KEY, "", true, true);

        assertEquals(DatabaseTabs.FAVORITES_TAB_ID, tab.id());
        assertTrue(tab.isFavoritesTab());
        assertTrue(tab.isSystemTab());
        assertFalse(tab.isConcreteTab());
    }

    @Test
    void shouldNormalizeBlankCustomNameToEmpty() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "   ", "", "minecraft:stone", false, false);

        assertEquals("", tab.customName());
    }

    @Test
    void shouldNormalizeNullCustomNameToEmpty() {
        DatabaseTab tab = new DatabaseTab("tab_abc", null, "", "minecraft:stone", false, false);

        assertEquals("", tab.customName());
    }

    @Test
    void shouldTrimCustomName() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "  工具  ", "", "minecraft:stone", false, false);

        assertEquals("工具", tab.customName());
    }

    @Test
    void shouldTruncateOverlongCustomName() {
        String longName = "a".repeat(DatabaseTabs.MAX_TAB_NAME_LENGTH + 10);
        DatabaseTab tab = new DatabaseTab("tab_abc", longName, "", "minecraft:stone", false, false);

        assertEquals(DatabaseTabs.MAX_TAB_NAME_LENGTH, tab.customName().length());
    }

    @Test
    void shouldNormalizeBlankIconItemIdForConcreteTab() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "名称", "", "   ", false, false);

        assertEquals(DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID, tab.iconItemId());
    }

    @Test
    void shouldNormalizeBlankIconItemIdForAllTab() {
        DatabaseTab tab = new DatabaseTab(DatabaseTabs.ALL_TAB_ID, "", "", "   ", true, true);

        assertEquals(DatabaseTabs.DEFAULT_ALL_ICON_ITEM_ID, tab.iconItemId());
    }

    @Test
    void shouldNormalizeBlankIconItemIdForFavoritesTab() {
        DatabaseTab tab = new DatabaseTab(DatabaseTabs.FAVORITES_TAB_ID, "", "", "   ", true, true);

        assertEquals(DatabaseTabs.DEFAULT_FAVORITES_ICON_ITEM_ID, tab.iconItemId());
    }

    @Test
    void shouldLowercaseIconItemId() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "名称", "", "MINECRAFT:DIAMOND", false, false);

        assertEquals("minecraft:diamond", tab.iconItemId());
    }

    @Test
    void shouldUseTranslationKeyWhenCustomNameIsBlank() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "", "screen.infiniteinventory.tab.default", "minecraft:stone", false, true);

        assertTrue(tab.usesTranslationKey());
        assertEquals("screen.infiniteinventory.tab.default", tab.displayName());
    }

    @Test
    void shouldUseCustomNameWhenNotBlank() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "建筑", "screen.infiniteinventory.tab.default", "minecraft:stone", false, true);

        assertFalse(tab.usesTranslationKey());
        assertEquals("建筑", tab.displayName());
    }

    @Test
    void systemTabShouldNotBeRenamable() {
        DatabaseTab allTab = DatabaseTabs.allTab();

        assertFalse(allTab.canRename());
    }

    @Test
    void concreteTabShouldBeRenamable() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "旧名", "", "minecraft:stone", false, false);

        assertTrue(tab.canRename());
    }

    @Test
    void protectedConcreteTabShouldNotBeDeletable() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "名称", "", "minecraft:stone", false, true);

        assertFalse(tab.canDelete());
    }

    @Test
    void unprotectedConcreteTabShouldBeDeletable() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "名称", "", "minecraft:stone", false, false);

        assertTrue(tab.canDelete());
    }

    @Test
    void systemTabShouldNotBeDeletable() {
        DatabaseTab allTab = DatabaseTabs.allTab();
        DatabaseTab favTab = DatabaseTabs.favoritesTab();

        assertFalse(allTab.canDelete());
        assertFalse(favTab.canDelete());
    }

    @Test
    void withNameShouldReturnNewTabWithUpdatedName() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "旧名", "", "minecraft:stone", false, false);
        DatabaseTab renamed = tab.withName("新名");

        assertEquals("新名", renamed.customName());
        assertEquals("", renamed.translationKey());
        assertEquals(tab.id(), renamed.id());
    }

    @Test
    void withNameShouldIgnoreBlankName() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "旧名", "", "minecraft:stone", false, false);
        DatabaseTab renamed = tab.withName("   ");

        assertEquals(tab, renamed);
    }

    @Test
    void withNameShouldIgnoreSystemTab() {
        DatabaseTab allTab = DatabaseTabs.allTab();
        DatabaseTab renamed = allTab.withName("新名");

        assertEquals(allTab, renamed);
    }

    @Test
    void withIconItemIdShouldReturnNewTabWithUpdatedIcon() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "名称", "", "minecraft:stone", false, false);
        DatabaseTab updated = tab.withIconItemId("minecraft:diamond");

        assertEquals("minecraft:diamond", updated.iconItemId());
        assertEquals(tab.id(), updated.id());
    }

    @Test
    void withIconItemIdShouldNormalizeBlankToDefaultForConcrete() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "名称", "", "minecraft:stone", false, false);
        DatabaseTab updated = tab.withIconItemId("   ");

        assertEquals(DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID, updated.iconItemId());
    }

    @Test
    void toTagAndFromTagShouldBeSymmetric() {
        DatabaseTab original = new DatabaseTab("tab_abc", "测试", "", "minecraft:diamond", false, false);
        CompoundTag tag = original.toTag();
        DatabaseTab restored = DatabaseTab.fromTag(tag);

        assertEquals(original.id(), restored.id());
        assertEquals(original.customName(), restored.customName());
        assertEquals(original.translationKey(), restored.translationKey());
        assertEquals(original.iconItemId(), restored.iconItemId());
        assertEquals(original.systemTab(), restored.systemTab());
        assertEquals(original.protectedTab(), restored.protectedTab());
    }

    @Test
    void fromTagShouldReturnDefaultConcreteTabWhenTagIsNull() {
        DatabaseTab tab = DatabaseTab.fromTag(null);

        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, tab.id());
    }

    @Test
    void fromTagShouldReturnDefaultConcreteTabWhenTagIsEmpty() {
        DatabaseTab tab = DatabaseTab.fromTag(new CompoundTag());

        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, tab.id());
    }

    @Test
    void shouldPreserveProtectedFlag() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "名称", "", "minecraft:stone", false, true);

        assertTrue(tab.protectedTab());
    }

    @Test
    void shouldPreserveSystemTabFlag() {
        DatabaseTab tab = new DatabaseTab(DatabaseTabs.ALL_TAB_ID, "", "", "", true, true);

        assertTrue(tab.systemTab());
        assertTrue(tab.isSystemTab());
    }

    @Test
    void concreteTabShouldNotBeSystemTab() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "名称", "", "minecraft:stone", false, false);

        assertFalse(tab.isSystemTab());
        assertTrue(tab.isConcreteTab());
    }

    @Test
    void shouldNormalizeNullTranslationKeyToEmpty() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "名称", null, "minecraft:stone", false, false);

        assertEquals("", tab.translationKey());
    }

    @Test
    void shouldTrimTranslationKey() {
        DatabaseTab tab = new DatabaseTab("tab_abc", "", "  key  ", "minecraft:stone", false, false);

        assertEquals("key", tab.translationKey());
    }
}
