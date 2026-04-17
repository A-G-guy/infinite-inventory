package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class PersonalDatabaseScreenTabHelper {
    private PersonalDatabaseScreenTabHelper() {
    }

    static List<DatabaseScopedTabRef> visibleTopTabs(PersonalDatabaseScreen screen) {
        List<DatabaseScopedTabRef> tabs = PersonalDatabaseScreenCommonHelper.allTopTabs(screen);
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

        LinkedHashSet<DatabaseScopedTabRef> visibleTabs = new LinkedHashSet<>();
        for (DatabaseScopedTabRef tab : tabs) {
            if (visibleTabs.size() >= visibleCount) {
                break;
            }
            visibleTabs.add(tab);
        }

        DatabaseScopedTabRef focusedTab = screen.databaseMenu.viewState().query().focusedTab();
        if (!visibleTabs.contains(focusedTab)) {
            List<DatabaseScopedTabRef> orderedTabs = new ArrayList<>(visibleTabs);
            if (!orderedTabs.isEmpty()) {
                orderedTabs.set(orderedTabs.size() - 1, focusedTab);
                visibleTabs.clear();
                visibleTabs.addAll(orderedTabs);
            }
        }

        List<DatabaseScopedTabRef> orderedVisibleTabs = new ArrayList<>(visibleTabs.size());
        for (DatabaseScopedTabRef tab : tabs) {
            if (visibleTabs.contains(tab)) {
                orderedVisibleTabs.add(tab);
            }
        }
        return List.copyOf(orderedVisibleTabs);
    }

    static List<DatabaseScopedTabRef> hiddenTopTabs(PersonalDatabaseScreen screen) {
        LinkedHashSet<DatabaseScopedTabRef> visibleTabs = new LinkedHashSet<>(visibleTopTabs(screen));
        List<DatabaseScopedTabRef> hiddenTabs = new ArrayList<>();
        for (DatabaseScopedTabRef tab : PersonalDatabaseScreenCommonHelper.allTopTabs(screen)) {
            if (!visibleTabs.contains(tab)) {
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
        List<DatabaseScopedTabRef> visibleTabs = visibleTopTabs(screen);
        List<DatabaseScopedTabRef> hiddenTabs = hiddenTopTabs(screen);
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        for (int index = 0; index < visibleTabs.size(); index++) {
            DatabaseScopedTabRef scopedTab = visibleTabs.get(index);
            DatabaseTab tab = PersonalDatabaseScreenCommonHelper.findTab(screen, scopedTab);
            PersonalDatabaseLayout.Rect tabRect = topTabRect(screen, index, visibleTabs.size(), !hiddenTabs.isEmpty());
            boolean hovered = tabRect.contains(mouseX, mouseY);
            boolean selected = query.visibleTabs().contains(scopedTab);
            boolean focused = query.focusedTab().equals(scopedTab);
            VanillaWidgetRenderer.renderTab(guiGraphics, tabRect, selected, hovered);
            if (focused) {
                guiGraphics.fill(tabRect.x() + 3, tabRect.bottom() - 3, tabRect.right() - 3, tabRect.bottom() - 1, 0xFFD4B16A);
            }
            guiGraphics.renderItem(PersonalDatabaseScreenCommonHelper.tabIcon(screen, tab), tabRect.x() + PersonalDatabaseScreen.TAB_ICON_LEFT_PADDING, tabRect.y() + 4);
            int labelX = tabRect.x() + PersonalDatabaseScreen.TAB_ICON_LEFT_PADDING
                    + PersonalDatabaseScreen.TAB_ICON_SIZE
                    + PersonalDatabaseScreen.TAB_TEXT_GAP;
            int labelWidth = Math.max(0, tabRect.right() - 4 - labelX);
            int color = focused
                    ? PersonalDatabaseScreen.TOP_TAB_ACTIVE_TEXT_COLOR
                    : PersonalDatabaseScreen.TOP_TAB_INACTIVE_TEXT_COLOR;
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenGeometry.truncateToWidth(
                            screen,
                            PersonalDatabaseScreenCommonHelper.scopedTabLabel(screen, scopedTab).getString(),
                            labelWidth
                    ),
                    labelX,
                    tabRect.y() + 8,
                    color,
                    true
            );
        }
        if (!hiddenTabs.isEmpty()) {
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
        PersonalDatabaseScreenViewSelectorHelper.renderViewSelector(screen, guiGraphics, mouseX, mouseY);
    }

    static void renderMoreTabsDropdown(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseScreenViewSelectorHelper.renderMoreTabsDropdown(screen, guiGraphics, mouseX, mouseY);
    }

    static void renderTopTabActionPrompt(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseScreenTopTabPromptHelper.renderTopTabActionPrompt(screen, guiGraphics, mouseX, mouseY);
    }

    static void renderTopTabReplacePrompt(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseScreenTopTabPromptHelper.renderTopTabReplacePrompt(screen, guiGraphics, mouseX, mouseY);
    }

    static boolean handleTabClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (screen.layout == null) {
            return false;
        }
        List<DatabaseScopedTabRef> visibleTabs = visibleTopTabs(screen);
        for (int index = 0; index < visibleTabs.size(); index++) {
            DatabaseScopedTabRef scopedTab = visibleTabs.get(index);
            PersonalDatabaseLayout.Rect tabRect = topTabRect(screen, index, visibleTabs.size(), !hiddenTopTabs(screen).isEmpty());
            if (!tabRect.contains(mouseX, mouseY)) {
                continue;
            }
            PersonalDatabaseScreenTopTabPromptHelper.handleTopTabSelection(screen, scopedTab);
            return true;
        }
        if (!hiddenTopTabs(screen).isEmpty() && moreTabsButtonRect(screen).contains(mouseX, mouseY)) {
            screen.moreTabsExpanded = !screen.moreTabsExpanded;
            screen.topTabActionPromptExpanded = false;
            screen.topTabReplaceExpanded = false;
            return true;
        }
        return false;
    }

    static boolean handleViewSelectorClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        return PersonalDatabaseScreenViewSelectorHelper.handleViewSelectorClick(screen, mouseX, mouseY);
    }

    static boolean handleMoreTabsClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        return PersonalDatabaseScreenViewSelectorHelper.handleMoreTabsClick(screen, mouseX, mouseY);
    }

    static boolean handleTopTabActionPromptClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        return PersonalDatabaseScreenTopTabPromptHelper.handleTopTabActionPromptClick(screen, mouseX, mouseY);
    }

    static boolean handleTopTabReplacePromptClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        return PersonalDatabaseScreenTopTabPromptHelper.handleTopTabReplacePromptClick(screen, mouseX, mouseY);
    }

    static void applyPrimaryTopTabAction(PersonalDatabaseScreen screen) {
        PersonalDatabaseScreenTopTabPromptHelper.applyPrimaryTopTabAction(screen);
    }

    static void closeTopTabPrompt(PersonalDatabaseScreen screen) {
        PersonalDatabaseScreenTopTabPromptHelper.closeTopTabPrompt(screen);
    }

    private static int inlineTabGap(int visibleTabCount, boolean hasMore) {
        return visibleTabCount + (hasMore ? 1 : 0) >= 5
                ? PersonalDatabaseScreen.INLINE_TAB_TIGHT_GAP
                : PersonalDatabaseLayout.TAB_GAP;
    }
}
