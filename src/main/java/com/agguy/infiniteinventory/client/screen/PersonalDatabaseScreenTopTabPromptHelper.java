package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class PersonalDatabaseScreenTopTabPromptHelper {
    private PersonalDatabaseScreenTopTabPromptHelper() {
    }

    static void renderTopTabActionPrompt(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.topTabActionPromptRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0 || screen.pendingTopTabActionTab == null) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 254.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        PersonalDatabaseScreenOverlayRenderHelper.renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);
        guiGraphics.drawString(
                screen.screenFont(),
                PersonalDatabaseScreenCommonHelper.scopedTabLabel(screen, screen.pendingTopTabActionTab),
                panelRect.x() + 8,
                panelRect.y() + 8,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        guiGraphics.fill(
                panelRect.x() + 8,
                panelRect.y() + 8 + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT,
                panelRect.right() - 8,
                panelRect.y() + 9 + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT,
                0x70A89E8C
        );

        boolean canJoin = screen.databaseMenu.viewState().query().visibleTabs().size()
                < PersonalDatabaseScreenCommonHelper.maxVisiblePanels(screen);
        List<Component> options = topTabActionLabels();
        for (int index = 0; index < options.size(); index++) {
            PersonalDatabaseLayout.Rect rowRect = topTabActionRowRect(panelRect, index);
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean enabled = index != 0 || canJoin;
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, false);
            guiGraphics.drawString(
                    screen.screenFont(),
                    options.get(index),
                    rowRect.x() + 6,
                    rowRect.y() + 6,
                    enabled ? PersonalDatabaseScreen.OVERLAY_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR,
                    false
            );
        }
        guiGraphics.pose().popPose();
    }

    static void renderTopTabReplacePrompt(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.topTabReplacePromptRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0 || screen.pendingTopTabActionTab == null) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 255.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        PersonalDatabaseScreenOverlayRenderHelper.renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.top_tab_prompt.replace_title"),
                panelRect.x() + 8,
                panelRect.y() + 8,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        guiGraphics.fill(
                panelRect.x() + 8,
                panelRect.y() + 8 + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT,
                panelRect.right() - 8,
                panelRect.y() + 9 + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT,
                0x70A89E8C
        );

        List<DatabaseScopedTabRef> visibleTabs = screen.databaseMenu.viewState().query().visibleTabs();
        PersonalDatabaseLayout.Rect bodyRect = replacePromptBodyRect(panelRect);
        PersonalDatabaseScreenListHelper.VisibleRange visibleRange = PersonalDatabaseScreenListHelper.visibleRange(
                visibleTabs.size(),
                screen.topTabReplaceScrollIndex,
                PersonalDatabaseScreenListHelper.maxVisibleRows(bodyRect, PersonalDatabaseScreen.TOP_TAB_ACTION_ROW_HEIGHT)
        );
        screen.topTabReplaceScrollIndex = visibleRange.scrollIndex();
        PersonalDatabaseScreenListHelper.enableScissor(guiGraphics, bodyRect);
        for (int index = visibleRange.fromIndex(); index < visibleRange.toIndex(); index++) {
            DatabaseScopedTabRef scopedTab = visibleTabs.get(index);
            DatabaseTab tab = PersonalDatabaseScreenCommonHelper.findTab(screen, scopedTab);
            int rowIndex = index - visibleRange.fromIndex();
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    bodyRect.x(),
                    bodyRect.y() + rowIndex * PersonalDatabaseScreen.TOP_TAB_ACTION_ROW_HEIGHT,
                    bodyRect.width(),
                    PersonalDatabaseScreen.TOP_TAB_ACTION_ROW_HEIGHT - 1
            );
            boolean hovered = rowRect.contains(mouseX, mouseY);
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, false);
            guiGraphics.renderItem(PersonalDatabaseScreenCommonHelper.tabIcon(screen, tab), rowRect.x() + 3, rowRect.y() + 2);
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenCommonHelper.truncateToWidth(
                            screen,
                            PersonalDatabaseScreenCommonHelper.scopedTabLabel(screen, scopedTab).getString(),
                            Math.max(0, rowRect.width() - 28)
                    ),
                    rowRect.x() + 24,
                    rowRect.y() + 6,
                    PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                    false
            );
        }
        guiGraphics.disableScissor();
        PersonalDatabaseScreenListHelper.renderScrollIndicators(screen, guiGraphics, bodyRect, visibleRange);
        guiGraphics.pose().popPose();
    }

    static boolean handleTopTabActionPromptClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.topTabActionPromptExpanded || screen.pendingTopTabActionTab == null) {
            return false;
        }
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.topTabActionPromptRect(screen);
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(panelRect, mouseX, mouseY)) {
            closeTopTabPrompt(screen);
            return true;
        }
        if (!panelRect.contains(mouseX, mouseY)) {
            closeTopTabPrompt(screen);
            return true;
        }
        boolean canJoin = screen.databaseMenu.viewState().query().visibleTabs().size()
                < PersonalDatabaseScreenCommonHelper.maxVisiblePanels(screen);
        for (int index = 0; index < 3; index++) {
            PersonalDatabaseLayout.Rect rowRect = topTabActionRowRect(panelRect, index);
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            if (index == 0 && canJoin) {
                applyJoinCurrentView(screen, screen.pendingTopTabActionTab);
            } else if (index == 1) {
                applySingleView(screen, screen.pendingTopTabActionTab);
            } else if (index == 2) {
                screen.topTabActionPromptExpanded = false;
                screen.topTabReplaceExpanded = true;
                screen.topTabReplaceScrollIndex = 0;
            }
            return true;
        }
        return true;
    }

    static boolean handleTopTabReplacePromptClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.topTabReplaceExpanded || screen.pendingTopTabActionTab == null) {
            return false;
        }
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.topTabReplacePromptRect(screen);
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(panelRect, mouseX, mouseY)) {
            closeTopTabPrompt(screen);
            return true;
        }
        if (!panelRect.contains(mouseX, mouseY)) {
            closeTopTabPrompt(screen);
            return true;
        }
        List<DatabaseScopedTabRef> visibleTabs = screen.databaseMenu.viewState().query().visibleTabs();
        PersonalDatabaseLayout.Rect bodyRect = replacePromptBodyRect(panelRect);
        PersonalDatabaseScreenListHelper.VisibleRange visibleRange = PersonalDatabaseScreenListHelper.visibleRange(
                visibleTabs.size(),
                screen.topTabReplaceScrollIndex,
                PersonalDatabaseScreenListHelper.maxVisibleRows(bodyRect, PersonalDatabaseScreen.TOP_TAB_ACTION_ROW_HEIGHT)
        );
        screen.topTabReplaceScrollIndex = visibleRange.scrollIndex();
        for (int index = visibleRange.fromIndex(); index < visibleRange.toIndex(); index++) {
            int rowIndex = index - visibleRange.fromIndex();
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    bodyRect.x(),
                    bodyRect.y() + rowIndex * PersonalDatabaseScreen.TOP_TAB_ACTION_ROW_HEIGHT,
                    bodyRect.width(),
                    PersonalDatabaseScreen.TOP_TAB_ACTION_ROW_HEIGHT - 1
            );
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            applyReplaceCurrentView(screen, screen.pendingTopTabActionTab, index);
            return true;
        }
        return true;
    }

    static void applyPrimaryTopTabAction(PersonalDatabaseScreen screen) {
        if (screen.pendingTopTabActionTab != null) {
            applySingleView(screen, screen.pendingTopTabActionTab);
        }
    }

    static void closeTopTabPrompt(PersonalDatabaseScreen screen) {
        screen.topTabActionPromptExpanded = false;
        screen.topTabReplaceExpanded = false;
        screen.pendingTopTabActionTab = null;
        screen.topTabReplaceScrollIndex = 0;
    }

    static void handleTopTabSelection(PersonalDatabaseScreen screen, DatabaseScopedTabRef scopedTab) {
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        if (query.visibleTabs().contains(scopedTab)) {
            if (!query.focusedTab().equals(scopedTab)) {
                PersonalDatabaseScreenLayoutHelper.sendQuery(screen, query.withFocusedTab(scopedTab));
            }
            return;
        }
        if (!query.isMultiTabView()) {
            applySingleView(screen, scopedTab);
            return;
        }
        openTopTabActionPrompt(screen, scopedTab);
    }

    private static void openTopTabActionPrompt(PersonalDatabaseScreen screen, DatabaseScopedTabRef scopedTab) {
        screen.pendingTopTabActionTab = scopedTab;
        screen.topTabActionPromptExpanded = true;
        screen.topTabReplaceExpanded = false;
        screen.topTabReplaceScrollIndex = 0;
        screen.moreTabsExpanded = false;
        screen.viewSelectorExpanded = false;
    }

    static void applyJoinCurrentView(PersonalDatabaseScreen screen, DatabaseScopedTabRef scopedTab) {
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        LinkedHashSet<DatabaseScopedTabRef> nextVisibleTabs = new LinkedHashSet<>(query.visibleTabs());
        nextVisibleTabs.add(scopedTab);
        PersonalDatabaseScreenLayoutHelper.sendQuery(
                screen,
                query.withVisibleTabs(new ArrayList<>(nextVisibleTabs)).withFocusedTab(scopedTab)
        );
        closeTopTabPrompt(screen);
    }

    static void applySingleView(PersonalDatabaseScreen screen, DatabaseScopedTabRef scopedTab) {
        PersonalDatabaseScreenLayoutHelper.sendQuery(
                screen,
                screen.databaseMenu.viewState().query().withSingleVisibleTab(scopedTab).withFocusedTab(scopedTab)
        );
        closeTopTabPrompt(screen);
    }

    private static void applyReplaceCurrentView(PersonalDatabaseScreen screen, DatabaseScopedTabRef scopedTab, int replaceIndex) {
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        if (replaceIndex < 0 || replaceIndex >= query.visibleTabs().size()) {
            return;
        }
        java.util.ArrayList<DatabaseScopedTabRef> nextVisibleTabs = new java.util.ArrayList<>(query.visibleTabs());
        nextVisibleTabs.set(replaceIndex, scopedTab);
        PersonalDatabaseScreenLayoutHelper.sendQuery(
                screen,
                query.withVisibleTabs(nextVisibleTabs).withFocusedTab(scopedTab)
        );
        closeTopTabPrompt(screen);
    }

    private static List<Component> topTabActionLabels() {
        return List.of(
                Component.translatable("screen.infiniteinventory.top_tab_prompt.join_current"),
                Component.translatable("screen.infiniteinventory.top_tab_prompt.single_view"),
                Component.translatable("screen.infiniteinventory.top_tab_prompt.replace_current")
        );
    }

    private static PersonalDatabaseLayout.Rect topTabActionRowRect(PersonalDatabaseLayout.Rect panelRect, int index) {
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                        + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                        + 8
                        + index * PersonalDatabaseScreen.TOP_TAB_ACTION_ROW_HEIGHT,
                panelRect.width() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2,
                PersonalDatabaseScreen.TOP_TAB_ACTION_ROW_HEIGHT - 1
        );
    }

    static boolean scrollTopTabReplacePrompt(PersonalDatabaseScreen screen, double mouseX, double mouseY, int deltaRows) {
        if (!screen.topTabReplaceExpanded || deltaRows == 0) {
            return false;
        }
        PersonalDatabaseLayout.Rect bodyRect = replacePromptBodyRect(PersonalDatabaseScreenGeometry.topTabReplacePromptRect(screen));
        if (!bodyRect.contains(mouseX, mouseY)) {
            return false;
        }
        PersonalDatabaseScreenListHelper.VisibleRange visibleRange = PersonalDatabaseScreenListHelper.visibleRange(
                screen.databaseMenu.viewState().query().visibleTabs().size(),
                screen.topTabReplaceScrollIndex,
                PersonalDatabaseScreenListHelper.maxVisibleRows(bodyRect, PersonalDatabaseScreen.TOP_TAB_ACTION_ROW_HEIGHT)
        );
        int maxScrollIndex = Math.max(0, visibleRange.totalRows() - visibleRange.maxVisibleRows());
        int nextScrollIndex = Math.max(0, Math.min(maxScrollIndex, visibleRange.scrollIndex() + deltaRows));
        if (nextScrollIndex == screen.topTabReplaceScrollIndex) {
            return false;
        }
        screen.topTabReplaceScrollIndex = nextScrollIndex;
        return true;
    }

    private static PersonalDatabaseLayout.Rect replacePromptBodyRect(PersonalDatabaseLayout.Rect panelRect) {
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                        + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                        + 8,
                panelRect.width() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2,
                Math.max(0, panelRect.bottom() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                        - (panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                        + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT + 8))
        );
    }
}
