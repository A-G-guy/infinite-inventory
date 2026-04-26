package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseTab;
import java.util.List;

final class PersonalDatabaseScreenTabContextMenuActivator {
    private PersonalDatabaseScreenTabContextMenuActivator() {
    }

    static void activate(PersonalDatabaseScreen screen, PersonalDatabaseScreenTabContextMenuItem item) {
        PersonalDatabaseScreenCommonHelper.playButtonClickSound(screen);
        DatabaseScopedTabRef target = screen.tabContextMenuTarget;
        PersonalDatabaseScreenTabHelper.closeTabContextMenu(screen);
        if (target == null) {
            return;
        }
        switch (item.action()) {
            case JOIN_CURRENT_VIEW -> PersonalDatabaseScreenTopTabPromptHelper.applyJoinCurrentView(screen, target);
            case REMOVE_FROM_VIEW -> {
                DatabaseQuery query = screen.databaseMenu.viewState().query();
                List<DatabaseScopedTabRef> nextVisibleTabs = query.visibleTabs().stream()
                        .filter(t -> !t.equals(target))
                        .toList();
                PersonalDatabaseScreenLayoutHelper.sendQuery(screen, query.withVisibleTabs(nextVisibleTabs));
            }
            case SINGLE_VIEW -> PersonalDatabaseScreenTopTabPromptHelper.applySingleView(screen, target);
            case MOVE_LEFT -> {
                DatabaseTab tab = PersonalDatabaseScreenCommonHelper.findTab(screen, target);
                if (tab != null) {
                    PersonalDatabaseScreenManagementHelper.sendTabMutation(
                            screen, target.scope(), com.agguy.infiniteinventory.network.DatabaseTabMutationAction.MOVE_LEFT,
                            tab.id(), "", "", ""
                    );
                }
            }
            case MOVE_RIGHT -> {
                DatabaseTab tab = PersonalDatabaseScreenCommonHelper.findTab(screen, target);
                if (tab != null) {
                    PersonalDatabaseScreenManagementHelper.sendTabMutation(
                            screen, target.scope(), com.agguy.infiniteinventory.network.DatabaseTabMutationAction.MOVE_RIGHT,
                            tab.id(), "", "", ""
                    );
                }
            }
            case RENAME -> {
                PersonalDatabaseScreenManagementHelper.loadManagementDrafts(screen, target);
                screen.tabManagementExpanded = true;
                PersonalDatabaseScreenManagementHelper.ensureManagementWidgets(screen);
                if (screen.managementNameBox != null) {
                    screen.focusScreen(screen.managementNameBox);
                    screen.managementNameBox.setFocused(true);
                }
            }
            case CHANGE_ICON -> {
                PersonalDatabaseScreenManagementHelper.loadManagementDrafts(screen, target);
                screen.tabManagementExpanded = true;
                PersonalDatabaseScreenManagementHelper.openIconPicker(screen);
            }
            case TOGGLE_TOP_VISIBILITY -> {
                DatabaseTab tab = PersonalDatabaseScreenCommonHelper.findTab(screen, target);
                if (tab != null) {
                    PersonalDatabaseScreenManagementHelper.sendTabMutation(
                            screen, target.scope(), com.agguy.infiniteinventory.network.DatabaseTabMutationAction.TOGGLE_TOP_VISIBILITY,
                            tab.id(), "", "", ""
                    );
                }
            }
        }
    }
}
