package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.database.VisibleDatabaseEntry;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import java.util.List;

final class PersonalDatabaseScreenSelectionHelper {
    private PersonalDatabaseScreenSelectionHelper() {
    }

    static void syncSelectionWithViewState(PersonalDatabaseScreen screen) {
        DatabaseViewState currentViewState = screen.databaseMenu.viewState();
        if (screen.selectionTrackedViewState == currentViewState) {
            return;
        }
        screen.selectionTrackedViewState = currentViewState;
        clearSelection(screen);
    }

    static void replaceSelection(PersonalDatabaseScreen screen, DatabaseSelectionEntry selectionEntry) {
        clearSelectionEntries(screen);
        if (selectionEntry == null || selectionEntry.isEmpty()) {
            return;
        }
        screen.selectedDatabaseEntries.add(selectionEntry);
    }

    static void toggleSelection(PersonalDatabaseScreen screen, DatabaseSelectionEntry selectionEntry) {
        if (selectionEntry == null || selectionEntry.isEmpty()) {
            return;
        }
        if (screen.selectedDatabaseEntries.contains(selectionEntry)) {
            screen.selectedDatabaseEntries.remove(selectionEntry);
            if (screen.selectedDatabaseEntries.isEmpty()) {
                PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
            }
            return;
        }
        screen.selectedDatabaseEntries.add(selectionEntry);
    }

    static void addSelection(PersonalDatabaseScreen screen, DatabaseSelectionEntry selectionEntry) {
        if (selectionEntry == null || selectionEntry.isEmpty()) {
            return;
        }
        screen.selectedDatabaseEntries.add(selectionEntry);
    }

    static void clearSelection(PersonalDatabaseScreen screen) {
        clearSelectionEntries(screen);
        screen.selectionGestureModel.clearSelectionGesture();
    }

    static void clearSelectionEntries(PersonalDatabaseScreen screen) {
        screen.selectedDatabaseEntries.clear();
        PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
        PersonalDatabaseScreenCustomExtractOverlayHelper.closeOverlay(screen);
    }

    static boolean hasSelection(PersonalDatabaseScreen screen) {
        return !screen.selectedDatabaseEntries.isEmpty();
    }

    static List<DatabaseSelectionEntry> selectedEntries(PersonalDatabaseScreen screen) {
        return List.copyOf(screen.selectedDatabaseEntries);
    }

    static int selectedEntryCount(PersonalDatabaseScreen screen) {
        return screen.selectedDatabaseEntries.size();
    }

    static boolean isSelected(PersonalDatabaseScreen screen, VisibleDatabaseEntry entry) {
        DatabaseSelectionEntry selectionEntry = fromVisibleEntry(entry);
        return selectionEntry != null && screen.selectedDatabaseEntries.contains(selectionEntry);
    }

    static DatabaseSelectionEntry selectionEntryAt(PersonalDatabaseScreen screen, int panelIndex, int slotIndex) {
        if (panelIndex < 0 || panelIndex >= PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()) {
            return null;
        }
        var panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex);
        if (slotIndex < 0 || slotIndex >= panel.entries().size()) {
            return null;
        }
        return fromVisibleEntry(panel.entries().get(slotIndex));
    }

    static void sendSelectionAction(PersonalDatabaseScreen screen, DatabaseSelectionAction action, String targetTabId) {
        sendSelectionAction(screen, action, null, targetTabId, 0L);
    }

    static void sendSelectionAction(
            PersonalDatabaseScreen screen,
            DatabaseSelectionAction action,
            String targetTabId,
            long requestedAmount
    ) {
        sendSelectionAction(screen, action, null, targetTabId, requestedAmount);
    }

    static void sendSelectionAction(
            PersonalDatabaseScreen screen,
            DatabaseSelectionAction action,
            DatabaseScope targetScope,
            String targetTabId,
            long requestedAmount
    ) {
        List<DatabaseSelectionEntry> selectedEntries = selectedEntries(screen);
        if (selectedEntries.isEmpty()) {
            PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
            return;
        }
        PersonalDatabaseScreenLayoutHelper.sendDatabaseSelection(
                screen,
                action,
                targetScope,
                targetTabId,
                requestedAmount,
                selectedEntries
        );
        clearSelection(screen);
    }

    private static DatabaseSelectionEntry fromVisibleEntry(VisibleDatabaseEntry entry) {
        if (entry == null || entry.stack().isEmpty()) {
            return null;
        }
        return new DatabaseSelectionEntry(entry.scope(), entry.tabId(), entry.stack());
    }
}
