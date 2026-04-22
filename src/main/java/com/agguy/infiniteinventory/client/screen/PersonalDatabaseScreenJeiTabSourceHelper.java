package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.database.JeiCraftingTabSourceConfig;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.JeiCraftingTabSourcePayload;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

final class PersonalDatabaseScreenJeiTabSourceHelper {
    private static final int CHECKBOX_SIZE = 14;
    private static final int CHECKBOX_MARGIN = 3;

    private PersonalDatabaseScreenJeiTabSourceHelper() {
    }

    static void openJeiTabSourceOverlay(PersonalDatabaseScreen screen) {
        PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
        PersonalDatabaseScreenCustomExtractOverlayHelper.closeOverlay(screen);
        screen.sortDropdownExpanded = false;
        screen.pagePickerExpanded = false;
        screen.moreTabsExpanded = false;
        screen.viewSelectorExpanded = false;
        screen.targetSelectorExpanded = false;
        screen.jeiTabSourceOverlayExpanded = true;
        screen.jeiTabSourceScrollIndex = 0;
        screen.jeiTabSourceScopeFilter = screen.databaseMenu.viewState().query().focusedTab().scope();
    }

    static void closeJeiTabSourceOverlay(PersonalDatabaseScreen screen) {
        screen.jeiTabSourceOverlayExpanded = false;
        screen.jeiTabSourceScrollIndex = 0;
    }

    static void renderJeiTabSourceOverlay(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.jeiTabSourceOverlayRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 254.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.jei.crafting_tab_source.title"),
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        guiGraphics.fill(
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                        + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                        - 2,
                panelRect.right() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                        + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                        - 1,
                0x70A89E8C
        );
        PersonalDatabaseScreenOverlayRenderHelper.renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);

        renderScopeFilter(screen, guiGraphics, mouseX, mouseY);

