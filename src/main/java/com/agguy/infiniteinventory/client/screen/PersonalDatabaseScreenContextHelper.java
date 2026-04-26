package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseStarPayload;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

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
        int menuWidth = PersonalDatabaseScreenContextMenuBuilder.contextMenuWidth(screen);
        int menuHeight = PersonalDatabaseScreenContextMenuBuilder.contextMenuHeight(screen);
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

    @Nullable
    static PersonalDatabaseContextMenuItem contextMenuItemAt(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.contextMenuExpanded) {
            return null;
        }
        java.util.List<PersonalDatabaseContextMenuItem> items = PersonalDatabaseScreenContextMenuBuilder.contextMenuItems(screen);
        int menuWidth = PersonalDatabaseScreenContextMenuBuilder.contextMenuWidth(screen);
        for (int index = 0; index < items.size(); index++) {
            int rowY = screen.contextMenuY + index * PersonalDatabaseScreen.CONTEXT_MENU_ROW_HEIGHT;
            if (mouseX < screen.contextMenuX
                    || mouseX >= screen.contextMenuX + menuWidth
                    || mouseY < rowY
                    || mouseY >= rowY + PersonalDatabaseScreen.CONTEXT_MENU_ROW_HEIGHT) {
                continue;
            }
            return items.get(index);
        }
        return null;
    }

    static void activateContextMenuItem(PersonalDatabaseScreen screen, PersonalDatabaseContextMenuItem item) {
        if (item == null) {
            return;
        }
        PersonalDatabaseScreenCommonHelper.playButtonClickSound(screen);
        if (item.clickAction() != null) {
            triggerSingleClickAction(screen, item.clickAction());
            return;
        }
        if (item.selectionAction() != null) {
            triggerSelectionAction(screen, item.selectionAction());
            return;
        }
        if (item.localAction() == null) {
            return;
        }
        closeContextMenu(screen);
        if (item.localAction() == PersonalDatabaseContextMenuItem.LocalAction.OPEN_CUSTOM_EXTRACT_OVERLAY) {
            PersonalDatabaseScreenCustomExtractOverlayHelper.openOverlay(screen);
        } else if (item.localAction() == PersonalDatabaseContextMenuItem.LocalAction.OPEN_NOTE_OVERLAY) {
            PersonalDatabaseScreenNoteOverlayHelper.openOverlay(screen);
        } else if (item.localAction() == PersonalDatabaseContextMenuItem.LocalAction.TOGGLE_STAR) {
            sendStarAction(screen, DatabaseStarPayload.StarAction.TOGGLE);
        } else if (item.localAction() == PersonalDatabaseContextMenuItem.LocalAction.STAR_ALL) {
            sendStarAction(screen, DatabaseStarPayload.StarAction.STAR_ALL);
        } else if (item.localAction() == PersonalDatabaseContextMenuItem.LocalAction.UNSTAR_ALL) {
            sendStarAction(screen, DatabaseStarPayload.StarAction.UNSTAR_ALL);
        }
    }

    private static void sendStarAction(PersonalDatabaseScreen screen, DatabaseStarPayload.StarAction action) {
        List<DatabaseSelectionEntry> selectedEntries = PersonalDatabaseScreenSelectionHelper.selectedEntries(screen);
        if (selectedEntries.isEmpty()) {
            return;
        }
        Map<DatabaseScope, List<ItemStack>> stacksByScope = new LinkedHashMap<>();
        for (DatabaseSelectionEntry entry : selectedEntries) {
            if (entry.isEmpty()) {
                continue;
            }
            stacksByScope.computeIfAbsent(entry.scope(), ignored -> new ArrayList<>()).add(entry.displayStack());
        }
        for (Map.Entry<DatabaseScope, List<ItemStack>> entry : stacksByScope.entrySet()) {
            PacketDistributor.sendToServer(new DatabaseStarPayload(
                    screen.databaseMenu.containerId,
                    screen.databaseMenu.viewState().sessionId(),
                    entry.getKey(),
                    entry.getValue(),
                    action
            ));
        }
        PersonalDatabaseScreenSelectionHelper.clearSelection(screen);
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
        int menuWidth = PersonalDatabaseScreenContextMenuBuilder.contextMenuWidth(screen);
        return mouseX >= screen.contextMenuX
                && mouseX < screen.contextMenuX + menuWidth
                && mouseY >= screen.contextMenuY
                && mouseY < screen.contextMenuY
                + PersonalDatabaseScreenContextMenuBuilder.contextMenuHeight(screen);
    }

    private static void triggerSelectionAction(PersonalDatabaseScreen screen, com.agguy.infiniteinventory.network.DatabaseSelectionAction action) {
        if (action.requiresTargetTab()) {
            DatabaseScope sourceScope = screen.databaseMenu.viewState().query().focusedTab().scope();
            if (screen.contextMenuPanelIndex >= 0 && screen.contextMenuPanelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()) {
                sourceScope = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(screen.contextMenuPanelIndex).scopedTab().scope();
            }
            PersonalDatabaseScreenTargetHelper.openTargetSelector(
                    screen,
                    PersonalDatabaseScreenEnums.TargetSelectorMode.TRANSFER_SELECTION,
                    -1,
                    -1,
                    sourceScope,
                    ""
            );
            return;
        }
        PersonalDatabaseScreenSelectionHelper.sendSelectionAction(screen, action, "", 0L);
    }

    private static void triggerSingleClickAction(
            PersonalDatabaseScreen screen,
            com.agguy.infiniteinventory.network.DatabaseClickAction action
    ) {
        if (screen.contextMenuPanelIndex < 0
                || screen.contextMenuPanelIndex >= PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()) {
            closeContextMenu(screen);
            return;
        }
        DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(screen.contextMenuPanelIndex);
        if (screen.contextMenuSlotIndex < 0 || screen.contextMenuSlotIndex >= panel.entries().size()) {
            closeContextMenu(screen);
            return;
        }
        PersonalDatabaseScreenLayoutHelper.sendDatabaseClick(
                screen,
                screen.contextMenuPanelIndex,
                screen.contextMenuSlotIndex,
                action,
                panel.scopedTab().scope(),
                panel.tab().id()
        );
        PersonalDatabaseScreenSelectionHelper.clearSelection(screen);
    }
}
