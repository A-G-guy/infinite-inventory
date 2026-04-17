package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

final class PersonalDatabaseScreenGestureHelper {
    private PersonalDatabaseScreenGestureHelper() {
    }

    static boolean mouseDragged(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0 || !screen.selectionGestureModel.hasSelectionGesture()) {
            return screen.invokeSuperMouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        tryExtendPrimaryDragSelection(screen, mouseX, mouseY);
        return true;
    }

    static boolean mouseReleased(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button) {
        if (button != 0 || !screen.selectionGestureModel.hasSelectionGesture()) {
            return screen.invokeSuperMouseReleased(mouseX, mouseY, button);
        }
        finishPrimarySelectionGesture(screen);
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

    static DatabaseSelectionGestureModel.PointerTarget resolvePointerTarget(
            PersonalDatabaseScreen screen,
            double mouseX,
            double mouseY
    ) {
        PersonalDatabaseScreen.DatabaseHitResult hitResult = PersonalDatabaseScreenGeometry.findDatabaseSlot(screen, mouseX, mouseY);
        if (hitResult != null) {
            int panelIndex = hitResult.panelIndex();
            if (panelIndex >= 0 && panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()) {
                DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex);
                return hitResult.slotIndex() < panel.entries().size()
                        ? DatabaseSelectionGestureModel.PointerTarget.filledSlot(panelIndex, hitResult.slotIndex())
                        : DatabaseSelectionGestureModel.PointerTarget.emptySlot(panelIndex, hitResult.slotIndex());
            }
        }
        int panelIndex = PersonalDatabaseScreenTabHelper.findDatabasePanel(screen, mouseX, mouseY);
        return panelIndex >= 0
                ? DatabaseSelectionGestureModel.PointerTarget.panelBackground(panelIndex)
                : DatabaseSelectionGestureModel.PointerTarget.outsidePanel();
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

    private static void tryExtendPrimaryDragSelection(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        DatabaseSelectionGestureModel.PointerTarget currentTarget = resolvePointerTarget(screen, mouseX, mouseY);
        if (screen.selectionGestureModel.shouldPromoteToDrag(currentTarget)) {
            DatabaseSelectionGestureModel.SelectionGesture selectionGesture = screen.selectionGestureModel.pendingSelectionGesture();
            if (selectionGesture == null || !screen.selectionGestureModel.activateDragSelection()) {
                return;
            }
            if (selectionGesture.mode() == DatabaseSelectionGestureModel.SelectionMode.REPLACE) {
                PersonalDatabaseScreenSelectionHelper.clearSelectionEntries(screen);
            }
            addDraggedSelection(screen, selectionGesture.startTarget());
        }
        if (!screen.selectionGestureModel.isDragSelectionActive()) {
            return;
        }
        addDraggedSelection(screen, currentTarget);
    }

    private static void addDraggedSelection(
            PersonalDatabaseScreen screen,
            DatabaseSelectionGestureModel.PointerTarget target
    ) {
        if (!target.isFilledSlot() || !screen.selectionGestureModel.recordDraggedSlot(target.panelIndex(), target.slotIndex())) {
            return;
        }
        PersonalDatabaseScreenSelectionHelper.addSelection(
                screen,
                PersonalDatabaseScreenSelectionHelper.selectionEntryAt(screen, target.panelIndex(), target.slotIndex())
        );
    }

    private static void finishPrimarySelectionGesture(PersonalDatabaseScreen screen) {
        try {
            DatabaseSelectionGestureModel.SelectionGesture selectionGesture = screen.selectionGestureModel.pendingSelectionGesture();
            if (selectionGesture == null || screen.selectionGestureModel.isDragSelectionActive()) {
                return;
            }
            if (selectionGesture.mode() == DatabaseSelectionGestureModel.SelectionMode.ADDITIVE) {
                finishAdditiveClick(screen, selectionGesture.startTarget());
                return;
            }
            finishReplaceClick(screen, selectionGesture);
        } finally {
            screen.selectionGestureModel.clearSelectionGesture();
        }
    }

    private static void finishAdditiveClick(
            PersonalDatabaseScreen screen,
            DatabaseSelectionGestureModel.PointerTarget startTarget
    ) {
        if (!startTarget.isFilledSlot()) {
            return;
        }
        PersonalDatabaseScreenSelectionHelper.toggleSelection(
                screen,
                PersonalDatabaseScreenSelectionHelper.selectionEntryAt(
                        screen,
                        startTarget.panelIndex(),
                        startTarget.slotIndex()
                )
        );
    }

    private static void finishReplaceClick(
            PersonalDatabaseScreen screen,
            DatabaseSelectionGestureModel.SelectionGesture selectionGesture
    ) {
        DatabaseSelectionGestureModel.PointerTarget startTarget = selectionGesture.startTarget();
        if (!startTarget.isFilledSlot()) {
            PersonalDatabaseScreenSelectionHelper.clearSelection(screen);
            return;
        }
        if (selectionGesture.clickedEntrySelected()) {
            DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(startTarget.panelIndex());
            PersonalDatabaseScreenLayoutHelper.sendDatabaseClick(
                    screen,
                    startTarget.panelIndex(),
                    startTarget.slotIndex(),
                    DatabaseClickAction.TAKE_SINGLE,
                    panel.tab().id()
            );
            PersonalDatabaseScreenSelectionHelper.clearSelection(screen);
            return;
        }
        PersonalDatabaseScreenSelectionHelper.replaceSelection(
                screen,
                PersonalDatabaseScreenSelectionHelper.selectionEntryAt(
                        screen,
                        startTarget.panelIndex(),
                        startTarget.slotIndex()
                )
        );
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
