package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

final class PersonalDatabaseScreenInteractionHelper {
    private PersonalDatabaseScreenInteractionHelper() {
    }
    static boolean mouseClicked(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button) {
        if (!PersonalDatabaseScreenCommonHelper.supportsFullUi(screen)) {
            return true;
        }
        if (button == 0) {
            PersonalDatabaseScreenLayoutHelper.updateSearchFocusFromClick(screen, mouseX, mouseY);
        }
        if (screen.customExtractOverlayExpanded
                && PersonalDatabaseScreenCustomExtractOverlayHelper.handleMouseClicked(screen, mouseX, mouseY, button)) {
            return true;
        }
        if (screen.noteOverlayExpanded
                && PersonalDatabaseScreenNoteOverlayHelper.handleMouseClicked(screen, mouseX, mouseY, button)) {
            return true;
        }
        if (screen.logPanelExpanded
                && PersonalDatabaseScreenLogHelper.handleLogPanelClick(screen, mouseX, mouseY, button)) {
            return true;
        }
        if (screen.jeiTabSourceOverlayExpanded
                && PersonalDatabaseScreenJeiTabSourceHelper.handleJeiTabSourceOverlayClick(screen, mouseX, mouseY)) {
            return true;
        }
        if (screen.advancedSearchExpanded
                && PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(
                        PersonalDatabaseScreenGeometry.advancedSearchPanelRect(screen),
                        mouseX,
                        mouseY
                )) {
            screen.advancedSearchExpanded = false;
            return true;
        }
        if (screen.advancedSearchExpanded && !PersonalDatabaseScreenGeometry.isWithinAdvancedSearchPanel(screen, mouseX, mouseY)) {
            screen.advancedSearchExpanded = false;
        }
        if (screen.enhancementPanelExpanded
                && PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(
                        PersonalDatabaseScreenGeometry.enhancementPanelRect(screen),
                        mouseX,
                        mouseY
                )) {
            screen.enhancementPanelExpanded = false;
            return true;
        }
        if (screen.enhancementPanelExpanded && !PersonalDatabaseScreenGeometry.isWithinEnhancementPanel(screen, mouseX, mouseY)) {
            screen.enhancementPanelExpanded = false;
        }
        if (screen.iconPickerExpanded && PersonalDatabaseScreenManagementHelper.handleIconPickerClick(screen, mouseX, mouseY)) {
            return true;
        }
        if (screen.targetSelectorExpanded && PersonalDatabaseScreenTargetHelper.handleTargetSelectorClick(screen, mouseX, mouseY)) {
            return true;
        }
        if (screen.topTabReplaceExpanded && PersonalDatabaseScreenTabHelper.handleTopTabReplacePromptClick(screen, mouseX, mouseY)) {
            return true;
        }
        if (screen.topTabActionPromptExpanded && PersonalDatabaseScreenTabHelper.handleTopTabActionPromptClick(screen, mouseX, mouseY)) {
            return true;
        }
        if (screen.viewSelectorExpanded && PersonalDatabaseScreenTabHelper.handleViewSelectorClick(screen, mouseX, mouseY)) {
            return true;
        }
        if (screen.moreTabsExpanded && PersonalDatabaseScreenTabHelper.handleMoreTabsClick(screen, mouseX, mouseY)) {
            return true;
        }
        if (screen.tabManagementExpanded && PersonalDatabaseScreenManagementHelper.handleTabManagementClick(screen, mouseX, mouseY)) {
            return true;
        }
        if (screen.pagePickerExpanded && PersonalDatabaseScreenPopupInteractionHelper.handlePagePickerClick(screen, mouseX, mouseY)) {
            return true;
        }
        if (screen.advancedSearchExpanded && PersonalDatabaseScreenGeometry.isWithinAdvancedSearchPanel(screen, mouseX, mouseY)) {
            screen.invokeSuperMouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (screen.enhancementPanelExpanded && PersonalDatabaseScreenGeometry.isWithinEnhancementPanel(screen, mouseX, mouseY)) {
            if (PersonalDatabaseScreenTargetHelper.handleEnhancementPanelClick(screen, mouseX, mouseY)) {
                return true;
            }
            if (com.agguy.infiniteinventory.compat.jei.JeiCompat.isAvailable()
                    && PersonalDatabaseScreenGeometry.enhancementJeiTabSourceRowRect(screen).contains(mouseX, mouseY)) {
                PersonalDatabaseScreenJeiTabSourceHelper.openJeiTabSourceOverlay(screen);
                return true;
            }
            screen.invokeSuperMouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (screen.contextMenuExpanded && PersonalDatabaseScreenPopupInteractionHelper.handleContextMenuClick(screen, mouseX, mouseY)) {
            return true;
        }
        if (screen.sortDropdownExpanded && PersonalDatabaseScreenPopupInteractionHelper.handleSortDropdownClick(screen, mouseX, mouseY)) {
            return true;
        }
        if (screen.accessoriesExpanded && PersonalDatabaseScreenGeometry.isWithinAccessoriesPanel(screen, mouseX, mouseY)) {
            screen.invokeSuperMouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (PersonalDatabaseScreenTabHelper.handleTabClick(screen, mouseX, mouseY)) {
            return true;
        }
        if (PersonalDatabaseScreenTargetHelper.handleQuickDepositClick(screen, mouseX, mouseY, button)) {
            return true;
        }
        if (handleDatabaseClick(screen, mouseX, mouseY, button)) {
            return true;
        }
        boolean handled = screen.invokeSuperMouseClicked(mouseX, mouseY, button);
        if (!handled) {
            PersonalDatabaseScreenTargetHelper.closeTransientOverlays(screen);
        }
        return handled;
    }

    static boolean mouseScrolled(PersonalDatabaseScreen screen, double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!PersonalDatabaseScreenCommonHelper.supportsFullUi(screen)) {
            return true;
        }
        if (screen.customExtractOverlayExpanded) {
            return true;
        }
        if (PersonalDatabaseScreenLogHelper.handleLogPanelScroll(screen, scrollY)) {
            return true;
        }
        if (screen.moreTabsExpanded
                && PersonalDatabaseScreenViewSelectorHelper.scrollMoreTabsDropdown(
                        screen,
                        mouseX,
                        mouseY,
                        (int) -Math.signum(scrollY)
                )) {
            return true;
        }
        if (screen.viewSelectorExpanded
                && PersonalDatabaseScreenViewSelectorHelper.scrollViewSelectorColumn(
                        screen,
                        mouseX,
                        mouseY,
                        (int) -Math.signum(scrollY)
                )) {
            return true;
        }
        if (screen.topTabReplaceExpanded
                && PersonalDatabaseScreenTopTabPromptHelper.scrollTopTabReplacePrompt(
                        screen,
                        mouseX,
                        mouseY,
                        (int) -Math.signum(scrollY)
                )) {
            return true;
        }
        if (screen.tabManagementExpanded
                && PersonalDatabaseScreenManagementPanelHelper.scrollManagementList(
                        screen,
                        mouseX,
                        mouseY,
                        (int) -Math.signum(scrollY)
                )) {
            return true;
        }
        if (screen.targetSelectorExpanded
                && PersonalDatabaseScreenGeometry.targetSelectorRect(screen).contains(mouseX, mouseY)
                && PersonalDatabaseScreenTargetHelper.scrollTargetSelector(screen, (int) -Math.signum(scrollY))) {
            return true;
        }
        if (screen.jeiTabSourceOverlayExpanded
                && PersonalDatabaseScreenGeometry.jeiTabSourceOverlayRect(screen).contains(mouseX, mouseY)
                && PersonalDatabaseScreenJeiTabSourceHelper.scrollJeiTabSourceOverlay(screen, (int) -Math.signum(scrollY))) {
            return true;
        }
        if (screen.accessoriesExpanded
                && PersonalDatabaseScreenGeometry.isWithinAccessoriesPanel(screen, mouseX, mouseY)
                && PersonalDatabaseScreenLayoutHelper.scrollAccessories(screen, (int) -Math.signum(scrollY))) {
            return true;
        }
        return screen.invokeSuperMouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    static boolean mouseDragged(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!PersonalDatabaseScreenCommonHelper.supportsFullUi(screen)) {
            return true;
        }
        return PersonalDatabaseScreenGestureHelper.mouseDragged(screen, mouseX, mouseY, button, dragX, dragY);
    }

    static boolean mouseReleased(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button) {
        if (!PersonalDatabaseScreenCommonHelper.supportsFullUi(screen)) {
            return true;
        }
        return PersonalDatabaseScreenGestureHelper.mouseReleased(screen, mouseX, mouseY, button);
    }

    static boolean keyPressed(PersonalDatabaseScreen screen, int keyCode, int scanCode, int modifiers) {
        if (!PersonalDatabaseScreenCommonHelper.supportsFullUi(screen)) {
            return screen.invokeSuperKeyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && PersonalDatabaseScreenPopupInteractionHelper.closeTopOverlay(screen)) {
            return true;
        }
        if (screen.customExtractOverlayExpanded
                && PersonalDatabaseScreenCustomExtractOverlayHelper.keyPressed(screen, keyCode, scanCode, modifiers)) {
            return true;
        }
        if (screen.noteOverlayExpanded
                && PersonalDatabaseScreenNoteOverlayHelper.keyPressed(screen, keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (screen.topTabActionPromptExpanded) {
                PersonalDatabaseScreenTabHelper.applyPrimaryTopTabAction(screen);
                return true;
            }
            if (screen.iconPickerExpanded) {
                PersonalDatabaseScreenManagementHelper.applyIconPickerSelection(screen);
                return true;
            }
            if (screen.tabManagementExpanded && PersonalDatabaseScreenManagementHelper.handleManagementEnter(screen)) {
                return true;
            }
        }
        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_F) {
            int panelIndex = PersonalDatabaseScreenCommonHelper.focusedPanelIndex(screen);
            if (panelIndex >= 0 && panelIndex < screen.panelSearchBoxes.size()) {
                screen.activeSearchTab = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex).scopedTab();
                screen.focusScreen(screen.panelSearchBoxes.get(panelIndex));
                screen.panelSearchBoxes.get(panelIndex).setFocused(true);
                return true;
            }
        }
        if (screen.iconPickerExpanded
                && screen.iconSearchBox != null
                && screen.iconSearchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (screen.tabManagementExpanded
                && screen.managementNameBox != null
                && screen.managementNameBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        for (var searchBox : screen.panelSearchBoxes) {
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (PersonalDatabaseScreenGestureHelper.handleHoveredDatabaseDiscardKey(screen, keyCode, scanCode)) {
            return true;
        }
        return screen.invokeSuperKeyPressed(keyCode, scanCode, modifiers);
    }

    static boolean keyReleased(PersonalDatabaseScreen screen, int keyCode, int scanCode, int modifiers) {
        if (!PersonalDatabaseScreenCommonHelper.supportsFullUi(screen)) {
            return screen.invokeSuperKeyReleased(keyCode, scanCode, modifiers);
        }
        return PersonalDatabaseScreenGestureHelper.keyReleased(screen, keyCode, scanCode, modifiers);
    }

    static boolean charTyped(PersonalDatabaseScreen screen, char codePoint, int modifiers) {
        if (!PersonalDatabaseScreenCommonHelper.supportsFullUi(screen)) {
            return true;
        }
        if (screen.customExtractOverlayExpanded
                && PersonalDatabaseScreenCustomExtractOverlayHelper.charTyped(screen, codePoint, modifiers)) {
            return true;
        }
        if (screen.noteOverlayExpanded
                && PersonalDatabaseScreenNoteOverlayHelper.charTyped(screen, codePoint, modifiers)) {
            return true;
        }
        if (screen.iconPickerExpanded
                && screen.iconSearchBox != null
                && screen.iconSearchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (screen.tabManagementExpanded
                && screen.managementNameBox != null
                && screen.managementNameBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        for (var searchBox : screen.panelSearchBoxes) {
            if (searchBox.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return screen.invokeSuperCharTyped(codePoint, modifiers);
    }

    private static boolean handleDatabaseClick(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button) {
        DatabaseSelectionGestureModel.PointerTarget pointerTarget = PersonalDatabaseScreenGestureHelper.resolvePointerTarget(
                screen,
                mouseX,
                mouseY
        );
        int panelIndex = pointerTarget.panelIndex();
        boolean carryingStack = !screen.databaseMenu.getCarried().isEmpty();
        if (!pointerTarget.isWithinPanel()) {
            return false;
        }
        DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex);
        if (carryingStack) {
            screen.selectionGestureModel.clearSelectionGesture();
            DatabaseClickAction action;
            if (button == 0) {
                action = DatabaseClickAction.STORE_STACK;
            } else if (button == 1) {
                action = DatabaseClickAction.STORE_SINGLE;
            } else {
                return false;
            }
            PersonalDatabaseScreenGestureHelper.prepareForPrimaryDatabaseInteraction(screen);
            if (panel.tab().isAllTab() || panel.tab().isFavoritesTab()) {
                screen.pendingTargetStoresSingle = action == DatabaseClickAction.STORE_SINGLE;
                PersonalDatabaseScreenTargetHelper.openTargetSelector(
                        screen,
                        PersonalDatabaseScreen.TargetSelectorMode.CARRIED_STORE,
                        panelIndex,
                        -1,
                        panel.scopedTab().scope(),
                        ""
                );
                return true;
            }
            PersonalDatabaseScreenLayoutHelper.sendDatabaseClick(
                    screen,
                    panelIndex,
                    pointerTarget.slotIndex() >= 0 ? pointerTarget.slotIndex() : 0,
                    action,
                    panel.scopedTab().scope(),
                    panel.tab().id()
            );
            return true;
        }
        DatabaseSelectionEntry selectionEntry = pointerTarget.isFilledSlot()
                ? PersonalDatabaseScreenSelectionHelper.selectionEntryAt(screen, panelIndex, pointerTarget.slotIndex())
                : null;
        if (button == 0) {
            PersonalDatabaseScreenGestureHelper.prepareForPrimaryDatabaseInteraction(screen);
            if (!screen.databaseMenu.viewState().query().focusedTab().equals(panel.scopedTab())) {
                PersonalDatabaseScreenLayoutHelper.sendQuery(
                        screen,
                        screen.databaseMenu.viewState().query().withFocusedTab(panel.scopedTab())
                );
            }
            if (selectionEntry != null) {
                switch (PersonalDatabasePrimaryClickModel.resolve(
                        Screen.hasShiftDown(),
                        Screen.hasControlDown()
                )) {
                    case TAKE_STACK_TO_INVENTORY -> {
                        screen.selectionGestureModel.clearSelectionGesture();
                        PersonalDatabaseScreenLayoutHelper.sendDatabaseClick(
                                screen,
                                panelIndex,
                                pointerTarget.slotIndex(),
                                DatabaseClickAction.TAKE_STACK_TO_INVENTORY,
                                panel.scopedTab().scope(),
                                panel.tab().id()
                        );
                        return true;
                    }
                    case START_ADDITIVE_SELECTION, START_REPLACE_SELECTION -> {
                    }
                }
            }
            boolean clickedEntrySelected = selectionEntry != null && screen.selectedDatabaseEntries.contains(selectionEntry);
            DatabaseSelectionGestureModel.SelectionMode selectionMode = Screen.hasControlDown()
                    ? DatabaseSelectionGestureModel.SelectionMode.ADDITIVE
                    : DatabaseSelectionGestureModel.SelectionMode.REPLACE;
            screen.selectionGestureModel.beginSelectionGesture(selectionMode, pointerTarget, clickedEntrySelected);
            return true;
        }
        if (button == 1) {
            screen.selectionGestureModel.clearSelectionGesture();
            screen.sortDropdownExpanded = false;
            screen.pagePickerExpanded = false;
            screen.enhancementPanelExpanded = false;
            if (!pointerTarget.isFilledSlot()) {
                if (pointerTarget.type() == DatabaseSelectionGestureModel.PointerTargetType.EMPTY_SLOT) {
                    PersonalDatabaseScreenSelectionHelper.clearSelection(screen);
                    return true;
                }
                if (PersonalDatabaseScreenSelectionHelper.hasSelection(screen)) {
                    PersonalDatabaseScreenSelectionHelper.clearSelection(screen);
                    return true;
                }
                return false;
            }
            if (selectionEntry != null && !screen.selectedDatabaseEntries.contains(selectionEntry)) {
                PersonalDatabaseScreenSelectionHelper.replaceSelection(screen, selectionEntry);
            }
            PersonalDatabaseScreenContextHelper.openContextMenu(screen, panelIndex, pointerTarget.slotIndex());
            return true;
        }
        return false;
    }

}
