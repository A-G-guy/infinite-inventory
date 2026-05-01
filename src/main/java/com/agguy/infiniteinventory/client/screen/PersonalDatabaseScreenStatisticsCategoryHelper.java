package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.statistics.CategoryBreakdown;
import com.agguy.infiniteinventory.database.statistics.DatabaseStatisticsSnapshot;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 统计面板分类页渲染辅助类。
 */
final class PersonalDatabaseScreenStatisticsCategoryHelper {
    private static final int CATEGORY_COLORS[] = {
            GuiTheme.CHART_COLORS[0], GuiTheme.CHART_COLORS[1], GuiTheme.CHART_COLORS[2],
            GuiTheme.CHART_COLORS[3], GuiTheme.CHART_COLORS[4], GuiTheme.CHART_COLORS[5],
            GuiTheme.CHART_COLORS[6]
    };

    private PersonalDatabaseScreenStatisticsCategoryHelper() {
    }

    static void render(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            int mouseX, int mouseY, DatabaseStatisticsSnapshot snapshot,
            PersonalDatabaseLayout.Rect contentRect) {
        List<PersonalDatabaseScreenStatisticsBarChartHelper.BarChartItem> items = buildItems(snapshot);
        PersonalDatabaseScreenStatisticsBarChartHelper.renderBarChart(
                screen, guiGraphics, mouseX, mouseY, items, contentRect, screen.statisticsCategoryScrollIndex);
    }

    private static List<PersonalDatabaseScreenStatisticsBarChartHelper.BarChartItem> buildItems(
            DatabaseStatisticsSnapshot snapshot) {
        List<PersonalDatabaseScreenStatisticsBarChartHelper.BarChartItem> items = new ArrayList<>();
        for (CategoryBreakdown breakdown : snapshot.categoryBreakdowns()) {
            int color = CATEGORY_COLORS[Math.floorMod(breakdown.category().ordinal(), CATEGORY_COLORS.length)];
            Component label = Component.translatable(breakdown.category().translationKey());
            items.add(new PersonalDatabaseScreenStatisticsBarChartHelper.BarChartItem(
                    label, breakdown.itemCount(), breakdown.percentage(), color));
        }
        return items;
    }
}
