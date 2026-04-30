package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseTab;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

final class PersonalDatabaseScreenTabContextMenuBuilder {
    private PersonalDatabaseScreenTabContextMenuBuilder() {
    }

    static List<PersonalDatabaseScreenTabContextMenuItem> buildMenuItems(PersonalDatabaseScreen screen, DatabaseScopedTabRef scopedTab) {
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        DatabaseTab tab = PersonalDatabaseScreenCommonHelper.findTab(screen, scopedTab);

        boolean inVisibleTabs = query.visibleTabs().contains(scopedTab);
        int visibleTabCount = query.visibleTabs().size();
        int maxPanels = PersonalDatabaseScreenCommonHelper.maxVisiblePanels(screen);

        boolean canMoveLeft = PersonalDatabaseScreenManagementLogic.canMoveManagementTab(
                screen, scopedTab.scope(), tab, -1
        );
        boolean canMoveRight = PersonalDatabaseScreenManagementLogic.canMoveManagementTab(
                screen, scopedTab.scope(), tab, 1
        );
        boolean isHidden = query.isTopTabHidden(scopedTab);

        return buildMenuItems(tab, inVisibleTabs, visibleTabCount, maxPanels, canMoveLeft, canMoveRight, isHidden);
    }

    static List<PersonalDatabaseScreenTabContextMenuItem> buildMenuItems(
            DatabaseTab tab,
            boolean inVisibleTabs,
            int visibleTabCount,
            int maxVisiblePanels,
            boolean canMoveLeft,
            boolean canMoveRight,
            boolean isHiddenInTop
    ) {
        List<PersonalDatabaseScreenTabContextMenuItem> items = new ArrayList<>();

        if (!inVisibleTabs && visibleTabCount < maxVisiblePanels) {
            items.add(new PersonalDatabaseScreenTabContextMenuItem(
                    "screen.infiniteinventory.tab_context.join_current",
                    PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.JOIN_CURRENT_VIEW
            ));
        }
        if (inVisibleTabs && visibleTabCount > 1) {
            items.add(new PersonalDatabaseScreenTabContextMenuItem(
                    "screen.infiniteinventory.tab_context.remove_from_view",
                    PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.REMOVE_FROM_VIEW
            ));
        }

        items.add(new PersonalDatabaseScreenTabContextMenuItem(
                "screen.infiniteinventory.tab_context.single_view",
                PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.SINGLE_VIEW
        ));

        if (canMoveLeft) {
            items.add(new PersonalDatabaseScreenTabContextMenuItem(
                    "screen.infiniteinventory.tab_context.move_left",
                    PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.MOVE_LEFT
            ));
        }

        if (canMoveRight) {
            items.add(new PersonalDatabaseScreenTabContextMenuItem(
                    "screen.infiniteinventory.tab_context.move_right",
                    PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.MOVE_RIGHT
            ));
        }

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

        items.add(new PersonalDatabaseScreenTabContextMenuItem(
                isHiddenInTop
                        ? "screen.infiniteinventory.tab_context.show_in_top"
                        : "screen.infiniteinventory.tab_context.hide_in_top",
                PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.TOGGLE_TOP_VISIBILITY
        ));

        return java.util.Collections.unmodifiableList(new java.util.ArrayList<>(items));
    }

    @Nullable
    static RemixIcon tabContextMenuItemIcon(PersonalDatabaseScreenTabContextMenuItem item) {
        return RemixIcon.forTabContextMenuAction(item.action());
    }

    static int menuWidth(PersonalDatabaseScreen screen, List<PersonalDatabaseScreenTabContextMenuItem> items) {
        if (screen == null) {
            return PersonalDatabaseScreen.CONTEXT_MENU_MIN_WIDTH;
        }
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
        return Math.max(PersonalDatabaseScreen.CONTEXT_MENU_MIN_WIDTH, maxTextWidth + 36);
    }

    static int menuHeight(List<PersonalDatabaseScreenTabContextMenuItem> items) {
        return items.size() * PersonalDatabaseScreen.CONTEXT_MENU_ROW_HEIGHT + 4;
    }
}
