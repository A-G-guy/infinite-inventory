package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.statistics.DatabaseStatisticsSnapshot;
import com.agguy.infiniteinventory.database.statistics.TabBreakdown;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 统计面板标签页渲染辅助类。
 */
final class PersonalDatabaseScreenStatisticsTabsHelper {
    private static final int TAB_COLORS[] = GuiTheme.CHART_COLORS;

    private PersonalDatabaseScreenStatisticsTabsHelper() {
    }

    static void render(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            int mouseX, int mouseY, DatabaseStatisticsSnapshot snapshot,
            PersonalDatabaseLayout.Rect contentRect) {
        List<PersonalDatabaseScreenStatisticsBarChartHelper.BarChartItem> items = buildItems(snapshot);
        PersonalDatabaseScreenStatisticsBarChartHelper.renderBarChart(
                screen, guiGraphics, mouseX, mouseY, items, contentRect, screen.statisticsTabsScrollIndex);
    }

    private static List<PersonalDatabaseScreenStatisticsBarChartHelper.BarChartItem> buildItems(
            DatabaseStatisticsSnapshot snapshot) {
        List<PersonalDatabaseScreenStatisticsBarChartHelper.BarChartItem> items = new ArrayList<>();
        int i = 0;
        for (TabBreakdown breakdown : snapshot.tabBreakdowns()) {
            int color = TAB_COLORS[i % TAB_COLORS.length];
            String displayName = breakdown.tabDisplayName();
            if (displayName == null || displayName.isBlank()) {
                displayName = breakdown.tabId();
            }
            Component label = Component.literal(displayName);
            items.add(new PersonalDatabaseScreenStatisticsBarChartHelper.BarChartItem(
                    label, breakdown.itemCount(), breakdown.percentage(), color));
            i++;
        }
        return items;
    }
}
