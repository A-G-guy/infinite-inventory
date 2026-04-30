package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.statistics.CategoryBreakdown;
import com.agguy.infiniteinventory.database.statistics.DatabaseStatisticsSnapshot;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 统计面板总览页渲染辅助类。
 */
final class PersonalDatabaseScreenStatisticsOverviewHelper {
    private static final int CARD_WIDTH = 120;
    private static final int CARD_HEIGHT = 56;
    private static final int CARD_GAP = 8;
    private static final int PIE_COLORS[] = {
            0xFFE74C3C, 0xFF3498DB, 0xFF2ECC71, 0xFFF39C12,
            0xFF9B59B6, 0xFF1ABC9C, 0xFFE67E22
    };

    private PersonalDatabaseScreenStatisticsOverviewHelper() {
    }

    static void render(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            int mouseX, int mouseY, DatabaseStatisticsSnapshot snapshot,
            PersonalDatabaseLayout.Rect contentRect) {
        renderStatCards(screen, guiGraphics, snapshot, contentRect);

        if (!snapshot.categoryBreakdowns().isEmpty()) {
            renderPieChart(screen, guiGraphics, snapshot, contentRect);
        }
    }

    private static void renderStatCards(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            DatabaseStatisticsSnapshot snapshot, PersonalDatabaseLayout.Rect contentRect) {
        int cardY = contentRect.y() + 8;
        int totalWidth = CARD_WIDTH * 3 + CARD_GAP * 2;
        int startX = contentRect.x() + (contentRect.width() - totalWidth) / 2;

        renderCard(screen, guiGraphics, startX, cardY,
                Component.translatable("screen.infiniteinventory.statistics.total_entries"),
                formatCompactNumber(snapshot.totalEntries()),
                0xFF2E8B57);

        renderCard(screen, guiGraphics, startX + CARD_WIDTH + CARD_GAP, cardY,
                Component.translatable("screen.infiniteinventory.statistics.total_items"),
                formatCompactNumber(snapshot.totalItems()),
                0xFF4682B4);

        long dailyChange = calculateDailyChange(snapshot);
        renderCard(screen, guiGraphics, startX + (CARD_WIDTH + CARD_GAP) * 2, cardY,
                Component.translatable("screen.infiniteinventory.statistics.daily_change"),
                (dailyChange >= 0 ? "+" : "") + formatCompactNumber(dailyChange),
                dailyChange >= 0 ? 0xFF2E8B57 : 0xFFCD5C5C);
    }

