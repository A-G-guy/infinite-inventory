package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.database.VisibleDatabaseEntry;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

final class PersonalDatabaseScreenContextHelper {
    private PersonalDatabaseScreenContextHelper() {
    }

    static void validateContextMenu(PersonalDatabaseScreen screen, DatabaseViewState viewState) {
        if (!screen.contextMenuExpanded) {
            return;
        }
        if (screen.contextMenuPanelIndex < 0 || screen.contextMenuPanelIndex >= viewState.panels().size()) {
            closeContextMenu(screen);
            return;
        }
        var panel = viewState.panels().get(screen.contextMenuPanelIndex);
        if (screen.contextMenuSlotIndex < 0 || screen.contextMenuSlotIndex >= panel.entries().size()) {
            closeContextMenu(screen);
            return;
        }
        ItemStack currentStack = panel.entries().get(screen.contextMenuSlotIndex).stack();
        if (!ItemStack.isSameItemSameComponents(screen.contextMenuEntryStack, currentStack)) {
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
        java.util.List<VisibleDatabaseEntry> entries = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex).entries();
        if (slotIndex < 0 || slotIndex >= entries.size()) {
            closeContextMenu(screen);
            return;
        }
        VisibleDatabaseEntry entry = entries.get(slotIndex);
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
        screen.contextMenuEntryStack = entry.stack().copyWithCount(1);
        screen.contextMenuExpanded = true;
    }

    static void closeContextMenu(PersonalDatabaseScreen screen) {
        screen.contextMenuExpanded = false;
        screen.contextMenuPanelIndex = -1;
        screen.contextMenuSlotIndex = -1;
        screen.contextMenuEntryStack = ItemStack.EMPTY;
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
