package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.statistics.DailyTrend;
import com.agguy.infiniteinventory.database.statistics.DatabaseStatisticsSnapshot;
import com.agguy.infiniteinventory.database.statistics.HourlyTrend;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 统计面板趋势页渲染辅助类，绘制日趋势与小时趋势折线图。
 */
final class PersonalDatabaseScreenStatisticsTrendsHelper {
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final int DEPOSIT_COLOR = 0xFF2E8B57;
    private static final int EXTRACT_COLOR = 0xFFD2691E;
    private static final int GRID_COLOR = 0x20A89E8C;

    private PersonalDatabaseScreenStatisticsTrendsHelper() {
    }

    static void render(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            int mouseX, int mouseY, DatabaseStatisticsSnapshot snapshot,
            PersonalDatabaseLayout.Rect contentRect) {
        List<DailyTrend> daily = snapshot.dailyTrends();
        List<HourlyTrend> hourly = snapshot.hourlyTrends();

        if (daily.isEmpty() && hourly.isEmpty()) {
            renderEmpty(screen, guiGraphics, contentRect);
            return;
        }

        int midY = contentRect.y() + contentRect.height() / 2;

        if (!daily.isEmpty()) {
            PersonalDatabaseLayout.Rect dailyRect = new PersonalDatabaseLayout.Rect(
                    contentRect.x() + 8, contentRect.y() + 8,
                    Math.max(1, contentRect.width() - 16), Math.max(1, midY - contentRect.y() - 12));
            renderDailyTrend(screen, guiGraphics, dailyRect, daily);
        }

        if (!hourly.isEmpty()) {
            int hourlyTop = daily.isEmpty() ? contentRect.y() + 8 : midY + 4;
            PersonalDatabaseLayout.Rect hourlyRect = new PersonalDatabaseLayout.Rect(
                    contentRect.x() + 8, hourlyTop,
                    Math.max(1, contentRect.width() - 16), Math.max(1, contentRect.bottom() - hourlyTop - 8));
            renderHourlyTrend(screen, guiGraphics, hourlyRect, hourly);
        }
    }

    private static void renderDailyTrend(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect rect, List<DailyTrend> trends) {
        List<Long> deposits = new ArrayList<>();
        List<Long> extracts = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (DailyTrend t : trends) {
            deposits.add(t.depositAmount());
            extracts.add(t.extractAmount());
            labels.add(DAY_FORMATTER.format(Instant.ofEpochMilli(t.dayStartMillis())));
        }
        renderTrendChart(screen, guiGraphics, rect,
                Component.translatable("screen.infiniteinventory.statistics.daily_trend"),
                deposits, extracts, labels, DEPOSIT_COLOR, EXTRACT_COLOR);
    }

    private static void renderHourlyTrend(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect rect, List<HourlyTrend> trends) {
        List<Long> deposits = new ArrayList<>();
        List<Long> extracts = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (HourlyTrend t : trends) {
            deposits.add(t.depositAmount());
            extracts.add(t.extractAmount());
            labels.add(HOUR_FORMATTER.format(Instant.ofEpochMilli(t.hourStartMillis())));
        }
        renderTrendChart(screen, guiGraphics, rect,
                Component.translatable("screen.infiniteinventory.statistics.hourly_trend"),
                deposits, extracts, labels, DEPOSIT_COLOR, EXTRACT_COLOR);
    }

