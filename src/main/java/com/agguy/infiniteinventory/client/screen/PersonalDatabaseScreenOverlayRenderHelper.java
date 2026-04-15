package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class PersonalDatabaseScreenOverlayRenderHelper {
    private PersonalDatabaseScreenOverlayRenderHelper() {
    }

    static void renderAdvancedSearchPanel(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.advancedSearchPanelRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 240.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.search_advanced_title"),
                panelRect.x() + PersonalDatabaseScreen.ADVANCED_SEARCH_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.ADVANCED_SEARCH_PANEL_PADDING,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        guiGraphics.fill(
                panelRect.x() + PersonalDatabaseScreen.ADVANCED_SEARCH_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.ADVANCED_SEARCH_PANEL_PADDING
                        + PersonalDatabaseScreen.ADVANCED_SEARCH_TITLE_HEIGHT
                        - 2,
                panelRect.right() - PersonalDatabaseScreen.ADVANCED_SEARCH_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.ADVANCED_SEARCH_PANEL_PADDING
                        + PersonalDatabaseScreen.ADVANCED_SEARCH_TITLE_HEIGHT
                        - 1,
                0x70A89E8C
        );
        renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);

        DatabaseSearchConfig searchConfig = screen.databaseMenu.viewState().query().searchConfig();
        for (DatabaseSearchField field : DatabaseSearchField.values()) {
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.advancedSearchRowRect(screen, field);
            PersonalDatabaseLayout.Rect toggleRect = new PersonalDatabaseLayout.Rect(
                    rowRect.x(),
                    rowRect.y(),
                    PersonalDatabaseScreen.ADVANCED_SEARCH_TOGGLE_WIDTH,
                    PersonalDatabaseScreen.ADVANCED_SEARCH_ROW_HEIGHT
            );
            PersonalDatabaseLayout.Rect weightRect = new PersonalDatabaseLayout.Rect(
                    rowRect.right() - PersonalDatabaseScreen.ADVANCED_SEARCH_WEIGHT_WIDTH,
                    rowRect.y(),
                    PersonalDatabaseScreen.ADVANCED_SEARCH_WEIGHT_WIDTH,
                    PersonalDatabaseScreen.ADVANCED_SEARCH_ROW_HEIGHT
            );
            DatabaseSearchWeight weight = searchConfig.weightFor(field);
            boolean enabled = weight != DatabaseSearchWeight.OFF;
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, rowRect.contains(mouseX, mouseY), false);
            VanillaWidgetRenderer.renderOverlayChip(
                    guiGraphics,
                    toggleRect,
                    toggleRect.contains(mouseX, mouseY),
                    enabled,
                    PersonalDatabaseScreenCommonHelper.isAdvancedToggleClickable(field, searchConfig)
            );
            VanillaWidgetRenderer.renderOverlayChip(
                    guiGraphics,
                    weightRect,
                    weightRect.contains(mouseX, mouseY),
                    enabled,
                    enabled
            );
            PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                    screen,
                    guiGraphics,
                    Component.literal(enabled ? "ON" : "OFF"),
                    toggleRect.x(),
                    toggleRect.right(),
                    toggleRect.y() + 6,
                    enabled ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR
            );
            int labelX = rowRect.x() + PersonalDatabaseScreen.ADVANCED_SEARCH_TOGGLE_WIDTH + 6;
            int labelWidth = Math.max(
                    0,
                    rowRect.width() - PersonalDatabaseScreen.ADVANCED_SEARCH_TOGGLE_WIDTH
                            - PersonalDatabaseScreen.ADVANCED_SEARCH_WEIGHT_WIDTH
                            - 12
            );
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenGeometry.truncateToWidth(
                            screen,
                            Component.translatable(field.translationKey()).getString(),
                            labelWidth
                    ),
                    labelX,
                    rowRect.y() + 6,
                    enabled ? PersonalDatabaseScreen.OVERLAY_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR,
                    false
            );
            PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                    screen,
                    guiGraphics,
                    enabled ? Component.translatable(weight.translationKey()) : Component.literal("OFF"),
                    weightRect.x(),
                    weightRect.right(),
                    weightRect.y() + 6,
                    enabled ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR
            );
        }
        guiGraphics.pose().popPose();
    }

    static void renderEnhancementPanel(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.enhancementPanelRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 245.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.enhancement_title"),
                panelRect.x() + PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        guiGraphics.fill(
                panelRect.x() + PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING
                        + PersonalDatabaseScreen.ENHANCEMENT_TITLE_HEIGHT
                        - 2,
                panelRect.right() - PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.ENHANCEMENT_PANEL_PADDING
                        + PersonalDatabaseScreen.ENHANCEMENT_TITLE_HEIGHT
                        - 1,
                0x70A89E8C
        );
        renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);

        DatabaseEnhancementConfig config = screen.databaseMenu.viewState().enhancementConfig();
        for (DatabaseEnhancementOption option : DatabaseEnhancementOption.orderedValues()) {
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.enhancementRowRect(screen, option);
            PersonalDatabaseLayout.Rect toggleRect = new PersonalDatabaseLayout.Rect(
                    rowRect.x(),
                    rowRect.y(),
                    PersonalDatabaseScreen.ENHANCEMENT_TOGGLE_WIDTH,
                    PersonalDatabaseScreen.ENHANCEMENT_ROW_HEIGHT
            );
            boolean enabled = config.isEnabled(option);
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, rowRect.contains(mouseX, mouseY), false);
            VanillaWidgetRenderer.renderOverlayChip(guiGraphics, toggleRect, toggleRect.contains(mouseX, mouseY), enabled, true);
            PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                    screen,
                    guiGraphics,
                    Component.literal(enabled ? "ON" : "OFF"),
                    toggleRect.x(),
                    toggleRect.right(),
                    toggleRect.y() + 6,
                    enabled ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR
            );
            int labelX = rowRect.x() + PersonalDatabaseScreen.ENHANCEMENT_TOGGLE_WIDTH + 6;
            int labelWidth = Math.max(0, rowRect.width() - PersonalDatabaseScreen.ENHANCEMENT_TOGGLE_WIDTH - 8);
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenGeometry.truncateToWidth(
                            screen,
                            Component.translatable(option.translationKey()).getString(),
                            labelWidth
                    ),
                    labelX,
                    rowRect.y() + 6,
                    enabled ? PersonalDatabaseScreen.OVERLAY_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR,
                    false
            );
        }
        PersonalDatabaseLayout.Rect autoStoreRowRect = PersonalDatabaseScreenGeometry.enhancementAutoStoreRowRect(screen);
        boolean hovered = autoStoreRowRect.contains(mouseX, mouseY);
        VanillaWidgetRenderer.renderOverlayRow(guiGraphics, autoStoreRowRect, hovered, false);
        guiGraphics.drawString(
                screen.screenFont(),
                PersonalDatabaseScreenGeometry.truncateToWidth(
                        screen,
                        Component.translatable("screen.infiniteinventory.enhancement.auto_store_target").getString(),
                        Math.max(0, autoStoreRowRect.width() - 70)
                ),
                autoStoreRowRect.x() + 6,
                autoStoreRowRect.y() + 6,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                screen,
                guiGraphics,
                PersonalDatabaseScreenCommonHelper.tabLabel(
                        screen,
                        PersonalDatabaseScreenCommonHelper.findTab(screen, screen.databaseMenu.viewState().autoStoreTargetTabId())
                ),
                autoStoreRowRect.right() - 62,
                autoStoreRowRect.right() - 6,
                autoStoreRowRect.y() + 6,
                PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR
        );
        guiGraphics.pose().popPose();
    }

    static void renderSortDropdown(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect dropdownRect = PersonalDatabaseScreenGeometry.sortDropdownRect(screen);
        if (dropdownRect == null) {
            return;
        }
        int panelIndex = PersonalDatabaseScreenCommonHelper.activeSortPanelIndex(screen);
        DatabaseSortOption currentSort = panelIndex >= 0 && panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()
                ? screen.databaseMenu.viewState().query().sortOptionFor(
                        PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex).tab().id()
                )
                : screen.databaseMenu.viewState().query().sortOption();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 250.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, dropdownRect);
        List<DatabaseSortOption> sortOptions = DatabaseSortOption.orderedValues();
        for (int index = 0; index < sortOptions.size(); index++) {
            DatabaseSortOption option = sortOptions.get(index);
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    dropdownRect.x() + 2,
                    dropdownRect.y() + index * PersonalDatabaseScreen.DROPDOWN_ROW_HEIGHT + 2,
                    dropdownRect.width() - 4,
                    PersonalDatabaseScreen.DROPDOWN_ROW_HEIGHT - 1
            );
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean selected = currentSort == option;
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, selected);
            Component label = selected
                    ? Component.translatable(option.translationKey()).withStyle(ChatFormatting.GOLD)
                    : Component.translatable(option.translationKey());
            guiGraphics.drawString(
                    screen.screenFont(),
                    label,
                    rowRect.x() + 6,
                    rowRect.y() + 5,
                    selected ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                    false
            );
        }
        guiGraphics.pose().popPose();
    }

    static void renderPagePicker(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect pickerRect = PersonalDatabaseScreenGeometry.pagePickerRect(screen);
        if (pickerRect == null) {
            return;
        }
        List<DatabasePagePickerModel.PageOption> options = PersonalDatabaseScreenCommonHelper.pagePickerOptions(screen);
        int panelIndex = PersonalDatabaseScreenCommonHelper.activePagePickerPanelIndex(screen);
        int currentPageIndex = panelIndex >= 0 && panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size()
                ? PersonalDatabaseScreenCommonHelper.currentPanels(screen).get(panelIndex).pageIndex()
                : 0;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 255.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, pickerRect);
        for (int index = 0; index < options.size(); index++) {
            DatabasePagePickerModel.PageOption option = options.get(index);
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    pickerRect.x() + 2,
                    pickerRect.y() + index * PersonalDatabaseScreen.PAGE_PICKER_ROW_HEIGHT + 2,
                    pickerRect.width() - 4,
                    PersonalDatabaseScreen.PAGE_PICKER_ROW_HEIGHT - 1
            );
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean selected = option.pageIndex() == currentPageIndex;
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, selected);
            Component label = selected
                    ? PersonalDatabaseScreenCommonHelper.pagePickerLabel(screen, option).copy().withStyle(ChatFormatting.GOLD)
                    : PersonalDatabaseScreenCommonHelper.pagePickerLabel(screen, option);
            guiGraphics.drawString(
                    screen.screenFont(),
                    label,
                    rowRect.x() + 6,
                    rowRect.y() + 5,
                    selected ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                    false
            );
        }
        guiGraphics.pose().popPose();
    }

    static void renderContextMenu(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!screen.contextMenuExpanded) {
            return;
        }
        int menuWidth = PersonalDatabaseScreenCommonHelper.contextMenuWidth(screen);
        PersonalDatabaseLayout.Rect menuRect = new PersonalDatabaseLayout.Rect(
                screen.contextMenuX,
                screen.contextMenuY,
                menuWidth,
                PersonalDatabaseScreen.CONTEXT_MENU_ACTIONS.length * PersonalDatabaseScreen.CONTEXT_MENU_ROW_HEIGHT
        );
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 260.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, menuRect);
        for (int index = 0; index < PersonalDatabaseScreen.CONTEXT_MENU_ACTIONS.length; index++) {
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    menuRect.x() + 2,
                    menuRect.y() + index * PersonalDatabaseScreen.CONTEXT_MENU_ROW_HEIGHT + 2,
                    menuRect.width() - 4,
                    PersonalDatabaseScreen.CONTEXT_MENU_ROW_HEIGHT - 1
            );
            boolean hovered = rowRect.contains(mouseX, mouseY);
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, false);
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenCommonHelper.contextMenuLabel(PersonalDatabaseScreen.CONTEXT_MENU_ACTIONS[index]),
                    rowRect.x() + 6,
                    rowRect.y() + 5,
                    PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                    false
            );
        }
        guiGraphics.pose().popPose();
    }

    static void renderOverlayCloseButton(
            PersonalDatabaseScreen screen,
            GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect panelRect,
            int mouseX,
            int mouseY
    ) {
        PersonalDatabaseLayout.Rect closeRect = PersonalDatabaseScreenGeometry.overlayCloseButtonRect(panelRect);
        boolean hovered = closeRect.contains(mouseX, mouseY);
        VanillaWidgetRenderer.renderOverlayChip(guiGraphics, closeRect, hovered, false, true);
        PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                screen,
                guiGraphics,
                Component.literal("X"),
                closeRect.x() + 1,
                closeRect.right() - 1,
                closeRect.y() + 4,
                hovered ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR
        );
    }

    static boolean isOverlayCloseClicked(PersonalDatabaseLayout.Rect panelRect, double mouseX, double mouseY) {
        return PersonalDatabaseScreenGeometry.overlayCloseButtonRect(panelRect).contains(mouseX, mouseY);
    }
}
