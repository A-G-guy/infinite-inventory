package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseTab;
import java.util.ArrayList;
import java.util.List;

final class PersonalDatabaseScreenTabContextMenuBuilder {
    private PersonalDatabaseScreenTabContextMenuBuilder() {
    }

    static List<PersonalDatabaseScreenTabContextMenuItem> buildMenuItems(PersonalDatabaseScreen screen, DatabaseScopedTabRef scopedTab) {
        List<PersonalDatabaseScreenTabContextMenuItem> items = new ArrayList<>();
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        DatabaseTab tab = PersonalDatabaseScreenCommonHelper.findTab(screen, scopedTab);

        boolean inVisibleTabs = query.visibleTabs().contains(scopedTab);
        boolean canJoin = !inVisibleTabs
                && query.visibleTabs().size() < PersonalDatabaseScreenCommonHelper.maxVisiblePanels(screen);
        if (canJoin) {
            items.add(new PersonalDatabaseScreenTabContextMenuItem(
                    "screen.infiniteinventory.tab_context.join_current",
                    PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.JOIN_CURRENT_VIEW
            ));
        }

        items.add(new PersonalDatabaseScreenTabContextMenuItem(
                "screen.infiniteinventory.tab_context.single_view",
                PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.SINGLE_VIEW
        ));

        items.add(null);

        boolean canMoveLeft = PersonalDatabaseScreenManagementLogic.canMoveManagementTab(
                screen, scopedTab.scope(), tab, -1
        );
        if (canMoveLeft) {
            items.add(new PersonalDatabaseScreenTabContextMenuItem(
                    "screen.infiniteinventory.tab_context.move_left",
                    PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.MOVE_LEFT
            ));
        }

        boolean canMoveRight = PersonalDatabaseScreenManagementLogic.canMoveManagementTab(
                screen, scopedTab.scope(), tab, 1
        );
        if (canMoveRight) {
            items.add(new PersonalDatabaseScreenTabContextMenuItem(
                    "screen.infiniteinventory.tab_context.move_right",
                    PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.MOVE_RIGHT
            ));
        }

        items.add(null);

        if (tab.canRename()) {
            items.add(new PersonalDatabaseScreenTabContextMenuItem(
                    "screen.infiniteinventory.tab_context.rename",
                    PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.RENAME
            ));
        }

        items.add(new PersonalDatabaseScreenTabContextMenuItem(
                "screen.infiniteinventory.tab_context.change_icon",
                PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.CHANGE_ICON
        ));

        items.add(null);

        boolean isHidden = query.isTopTabHidden(scopedTab);
        items.add(new PersonalDatabaseScreenTabContextMenuItem(
                isHidden
                        ? "screen.infiniteinventory.tab_context.show_in_top"
                        : "screen.infiniteinventory.tab_context.hide_in_top",
                PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.TOGGLE_TOP_VISIBILITY
        ));

        return List.copyOf(items);
    }

    static int menuWidth(PersonalDatabaseScreen screen, List<PersonalDatabaseScreenTabContextMenuItem> items) {
        int maxTextWidth = 0;
        for (PersonalDatabaseScreenTabContextMenuItem item : items) {
            if (item == null) {
                continue;
            }
            int textWidth = screen.screenFont().width(
                    net.minecraft.network.chat.Component.translatable(item.translationKey())
            );
            maxTextWidth = Math.max(maxTextWidth, textWidth);
        }
        return Math.max(PersonalDatabaseScreen.CONTEXT_MENU_MIN_WIDTH, maxTextWidth + 28);
    }

    static int menuHeight(List<PersonalDatabaseScreenTabContextMenuItem> items) {
        int rowCount = 0;
        for (PersonalDatabaseScreenTabContextMenuItem item : items) {
            if (item != null) {
                rowCount++;
            }
        }
        return rowCount * PersonalDatabaseScreen.CONTEXT_MENU_ROW_HEIGHT + 4;
    }
}