        List<DatabaseTab> tabs = tabRows(screen);
        PersonalDatabaseLayout.Rect bodyRect = jeiTabSourceBodyRect(panelRect);
        int maxVisibleRows = PersonalDatabaseScreenListHelper.maxVisibleRows(bodyRect, PersonalDatabaseScreen.JEI_TAB_SOURCE_ROW_HEIGHT);
        PersonalDatabaseScreenListHelper.VisibleRange visibleRange = PersonalDatabaseScreenListHelper.visibleRange(
                tabs.size(),
                screen.jeiTabSourceScrollIndex,
                maxVisibleRows
        );
        screen.jeiTabSourceScrollIndex = visibleRange.scrollIndex();
        PersonalDatabaseScreenListHelper.enableScissor(guiGraphics, bodyRect);
        for (int index = 0; index < tabs.size(); index++) {
            if (index < visibleRange.fromIndex() || index >= visibleRange.toIndex()) {
                continue;
            }
            int visibleIndex = index - visibleRange.fromIndex();
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.jeiTabSourceRowRect(screen, visibleIndex);
            DatabaseTab tab = tabs.get(index);
            boolean enabled = isTabEnabled(screen, tab.id());
            boolean hovered = rowRect.contains(mouseX, mouseY);
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, false);
            renderCheckbox(screen, guiGraphics, rowRect, enabled);
            int iconX = rowRect.x() + CHECKBOX_SIZE + CHECKBOX_MARGIN * 2;
            guiGraphics.renderItem(PersonalDatabaseScreenCommonHelper.tabIcon(screen, tab), iconX, rowRect.y() + 2);
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenCommonHelper.truncateToWidth(
                            screen,
                            PersonalDatabaseScreenCommonHelper.tabLabel(screen, tab).getString(),
                            Math.max(0, rowRect.right() - iconX - 22)
                    ),
                    iconX + 20,
                    rowRect.y() + 6,
                    PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                    false
            );
        }
        guiGraphics.disableScissor();
        PersonalDatabaseScreenListHelper.renderScrollIndicators(screen, guiGraphics, bodyRect, visibleRange);

        renderActionButtons(screen, guiGraphics, mouseX, mouseY);
        guiGraphics.pose().popPose();
    }

    static boolean handleJeiTabSourceOverlayClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.jeiTabSourceOverlayExpanded) {
            return false;
        }
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.jeiTabSourceOverlayRect(screen);
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(panelRect, mouseX, mouseY)) {
            closeJeiTabSourceOverlay(screen);
            return true;
        }
        if (!panelRect.contains(mouseX, mouseY)) {
            closeJeiTabSourceOverlay(screen);
            return true;
        }
        if (handleScopeFilterClick(screen, mouseX, mouseY)) {
            return true;
        }
        if (handleActionButtonClick(screen, mouseX, mouseY)) {
            return true;
        }
        List<DatabaseTab> tabs = tabRows(screen);
        PersonalDatabaseLayout.Rect bodyRect = jeiTabSourceBodyRect(panelRect);
        int maxVisibleRows = PersonalDatabaseScreenListHelper.maxVisibleRows(bodyRect, PersonalDatabaseScreen.JEI_TAB_SOURCE_ROW_HEIGHT);
        PersonalDatabaseScreenListHelper.VisibleRange visibleRange = PersonalDatabaseScreenListHelper.visibleRange(
                tabs.size(),
                screen.jeiTabSourceScrollIndex,
                maxVisibleRows
        );
        for (int index = visibleRange.fromIndex(); index < visibleRange.toIndex(); index++) {
            int visibleIndex = index - visibleRange.fromIndex();
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.jeiTabSourceRowRect(screen, visibleIndex);
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            DatabaseTab tab = tabs.get(index);
            toggleTab(screen, tab.id());
            return true;
        }
        return true;
    }

    static boolean scrollJeiTabSourceOverlay(PersonalDatabaseScreen screen, int deltaRows) {
        if (!screen.jeiTabSourceOverlayExpanded || deltaRows == 0) {
            return false;
        }
        List<DatabaseTab> tabs = tabRows(screen);
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.jeiTabSourceOverlayRect(screen);
        PersonalDatabaseLayout.Rect bodyRect = jeiTabSourceBodyRect(panelRect);
        int maxVisibleRows = PersonalDatabaseScreenListHelper.maxVisibleRows(bodyRect, PersonalDatabaseScreen.JEI_TAB_SOURCE_ROW_HEIGHT);
        int maxScrollIndex = Math.max(0, tabs.size() - maxVisibleRows);
        int nextScrollIndex = Math.max(0, Math.min(maxScrollIndex, screen.jeiTabSourceScrollIndex + deltaRows));
        if (nextScrollIndex == screen.jeiTabSourceScrollIndex) {
            return false;
        }
        screen.jeiTabSourceScrollIndex = nextScrollIndex;
        return true;
    }

    static List<DatabaseTab> tabRows(PersonalDatabaseScreen screen) {
        return screen.databaseMenu.viewState().tabsForScope(screen.jeiTabSourceScopeFilter);
    }

    private static void renderScopeFilter(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (boolean personal : new boolean[]{true, false}) {
            PersonalDatabaseLayout.Rect rect = PersonalDatabaseScreenGeometry.jeiTabSourceScopeHeaderRect(screen, personal);
            DatabaseScope scope = personal ? DatabaseScope.PERSONAL : DatabaseScope.PUBLIC;
            boolean selected = screen.jeiTabSourceScopeFilter == scope;
            boolean hovered = rect.contains(mouseX, mouseY);
            VanillaWidgetRenderer.renderOverlayChip(guiGraphics, rect, hovered, selected, true);
            PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                    screen,
                    guiGraphics,
                    Component.translatable(scope.translationKey()),
                    rect.x(),
                    rect.right(),
                    rect.y() + 6,
                    selected ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR
            );
        }
    }

    private static boolean handleScopeFilterClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        for (boolean personal : new boolean[]{true, false}) {
            PersonalDatabaseLayout.Rect rect = PersonalDatabaseScreenGeometry.jeiTabSourceScopeHeaderRect(screen, personal);
            if (rect.contains(mouseX, mouseY)) {
                screen.jeiTabSourceScopeFilter = personal ? DatabaseScope.PERSONAL : DatabaseScope.PUBLIC;
                screen.jeiTabSourceScrollIndex = 0;
                return true;
            }
        }
        return false;
    }

    private static void renderActionButtons(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (boolean selectAll : new boolean[]{true, false}) {
            PersonalDatabaseLayout.Rect rect = PersonalDatabaseScreenGeometry.jeiTabSourceButtonRect(screen, selectAll);
            boolean hovered = rect.contains(mouseX, mouseY);
            VanillaWidgetRenderer.renderOverlayChip(guiGraphics, rect, hovered, false, true);
            PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                    screen,
                    guiGraphics,
                    Component.translatable(selectAll
                            ? "screen.infiniteinventory.jei.crafting_tab_source.select_all"
                            : "screen.infiniteinventory.jei.crafting_tab_source.clear_all"),
                    rect.x(),
                    rect.right(),
                    rect.y() + 6,
                    PersonalDatabaseScreen.OVERLAY_TEXT_COLOR
            );
        }
    }

    private static boolean handleActionButtonClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        for (boolean selectAll : new boolean[]{true, false}) {
            PersonalDatabaseLayout.Rect rect = PersonalDatabaseScreenGeometry.jeiTabSourceButtonRect(screen, selectAll);
            if (rect.contains(mouseX, mouseY)) {
                applyAllTabs(screen, selectAll);
                return true;
            }
        }
        return false;
    }

    private static void renderCheckbox(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect rowRect, boolean checked) {
        int x = rowRect.x() + CHECKBOX_MARGIN;
        int y = rowRect.y() + (rowRect.height() - CHECKBOX_SIZE) / 2;
        PersonalDatabaseLayout.Rect checkboxRect = new PersonalDatabaseLayout.Rect(x, y, CHECKBOX_SIZE, CHECKBOX_SIZE);
        VanillaWidgetRenderer.renderOverlayChip(guiGraphics, checkboxRect, false, checked, true);
        if (checked) {
            int cx = x + CHECKBOX_SIZE / 2;
            int cy = y + CHECKBOX_SIZE / 2;
            guiGraphics.fill(cx - 2, cy - 1, cx + 3, cy + 2, PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR);
            guiGraphics.fill(cx - 1, cy - 2, cx + 2, cy + 3, PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR);
        }
    }

    private static boolean isTabEnabled(PersonalDatabaseScreen screen, String tabId) {
        return screen.databaseMenu.viewState().jeiCraftingTabSources().isTabEnabled(screen.jeiTabSourceScopeFilter, tabId);
    }

    private static void toggleTab(PersonalDatabaseScreen screen, String tabId) {
        boolean currentlyEnabled = isTabEnabled(screen, tabId);
        boolean newEnabled = !currentlyEnabled;
        updateLocalJeiTabSourceConfig(screen, screen.jeiTabSourceScopeFilter, tabId, newEnabled);
        PacketDistributor.sendToServer(new JeiCraftingTabSourcePayload(
                screen.jeiTabSourceScopeFilter,
                tabId,
                newEnabled
        ));
    }

    private static void applyAllTabs(PersonalDatabaseScreen screen, boolean enable) {
        List<DatabaseTab> tabs = tabRows(screen);
        for (DatabaseTab tab : tabs) {
            if (isTabEnabled(screen, tab.id()) != enable) {
                updateLocalJeiTabSourceConfig(screen, screen.jeiTabSourceScopeFilter, tab.id(), enable);
                PacketDistributor.sendToServer(new JeiCraftingTabSourcePayload(
                        screen.jeiTabSourceScopeFilter,
                        tab.id(),
                        enable
                ));
            }
        }
    }

    private static void updateLocalJeiTabSourceConfig(PersonalDatabaseScreen screen, DatabaseScope scope, String tabId, boolean enabled) {
        DatabaseViewState currentState = screen.databaseMenu.viewState();
        JeiCraftingTabSourceConfig currentConfig = currentState.jeiCraftingTabSources();
        JeiCraftingTabSourceConfig updatedConfig = currentConfig.withTabEnabled(scope, tabId, enabled);
        if (updatedConfig == currentConfig) {
            return;
        }
        DatabaseViewState updatedState = new DatabaseViewState(
                currentState.containerId(),
                currentState.sessionId(),
                currentState.query(),
                currentState.enhancementConfig(),
                currentState.autoStoreTarget(),
                updatedConfig,
                currentState.personalTabs(),
                currentState.publicTabs(),
                currentState.panels()
        );
        screen.databaseMenu.applyViewState(updatedState);
    }

    private static PersonalDatabaseLayout.Rect jeiTabSourceBodyRect(PersonalDatabaseLayout.Rect panelRect) {
        int contentTop = panelRect.y()
                + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                + 8
                + PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT
                + 6;
        int contentBottom = panelRect.bottom()
                - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                - PersonalDatabaseScreen.JEI_TAB_SOURCE_ROW_HEIGHT
                - 8;
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                contentTop,
                Math.max(0, panelRect.width() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2),
                Math.max(0, contentBottom - contentTop)
        );
    }
}
