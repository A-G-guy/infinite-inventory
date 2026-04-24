package com.agguy.infiniteinventory.client.screen;

record PersonalDatabaseScreenTabContextMenuItem(
        String translationKey,
        TabContextMenuAction action
) {
    PersonalDatabaseScreenTabContextMenuItem {
        translationKey = translationKey == null ? "" : translationKey;
    }

    enum TabContextMenuAction {
        JOIN_CURRENT_VIEW,
        SINGLE_VIEW,
        MOVE_LEFT,
        MOVE_RIGHT,
        RENAME,
        CHANGE_ICON,
        TOGGLE_TOP_VISIBILITY
    }
}