    private static void renderTrendChart(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect chartRect, Component title,
            List<Long> depositValues, List<Long> extractValues,
            List<String> xLabels, int depositColor, int extractColor) {

        guiGraphics.drawString(screen.screenFont(), title, chartRect.x(), chartRect.y(),
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR, false);

        long maxValue = Math.max(
                depositValues.stream().mapToLong(Long::longValue).max().orElse(1L),
                extractValues.stream().mapToLong(Long::longValue).max().orElse(1L)
        );
        maxValue = Math.max(1L, maxValue);

        int plotTop = chartRect.y() + 18;
        int plotBottom = chartRect.bottom() - 16;
        int plotLeft = chartRect.x() + 40;
        int plotRight = chartRect.right() - 4;
        int plotHeight = Math.max(1, plotBottom - plotTop);
        int plotWidth = Math.max(1, plotRight - plotLeft);

        for (int i = 0; i <= 3; i++) {
            int y = plotTop + plotHeight * i / 3;
            guiGraphics.fill(plotLeft, y, plotRight, y + 1, GRID_COLOR);
        }

        for (int i = 0; i <= 3; i++) {
            long value = maxValue * (3 - i) / 3;
            String label = PersonalDatabaseScreenStatisticsBarChartHelper.formatCompactNumber(value);
            int y = plotTop + plotHeight * i / 3 - screen.screenFont().lineHeight / 2;
            guiGraphics.drawString(screen.screenFont(), label, chartRect.x(), y,
                    PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR, false);
        }

        int points = depositValues.size();
        if (points > 1 && plotWidth > 0) {
            int xStep = plotWidth / (points - 1);

            for (int i = 0; i < points - 1; i++) {
                int x1 = plotLeft + xStep * i;
                int x2 = plotLeft + xStep * (i + 1);
                int y1 = plotBottom - (int) (plotHeight * depositValues.get(i) / maxValue);
                int y2 = plotBottom - (int) (plotHeight * depositValues.get(i + 1) / maxValue);
                drawLine(guiGraphics, x1, y1, x2, y2, depositColor);
            }

            for (int i = 0; i < points - 1; i++) {
                int x1 = plotLeft + xStep * i;
                int x2 = plotLeft + xStep * (i + 1);
                int y1 = plotBottom - (int) (plotHeight * extractValues.get(i) / maxValue);
                int y2 = plotBottom - (int) (plotHeight * extractValues.get(i + 1) / maxValue);
                drawLine(guiGraphics, x1, y1, x2, y2, extractColor);
            }

            for (int i = 0; i < points; i++) {
                int x = plotLeft + xStep * i;
                int yDeposit = plotBottom - (int) (plotHeight * depositValues.get(i) / maxValue);
                int yExtract = plotBottom - (int) (plotHeight * extractValues.get(i) / maxValue);
                guiGraphics.fill(x - 1, yDeposit - 1, x + 2, yDeposit + 2, depositColor);
                guiGraphics.fill(x - 1, yExtract - 1, x + 2, yExtract + 2, extractColor);
            }
        }

        int labelInterval = Math.max(1, points / 4);
        for (int i = 0; i < points; i += labelInterval) {
            int x = plotLeft + (plotWidth * i / Math.max(1, points - 1));
            String label = xLabels.get(i);
            int labelWidth = screen.screenFont().width(label);
            guiGraphics.drawString(screen.screenFont(), label, x - labelWidth / 2, plotBottom + 2,
                    PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR, false);
        }

        int legendX = plotRight - 70;
        int legendY = chartRect.y();
        if (legendX >= plotLeft) {
            guiGraphics.fill(legendX, legendY + 4, legendX + 8, legendY + 12, depositColor);
            guiGraphics.drawString(screen.screenFont(),
                    Component.translatable("screen.infiniteinventory.statistics.deposit_legend"),
                    legendX + 12, legendY + 3, PersonalDatabaseScreen.OVERLAY_TEXT_COLOR, false);
            guiGraphics.fill(legendX + 52, legendY + 4, legendX + 60, legendY + 12, extractColor);
            guiGraphics.drawString(screen.screenFont(),
                    Component.translatable("screen.infiniteinventory.statistics.extract_legend"),
                    legendX + 64, legendY + 3, PersonalDatabaseScreen.OVERLAY_TEXT_COLOR, false);
        }
    }

    private static void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            guiGraphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            guiGraphics.fill(x, y, x + 1, y + 1, color);
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
}
