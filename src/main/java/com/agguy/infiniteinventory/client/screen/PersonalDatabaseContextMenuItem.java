package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import org.jetbrains.annotations.Nullable;

record PersonalDatabaseContextMenuItem(
        String translationKey,
        @Nullable DatabaseClickAction clickAction,
        @Nullable DatabaseSelectionAction selectionAction,
        @Nullable LocalAction localAction
) {
    PersonalDatabaseContextMenuItem {
        translationKey = translationKey == null ? "" : translationKey;
        int actionCount = (clickAction == null ? 0 : 1)
                + (selectionAction == null ? 0 : 1)
                + (localAction == null ? 0 : 1);
        if (actionCount != 1) {
            throw new IllegalArgumentException("右键菜单项必须且只能绑定一种动作");
        }
    }

    static PersonalDatabaseContextMenuItem click(String translationKey, DatabaseClickAction clickAction) {
        return new PersonalDatabaseContextMenuItem(translationKey, clickAction, null, null);
    }

    static PersonalDatabaseContextMenuItem selection(String translationKey, DatabaseSelectionAction selectionAction) {
        return new PersonalDatabaseContextMenuItem(translationKey, null, selectionAction, null);
    }

    static PersonalDatabaseContextMenuItem local(String translationKey, LocalAction localAction) {
        return new PersonalDatabaseContextMenuItem(translationKey, null, null, localAction);
    }

    enum LocalAction {
        OPEN_CUSTOM_EXTRACT_OVERLAY,
        OPEN_NOTE_OVERLAY,
        TOGGLE_STAR,
        STAR_ALL,
        UNSTAR_ALL
    }
}
