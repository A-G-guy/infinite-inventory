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
            0xFF95A5A6, 0xFFE74C3C, 0xFF3498DB, 0xFF2ECC71,
            0xFFF39C12, 0xFF9B59B6, 0xFF1ABC9C
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
