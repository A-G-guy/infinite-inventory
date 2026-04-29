package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalDatabaseScreenTabContextMenuBuilderTest {

    private static DatabaseTab concreteTab(String id, String name) {
        return new DatabaseTab(id, name, "", DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID, false, false);
    }

    private static DatabaseTab systemTab(String id, String translationKey) {
        return new DatabaseTab(id, "", translationKey, DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID, true, false);
    }

    private static List<PersonalDatabaseScreenTabContextMenuItem> nonNullItems(
            List<PersonalDatabaseScreenTabContextMenuItem> items
    ) {
        return items.stream().filter(i -> i != null).toList();
    }

    private static boolean hasAction(
            List<PersonalDatabaseScreenTabContextMenuItem> items,
            PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction action
    ) {
        return nonNullItems(items).stream().anyMatch(i -> i.action() == action);
    }

    private static boolean hasTranslationKey(
            List<PersonalDatabaseScreenTabContextMenuItem> items,
            String key
    ) {
        return nonNullItems(items).stream().anyMatch(i -> i.translationKey().equals(key));
    }

    // ========== 视图操作项 ==========

    @Test
    void notInViewAndUnderCapacityShouldShowJoinOnly() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = PersonalDatabaseScreenTabContextMenuBuilder.buildMenuItems(
                concreteTab("t1", "Tab1"),
                false, 1, 4, false, false, false
        );

        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.JOIN_CURRENT_VIEW));
        assertFalse(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.REMOVE_FROM_VIEW));
        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.SINGLE_VIEW));
    }

    @Test
    void inViewAndMoreThanOneShouldShowRemoveOnly() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = PersonalDatabaseScreenTabContextMenuBuilder.buildMenuItems(
                concreteTab("t1", "Tab1"),
                true, 2, 4, false, false, false
        );

        assertFalse(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.JOIN_CURRENT_VIEW));
        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.REMOVE_FROM_VIEW));
        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.SINGLE_VIEW));
    }

    @Test
    void inViewAndOnlyOneShouldShowNeitherJoinNorRemove() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = PersonalDatabaseScreenTabContextMenuBuilder.buildMenuItems(
                concreteTab("t1", "Tab1"),
                true, 1, 4, false, false, false
        );

        assertFalse(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.JOIN_CURRENT_VIEW));
        assertFalse(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.REMOVE_FROM_VIEW));
        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.SINGLE_VIEW));
    }

    @Test
    void notInViewAndAtCapacityShouldShowNeitherJoinNorRemove() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = PersonalDatabaseScreenTabContextMenuBuilder.buildMenuItems(
                concreteTab("t1", "Tab1"),
                false, 4, 4, false, false, false
        );

        assertFalse(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.JOIN_CURRENT_VIEW));
        assertFalse(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.REMOVE_FROM_VIEW));
        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.SINGLE_VIEW));
    }

    // ========== 移动项 ==========

    @Test
    void canMoveLeftShouldShowLeftOnly() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = PersonalDatabaseScreenTabContextMenuBuilder.buildMenuItems(
                concreteTab("t1", "Tab1"),
                true, 1, 4, true, false, false
        );

        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.MOVE_LEFT));
        assertFalse(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.MOVE_RIGHT));
    }

    @Test
    void canMoveRightShouldShowRightOnly() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = PersonalDatabaseScreenTabContextMenuBuilder.buildMenuItems(
                concreteTab("t1", "Tab1"),
                true, 1, 4, false, true, false
        );

        assertFalse(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.MOVE_LEFT));
        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.MOVE_RIGHT));
    }

    @Test
    void canMoveBothShouldShowBoth() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = PersonalDatabaseScreenTabContextMenuBuilder.buildMenuItems(
                concreteTab("t1", "Tab1"),
                true, 1, 4, true, true, false
        );

        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.MOVE_LEFT));
        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.MOVE_RIGHT));
    }

    @Test
    void cannotMoveShouldShowNeither() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = PersonalDatabaseScreenTabContextMenuBuilder.buildMenuItems(
                concreteTab("t1", "Tab1"),
                true, 1, 4, false, false, false
        );

        assertFalse(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.MOVE_LEFT));
        assertFalse(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.MOVE_RIGHT));
    }

    // ========== 编辑项 ==========

    @Test
    void concreteTabShouldShowRenameAndChangeIcon() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = PersonalDatabaseScreenTabContextMenuBuilder.buildMenuItems(
                concreteTab("t1", "Tab1"),
                true, 1, 4, false, false, false
        );

        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.RENAME));
        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.CHANGE_ICON));
    }

    @Test
    void systemTabShouldNotShowRenameButShowChangeIcon() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = PersonalDatabaseScreenTabContextMenuBuilder.buildMenuItems(
                systemTab("__all", "screen.infiniteinventory.tab.all"),
                true, 1, 4, false, false, false
        );

        assertFalse(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.RENAME));
        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.CHANGE_ICON));
    }

    // ========== 显示控制项 ==========

    @Test
    void hiddenTabShouldShowShowInTop() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = PersonalDatabaseScreenTabContextMenuBuilder.buildMenuItems(
                concreteTab("t1", "Tab1"),
                true, 1, 4, false, false, true
        );

        assertTrue(hasTranslationKey(items, "screen.infiniteinventory.tab_context.show_in_top"));
        assertFalse(hasTranslationKey(items, "screen.infiniteinventory.tab_context.hide_in_top"));
        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.TOGGLE_TOP_VISIBILITY));
    }

    @Test
    void visibleTabShouldShowHideInTop() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = PersonalDatabaseScreenTabContextMenuBuilder.buildMenuItems(
                concreteTab("t1", "Tab1"),
                true, 1, 4, false, false, false
        );

        assertTrue(hasTranslationKey(items, "screen.infiniteinventory.tab_context.hide_in_top"));
        assertFalse(hasTranslationKey(items, "screen.infiniteinventory.tab_context.show_in_top"));
        assertTrue(hasAction(items, PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.TOGGLE_TOP_VISIBILITY));
    }

    // ========== 分隔线 ==========

    @Test
    void menuShouldContainNoSeparators() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = PersonalDatabaseScreenTabContextMenuBuilder.buildMenuItems(
                concreteTab("t1", "Tab1"),
                true, 2, 4, true, true, false
        );

        for (PersonalDatabaseScreenTabContextMenuItem item : items) {
            assertNotNull(item);
        }
    }

    @Test
    void fullMenuShouldContainAllExpectedItems() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = PersonalDatabaseScreenTabContextMenuBuilder.buildMenuItems(
                concreteTab("t1", "Tab1"),
                true, 2, 4, true, true, false
        );

        List<PersonalDatabaseScreenTabContextMenuItem> nonNull = nonNullItems(items);
        assertEquals(7, nonNull.size());
        assertEquals(PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.REMOVE_FROM_VIEW, nonNull.get(0).action());
        assertEquals(PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.SINGLE_VIEW, nonNull.get(1).action());
        assertEquals(PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.MOVE_LEFT, nonNull.get(2).action());
        assertEquals(PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.MOVE_RIGHT, nonNull.get(3).action());
        assertEquals(PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.RENAME, nonNull.get(4).action());
        assertEquals(PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.CHANGE_ICON, nonNull.get(5).action());
        assertEquals(PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.TOGGLE_TOP_VISIBILITY, nonNull.get(6).action());
    }

    // ========== 菜单尺寸 ==========

    @Test
    void menuHeightShouldCountAllItemsIncludingSeparators() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = java.util.Arrays.asList(
                new PersonalDatabaseScreenTabContextMenuItem("a", PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.SINGLE_VIEW),
                null,
                new PersonalDatabaseScreenTabContextMenuItem("b", PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.RENAME)
        );

        int expected = 3 * PersonalDatabaseScreen.CONTEXT_MENU_ROW_HEIGHT + 4;
        assertEquals(expected, PersonalDatabaseScreenTabContextMenuBuilder.menuHeight(items));
    }

    @Test
    void menuHeightShouldBeFourForEmptyList() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = List.of();
        assertEquals(4, PersonalDatabaseScreenTabContextMenuBuilder.menuHeight(items));
    }

    @Test
    void menuHeightShouldHandleAllSeparators() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = java.util.Arrays.asList(null, null, null);
        int expected = 3 * PersonalDatabaseScreen.CONTEXT_MENU_ROW_HEIGHT + 4;
        assertEquals(expected, PersonalDatabaseScreenTabContextMenuBuilder.menuHeight(items));
    }

    @Test
    void menuWidthShouldReturnAtLeastMinWidth() {
        List<PersonalDatabaseScreenTabContextMenuItem> items = List.of(
                new PersonalDatabaseScreenTabContextMenuItem("a", PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.SINGLE_VIEW)
        );

        assertTrue(PersonalDatabaseScreenTabContextMenuBuilder.menuWidth(null, items) >= PersonalDatabaseScreen.CONTEXT_MENU_MIN_WIDTH);
    }
}
