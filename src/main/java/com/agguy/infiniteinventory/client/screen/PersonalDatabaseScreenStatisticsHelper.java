package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.client.PersonalDatabaseClient;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.statistics.DatabaseStatisticsSnapshot;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 统计面板主渲染与交互辅助类。
 */
final class PersonalDatabaseScreenStatisticsHelper {
    private static final int NAV_BACKGROUND_COLOR = GuiTheme.NAV_BACKGROUND;
    private static final int NAV_ACTIVE_BACKGROUND = GuiTheme.NAV_ACTIVE_BACKGROUND;
    private static final int NAV_HOVER_BACKGROUND = GuiTheme.NAV_HOVER_BACKGROUND;
    private static final int NAV_ACTIVE_INDICATOR_COLOR = GuiTheme.NAV_ACTIVE_INDICATOR;
    private static final int DIVIDER_COLOR = GuiTheme.DIVIDER;
    static final int CONTENT_BACKGROUND_COLOR = GuiTheme.CONTENT_BACKGROUND;

    private PersonalDatabaseScreenStatisticsHelper() {
    }

    static void renderStatisticsPanel(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (screen.layout == null) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 265.0F);

        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenStatisticsGeometry.statisticsPanelRect(screen);

        // 全屏遮罩层
        guiGraphics.fill(0, 0, screen.screenWidthValue(), screen.screenHeightValue(), 0xC0101010);

        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);

        // 整体背景
        VanillaWidgetRenderer.renderPanel(guiGraphics, panelRect);

        // 标题栏背景
        int titleBarBottom = panelRect.y() + PersonalDatabaseScreenStatisticsGeometry.TITLE_BAR_HEIGHT;
        guiGraphics.fill(panelRect.x(), panelRect.y(), panelRect.right(), titleBarBottom, GuiTheme.TITLE_BAR_BACKGROUND);
        guiGraphics.fill(panelRect.x(), titleBarBottom, panelRect.right(), titleBarBottom + 1, DIVIDER_COLOR);

        // 标题
        String title = Component.translatable("screen.infiniteinventory.statistics.title").getString();
        int titleWidth = screen.screenFont().width(title);
        guiGraphics.drawString(
                screen.screenFont(),
                title,
                panelRect.x() + (panelRect.width() - titleWidth) / 2,
                panelRect.y() + 10,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );

        // 导航栏背景
        PersonalDatabaseLayout.Rect navRect = PersonalDatabaseScreenStatisticsGeometry.statisticsNavRect(screen);
        guiGraphics.fill(navRect.x(), navRect.y(), navRect.right(), navRect.bottom(), NAV_BACKGROUND_COLOR);
        guiGraphics.fill(navRect.right(), navRect.y(), navRect.right() + 1, navRect.bottom(), DIVIDER_COLOR);

        // 导航栏
        renderNavBar(screen, guiGraphics, navRect, mouseX, mouseY);

        // 右侧内容区背景
        PersonalDatabaseLayout.Rect contentRect = PersonalDatabaseScreenStatisticsGeometry.statisticsContentRect(screen);
        guiGraphics.fill(contentRect.x(), contentRect.y(), contentRect.right(), contentRect.bottom(), CONTENT_BACKGROUND_COLOR);

        // 作用域切换
        renderScopeToggles(screen, guiGraphics, mouseX, mouseY);

        // 关闭按钮
        PersonalDatabaseScreenOverlayRenderHelper.renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);

        // 根据当前标签渲染内容
        DatabaseStatisticsSnapshot snapshot = PersonalDatabaseClient.getStatisticsSnapshot(screen.statisticsPanelScope);
        if (snapshot == null) {
            renderLoading(screen, guiGraphics, contentRect);
        } else {
            renderContentByTab(screen, guiGraphics, mouseX, mouseY, snapshot, contentRect);
        }

        guiGraphics.pose().popPose();
    }

    private static void renderNavBar(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect navRect, int mouseX, int mouseY) {
        // 导航项
        PersonalDatabaseScreenEnums.StatisticsPanelTab[] tabs = PersonalDatabaseScreenEnums.StatisticsPanelTab.values();
        for (int i = 0; i < tabs.length; i++) {
            PersonalDatabaseLayout.Rect itemRect = PersonalDatabaseScreenStatisticsGeometry.statisticsNavItemRect(screen, i);
            if (itemRect.y() >= navRect.bottom() - PersonalDatabaseScreenStatisticsGeometry.NAV_PADDING) {
                break;
            }
            boolean isActive = screen.activeStatisticsTab == tabs[i];
            boolean isHovered = itemRect.contains(mouseX, mouseY);

            if (isActive) {
                guiGraphics.fill(itemRect.x(), itemRect.y(), itemRect.right(), itemRect.bottom(), NAV_ACTIVE_BACKGROUND);
                guiGraphics.fill(itemRect.x(), itemRect.y(), itemRect.x() + 3, itemRect.bottom(), NAV_ACTIVE_INDICATOR_COLOR);
            } else if (isHovered) {
                guiGraphics.fill(itemRect.x(), itemRect.y(), itemRect.right(), itemRect.bottom(), NAV_HOVER_BACKGROUND);
            }

            int textColor = isActive ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR;
            guiGraphics.drawString(
                    screen.screenFont(),
                    Component.translatable(tabs[i].translationKey()),
                    itemRect.x() + 8,
                    itemRect.y() + (itemRect.height() - screen.screenFont().lineHeight) / 2 + 1,
                    textColor,
                    false
            );
        }
    }

    private static void renderScopeToggles(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (DatabaseScope scope : DatabaseScope.values()) {
            PersonalDatabaseLayout.Rect toggleRect = PersonalDatabaseScreenStatisticsGeometry.statisticsScopeToggleRect(screen, scope);
            boolean selected = screen.statisticsPanelScope == scope;
            boolean hovered = toggleRect.contains(mouseX, mouseY);
            VanillaWidgetRenderer.renderOverlayChip(guiGraphics, toggleRect, hovered, selected, true);
            String label = Component.translatable(scope.translationKey()).getString();
            int textWidth = screen.screenFont().width(label);
            int textX = toggleRect.x() + (toggleRect.width() - textWidth) / 2;
            int textY = toggleRect.y() + (toggleRect.height() - screen.screenFont().lineHeight) / 2 + 1;
            guiGraphics.drawString(screen.screenFont(), label, textX, textY, PersonalDatabaseScreen.OVERLAY_TEXT_COLOR, false);
        }
    }

    private static void renderContentByTab(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            int mouseX, int mouseY, DatabaseStatisticsSnapshot snapshot,
            PersonalDatabaseLayout.Rect contentRect) {
        switch (screen.activeStatisticsTab) {
            case OVERVIEW -> PersonalDatabaseScreenStatisticsOverviewHelper.render(
                    screen, guiGraphics, mouseX, mouseY, snapshot, contentRect);
            case CATEGORY -> PersonalDatabaseScreenStatisticsCategoryHelper.render(
                    screen, guiGraphics, mouseX, mouseY, snapshot, contentRect);
            case MODS -> PersonalDatabaseScreenStatisticsModsHelper.render(
                    screen, guiGraphics, mouseX, mouseY, snapshot, contentRect);
            case TABS -> PersonalDatabaseScreenStatisticsTabsHelper.render(
                    screen, guiGraphics, mouseX, mouseY, snapshot, contentRect);
            case TRENDS -> PersonalDatabaseScreenStatisticsTrendsHelper.render(
                    screen, guiGraphics, mouseX, mouseY, snapshot, contentRect);
            case LOGS -> PersonalDatabaseScreenStatisticsLogHelper.render(
                    screen, guiGraphics, mouseX, mouseY, contentRect);
        }
    }

    private static void renderLoading(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect contentRect) {
        String text = Component.translatable("screen.infiniteinventory.statistics.loading").getString();
        int textWidth = screen.screenFont().width(text);
        int x = contentRect.x() + (contentRect.width() - textWidth) / 2;
        int y = contentRect.y() + contentRect.height() / 2;
        guiGraphics.drawString(screen.screenFont(), text, x, y, PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR, false);
    }

    static boolean handleStatisticsPanelClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenStatisticsGeometry.statisticsPanelRect(screen);
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(panelRect, mouseX, mouseY)) {
            screen.statisticsPanelExpanded = false;
            PersonalDatabaseScreenCommonHelper.playButtonClickSound(screen);
            return true;
        }

        // 导航项切换
        int clickedIndex = PersonalDatabaseScreenStatisticsGeometry.clickedNavItemIndex(screen, mouseX, mouseY);
        if (clickedIndex >= 0) {
            PersonalDatabaseScreenEnums.StatisticsPanelTab[] tabs = PersonalDatabaseScreenEnums.StatisticsPanelTab.values();
            if (clickedIndex < tabs.length && screen.activeStatisticsTab != tabs[clickedIndex]) {
                screen.activeStatisticsTab = tabs[clickedIndex];
                PersonalDatabaseScreenCommonHelper.playButtonClickSound(screen);
            }
            return true;
        }

        // 作用域切换
        for (DatabaseScope scope : DatabaseScope.values()) {
            PersonalDatabaseLayout.Rect toggleRect = PersonalDatabaseScreenStatisticsGeometry.statisticsScopeToggleRect(screen, scope);
            if (toggleRect.contains(mouseX, mouseY)) {
                screen.statisticsPanelScope = scope;
                screen.statisticsLogScrollIndex = 0;
                screen.statisticsCategoryScrollIndex = 0;
                screen.statisticsModsScrollIndex = 0;
                screen.statisticsTabsScrollIndex = 0;
                screen.statisticsTrendsScrollIndex = 0;
                PacketDistributor.sendToServer(
                        new com.agguy.infiniteinventory.network.DatabaseStatisticsRequestPayload(scope));
                if (screen.activeStatisticsTab == PersonalDatabaseScreenEnums.StatisticsPanelTab.LOGS) {
                    PacketDistributor.sendToServer(
                            new com.agguy.infiniteinventory.network.DatabaseLogRequestPayload(scope));
                }
                PersonalDatabaseScreenCommonHelper.playButtonClickSound(screen);
                return true;
            }
        }

        // 点击面板外部关闭
        if (!panelRect.contains(mouseX, mouseY)) {
            screen.statisticsPanelExpanded = false;
            return true;
        }

        // 委托给当前子标签页的点击处理
        if (screen.activeStatisticsTab == PersonalDatabaseScreenEnums.StatisticsPanelTab.LOGS) {
            return PersonalDatabaseScreenStatisticsLogHelper.handleLogClick(screen, mouseX, mouseY);
        }
        return true;
    }

    static boolean handleStatisticsPanelScroll(PersonalDatabaseScreen screen, double scrollY) {
        if (!screen.statisticsPanelExpanded) {
            return false;
        }
        int delta = (int) -Math.signum(scrollY);
        return switch (screen.activeStatisticsTab) {
            case CATEGORY -> adjustScroll(screen, delta, screen.statisticsCategoryScrollIndex,
                    v -> screen.statisticsCategoryScrollIndex = v,
                    PersonalDatabaseClient.getStatisticsSnapshot(screen.statisticsPanelScope) != null
                            ? PersonalDatabaseClient.getStatisticsSnapshot(screen.statisticsPanelScope).categoryBreakdowns().size() : 0);
            case MODS -> adjustScroll(screen, delta, screen.statisticsModsScrollIndex,
                    v -> screen.statisticsModsScrollIndex = v,
                    PersonalDatabaseClient.getStatisticsSnapshot(screen.statisticsPanelScope) != null
                            ? PersonalDatabaseClient.getStatisticsSnapshot(screen.statisticsPanelScope).namespaceBreakdowns().size() : 0);
            case TABS -> adjustScroll(screen, delta, screen.statisticsTabsScrollIndex,
                    v -> screen.statisticsTabsScrollIndex = v,
                    PersonalDatabaseClient.getStatisticsSnapshot(screen.statisticsPanelScope) != null
                            ? PersonalDatabaseClient.getStatisticsSnapshot(screen.statisticsPanelScope).tabBreakdowns().size() : 0);
            case TRENDS -> adjustScroll(screen, delta, screen.statisticsTrendsScrollIndex,
                    v -> screen.statisticsTrendsScrollIndex = v, 0);
            case LOGS -> PersonalDatabaseScreenStatisticsLogHelper.handleLogScroll(screen, scrollY);
            default -> false;
        };
    }

    private static boolean adjustScroll(PersonalDatabaseScreen screen, int delta, int currentScroll,
            java.util.function.IntConsumer setter, int itemCount) {
        int maxVisibleRows = Math.max(1,
                (PersonalDatabaseScreenStatisticsGeometry.STATISTICS_PANEL_HEIGHT
                        - PersonalDatabaseScreenStatisticsGeometry.TITLE_BAR_HEIGHT - 16)
                        / PersonalDatabaseScreenStatisticsBarChartHelper.ROW_HEIGHT);
        int maxScroll = Math.max(0, itemCount - maxVisibleRows);
        int next = Math.max(0, Math.min(maxScroll, currentScroll + delta));
        if (next != currentScroll) {
            setter.accept(next);
            return true;
        }
        return false;
    }
}
