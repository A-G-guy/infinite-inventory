package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseSortDirection;
import com.agguy.infiniteinventory.database.DatabaseSortMethod;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;

final class PersonalDatabaseScreenPopupInteractionHelper {
    private PersonalDatabaseScreenPopupInteractionHelper() {
    }

    static boolean handleSortDropdownClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        PersonalDatabaseLayout.Rect dropdownRect = PersonalDatabaseScreenSortDropdownGeometry.dropdownRect(screen);
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
        DatabaseSortOption currentSort = screen.databaseMenu.viewState().query().sortOptionFor(panel.scopedTab());
        for (DatabaseSortDirection direction : DatabaseSortDirection.values()) {
            if (!PersonalDatabaseScreenSortDropdownGeometry.directionButtonRect(screen, direction).contains(mouseX, mouseY)) {
                continue;
            }
            DatabaseSortDropdownModel.SortAction action = DatabaseSortDropdownModel.actionForDirection(currentSort, direction);
            if (action.nextSort() != currentSort) {
                PersonalDatabaseScreenLayoutHelper.sendQuery(
                        screen,
                        screen.databaseMenu.viewState().query()
                                .withSortOption(panel.scopedTab(), action.nextSort())
                                .withFocusedTab(panel.scopedTab()),
                        true
                );
            }
            return true;
        }
        java.util.List<DatabaseSortMethod> sortMethods = DatabaseSortDropdownModel.methodOptions();
        for (int index = 0; index < sortMethods.size(); index++) {
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenSortDropdownGeometry.methodRowRect(screen, index);
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            DatabaseSortDropdownModel.SortAction action = DatabaseSortDropdownModel.actionForMethod(currentSort, sortMethods.get(index));
            if (action.nextSort() != currentSort) {
                PersonalDatabaseScreenLayoutHelper.sendQuery(
                        screen,
                        screen.databaseMenu.viewState().query()
                                .withSortOption(panel.scopedTab(), action.nextSort())
                                .withFocusedTab(panel.scopedTab())
                );
            }
            if (action.closeMenu()) {
                screen.sortDropdownExpanded = false;
                screen.activeSortPanelIndex = -1;
            }
            return true;
        }
        if (dropdownRect.contains(mouseX, mouseY)) {
            return true;
        }
        if (!PersonalDatabaseScreenGeometry.panelSortButtonRect(screen, panelIndex).contains(mouseX, mouseY)) {
            screen.sortDropdownExpanded = false;
            screen.activeSortPanelIndex = -1;
        }
        return false;
    }

    static boolean handlePagePickerClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
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

    static boolean handleContextMenuClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
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

    static boolean closeTopOverlay(PersonalDatabaseScreen screen) {
        if (screen.customExtractOverlayExpanded) {
            PersonalDatabaseScreenCustomExtractOverlayHelper.closeOverlay(screen);
            return true;
        }
        if (screen.noteOverlayExpanded) {
            PersonalDatabaseScreenNoteOverlayHelper.closeOverlay(screen);
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
        if (screen.jeiTabSourceOverlayExpanded) {
            PersonalDatabaseScreenJeiTabSourceHelper.closeJeiTabSourceOverlay(screen);
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
        if (screen.logPanelExpanded) {
            screen.logPanelExpanded = false;
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
