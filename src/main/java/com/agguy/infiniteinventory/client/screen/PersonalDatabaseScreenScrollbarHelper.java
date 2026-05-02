package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import net.minecraft.util.Mth;

/**
 * 滚动条拖拽交互辅助类，负责统计日志与饰品栏滚动条的点击检测与拖拽更新。
 */
final class PersonalDatabaseScreenScrollbarHelper {
    private PersonalDatabaseScreenScrollbarHelper() {
    }

    static boolean beginScrollbarDragIfHit(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (screen.statisticsPanelExpanded
                && screen.activeStatisticsTab == PersonalDatabaseScreenEnums.StatisticsPanelTab.LOGS
                && PersonalDatabaseScreenStatisticsLogHelper.isScrollbarThumbHit(screen, mouseX, mouseY)) {
            screen.scrollbarDragging = true;
            screen.scrollbarDragStartY = (int) mouseY;
            screen.scrollbarDragStartScrollIndex = screen.statisticsLogScrollIndex;
            screen.scrollbarDragTarget = PersonalDatabaseScreenEnums.ScrollbarDragTarget.STATS_LOG;
            return true;
        }
        if (screen.accessoriesExpanded
                && PersonalDatabaseScreenListHelper.isAccessoryScrollbarThumbHit(screen, mouseX, mouseY)) {
            screen.scrollbarDragging = true;
            screen.scrollbarDragStartY = (int) mouseY;
            screen.scrollbarDragStartScrollIndex = screen.accessoryScrollRow;
            screen.scrollbarDragTarget = PersonalDatabaseScreenEnums.ScrollbarDragTarget.ACCESSORY;
            return true;
        }
        return false;
    }

    static void updateScrollbarDrag(PersonalDatabaseScreen screen, int mouseY) {
        int deltaY = mouseY - screen.scrollbarDragStartY;
        switch (screen.scrollbarDragTarget) {
            case STATS_LOG -> {
                var entries = com.agguy.infiniteinventory.client.PersonalDatabaseClient.getLogEntries(screen.statisticsPanelScope);
                int availableHeight = PersonalDatabaseScreenStatisticsGeometry.STATISTICS_PANEL_HEIGHT
                        - PersonalDatabaseScreenStatisticsGeometry.TITLE_BAR_HEIGHT - 16;
                int visibleRows = Math.max(0, availableHeight / PersonalDatabaseScreenStatisticsLogHelper.ROW_HEIGHT);
                int maxScroll = Math.max(0, entries.size() - visibleRows);
                if (maxScroll <= 0) {
                    screen.statisticsLogScrollIndex = 0;
                    return;
                }
                PersonalDatabaseLayout.Rect contentRect = PersonalDatabaseScreenStatisticsGeometry.statisticsContentRect(screen);
                int listTop = contentRect.y() + 8;
                int listBottom = contentRect.bottom() - 8;
                int trackHeight = Math.max(1, listBottom - listTop);
                int thumbHeight = Math.max(8, trackHeight * visibleRows / Math.max(1, entries.size()));
                int thumbTravel = Math.max(0, trackHeight - thumbHeight);
                if (thumbTravel <= 0) {
                    screen.statisticsLogScrollIndex = 0;
                    return;
                }
                int newScroll = screen.scrollbarDragStartScrollIndex + deltaY * maxScroll / thumbTravel;
                screen.statisticsLogScrollIndex = Mth.clamp(newScroll, 0, maxScroll);
            }
            case ACCESSORY -> {
                if (screen.layout == null) {
                    return;
                }
                int totalRows = screen.layout.accessoryTotalRows();
                int visibleRows = screen.layout.accessoryVisibleRows();
                int maxScroll = Math.max(0, totalRows - visibleRows);
                if (maxScroll <= 0) {
                    screen.accessoryScrollRow = 0;
                    return;
                }
                PersonalDatabaseLayout.Rect panelRect = screen.layout.accessoriesPanelRect();
                int trackTop = panelRect.y() + 2;
                int trackBottom = panelRect.bottom() - 2;
                int trackHeight = Math.max(1, trackBottom - trackTop);
                int thumbHeight = Math.max(10, trackHeight * visibleRows / Math.max(1, totalRows));
                int thumbTravel = Math.max(0, trackHeight - thumbHeight);
                if (thumbTravel <= 0) {
                    screen.accessoryScrollRow = 0;
                    return;
                }
                int newScroll = screen.scrollbarDragStartScrollIndex + deltaY * maxScroll / thumbTravel;
                screen.accessoryScrollRow = Mth.clamp(newScroll, 0, maxScroll);
                PersonalDatabaseScreenLayoutHelper.rebuildLayout(screen);
            }
            default -> {
            }
        }
    }
}
