package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

final class PersonalDatabaseScreenGestureHelper {
    private PersonalDatabaseScreenGestureHelper() {
    }

    static boolean mouseDragged(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0 || !screen.selectionGestureModel.hasCtrlSelectionGesture()) {
            return screen.invokeSuperMouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        tryExtendCtrlDragSelection(screen, mouseX, mouseY);
        return true;
    }

    static boolean mouseReleased(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button) {
        if (button != 0 || !screen.selectionGestureModel.hasCtrlSelectionGesture()) {
            return screen.invokeSuperMouseReleased(mouseX, mouseY, button);
        }
        finishCtrlSelectionGesture(screen);
        return true;
    }

    static boolean keyReleased(PersonalDatabaseScreen screen, int keyCode, int scanCode, int modifiers) {
        if (isDropKey(screen, keyCode, scanCode)) {
            screen.selectionGestureModel.releaseDiscardKey();
        }
        return screen.invokeSuperKeyReleased(keyCode, scanCode, modifiers);
    }

    static boolean handleHoveredDatabaseDiscardKey(PersonalDatabaseScreen screen, int keyCode, int scanCode) {
        if (!isDropKey(screen, keyCode, scanCode)) {
            return false;
        }
        PersonalDatabaseScreen.DatabaseHitResult hitResult = findFilledDatabaseHit(screen, screen.lastMouseX, screen.lastMouseY);
        if (hitResult == null
                || hasBlockingDiscardOverlay(screen)
                || !screen.databaseMenu.getCarried().isEmpty()
                || !screen.selectionGestureModel.tryConsumeDiscardKey()) {
            return hitResult != null && screen.selectionGestureModel.isDiscardKeyConsumed();
        }
        DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(hitResult.panelIndex());
        PersonalDatabaseScreenLayoutHelper.sendDatabaseClick(
                screen,
                hitResult.panelIndex(),
                hitResult.slotIndex(),
                DatabaseClickAction.DROP_SINGLE,
                panel.tab().id()
        );
        return true;
    }

    static void prepareForPrimaryDatabaseInteraction(PersonalDatabaseScreen screen) {
        PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
        screen.sortDropdownExpanded = false;
        screen.pagePickerExpanded = false;
        screen.enhancementPanelExpanded = false;
    }

    @Nullable
    static PersonalDatabaseScreen.DatabaseHitResult findFilledDatabaseHit(
            PersonalDatabaseScreen screen,
            double mouseX,
            double mouseY
    ) {
        PersonalDatabaseScreen.DatabaseHitResult hitResult = PersonalDatabaseScreenGeometry.findDatabaseSlot(screen, mouseX, mouseY);
        if (hitResult == null) {
            return null;
        }
        int panelIndex = hitResult.panelIndex();
        if (panelIndex < 0 || panelIndex >= PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()) {
            return null;
        }
        DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex);
        return hitResult.slotIndex() < panel.entries().size() ? hitResult : null;
    }

    private static void tryExtendCtrlDragSelection(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        PersonalDatabaseScreen.DatabaseHitResult hitResult = findFilledDatabaseHit(screen, mouseX, mouseY);
        if (hitResult == null) {
            return;
        }
        if (screen.selectionGestureModel.shouldPromoteToDrag(hitResult.panelIndex(), hitResult.slotIndex())) {
            DatabaseSelectionGestureModel.SelectionPoint startPoint = screen.selectionGestureModel.pendingCtrlClick();
            if (startPoint == null || !screen.selectionGestureModel.activateDragSelection()) {
                return;
            }
            PersonalDatabaseScreenSelectionHelper.addSelection(
                    screen,
                    PersonalDatabaseScreenSelectionHelper.selectionEntryAt(screen, startPoint.panelIndex(), startPoint.slotIndex())
            );
            PersonalDatabaseScreenSelectionHelper.addSelection(
                    screen,
                    PersonalDatabaseScreenSelectionHelper.selectionEntryAt(screen, hitResult.panelIndex(), hitResult.slotIndex())
            );
            screen.selectionGestureModel.recordDraggedSlot(hitResult.panelIndex(), hitResult.slotIndex());
            return;
        }
        if (screen.selectionGestureModel.recordDraggedSlot(hitResult.panelIndex(), hitResult.slotIndex())) {
            PersonalDatabaseScreenSelectionHelper.addSelection(
                    screen,
                    PersonalDatabaseScreenSelectionHelper.selectionEntryAt(screen, hitResult.panelIndex(), hitResult.slotIndex())
            );
        }
    }

    private static void finishCtrlSelectionGesture(PersonalDatabaseScreen screen) {
        try {
            if (screen.selectionGestureModel.isDragSelectionActive()) {
                return;
            }
            DatabaseSelectionGestureModel.SelectionPoint pendingClick = screen.selectionGestureModel.pendingCtrlClick();
            if (pendingClick == null) {
                return;
            }
            PersonalDatabaseScreenSelectionHelper.toggleSelection(
                    screen,
                    PersonalDatabaseScreenSelectionHelper.selectionEntryAt(
                            screen,
                            pendingClick.panelIndex(),
                            pendingClick.slotIndex()
                    )
            );
        } finally {
            screen.selectionGestureModel.clearCtrlSelectionGesture();
        }
    }

    private static boolean isDropKey(PersonalDatabaseScreen screen, int keyCode, int scanCode) {
        Minecraft minecraft = screen.minecraftClient();
        return minecraft != null && minecraft.options.keyDrop.matches(keyCode, scanCode);
    }

    private static boolean hasBlockingDiscardOverlay(PersonalDatabaseScreen screen) {
        if (screen.customExtractOverlayExpanded
                || screen.contextMenuExpanded
                || screen.sortDropdownExpanded
                || screen.pagePickerExpanded
                || screen.advancedSearchExpanded
                || screen.enhancementPanelExpanded
                || screen.viewSelectorExpanded
                || screen.moreTabsExpanded
                || screen.targetSelectorExpanded
                || screen.tabManagementExpanded
                || screen.iconPickerExpanded) {
            return true;
        }
        if (screen.iconSearchBox != null && screen.iconSearchBox.isFocused()) {
            return true;
        }
        if (screen.managementNameBox != null && screen.managementNameBox.isFocused()) {
            return true;
        }
        if (screen.customExtractAmountBox != null && screen.customExtractAmountBox.isFocused()) {
            return true;
        }
        for (var searchBox : screen.panelSearchBoxes) {
            if (searchBox.isFocused()) {
                return true;
            }
        }
        return false;
    }
}
