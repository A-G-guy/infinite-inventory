package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.VisibleDatabaseEntry;
import java.util.List;
import net.minecraft.world.item.ItemStack;

final class PersonalDatabaseScreenStarMenuHelper {
    private PersonalDatabaseScreenStarMenuHelper() {
    }

    enum StarSelectionState {
        NONE, ALL_UNSTARRED, ALL_STARRED, MIXED
    }

    static StarSelectionState resolveStarSelectionState(PersonalDatabaseScreen screen) {
        List<DatabaseSelectionEntry> selected = PersonalDatabaseScreenSelectionHelper.selectedEntries(screen);
        if (selected.isEmpty()) return StarSelectionState.NONE;
        boolean anyStarred = false;
        boolean anyUnstarred = false;
        for (DatabaseSelectionEntry selectionEntry : selected) {
            boolean starred = isEntryStarred(screen, selectionEntry);
            if (starred) anyStarred = true;
            else anyUnstarred = true;
            if (anyStarred && anyUnstarred) return StarSelectionState.MIXED;
        }
        if (anyStarred) return StarSelectionState.ALL_STARRED;
        return StarSelectionState.ALL_UNSTARRED;
    }

    static boolean isEntryStarred(PersonalDatabaseScreen screen, DatabaseSelectionEntry selectionEntry) {
        if (selectionEntry == null || selectionEntry.isEmpty()) return false;
        for (DatabasePanelView panel : screen.databaseMenu.viewState().panels()) {
            if (panel.scopedTab().scope() != selectionEntry.scope()) continue;
            for (VisibleDatabaseEntry entry : panel.entries()) {
                if (entry.scope() == selectionEntry.scope()
                        && entry.tabId().equals(selectionEntry.sourceTabId())
                        && ItemStack.isSameItemSameComponents(entry.stack(), selectionEntry.displayStack())) {
                    return entry.starred();
                }
            }
        }
        return false;
    }

    static List<PersonalDatabaseContextMenuItem> starMenuItemsForState(StarSelectionState state) {
        return switch (state) {
            case ALL_UNSTARRED -> List.of(
                    PersonalDatabaseContextMenuItem.local(
                            "screen.infiniteinventory.context.add_star",
                            PersonalDatabaseContextMenuItem.LocalAction.STAR_ALL
                    )
            );
            case ALL_STARRED -> List.of(
                    PersonalDatabaseContextMenuItem.local(
                            "screen.infiniteinventory.context.remove_star",
                            PersonalDatabaseContextMenuItem.LocalAction.UNSTAR_ALL
                    )
            );
            default -> List.of(
                    PersonalDatabaseContextMenuItem.local(
                            "screen.infiniteinventory.context.toggle_star_mixed",
                            PersonalDatabaseContextMenuItem.LocalAction.TOGGLE_STAR
                    ),
                    PersonalDatabaseContextMenuItem.local(
                            "screen.infiniteinventory.context.star_all",
                            PersonalDatabaseContextMenuItem.LocalAction.STAR_ALL
                    ),
                    PersonalDatabaseContextMenuItem.local(
                            "screen.infiniteinventory.context.unstar_all",
                            PersonalDatabaseContextMenuItem.LocalAction.UNSTAR_ALL
                    )
            );
        };
    }

    static String singleSelectionStarKey(PersonalDatabaseScreen screen, DatabaseSelectionEntry entry) {
        boolean starred = isEntryStarred(screen, entry);
        return starred ? "screen.infiniteinventory.context.remove_star" : "screen.infiniteinventory.context.add_star";
    }
}