    private static void renderCard(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            int x, int y, Component label, String value, int accentColor) {
        PersonalDatabaseLayout.Rect cardRect = new PersonalDatabaseLayout.Rect(x, y, CARD_WIDTH, CARD_HEIGHT);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, cardRect);
        guiGraphics.drawString(screen.screenFont(), label, x + 8, y + 8,
                PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR, false);
        guiGraphics.drawString(screen.screenFont(), value, x + 8, y + 28,
                accentColor, false);
    }

    private static void renderPieChart(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            DatabaseStatisticsSnapshot snapshot, PersonalDatabaseLayout.Rect contentRect) {
        int centerX = contentRect.x() + contentRect.width() / 2;
        int centerY = contentRect.y() + 100;
        int radius = 48;

        List<CategoryBreakdown> categories = snapshot.categoryBreakdowns();
        double total = categories.stream().mapToDouble(CategoryBreakdown::percentage).sum();
        if (total <= 0) {
            return;
        }

        double currentAngle = -90.0;
        for (int i = 0; i < categories.size(); i++) {
            CategoryBreakdown breakdown = categories.get(i);
            double sweep = 360.0 * breakdown.percentage() / total;
            int color = PIE_COLORS[i % PIE_COLORS.length];
            fillPieSlice(guiGraphics, centerX, centerY, radius, currentAngle, sweep, color);
            currentAngle += sweep;
        }

        // 中心镂空形成圆环
        guiGraphics.fill(centerX - radius / 2, centerY - radius / 2,
                centerX + radius / 2, centerY + radius / 2, PersonalDatabaseScreenStatisticsHelper.CONTENT_BACKGROUND_COLOR);

        // 图例
        renderPieLegend(screen, guiGraphics, snapshot, contentRect, centerX, centerY + radius + 20);
    }

    private static void fillPieSlice(GuiGraphics guiGraphics, int cx, int cy, int radius,
            double startAngleDeg, double sweepDeg, int color) {
        int segments = Math.max(3, (int) (sweepDeg / 3));
        double startRad = Math.toRadians(startAngleDeg);
        double sweepRad = Math.toRadians(sweepDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = startRad + sweepRad * i / segments;
            double a2 = startRad + sweepRad * (i + 1) / segments;
            int x1 = cx + (int) (Math.cos(a1) * radius);
            int y1 = cy + (int) (Math.sin(a1) * radius);
            int x2 = cx + (int) (Math.cos(a2) * radius);
            int y2 = cy + (int) (Math.sin(a2) * radius);
            fillTriangle(guiGraphics, cx, cy, x1, y1, x2, y2, color);
        }
    }

    private static void fillTriangle(GuiGraphics guiGraphics, int x0, int y0, int x1, int y1, int x2, int y2, int color) {
        int minX = Math.min(x0, Math.min(x1, x2));
        int maxX = Math.max(x0, Math.max(x1, x2));
        int minY = Math.min(y0, Math.min(y1, y2));
        int maxY = Math.max(y0, Math.max(y1, y2));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (pointInTriangle(x, y, x0, y0, x1, y1, x2, y2)) {
                    guiGraphics.fill(x, y, x + 1, y + 1, color);
                }
            }
        }
    }

    private static boolean pointInTriangle(int px, int py, int x0, int y0, int x1, int y1, int x2, int y2) {
        double dX = px - x2;
        double dY = py - y2;
        double dX21 = x2 - x1;
        double dY12 = y1 - y2;
        double D = dY12 * (x0 - x2) + dX21 * (y0 - y2);
        double s = dY12 * dX + dX21 * dY;
        double t = (x2 - x0) * dY + (y0 - y2) * dX;
        if (D < 0) {
            return s <= 0 && t <= 0 && s + t >= D;
        }
        return s >= 0 && t >= 0 && s + t <= D;
    }

    private static void renderPieLegend(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            DatabaseStatisticsSnapshot snapshot, PersonalDatabaseLayout.Rect contentRect, int centerX, int legendY) {
        List<CategoryBreakdown> categories = snapshot.categoryBreakdowns();
        int columns = Math.min(3, categories.size());
        int columnWidth = contentRect.width() / columns;
        int x = contentRect.x() + 8;
        int rowY = legendY;
        for (int i = 0; i < categories.size(); i++) {
            if (i > 0 && i % columns == 0) {
                x = contentRect.x() + 8;
                rowY += 14;
            }
            CategoryBreakdown breakdown = categories.get(i);
            guiGraphics.fill(x, rowY, x + 10, rowY + 10, PIE_COLORS[i % PIE_COLORS.length]);
            String text = String.format("%s (%.1f%%)",
                    Component.translatable(breakdown.category().translationKey()).getString(),
                    breakdown.percentage());
            guiGraphics.drawString(screen.screenFont(), text, x + 14, rowY + 1,
                    PersonalDatabaseScreen.OVERLAY_TEXT_COLOR, false);
            x += columnWidth;
        }
    }

    private static long calculateDailyChange(DatabaseStatisticsSnapshot snapshot) {
        if (snapshot.dailyTrends().isEmpty()) {
            return 0;
        }
        var today = snapshot.dailyTrends().getLast();
        return today.depositAmount() - today.extractAmount();
    }

    private static String formatCompactNumber(long value) {
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
