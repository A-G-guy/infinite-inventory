package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.client.PersonalDatabaseClient;
import com.agguy.infiniteinventory.database.DatabaseLogEntry;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 统计面板日志子页渲染辅助类，复用原有日志渲染逻辑并适配统计面板内容区尺寸。
 */
final class PersonalDatabaseScreenStatisticsLogHelper {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;
    private static final int ACTION_CHIP_WIDTH = 28;
    private static final int AMOUNT_WIDTH = 40;
    private static final int PLAYER_WIDTH = 50;
    private static final int TIME_WIDTH = 64;
    private static final int ITEM_ICON_WIDTH = 18;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_MARGIN = 4;

    private PersonalDatabaseScreenStatisticsLogHelper() {
    }

    static void render(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            int mouseX, int mouseY, PersonalDatabaseLayout.Rect contentRect) {
        List<DatabaseLogEntry> entries = PersonalDatabaseClient.getLogEntries(screen.statisticsPanelScope);

        int listTop = contentRect.y() + 8;
        int listLeft = contentRect.x() + 8;
        int listRight = contentRect.right() - 8 - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
        int listBottom = contentRect.bottom() - 8;
        int listWidth = Math.max(1, listRight - listLeft);
        int listHeight = Math.max(1, listBottom - listTop);

        int visibleRows = Math.max(0, listHeight / ROW_HEIGHT);
        int maxScroll = Math.max(0, entries.size() - visibleRows);
        screen.statisticsLogScrollIndex = Math.min(screen.statisticsLogScrollIndex, maxScroll);

        if (entries.isEmpty()) {
            renderEmpty(screen, guiGraphics, contentRect);
            return;
        }

        int fixedWidth = TIME_WIDTH + ACTION_CHIP_WIDTH + ITEM_ICON_WIDTH + PLAYER_WIDTH + AMOUNT_WIDTH + ROW_GAP * 5;
        int itemTextWidth = Math.max(1, listWidth - fixedWidth);

        int endIndex = Math.min(entries.size(), screen.statisticsLogScrollIndex + visibleRows);
        for (int i = screen.statisticsLogScrollIndex; i < endIndex; i++) {
            DatabaseLogEntry entry = entries.get(entries.size() - 1 - i);
            int rowIndex = i - screen.statisticsLogScrollIndex;
            int rowY = listTop + rowIndex * ROW_HEIGHT;

            boolean hovered = mouseY >= rowY && mouseY < rowY + ROW_HEIGHT
                    && mouseX >= listLeft && mouseX < listRight;
            if (hovered) {
                guiGraphics.fill(listLeft, rowY, listRight, rowY + ROW_HEIGHT, 0x20A89E8C);
            }

            int y = rowY + (ROW_HEIGHT - screen.screenFont().lineHeight) / 2 + 1;
            int x = listLeft + ROW_GAP;

            String timeStr = TIME_FORMATTER.format(Instant.ofEpochMilli(entry.timestampMillis()));
            guiGraphics.drawString(screen.screenFont(), timeStr, x, y,
                    PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR, false);
            x += TIME_WIDTH + ROW_GAP;

            int actionColor = PersonalDatabaseScreenLogHelper.actionColor(entry.action());
            String actionLabel = Component.translatable(entry.action().translationKey()).getString();
            guiGraphics.drawString(screen.screenFont(), actionLabel, x, y, actionColor, false);
            x += ACTION_CHIP_WIDTH + ROW_GAP;

            if (!entry.stackSnapshot().isEmpty()) {
                guiGraphics.renderItem(entry.stackSnapshot(), x, rowY + 1);
            }
            x += ITEM_ICON_WIDTH + ROW_GAP;

            String itemName = entry.stackSnapshot().getHoverName().getString();
            String tabInfo = PersonalDatabaseScreenLogHelper.formatTabInfo(screen, entry);
            String displayText = tabInfo.isEmpty() ? itemName : itemName + " " + tabInfo;
            displayText = screen.screenFont().plainSubstrByWidth(displayText, itemTextWidth);
            guiGraphics.drawString(screen.screenFont(), displayText, x, y,
                    PersonalDatabaseScreen.OVERLAY_TEXT_COLOR, false);
            x += itemTextWidth + ROW_GAP;

            String playerName = entry.playerNameSnapshot();
            if (!playerName.isEmpty()) {
                playerName = screen.screenFont().plainSubstrByWidth(playerName, PLAYER_WIDTH);
                guiGraphics.drawString(screen.screenFont(), playerName, x, y,
                        PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR, false);
            }
            x += PLAYER_WIDTH + ROW_GAP;

            x = listRight - AMOUNT_WIDTH;
            String amountStr = "x" + entry.amount();
            guiGraphics.drawString(screen.screenFont(), amountStr, x, y,
                    PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR, false);
        }

        if (entries.size() > visibleRows) {
            int trackX = contentRect.right() - 8 - SCROLLBAR_WIDTH;
            int trackTop = listTop;
            int trackBottom = listBottom;
            guiGraphics.fill(trackX, trackTop, trackX + SCROLLBAR_WIDTH, trackBottom,
                    PersonalDatabaseScreen.SCROLLBAR_TRACK_COLOR);
            int thumbHeight = Math.max(8, (trackBottom - trackTop) * visibleRows / entries.size());
            int thumbY = maxScroll > 0
                    ? trackTop + (trackBottom - trackTop - thumbHeight) * screen.statisticsLogScrollIndex / maxScroll
                    : trackTop;
            guiGraphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight,
                    PersonalDatabaseScreen.SCROLLBAR_THUMB_COLOR);
        }
    }

    static boolean handleLogClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        return true;
    }

    static boolean handleLogScroll(PersonalDatabaseScreen screen, double scrollY) {
        List<DatabaseLogEntry> entries = PersonalDatabaseClient.getLogEntries(screen.statisticsPanelScope);
        int availableHeight = PersonalDatabaseScreenStatisticsGeometry.STATISTICS_PANEL_HEIGHT - 16;
        int visibleRows = Math.max(0, availableHeight / ROW_HEIGHT);
        int maxScroll = Math.max(0, entries.size() - visibleRows);
        int delta = (int) -Math.signum(scrollY);
        int next = Math.max(0, Math.min(maxScroll, screen.statisticsLogScrollIndex + delta));
        if (next != screen.statisticsLogScrollIndex) {
            screen.statisticsLogScrollIndex = next;
            return true;
        }
        return false;
    }

    private static void renderEmpty(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect contentRect) {
        String text = Component.translatable("screen.infiniteinventory.log.empty").getString();
        int textWidth = screen.screenFont().width(text);
        int x = contentRect.x() + (contentRect.width() - textWidth) / 2;
        int y = contentRect.y() + contentRect.height() / 2;
        guiGraphics.drawString(screen.screenFont(), text, x, y,
                PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR, false);
    }
}
