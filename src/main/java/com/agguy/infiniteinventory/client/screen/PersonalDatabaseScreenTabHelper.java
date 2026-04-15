package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class PersonalDatabaseScreenTabHelper {
    private PersonalDatabaseScreenTabHelper() {
    }

    static List<DatabaseTab> visibleTopTabs(PersonalDatabaseScreen screen) {
        List<DatabaseTab> tabs = PersonalDatabaseScreenCommonHelper.currentTabs(screen);
        if (screen.layout == null || tabs.size() <= 1) {
            return tabs;
        }
        int gapWithoutMore = inlineTabGap(tabs.size(), false);
        int maxVisibleWithoutMore = Math.max(
                1,
                (screen.layout.tabBarRect().width() + gapWithoutMore)
                        / (PersonalDatabaseScreen.INLINE_TAB_MIN_WIDTH + gapWithoutMore)
        );
        if (tabs.size() <= maxVisibleWithoutMore) {
            return tabs;
        }

        int gap = inlineTabGap(tabs.size(), true);
        int availableWidth = Math.max(1, screen.layout.tabBarRect().width() - PersonalDatabaseScreen.INLINE_TAB_MORE_WIDTH - gap);
        int maxVisibleWithMore = Math.max(
                1,
                (availableWidth + gap) / (PersonalDatabaseScreen.INLINE_TAB_MIN_WIDTH_WITH_MORE + gap)
        );
        int visibleCount = Math.max(1, maxVisibleWithMore);

        LinkedHashSet<String> visibleTabIds = new LinkedHashSet<>();
        for (DatabaseTab tab : tabs) {
            if (visibleTabIds.size() >= visibleCount) {
                break;
            }
            visibleTabIds.add(tab.id());
        }

        String focusedTabId = screen.databaseMenu.viewState().query().focusedTabId();
        if (!visibleTabIds.contains(focusedTabId)) {
            List<String> orderedTabIds = new ArrayList<>(visibleTabIds);
            if (!orderedTabIds.isEmpty()) {
                orderedTabIds.set(orderedTabIds.size() - 1, focusedTabId);
                visibleTabIds.clear();
                visibleTabIds.addAll(orderedTabIds);
            }
        }

        List<DatabaseTab> visibleTabs = new ArrayList<>(visibleTabIds.size());
        for (DatabaseTab tab : tabs) {
            if (visibleTabIds.contains(tab.id())) {
                visibleTabs.add(tab);
            }
        }
        return List.copyOf(visibleTabs);
    }

    static List<DatabaseTab> hiddenTopTabs(PersonalDatabaseScreen screen) {
        LinkedHashSet<String> visibleTabIds = new LinkedHashSet<>();
        for (DatabaseTab tab : visibleTopTabs(screen)) {
            visibleTabIds.add(tab.id());
        }
        List<DatabaseTab> hiddenTabs = new ArrayList<>();
        for (DatabaseTab tab : PersonalDatabaseScreenCommonHelper.currentTabs(screen)) {
            if (!visibleTabIds.contains(tab.id())) {
                hiddenTabs.add(tab);
            }
        }
        return List.copyOf(hiddenTabs);
    }

    static PersonalDatabaseLayout.Rect topTabRect(
            PersonalDatabaseScreen screen,
            int index,
            int visibleTabCount,
            boolean hasMore
    ) {
        if (screen.layout == null || visibleTabCount <= 0) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        PersonalDatabaseLayout.Rect barRect = screen.layout.tabBarRect();
        int gap = inlineTabGap(visibleTabCount, hasMore);
        int availableWidth = Math.max(1, barRect.width() - (hasMore ? PersonalDatabaseScreen.INLINE_TAB_MORE_WIDTH + gap : 0));
        int totalGap = Math.max(0, visibleTabCount - 1) * gap;
        int tabWidth = Math.max(1, (availableWidth - totalGap) / visibleTabCount);
        int x = barRect.x() + index * (tabWidth + gap);
        int right = index == visibleTabCount - 1 ? barRect.x() + availableWidth : x + tabWidth;
        return new PersonalDatabaseLayout.Rect(x, barRect.y(), Math.max(1, right - x), barRect.height());
    }

    static PersonalDatabaseLayout.Rect moreTabsButtonRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null || hiddenTopTabs(screen).isEmpty()) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        PersonalDatabaseLayout.Rect barRect = screen.layout.tabBarRect();
        int gap = inlineTabGap(visibleTopTabs(screen).size(), true);
        int availableWidth = Math.max(1, barRect.width() - PersonalDatabaseScreen.INLINE_TAB_MORE_WIDTH - gap);
        int x = barRect.x() + availableWidth + gap;
        return new PersonalDatabaseLayout.Rect(x, barRect.y(), Math.max(1, barRect.right() - x), barRect.height());
    }

    static int findDatabasePanel(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (screen.layout == null) {
            return -1;
        }
        for (int panelIndex = 0; panelIndex < PersonalDatabaseScreenCommonHelper.currentPanels(screen).size(); panelIndex++) {
            if (screen.layout.databaseViewportLayout(panelIndex).panelRect().contains(mouseX, mouseY)) {
                return panelIndex;
            }
        }
        return -1;
    }

    static void renderTabs(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (screen.layout == null) {
            return;
        }
        List<DatabaseTab> visibleTabs = visibleTopTabs(screen);
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        for (int index = 0; index < visibleTabs.size(); index++) {
            DatabaseTab tab = visibleTabs.get(index);
            PersonalDatabaseLayout.Rect tabRect = topTabRect(screen, index, visibleTabs.size(), !hiddenTopTabs(screen).isEmpty());
            boolean hovered = tabRect.contains(mouseX, mouseY);
            boolean selected = query.visibleTabIds().contains(tab.id());
            VanillaWidgetRenderer.renderTab(guiGraphics, tabRect, selected, hovered);
            guiGraphics.renderItem(PersonalDatabaseScreenCommonHelper.tabIcon(screen, tab), tabRect.x() + PersonalDatabaseScreen.TAB_ICON_LEFT_PADDING, tabRect.y() + 4);
            int labelX = tabRect.x() + PersonalDatabaseScreen.TAB_ICON_LEFT_PADDING
                    + PersonalDatabaseScreen.TAB_ICON_SIZE
                    + PersonalDatabaseScreen.TAB_TEXT_GAP;
            int labelWidth = Math.max(0, tabRect.right() - 4 - labelX);
            int color = query.focusedTabId().equals(tab.id()) ? 0x404040 : 0xFFFFFF;
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenGeometry.truncateToWidth(
                            screen,
                            PersonalDatabaseScreenCommonHelper.tabLabel(screen, tab).getString(),
                            labelWidth
                    ),
                    labelX,
                    tabRect.y() + 8,
                    color,
                    false
            );
        }
        if (!hiddenTopTabs(screen).isEmpty()) {
            PersonalDatabaseLayout.Rect moreRect = moreTabsButtonRect(screen);
            boolean hovered = moreRect.contains(mouseX, mouseY);
            VanillaWidgetRenderer.renderTab(guiGraphics, moreRect, screen.moreTabsExpanded, hovered);
            PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                    screen,
                    guiGraphics,
                    Component.translatable("screen.infiniteinventory.tab.more"),
                    moreRect.x(),
                    moreRect.right(),
                    moreRect.y() + 8,
                    hovered ? 0x404040 : 0xFFFFFF
            );
        }
    }

    static void renderViewSelector(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.viewSelectorRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 252.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.visible_tabs_title"),
                panelRect.x() + 8,
                panelRect.y() + 8,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        guiGraphics.fill(panelRect.x() + 8, panelRect.y() + 20, panelRect.right() - 8, panelRect.y() + 21, 0x70A89E8C);
        PersonalDatabaseScreenOverlayRenderHelper.renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);

        DatabaseQuery query = screen.databaseMenu.viewState().query();
        List<DatabaseTab> tabs = PersonalDatabaseScreenCommonHelper.currentTabs(screen);
        for (int index = 0; index < tabs.size(); index++) {
            DatabaseTab tab = tabs.get(index);
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.selectorRowRect(
                    panelRect,
                    index,
                    PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT
            );
            boolean visible = query.visibleTabIds().contains(tab.id());
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean enabled = visible || query.visibleTabIds().size() < DatabaseTabs.MAX_VISIBLE_TAB_COUNT;
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, visible);
            guiGraphics.renderItem(PersonalDatabaseScreenCommonHelper.tabIcon(screen, tab), rowRect.x() + 3, rowRect.y() + 2);
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenGeometry.truncateToWidth(
                            screen,
                            PersonalDatabaseScreenCommonHelper.tabLabel(screen, tab).getString(),
                            Math.max(0, rowRect.width() - 44)
                    ),
                    rowRect.x() + 24,
                    rowRect.y() + 6,
                    query.focusedTabId().equals(tab.id())
                            ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR
                            : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                    false
            );
            PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                    screen,
                    guiGraphics,
                    Component.literal(visible ? "ON" : "OFF"),
                    rowRect.right() - 30,
                    rowRect.right() - 4,
                    rowRect.y() + 6,
                    enabled ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR
            );
        }
        guiGraphics.pose().popPose();
    }

    static void renderMoreTabsDropdown(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.moreTabsDropdownRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 253.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        PersonalDatabaseScreenOverlayRenderHelper.renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);
        List<DatabaseTab> tabs = hiddenTopTabs(screen);
        for (int index = 0; index < tabs.size(); index++) {
            DatabaseTab tab = tabs.get(index);
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    panelRect.x() + 2,
                    panelRect.y() + 2 + index * PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT,
                    panelRect.width() - 4,
                    PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT - 1
            );
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean selected = screen.databaseMenu.viewState().query().focusedTabId().equals(tab.id());
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, selected);
            guiGraphics.renderItem(PersonalDatabaseScreenCommonHelper.tabIcon(screen, tab), rowRect.x() + 3, rowRect.y() + 2);
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenGeometry.truncateToWidth(
                            screen,
                            PersonalDatabaseScreenCommonHelper.tabLabel(screen, tab).getString(),
                            Math.max(0, rowRect.width() - 28)
                    ),
                    rowRect.x() + 24,
                    rowRect.y() + 6,
                    selected ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                    false
            );
        }
        guiGraphics.pose().popPose();
    }

    static boolean handleTabClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (screen.layout == null) {
            return false;
        }
        List<DatabaseTab> visibleTabs = visibleTopTabs(screen);
        for (int index = 0; index < visibleTabs.size(); index++) {
            DatabaseTab tab = visibleTabs.get(index);
            PersonalDatabaseLayout.Rect tabRect = topTabRect(screen, index, visibleTabs.size(), !hiddenTopTabs(screen).isEmpty());
            if (!tabRect.contains(mouseX, mouseY)) {
                continue;
            }
            DatabaseQuery currentQuery = screen.databaseMenu.viewState().query();
            PersonalDatabaseScreenLayoutHelper.sendQuery(
                    screen,
                    currentQuery.withSingleVisibleTab(tab.id()).withFocusedTabId(tab.id())
            );
            return true;
        }
        if (!hiddenTopTabs(screen).isEmpty() && moreTabsButtonRect(screen).contains(mouseX, mouseY)) {
            screen.moreTabsExpanded = !screen.moreTabsExpanded;
            return true;
        }
        return false;
    }

    static boolean handleViewSelectorClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.viewSelectorExpanded) {
            return false;
        }
        if (screen.layout != null && screen.layout.viewSelectorButtonRect().contains(mouseX, mouseY)) {
            screen.viewSelectorExpanded = false;
            return true;
        }
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.viewSelectorRect(screen);
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(panelRect, mouseX, mouseY)) {
            screen.viewSelectorExpanded = false;
            return true;
        }
        if (!panelRect.contains(mouseX, mouseY)) {
            screen.viewSelectorExpanded = false;
            return true;
        }
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        List<DatabaseTab> tabs = PersonalDatabaseScreenCommonHelper.currentTabs(screen);
        for (int index = 0; index < tabs.size(); index++) {
            DatabaseTab tab = tabs.get(index);
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.selectorRowRect(
                    panelRect,
                    index,
                    PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT
            );
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            if (query.visibleTabIds().contains(tab.id())) {
                if (query.visibleTabIds().size() <= 1) {
                    return true;
                }
                List<String> nextVisibleTabIds = query.visibleTabIds().stream()
                        .filter(tabId -> !tabId.equals(tab.id()))
                        .toList();
                PersonalDatabaseScreenLayoutHelper.sendQuery(screen, query.withVisibleTabIds(nextVisibleTabIds));
                return true;
            }
            if (query.visibleTabIds().size() >= DatabaseTabs.MAX_VISIBLE_TAB_COUNT) {
                return true;
            }
            LinkedHashSet<String> nextVisibleTabIds = new LinkedHashSet<>(query.visibleTabIds());
            nextVisibleTabIds.add(tab.id());
            PersonalDatabaseScreenLayoutHelper.sendQuery(
                    screen,
                    query.withVisibleTabIds(new ArrayList<>(nextVisibleTabIds)).withFocusedTabId(tab.id())
            );
            return true;
        }
        return true;
    }

    static boolean handleMoreTabsClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.moreTabsExpanded) {
            return false;
        }
        if (moreTabsButtonRect(screen).contains(mouseX, mouseY)) {
            screen.moreTabsExpanded = false;
            return true;
        }
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.moreTabsDropdownRect(screen);
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(panelRect, mouseX, mouseY)) {
            screen.moreTabsExpanded = false;
            return true;
        }
        if (!panelRect.contains(mouseX, mouseY)) {
            screen.moreTabsExpanded = false;
            return true;
        }
        List<DatabaseTab> tabs = hiddenTopTabs(screen);
        for (int index = 0; index < tabs.size(); index++) {
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    panelRect.x() + 2,
                    panelRect.y() + 2 + index * PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT,
                    panelRect.width() - 4,
                    PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT - 1
            );
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            DatabaseTab tab = tabs.get(index);
            screen.moreTabsExpanded = false;
            PersonalDatabaseScreenLayoutHelper.sendQuery(
                    screen,
                    screen.databaseMenu.viewState().query().withSingleVisibleTab(tab.id()).withFocusedTabId(tab.id())
            );
            return true;
        }
        return true;
    }

    private static int inlineTabGap(int visibleTabCount, boolean hasMore) {
        return visibleTabCount + (hasMore ? 1 : 0) >= 5
                ? PersonalDatabaseScreen.INLINE_TAB_TIGHT_GAP
                : PersonalDatabaseLayout.TAB_GAP;
    }
}
