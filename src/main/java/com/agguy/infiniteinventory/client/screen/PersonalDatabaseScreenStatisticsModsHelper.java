package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.statistics.DatabaseStatisticsSnapshot;
import com.agguy.infiniteinventory.database.statistics.NamespaceBreakdown;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 统计面板模组页渲染辅助类。
 */
final class PersonalDatabaseScreenStatisticsModsHelper {
    private static final int MOD_COLORS[] = GuiTheme.CHART_COLORS;

    private PersonalDatabaseScreenStatisticsModsHelper() {
    }

    static void render(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            int mouseX, int mouseY, DatabaseStatisticsSnapshot snapshot,
            PersonalDatabaseLayout.Rect contentRect) {
        List<PersonalDatabaseScreenStatisticsBarChartHelper.BarChartItem> items = buildItems(snapshot);
        PersonalDatabaseScreenStatisticsBarChartHelper.renderBarChart(
                screen, guiGraphics, mouseX, mouseY, items, contentRect, screen.statisticsModsScrollIndex);
    }

    private static List<PersonalDatabaseScreenStatisticsBarChartHelper.BarChartItem> buildItems(
            DatabaseStatisticsSnapshot snapshot) {
        List<PersonalDatabaseScreenStatisticsBarChartHelper.BarChartItem> items = new ArrayList<>();
        int i = 0;
        for (NamespaceBreakdown breakdown : snapshot.namespaceBreakdowns()) {
            int color = MOD_COLORS[i % MOD_COLORS.length];
            Component label = Component.literal(breakdown.namespace());
            items.add(new PersonalDatabaseScreenStatisticsBarChartHelper.BarChartItem(
                    label, breakdown.itemCount(), breakdown.percentage(), color));
            i++;
        }
        return items;
    }
}
