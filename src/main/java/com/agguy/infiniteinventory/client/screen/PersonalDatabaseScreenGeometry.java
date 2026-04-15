package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

final class PersonalDatabaseScreenGeometry {
    private static final int PANEL_HEADER_TITLE_HEIGHT = 14;
    private static final int PANEL_HEADER_ROW_GAP = 4;
    private static final int PANEL_SORT_BUTTON_WIDTH = 108;

    private PersonalDatabaseScreenGeometry() {
    }

    static PersonalDatabaseLayout.Rect overlayCloseButtonRect(PersonalDatabaseLayout.Rect panelRect) {
        return new PersonalDatabaseLayout.Rect(panelRect.right() - 16, panelRect.y() - 14, 16, 16);
    }

    static PersonalDatabaseLayout.Rect viewSelectorRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int height = PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2
                + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                + 8
                + PersonalDatabaseScreenCommonHelper.currentTabs(screen).size() * PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT;
        return dropdownPanelRect(screen, screen.layout.viewSelectorButtonRect(), PersonalDatabaseScreen.TAB_SELECTOR_WIDTH, height);
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
        int rowCount = Math.max(1, PersonalDatabaseScreenTargetHelper.targetSelectorTabs(screen).size());
        int height = PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2
                + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                + 8
                + rowCount * PersonalDatabaseScreen.TARGET_SELECTOR_ROW_HEIGHT;
        return centeredOverlayRect(screen, PersonalDatabaseScreen.TARGET_SELECTOR_WIDTH, height);
    }

    static PersonalDatabaseLayout.Rect enhancementAutoStoreRowRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = enhancementPanelRect(screen);
        int rowY = panelRect.y()
                + PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING
                + PersonalDatabaseScreen.ENHANCEMENT_TITLE_HEIGHT
                + DatabaseEnhancementOption.orderedValues().size()
                * (PersonalDatabaseScreen.ENHANCEMENT_ROW_HEIGHT + PersonalDatabaseScreen.ENHANCEMENT_ROW_GAP);
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING,
                rowY,
                panelRect.width() - PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING * 2,
                PersonalDatabaseScreen.ENHANCEMENT_ROW_HEIGHT
        );
    }

    static PersonalDatabaseLayout.Rect panelSearchFieldRect(PersonalDatabaseScreen screen, int panelIndex) {
        return panelHeaderLayout(screen, panelIndex).searchRect();
    }

    static PersonalDatabaseLayout.Rect panelSortButtonRect(PersonalDatabaseScreen screen, int panelIndex) {
        return panelHeaderLayout(screen, panelIndex).sortRect();
    }

    static PersonalDatabaseLayout.Rect panelPreviousPageButtonRect(PersonalDatabaseScreen screen, int panelIndex) {
        return panelHeaderLayout(screen, panelIndex).previousRect();
    }

    static PersonalDatabaseLayout.Rect panelPageButtonRect(PersonalDatabaseScreen screen, int panelIndex) {
        return panelHeaderLayout(screen, panelIndex).pageRect();
    }

    static PersonalDatabaseLayout.Rect panelNextPageButtonRect(PersonalDatabaseScreen screen, int panelIndex) {
        return panelHeaderLayout(screen, panelIndex).nextRect();
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

    static String truncateToWidth(PersonalDatabaseScreen screen, String text, int maxWidth) {
        if (maxWidth <= 0 || screen.screenFont().width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = screen.screenFont().width(suffix);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (screen.screenFont().width(builder.toString() + character) + suffixWidth > maxWidth) {
                break;
            }
            builder.append(character);
        }
        return builder.isEmpty() ? "" : builder.append(suffix).toString();
    }

    static PersonalDatabaseLayout.Rect advancedSearchPanelRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        PersonalDatabaseLayout.Rect anchorRect = screen.layout.advancedSearchButtonRect();
        int width = PersonalDatabaseScreen.ADVANCED_SEARCH_PANEL_WIDTH;
        int height = PersonalDatabaseScreen.ADVANCED_SEARCH_PANEL_PADDING * 2
                + PersonalDatabaseScreen.ADVANCED_SEARCH_TITLE_HEIGHT
                + DatabaseSearchField.values().length * PersonalDatabaseScreen.ADVANCED_SEARCH_ROW_HEIGHT
                + Math.max(0, DatabaseSearchField.values().length - 1) * PersonalDatabaseScreen.ADVANCED_SEARCH_ROW_GAP;
        int minX = screen.layout.frameRect().x() + PersonalDatabaseScreen.CONTEXT_MENU_MARGIN;
        int maxX = Math.max(minX, screen.layout.frameRect().right() - width - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN);
        int x = Mth.clamp(anchorRect.right() - width, minX, maxX);
        int minY = anchorRect.bottom() + 4;
        int maxY = Math.max(minY, screen.layout.frameRect().bottom() - height - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN);
        int y = Mth.clamp(minY, minY, maxY);
        return new PersonalDatabaseLayout.Rect(x, y, width, height);
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
        return screen.layout.advancedSearchButtonRect().contains(mouseX, mouseY)
                || advancedSearchPanelRect(screen).contains(mouseX, mouseY);
    }

    static PersonalDatabaseLayout.Rect enhancementPanelRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        PersonalDatabaseLayout.Rect anchorRect = screen.layout.enhancementButtonRect();
        int width = PersonalDatabaseScreen.ENHANCEMENT_PANEL_WIDTH;
        int height = PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING * 2
                + PersonalDatabaseScreen.ENHANCEMENT_TITLE_HEIGHT
                + DatabaseEnhancementOption.orderedValues().size() * PersonalDatabaseScreen.ENHANCEMENT_ROW_HEIGHT
                + Math.max(0, DatabaseEnhancementOption.orderedValues().size()) * PersonalDatabaseScreen.ENHANCEMENT_ROW_GAP
                + PersonalDatabaseScreen.ENHANCEMENT_ROW_HEIGHT;
        int minX = screen.layout.frameRect().x() + PersonalDatabaseScreen.CONTEXT_MENU_MARGIN;
        int maxX = Math.max(minX, screen.layout.frameRect().right() - width - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN);
        int x = Mth.clamp(anchorRect.right() - width, minX, maxX);
        int minY = anchorRect.bottom() + 4;
        int maxY = Math.max(minY, screen.layout.frameRect().bottom() - height - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN);
        int y = Mth.clamp(minY, minY, maxY);
        return new PersonalDatabaseLayout.Rect(x, y, width, height);
    }

    static PersonalDatabaseLayout.Rect enhancementRowRect(PersonalDatabaseScreen screen, DatabaseEnhancementOption option) {
        PersonalDatabaseLayout.Rect panelRect = enhancementPanelRect(screen);
        int rowY = panelRect.y()
                + PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING
                + PersonalDatabaseScreen.ENHANCEMENT_TITLE_HEIGHT
                + option.ordinal() * (PersonalDatabaseScreen.ENHANCEMENT_ROW_HEIGHT + PersonalDatabaseScreen.ENHANCEMENT_ROW_GAP);
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
        return screen.layout.enhancementButtonRect().contains(mouseX, mouseY)
                || enhancementPanelRect(screen).contains(mouseX, mouseY);
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
    static PersonalDatabaseLayout.Rect sortDropdownRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null || !screen.sortDropdownExpanded || screen.activeSortPanelIndex < 0) {
            return null;
        }
        PersonalDatabaseLayout.Rect sortButtonRect = panelSortButtonRect(screen, screen.activeSortPanelIndex);
        int width = Math.max(PersonalDatabaseScreen.SORT_DROPDOWN_WIDTH, sortButtonRect.width());
        int height = com.agguy.infiniteinventory.database.DatabaseSortOption.orderedValues().size()
                * PersonalDatabaseScreen.DROPDOWN_ROW_HEIGHT;
        int minX = screen.layout.frameRect().x() + PersonalDatabaseScreen.CONTEXT_MENU_MARGIN;
        int maxX = Math.max(minX, screen.layout.frameRect().right() - width - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN);
        int x = Mth.clamp(sortButtonRect.x(), minX, maxX);
        int y = anchoredPopupY(screen.layout.frameRect(), sortButtonRect, height, 2);
        return new PersonalDatabaseLayout.Rect(x, y, width, height);
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

    private static PanelHeaderLayout panelHeaderLayout(PersonalDatabaseScreen screen, int panelIndex) {
        if (screen.layout == null || panelIndex < 0 || panelIndex >= screen.layout.databaseViewportCount()) {
            return PanelHeaderLayout.empty();
        }
        PersonalDatabaseLayout.DatabaseViewportLayout viewportLayout = screen.layout.databaseViewportLayout(panelIndex);
        PersonalDatabaseLayout.Rect headerRect = viewportLayout.headerRect();
        if (headerRect.height() <= 0 || headerRect.width() <= 0) {
            return PanelHeaderLayout.empty();
        }

        int controlHeight = PersonalDatabaseLayout.CONTROL_HEIGHT;
        int headerX = headerRect.x();
        int titleY = headerRect.y();
        int searchY = titleY + PANEL_HEADER_TITLE_HEIGHT + PANEL_HEADER_ROW_GAP;
        int controlsY = searchY + controlHeight + PANEL_HEADER_ROW_GAP;
        PersonalDatabaseLayout.Rect searchRect = new PersonalDatabaseLayout.Rect(
                headerX,
                searchY,
                headerRect.width(),
                controlHeight
        );

        int pagerButtonWidth = PersonalDatabaseLayout.PAGE_BUTTON_WIDTH;
        int minPageWidth = 52;
        int maxPageWidth = 74;
        int pageWidth = Mth.clamp(headerRect.width() / 5, minPageWidth, maxPageWidth);
        int pagerWidth = pagerButtonWidth * 2 + pageWidth + PersonalDatabaseLayout.PAGE_BUTTON_GAP * 2;
        int availableSortWidth = headerRect.width() - pagerWidth - PersonalDatabaseLayout.PAGE_BUTTON_GAP;
        int sortWidth = Math.max(64, Math.min(PANEL_SORT_BUTTON_WIDTH, availableSortWidth));
        if (sortWidth + PersonalDatabaseLayout.PAGE_BUTTON_GAP + pagerWidth > headerRect.width()) {
            sortWidth = Math.max(
                    48,
                    headerRect.width() - pagerWidth - PersonalDatabaseLayout.PAGE_BUTTON_GAP
            );
        }
        int pagerX = headerRect.right() - pagerWidth;
        PersonalDatabaseLayout.Rect sortRect = new PersonalDatabaseLayout.Rect(
                headerX,
                controlsY,
                Math.max(1, Math.min(sortWidth, pagerX - headerX - PersonalDatabaseLayout.PAGE_BUTTON_GAP)),
                controlHeight
        );
        int previousX = Math.max(sortRect.right() + PersonalDatabaseLayout.PAGE_BUTTON_GAP, pagerX);
        PersonalDatabaseLayout.Rect previousRect = new PersonalDatabaseLayout.Rect(
                previousX,
                controlsY,
                pagerButtonWidth,
                controlHeight
        );
        PersonalDatabaseLayout.Rect pageRect = new PersonalDatabaseLayout.Rect(
                previousRect.right() + PersonalDatabaseLayout.PAGE_BUTTON_GAP,
                controlsY,
                pageWidth,
                controlHeight
        );
        PersonalDatabaseLayout.Rect nextRect = new PersonalDatabaseLayout.Rect(
                pageRect.right() + PersonalDatabaseLayout.PAGE_BUTTON_GAP,
                controlsY,
                pagerButtonWidth,
                controlHeight
        );
        return new PanelHeaderLayout(searchRect, sortRect, previousRect, pageRect, nextRect);
    }

    private record PanelHeaderLayout(
            PersonalDatabaseLayout.Rect searchRect,
            PersonalDatabaseLayout.Rect sortRect,
            PersonalDatabaseLayout.Rect previousRect,
            PersonalDatabaseLayout.Rect pageRect,
            PersonalDatabaseLayout.Rect nextRect
    ) {
        private static PanelHeaderLayout empty() {
            PersonalDatabaseLayout.Rect empty = PersonalDatabaseLayout.Rect.empty();
            return new PanelHeaderLayout(empty, empty, empty, empty, empty);
        }
    }

}
