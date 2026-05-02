package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import java.util.List;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

final class PersonalDatabaseScreenGeometry {
    private static final int OVERLAY_CLOSE_BUTTON_SIZE = PersonalDatabaseLayout.CONTROL_HEIGHT;
    private static final int OVERLAY_CLOSE_BUTTON_MARGIN = 4;

    private PersonalDatabaseScreenGeometry() {
    }

    static PersonalDatabaseLayout.Rect overlayCloseButtonRect(PersonalDatabaseLayout.Rect panelRect) {
        return new PersonalDatabaseLayout.Rect(
                panelRect.right() - OVERLAY_CLOSE_BUTTON_SIZE - OVERLAY_CLOSE_BUTTON_MARGIN,
                panelRect.y() + OVERLAY_CLOSE_BUTTON_MARGIN,
                OVERLAY_CLOSE_BUTTON_SIZE,
                OVERLAY_CLOSE_BUTTON_SIZE
        );
    }

    static PersonalDatabaseLayout.Rect viewSelectorRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        PersonalDatabaseScreenFitProfile fitProfile = PersonalDatabaseScreenCommonHelper.fitProfile(screen);
        int rowCount = Math.max(
                PersonalDatabaseScreenCommonHelper.tabsForScope(screen, com.agguy.infiniteinventory.database.DatabaseScope.PERSONAL).size(),
                PersonalDatabaseScreenCommonHelper.tabsForScope(screen, com.agguy.infiniteinventory.database.DatabaseScope.PUBLIC).size()
        );
        int height = fitProfile.showsViewSelectorPreview()
                ? PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2
                        + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                        + 8
                        + 132
                        + 14
                        + PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT
                        + 6
                        + rowCount * PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT
                : PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2
                        + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                        + 8
                        + PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT
                        + 6
                        + rowCount * PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT;
        return centeredOverlayRect(screen, PersonalDatabaseScreen.TAB_SELECTOR_WIDTH, height);
    }

    static PersonalDatabaseLayout.Rect viewSelectorPreviewRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = viewSelectorRect(screen);
        if (!PersonalDatabaseScreenCommonHelper.fitProfile(screen).showsViewSelectorPreview()) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT + 8,
                panelRect.width() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2,
                132
        );
    }

    static PersonalDatabaseLayout.Rect viewSelectorColumnRect(PersonalDatabaseScreen screen, boolean personalColumn) {
        PersonalDatabaseLayout.Rect panelRect = viewSelectorRect(screen);
        int top = PersonalDatabaseScreenCommonHelper.fitProfile(screen).showsViewSelectorPreview()
                ? viewSelectorPreviewRect(screen).bottom() + 14
                : panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT + 8;
        int availableWidth = panelRect.width() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2;
        int width = Math.max(1, (availableWidth - 12) / 2);
        int x = personalColumn
                ? panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                : panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING + width + 12;
        return new PersonalDatabaseLayout.Rect(
                x,
                top,
                width,
                Math.max(1, viewSelectorRect(screen).bottom() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING - top)
        );
    }

    static PersonalDatabaseLayout.Rect viewSelectorScopeHeaderRect(PersonalDatabaseScreen screen, boolean personalColumn) {
        PersonalDatabaseLayout.Rect columnRect = viewSelectorColumnRect(screen, personalColumn);
        return new PersonalDatabaseLayout.Rect(columnRect.x(), columnRect.y(), columnRect.width(), PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT);
    }

    static PersonalDatabaseLayout.Rect viewSelectorScopeRowRect(PersonalDatabaseScreen screen, boolean personalColumn, int rowIndex) {
        PersonalDatabaseLayout.Rect columnRect = viewSelectorColumnRect(screen, personalColumn);
        return new PersonalDatabaseLayout.Rect(
                columnRect.x(),
                columnRect.y() + PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT + 6 + rowIndex * PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT,
                columnRect.width(),
                PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT - 1
        );
    }

    static PersonalDatabaseLayout.Rect moreTabsDropdownRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null || PersonalDatabaseScreenTabHelper.hiddenTopTabs(screen).isEmpty()) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        PersonalDatabaseLayout.Rect anchorRect = PersonalDatabaseScreenTabHelper.moreTabsButtonRect(screen);
        int width = Math.max(PersonalDatabaseScreen.MORE_TABS_WIDTH, anchorRect.width());
        int height = PersonalDatabaseScreenTabHelper.hiddenTopTabs(screen).size() * PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT + 4;
        return dropdownPanelRect(screen, anchorRect, width, height);
    }

    static PersonalDatabaseLayout.Rect targetSelectorRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int rowCount = Math.max(1, PersonalDatabaseScreenTargetHelper.targetSelectorRows(screen).size());
        int height = PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2
                + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                + 8
                + rowCount * PersonalDatabaseScreen.TARGET_SELECTOR_ROW_HEIGHT;
        return centeredOverlayRect(screen, PersonalDatabaseScreen.TARGET_SELECTOR_WIDTH, height);
    }

    static PersonalDatabaseLayout.Rect topTabActionPromptRect(PersonalDatabaseScreen screen) {
        return centeredOverlayRect(screen, PersonalDatabaseScreen.TOP_TAB_ACTION_WIDTH, 132);
    }

    static PersonalDatabaseLayout.Rect topTabReplacePromptRect(PersonalDatabaseScreen screen) {
        int rowCount = Math.max(1, screen.databaseMenu.viewState().query().visibleTabs().size());
        int height = PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2
                + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                + 8
                + rowCount * PersonalDatabaseScreen.TOP_TAB_ACTION_ROW_HEIGHT;
        return centeredOverlayRect(screen, PersonalDatabaseScreen.TOP_TAB_ACTION_WIDTH, height);
    }

    static PersonalDatabaseLayout.Rect enhancementAutoStoreRowRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = enhancementPanelRect(screen);
        int rowY = panelRect.y()
                + PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING
                + PersonalDatabaseScreen.ENHANCEMENT_TITLE_HEIGHT
                + DatabaseEnhancementOption.uiVisibleValues().size()
                * (PersonalDatabaseScreen.ENHANCEMENT_ROW_HEIGHT + PersonalDatabaseScreen.ENHANCEMENT_ROW_GAP);
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING,
                rowY,
                panelRect.width() - PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING * 2,
                PersonalDatabaseScreen.ENHANCEMENT_ROW_HEIGHT
        );
    }

    static PersonalDatabaseLayout.Rect panelSearchFieldRect(PersonalDatabaseScreen screen, int panelIndex) {
        return PersonalDatabaseScreenHeaderGeometry.panelHeaderLayout(screen, panelIndex).searchRect();
    }

    static PersonalDatabaseLayout.Rect panelSortButtonRect(PersonalDatabaseScreen screen, int panelIndex) {
        return PersonalDatabaseScreenHeaderGeometry.panelHeaderLayout(screen, panelIndex).sortRect();
    }

    static PersonalDatabaseLayout.Rect panelPreviousPageButtonRect(PersonalDatabaseScreen screen, int panelIndex) {
        return PersonalDatabaseScreenHeaderGeometry.panelHeaderLayout(screen, panelIndex).previousRect();
    }

    static PersonalDatabaseLayout.Rect panelPageButtonRect(PersonalDatabaseScreen screen, int panelIndex) {
        return PersonalDatabaseScreenHeaderGeometry.panelHeaderLayout(screen, panelIndex).pageRect();
    }

    static PersonalDatabaseLayout.Rect panelNextPageButtonRect(PersonalDatabaseScreen screen, int panelIndex) {
        return PersonalDatabaseScreenHeaderGeometry.panelHeaderLayout(screen, panelIndex).nextRect();
    }

    static PersonalDatabaseLayout.Rect selectorRowRect(
            PersonalDatabaseLayout.Rect panelRect,
            int rowIndex,
            int rowHeight
    ) {
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                        + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                        + 8
                        + rowIndex * rowHeight,
                panelRect.width() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2,
                rowHeight - 1
        );
    }

    static PersonalDatabaseLayout.Rect dropdownPanelRect(
            PersonalDatabaseScreen screen,
            PersonalDatabaseLayout.Rect anchorRect,
            int width,
            int height
    ) {
        if (screen.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int minX = screen.layout.frameRect().x() + PersonalDatabaseScreen.CONTEXT_MENU_MARGIN;
        int maxX = Math.max(minX, screen.layout.frameRect().right() - width - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN);
        int x = Mth.clamp(anchorRect.right() - width, minX, maxX);
        int y = anchoredPopupY(screen.layout.frameRect(), anchorRect, height, 2);
        return new PersonalDatabaseLayout.Rect(x, y, width, height);
    }

    static PersonalDatabaseLayout.Rect centeredOverlayRect(PersonalDatabaseScreen screen, int width, int desiredHeight) {
        if (screen.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int maxWidth = Math.max(1, screen.layout.frameRect().width() - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN * 2);
        int maxHeight = Math.max(1, screen.layout.frameRect().height() - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN * 2);
        int resolvedWidth = Math.min(width, maxWidth);
        int resolvedHeight = Math.min(desiredHeight, maxHeight);
        int x = screen.layout.frameRect().x() + Math.max(0, (screen.layout.frameRect().width() - resolvedWidth) / 2);
        int y = screen.layout.frameRect().y() + Math.max(0, (screen.layout.frameRect().height() - resolvedHeight) / 2);
        return new PersonalDatabaseLayout.Rect(x, y, resolvedWidth, resolvedHeight);
    }

    static PersonalDatabaseLayout.Rect advancedSearchPanelRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        if (screen.settingsPanelExpanded && screen.activeSettingsTab == PersonalDatabaseScreenEnums.SettingsPanelTab.ADVANCED_SEARCH) {
            return PersonalDatabaseScreenSettingsGeometry.settingsContentInnerRect(screen);
        }
        int width = PersonalDatabaseScreen.ADVANCED_SEARCH_PANEL_WIDTH;
        int height = PersonalDatabaseScreen.ADVANCED_SEARCH_PANEL_PADDING * 2
                + PersonalDatabaseScreen.ADVANCED_SEARCH_TITLE_HEIGHT
                + DatabaseSearchField.values().length * PersonalDatabaseScreen.ADVANCED_SEARCH_ROW_HEIGHT
                + Math.max(0, DatabaseSearchField.values().length - 1) * PersonalDatabaseScreen.ADVANCED_SEARCH_ROW_GAP;
        return centeredOverlayRect(screen, width, height);
    }

    static PersonalDatabaseLayout.Rect advancedSearchRowRect(PersonalDatabaseScreen screen, DatabaseSearchField field) {
        PersonalDatabaseLayout.Rect panelRect = advancedSearchPanelRect(screen);
        int rowY = panelRect.y()
                + PersonalDatabaseScreen.ADVANCED_SEARCH_PANEL_PADDING
                + PersonalDatabaseScreen.ADVANCED_SEARCH_TITLE_HEIGHT
                + field.ordinal() * (PersonalDatabaseScreen.ADVANCED_SEARCH_ROW_HEIGHT + PersonalDatabaseScreen.ADVANCED_SEARCH_ROW_GAP);
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + PersonalDatabaseScreen.ADVANCED_SEARCH_PANEL_PADDING,
                rowY,
                panelRect.width() - PersonalDatabaseScreen.ADVANCED_SEARCH_PANEL_PADDING * 2,
                PersonalDatabaseScreen.ADVANCED_SEARCH_ROW_HEIGHT
        );
    }

    static boolean isWithinAdvancedSearchPanel(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.advancedSearchExpanded || screen.layout == null) {
            return false;
        }
        return advancedSearchPanelRect(screen).contains(mouseX, mouseY);
    }

    static PersonalDatabaseLayout.Rect enhancementPanelRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        if (screen.settingsPanelExpanded && screen.activeSettingsTab == PersonalDatabaseScreenEnums.SettingsPanelTab.ENHANCEMENT) {
            return PersonalDatabaseScreenSettingsGeometry.settingsContentInnerRect(screen);
        }
        int height = PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING * 2
                + PersonalDatabaseScreen.ENHANCEMENT_TITLE_HEIGHT
                + DatabaseEnhancementOption.uiVisibleValues().size() * PersonalDatabaseScreen.ENHANCEMENT_ROW_HEIGHT
                + Math.max(0, DatabaseEnhancementOption.uiVisibleValues().size()) * PersonalDatabaseScreen.ENHANCEMENT_ROW_GAP
                + PersonalDatabaseScreen.ENHANCEMENT_ROW_HEIGHT;
        return centeredOverlayRect(screen, PersonalDatabaseScreen.ENHANCEMENT_PANEL_WIDTH, height);
    }

    static PersonalDatabaseLayout.Rect enhancementRowRect(PersonalDatabaseScreen screen, DatabaseEnhancementOption option) {
        PersonalDatabaseLayout.Rect panelRect = enhancementPanelRect(screen);
        List<DatabaseEnhancementOption> visibleOptions = DatabaseEnhancementOption.uiVisibleValues();
        int index = visibleOptions.indexOf(option);
        if (index < 0) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int rowY = panelRect.y()
                + PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING
                + PersonalDatabaseScreen.ENHANCEMENT_TITLE_HEIGHT
                + index * (PersonalDatabaseScreen.ENHANCEMENT_ROW_HEIGHT + PersonalDatabaseScreen.ENHANCEMENT_ROW_GAP);
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING,
                rowY,
                panelRect.width() - PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING * 2,
                PersonalDatabaseScreen.ENHANCEMENT_ROW_HEIGHT
        );
    }

    static boolean isWithinEnhancementPanel(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.enhancementPanelExpanded || screen.layout == null) {
            return false;
        }
        return enhancementPanelRect(screen).contains(mouseX, mouseY);
    }

    static boolean isWithinAccessoriesPanel(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        return screen.layout != null
                && screen.accessoriesExpanded
                && screen.layout.accessoriesPanelRect().contains(mouseX, mouseY);
    }

    @Nullable
    static PersonalDatabaseLayout.AccessorySlotLayout findHoveredAccessorySlot(
            PersonalDatabaseScreen screen,
            double mouseX,
            double mouseY
    ) {
        if (screen.layout == null || !screen.accessoriesExpanded) {
            return null;
        }
        for (PersonalDatabaseLayout.AccessorySlotLayout slotLayout : screen.layout.accessorySlotLayouts()) {
            if (slotLayout.visible() && slotLayout.slotRect().contains(mouseX, mouseY)) {
                return slotLayout;
            }
        }
        return null;
    }

    @Nullable
    static PersonalDatabaseLayout.AccessorySlotLayout resolveAccessorySlotLayout(
            PersonalDatabaseScreen screen,
            @Nullable Slot slot
    ) {
        if (slot == null || screen.layout == null) {
            return null;
        }
        int menuSlotIndex = screen.databaseMenu.slots.indexOf(slot);
        if (!screen.databaseMenu.isAccessorySlotIndex(menuSlotIndex)) {
            return null;
        }
        for (PersonalDatabaseLayout.AccessorySlotLayout slotLayout : screen.layout.accessorySlotLayouts()) {
            if (slotLayout.slotIndex() == menuSlotIndex) {
                return slotLayout;
            }
        }
        return null;
    }

    @Nullable
    static PersonalDatabaseScreen.DatabaseHitResult findDatabaseSlot(
            PersonalDatabaseScreen screen,
            double mouseX,
            double mouseY
    ) {
        if (screen.layout == null) {
            return null;
        }
        for (int panelIndex = 0; panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size(); panelIndex++) {
            int visibleSlotCount = screen.layout.visibleDatabaseSlotCount(panelIndex);
            for (int slotIndex = 0; slotIndex < visibleSlotCount; slotIndex++) {
                PersonalDatabaseLayout.Rect slotRect = screen.layout.visibleDatabaseSlotBounds(panelIndex, slotIndex);
                if (slotRect.contains(mouseX, mouseY)) {
                    return new PersonalDatabaseScreen.DatabaseHitResult(panelIndex, slotIndex);
                }
            }
        }
        return null;
    }

    @Nullable
    static PersonalDatabaseLayout.Rect pagePickerRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null || !screen.pagePickerExpanded || screen.activePagePickerPanelIndex < 0) {
            return null;
        }
        var options = PersonalDatabaseScreenCommonHelper.pagePickerOptions(screen);
        int width = pagePickerWidth(screen, options);
        int height = options.size() * PersonalDatabaseScreen.PAGE_PICKER_ROW_HEIGHT;
        int minX = screen.layout.frameRect().x() + PersonalDatabaseScreen.CONTEXT_MENU_MARGIN;
        int maxX = Math.max(minX, screen.layout.frameRect().right() - width - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN);
        PersonalDatabaseLayout.Rect pageRect = panelPageButtonRect(screen, screen.activePagePickerPanelIndex);
        int x = Mth.clamp(pageRect.centerX() - width / 2, minX, maxX);
        int y = anchoredPopupY(screen.layout.frameRect(), pageRect, height, 2);
        return new PersonalDatabaseLayout.Rect(x, y, width, height);
    }

    static int pagePickerWidth(PersonalDatabaseScreen screen, java.util.List<DatabasePagePickerModel.PageOption> options) {
        int width = PersonalDatabaseScreen.PAGE_PICKER_MIN_WIDTH;
        for (DatabasePagePickerModel.PageOption option : options) {
            width = Math.max(width, screen.screenFont().width(PersonalDatabaseScreenCommonHelper.pagePickerLabel(screen, option)) + 16);
        }
        return width;
    }

    private static int anchoredPopupY(
            PersonalDatabaseLayout.Rect frameRect,
            PersonalDatabaseLayout.Rect anchorRect,
            int height,
            int gap
    ) {
        int minY = frameRect.y() + PersonalDatabaseScreen.CONTEXT_MENU_MARGIN;
        int maxY = Math.max(minY, frameRect.bottom() - height - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN);
        int belowY = anchorRect.bottom() + gap;
        if (belowY + height <= frameRect.bottom() - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN) {
            return belowY;
        }
        int aboveY = anchorRect.y() - gap - height;
        if (aboveY >= minY) {
            return aboveY;
        }
        return Mth.clamp(belowY, minY, maxY);
    }

}
