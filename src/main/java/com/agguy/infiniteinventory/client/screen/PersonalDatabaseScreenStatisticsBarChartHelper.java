package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 通用条形图渲染辅助类，用于分类、模组、标签页统计。
 */
final class PersonalDatabaseScreenStatisticsBarChartHelper {
    static final int ROW_HEIGHT = 22;
    private static final int BAR_HEIGHT = 12;
    private static final int LABEL_WIDTH = 100;
    private static final int VALUE_WIDTH = 60;
    private static final int BAR_MAX_WIDTH = 160;

    private PersonalDatabaseScreenStatisticsBarChartHelper() {
    }

    record BarChartItem(Component label, long value, double percentage, int color) {
    }

    static void renderBarChart(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            int mouseX, int mouseY, List<BarChartItem> items,
            PersonalDatabaseLayout.Rect contentRect, int scrollIndex) {
        if (items.isEmpty()) {
            renderEmpty(screen, guiGraphics, contentRect);
            return;
        }

        int maxVisibleRows = Math.max(1, (contentRect.height() - 16) / ROW_HEIGHT);
        int maxScroll = Math.max(0, items.size() - maxVisibleRows);
        scrollIndex = Math.max(0, Math.min(maxScroll, scrollIndex));

        int listTop = contentRect.y() + 8;
        int listLeft = contentRect.x() + 12;

        long maxValue = items.stream().mapToLong(BarChartItem::value).max().orElse(1);

        for (int i = scrollIndex; i < Math.min(items.size(), scrollIndex + maxVisibleRows); i++) {
            BarChartItem item = items.get(i);
            int rowY = listTop + (i - scrollIndex) * ROW_HEIGHT;

            guiGraphics.drawString(screen.screenFont(), item.label, listLeft, rowY + 2,
                    PersonalDatabaseScreen.OVERLAY_TEXT_COLOR, false);

            String valueStr = formatCompactNumber(item.value());
            guiGraphics.drawString(screen.screenFont(), valueStr,
                    listLeft + LABEL_WIDTH, rowY + 2,
                    PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR, false);

            int barWidth = maxValue > 0 ? (int) (BAR_MAX_WIDTH * item.value() / maxValue) : 0;
            int barX = listLeft + LABEL_WIDTH + VALUE_WIDTH + 8;
            int barY = rowY + 3;
            guiGraphics.fill(barX, barY, barX + BAR_MAX_WIDTH, barY + BAR_HEIGHT, 0x30A89E8C);
            guiGraphics.fill(barX, barY, barX + barWidth, barY + BAR_HEIGHT, item.color);

            String pctStr = String.format("%.1f%%", item.percentage());
            guiGraphics.drawString(screen.screenFont(), pctStr,
                    barX + BAR_MAX_WIDTH + 8, rowY + 2,
                    PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR, false);
        }

        if (items.size() > maxVisibleRows) {
            renderScrollbar(guiGraphics, contentRect, scrollIndex, maxScroll, maxVisibleRows, items.size());
        }
    }

    private static void renderEmpty(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect contentRect) {
        String text = Component.translatable("screen.infiniteinventory.statistics.empty").getString();
        int textWidth = screen.screenFont().width(text);
        int x = contentRect.x() + (contentRect.width() - textWidth) / 2;
        int y = contentRect.y() + contentRect.height() / 2;
        guiGraphics.drawString(screen.screenFont(), text, x, y,
                PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR, false);
    }

    private static void renderScrollbar(GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect contentRect,
            int scrollIndex, int maxScroll, int visibleRows, int totalRows) {
        int trackX = contentRect.right() - 8;
        int trackTop = contentRect.y() + 8;
        int trackBottom = contentRect.bottom() - 8;
        guiGraphics.fill(trackX, trackTop, trackX + 4, trackBottom, PersonalDatabaseScreen.SCROLLBAR_TRACK_COLOR);
        int thumbHeight = Math.max(8, (trackBottom - trackTop) * visibleRows / totalRows);
        int thumbY = maxScroll > 0
                ? trackTop + (trackBottom - trackTop - thumbHeight) * scrollIndex / maxScroll
                : trackTop;
        guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, PersonalDatabaseScreen.SCROLLBAR_THUMB_COLOR);
    }

    static String formatCompactNumber(long value) {
        if (value >= 1_000_000_000L) {
            return String.format("%.1fB", value / 1_000_000_000.0);
        }
        if (value >= 1_000_000L) {
            return String.format("%.1fM", value / 1_000_000.0);
        }
        if (value >= 1_000L) {
            return String.format("%.1fK", value / 1_000.0);
        }
        return Long.toString(value);
    }
}
