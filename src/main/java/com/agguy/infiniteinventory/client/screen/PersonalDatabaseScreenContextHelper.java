package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import net.minecraft.util.Mth;

final class PersonalDatabaseScreenContextHelper {
    private PersonalDatabaseScreenContextHelper() {
    }

    static void validateContextMenu(PersonalDatabaseScreen screen, DatabaseViewState viewState) {
        if (!screen.contextMenuExpanded) {
            return;
        }
        if (!PersonalDatabaseScreenSelectionHelper.hasSelection(screen)) {
            closeContextMenu(screen);
        }
    }

    static void openContextMenu(PersonalDatabaseScreen screen, int panelIndex, int slotIndex) {
        if (screen.layout == null) {
            return;
        }
        if (panelIndex < 0 || panelIndex >= PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()) {
            closeContextMenu(screen);
            return;
        }
        if (!PersonalDatabaseScreenSelectionHelper.hasSelection(screen)) {
            closeContextMenu(screen);
            return;
        }
        java.util.List<com.agguy.infiniteinventory.database.VisibleDatabaseEntry> entries =
                PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex).entries();
        if (slotIndex < 0 || slotIndex >= entries.size()) {
            closeContextMenu(screen);
            return;
        }
        PersonalDatabaseLayout.Rect slotRect = screen.layout.visibleDatabaseSlotBounds(panelIndex, slotIndex);
        int menuWidth = PersonalDatabaseScreenCommonHelper.contextMenuWidth(screen);
        int menuHeight = PersonalDatabaseScreen.CONTEXT_MENU_ACTIONS.length * PersonalDatabaseScreen.CONTEXT_MENU_ROW_HEIGHT;
        PersonalDatabaseLayout.Rect frameRect = screen.layout.frameRect();
        int minX = frameRect.x() + PersonalDatabaseScreen.CONTEXT_MENU_MARGIN;
        int maxX = Math.max(minX, frameRect.right() - menuWidth - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN);
        int preferredX = slotRect.right() + 2;
        if (preferredX > maxX) {
            preferredX = slotRect.x() - menuWidth - 2;
        }
        screen.contextMenuX = Mth.clamp(preferredX, minX, maxX);

        int minY = frameRect.y() + PersonalDatabaseScreen.CONTEXT_MENU_MARGIN;
        int maxY = Math.max(minY, frameRect.bottom() - menuHeight - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN);
        screen.contextMenuY = Mth.clamp(slotRect.y(), minY, maxY);
        screen.contextMenuPanelIndex = panelIndex;
        screen.contextMenuSlotIndex = slotIndex;
        screen.contextMenuExpanded = true;
    }

    static void closeContextMenu(PersonalDatabaseScreen screen) {
        screen.contextMenuExpanded = false;
        screen.contextMenuPanelIndex = -1;
        screen.contextMenuSlotIndex = -1;
    }

    static boolean isWithinContextMenu(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.contextMenuExpanded) {
            return false;
        }
        int menuWidth = PersonalDatabaseScreenCommonHelper.contextMenuWidth(screen);
        return mouseX >= screen.contextMenuX
                && mouseX < screen.contextMenuX + menuWidth
                && mouseY >= screen.contextMenuY
                && mouseY < screen.contextMenuY
                + PersonalDatabaseScreen.CONTEXT_MENU_ACTIONS.length * PersonalDatabaseScreen.CONTEXT_MENU_ROW_HEIGHT;
    }
}
