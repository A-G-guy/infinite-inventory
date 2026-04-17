package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

final class PersonalDatabaseScreenInteractionHelper {
    private PersonalDatabaseScreenInteractionHelper() {
    }

    static boolean mouseClicked(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button) {
        if (screen.customExtractOverlayExpanded
                && PersonalDatabaseScreenCustomExtractOverlayHelper.handleMouseClicked(screen, mouseX, mouseY, button)) {
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
        if (screen.pagePickerExpanded && handlePagePickerClick(screen, mouseX, mouseY)) {
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
            screen.invokeSuperMouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (screen.contextMenuExpanded && handleContextMenuClick(screen, mouseX, mouseY)) {
            return true;
        }
        if (screen.sortDropdownExpanded && handleSortDropdownClick(screen, mouseX, mouseY)) {
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
        if (screen.customExtractOverlayExpanded) {
            return true;
        }
        if (screen.targetSelectorExpanded
                && PersonalDatabaseScreenGeometry.targetSelectorRect(screen).contains(mouseX, mouseY)
                && PersonalDatabaseScreenTargetHelper.scrollTargetSelector(screen, (int) -Math.signum(scrollY))) {
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
        return PersonalDatabaseScreenGestureHelper.mouseDragged(screen, mouseX, mouseY, button, dragX, dragY);
    }

    static boolean mouseReleased(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button) {
        return PersonalDatabaseScreenGestureHelper.mouseReleased(screen, mouseX, mouseY, button);
    }

    static boolean keyPressed(PersonalDatabaseScreen screen, int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && closeTopOverlay(screen)) {
            return true;
        }
        if (screen.customExtractOverlayExpanded
                && PersonalDatabaseScreenCustomExtractOverlayHelper.keyPressed(screen, keyCode, scanCode, modifiers)) {
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
        return PersonalDatabaseScreenGestureHelper.keyReleased(screen, keyCode, scanCode, modifiers);
    }

    static boolean charTyped(PersonalDatabaseScreen screen, char codePoint, int modifiers) {
        if (screen.customExtractOverlayExpanded
                && PersonalDatabaseScreenCustomExtractOverlayHelper.charTyped(screen, codePoint, modifiers)) {
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

    private static boolean handleSortDropdownClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        PersonalDatabaseLayout.Rect dropdownRect = PersonalDatabaseScreenGeometry.sortDropdownRect(screen);
        if (dropdownRect == null) {
            return false;
        }
        int panelIndex = PersonalDatabaseScreenCommonHelper.activeSortPanelIndex(screen);
        if (panelIndex < 0 || panelIndex >= PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()) {
            screen.sortDropdownExpanded = false;
            screen.activeSortPanelIndex = -1;
            return false;
        }
        DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex);
        java.util.List<DatabaseSortOption> sortOptions = DatabaseSortOption.orderedValues();
        for (int index = 0; index < sortOptions.size(); index++) {
            int rowY = dropdownRect.y() + index * PersonalDatabaseScreen.DROPDOWN_ROW_HEIGHT;
            if (mouseX < dropdownRect.x()
                    || mouseX >= dropdownRect.right()
                    || mouseY < rowY
                    || mouseY >= rowY + PersonalDatabaseScreen.DROPDOWN_ROW_HEIGHT) {
                continue;
            }
            PersonalDatabaseScreenLayoutHelper.sendQuery(
                    screen,
                    screen.databaseMenu.viewState().query()
                            .withSortOption(panel.tab().id(), sortOptions.get(index))
                            .withFocusedTabId(panel.tab().id())
            );
            screen.sortDropdownExpanded = false;
            screen.activeSortPanelIndex = -1;
            return true;
        }
        if (!PersonalDatabaseScreenGeometry.panelSortButtonRect(screen, panelIndex).contains(mouseX, mouseY)) {
            screen.sortDropdownExpanded = false;
            screen.activeSortPanelIndex = -1;
        }
        return false;
    }

    private static boolean handlePagePickerClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.pagePickerExpanded) {
            return false;
        }
        PersonalDatabaseLayout.Rect pickerRect = PersonalDatabaseScreenGeometry.pagePickerRect(screen);
        int panelIndex = PersonalDatabaseScreenCommonHelper.activePagePickerPanelIndex(screen);
        if (panelIndex < 0 || panelIndex >= PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()) {
            screen.pagePickerExpanded = false;
            screen.activePagePickerPanelIndex = -1;
            return true;
        }
        DatabasePanelView panel = PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex);
        if (PersonalDatabaseScreenGeometry.panelPageButtonRect(screen, panelIndex).contains(mouseX, mouseY)) {
            screen.pagePickerExpanded = false;
            screen.activePagePickerPanelIndex = -1;
            return true;
        }
        if (pickerRect == null) {
            screen.pagePickerExpanded = false;
            screen.activePagePickerPanelIndex = -1;
            return false;
        }
        var options = PersonalDatabaseScreenCommonHelper.pagePickerOptions(screen);
        for (int index = 0; index < options.size(); index++) {
            int rowY = pickerRect.y() + index * PersonalDatabaseScreen.PAGE_PICKER_ROW_HEIGHT;
            if (mouseX < pickerRect.x()
                    || mouseX >= pickerRect.right()
                    || mouseY < rowY
                    || mouseY >= rowY + PersonalDatabaseScreen.PAGE_PICKER_ROW_HEIGHT) {
                continue;
            }
            var option = options.get(index);
            screen.pagePickerExpanded = false;
            screen.activePagePickerPanelIndex = -1;
            if (option.pageIndex() != panel.pageIndex()) {
                PersonalDatabaseScreenLayoutHelper.sendQuery(
                        screen,
                        screen.databaseMenu.viewState().query()
                                .withPageIndex(panel.tab().id(), option.pageIndex())
                                .withFocusedTabId(panel.tab().id())
                );
            }
            return true;
        }
        if (pickerRect.contains(mouseX, mouseY)) {
            return true;
        }
        screen.pagePickerExpanded = false;
        screen.activePagePickerPanelIndex = -1;
        return false;
    }

    private static boolean handleContextMenuClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.contextMenuExpanded) {
            return false;
        }
        PersonalDatabaseContextMenuItem item = PersonalDatabaseScreenContextHelper.contextMenuItemAt(screen, mouseX, mouseY);
        if (item != null) {
            PersonalDatabaseScreenContextHelper.activateContextMenuItem(screen, item);
            return true;
        }
        if (PersonalDatabaseScreenContextHelper.isWithinContextMenu(screen, mouseX, mouseY)) {
            return true;
        }
        PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
        return false;
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
            if (panel.tab().isAllTab()) {
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

    private static boolean closeTopOverlay(PersonalDatabaseScreen screen) {
        if (screen.customExtractOverlayExpanded) {
            PersonalDatabaseScreenCustomExtractOverlayHelper.closeOverlay(screen);
            return true;
        }
        if (screen.contextMenuExpanded) {
            PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
            return true;
        }
        if (screen.iconPickerExpanded) {
            PersonalDatabaseScreenManagementHelper.closeIconPicker(screen);
            return true;
        }
        if (screen.tabManagementExpanded) {
            PersonalDatabaseScreenManagementHelper.closeTabManagementOverlays(screen);
            return true;
        }
        if (screen.targetSelectorExpanded) {
            PersonalDatabaseScreenTargetHelper.closeTargetSelector(screen);
            return true;
        }
        if (screen.topTabReplaceExpanded || screen.topTabActionPromptExpanded) {
            PersonalDatabaseScreenTabHelper.closeTopTabPrompt(screen);
            return true;
        }
        if (screen.moreTabsExpanded) {
            screen.moreTabsExpanded = false;
            return true;
        }
        if (screen.viewSelectorExpanded) {
            screen.viewSelectorExpanded = false;
            return true;
        }
        if (screen.pagePickerExpanded) {
            screen.pagePickerExpanded = false;
            screen.activePagePickerPanelIndex = -1;
            return true;
        }
        if (screen.sortDropdownExpanded) {
            screen.sortDropdownExpanded = false;
            screen.activeSortPanelIndex = -1;
            return true;
        }
        if (screen.enhancementPanelExpanded) {
            screen.enhancementPanelExpanded = false;
            return true;
        }
        if (screen.advancedSearchExpanded) {
            screen.advancedSearchExpanded = false;
            return true;
        }
        return false;
    }
}
