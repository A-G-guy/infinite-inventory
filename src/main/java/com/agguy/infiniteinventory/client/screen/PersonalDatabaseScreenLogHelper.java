package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.client.PersonalDatabaseClient;
import com.agguy.infiniteinventory.database.DatabaseLogAction;
import com.agguy.infiniteinventory.database.DatabaseLogEntry;
import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class PersonalDatabaseScreenLogHelper {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter TOOLTIP_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final int ACTION_CHIP_WIDTH = 40;
    private static final int AMOUNT_WIDTH = 56;
    private static final int ROW_GAP = 2;

    private PersonalDatabaseScreenLogHelper() {
    }

    static void renderLogPanel(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 265.0F);
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenLogGeometry.logPanelRect(screen);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);

        renderTitle(screen, guiGraphics, panelRect);
        renderCloseButton(screen, guiGraphics, mouseX, mouseY);
        renderScopeToggles(screen, guiGraphics, mouseX, mouseY);
        renderLogList(screen, guiGraphics, mouseX, mouseY);
        renderScrollbar(screen, guiGraphics);
        guiGraphics.pose().popPose();
    }

    private static void renderTitle(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect panelRect) {
        String title = screen.screenFont().plainSubstrByWidth(
                Component.translatable("screen.infiniteinventory.log_panel.title").getString(),
                panelRect.width() - PersonalDatabaseScreenLogGeometry.LOG_PANEL_PADDING * 4 - PersonalDatabaseScreenLogGeometry.LOG_SCOPE_TOGGLE_WIDTH * 2
        );
        int titleX = panelRect.x() + PersonalDatabaseScreenLogGeometry.LOG_PANEL_PADDING + 2;
        int titleY = panelRect.y() + PersonalDatabaseScreenLogGeometry.LOG_PANEL_PADDING + (PersonalDatabaseScreenLogGeometry.LOG_TITLE_HEIGHT - screen.screenFont().lineHeight) / 2;
        guiGraphics.drawString(screen.screenFont(), title, titleX, titleY, PersonalDatabaseScreen.OVERLAY_TEXT_COLOR, false);
    }

    private static void renderCloseButton(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect closeRect = PersonalDatabaseScreenLogGeometry.logPanelCloseButtonRect(screen);
        boolean hovered = closeRect.contains(mouseX, mouseY);
        VanillaWidgetRenderer.renderOverlayChip(guiGraphics, closeRect, hovered, false, true);
        PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                screen,
                guiGraphics,
                Component.literal("X"),
                closeRect.x() + 1,
                closeRect.right() - 1,
                closeRect.y() + 4,
                hovered ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR
        );
    }

    private static void renderScopeToggles(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (DatabaseScope scope : DatabaseScope.values()) {
            PersonalDatabaseLayout.Rect toggleRect = PersonalDatabaseScreenLogGeometry.logPanelScopeToggleRect(screen, scope);
            boolean selected = screen.logPanelScope == scope;
            boolean hovered = toggleRect.contains(mouseX, mouseY);
            VanillaWidgetRenderer.renderOverlayChip(guiGraphics, toggleRect, hovered, selected, true);
            String label = Component.translatable(scope.translationKey()).getString();
            int textWidth = screen.screenFont().width(label);
            int textX = toggleRect.x() + (toggleRect.width() - textWidth) / 2;
            int textY = toggleRect.y() + (toggleRect.height() - screen.screenFont().lineHeight) / 2 + 1;
            guiGraphics.drawString(screen.screenFont(), label, textX, textY, PersonalDatabaseScreen.OVERLAY_TEXT_COLOR, false);
        }
    }

    private static void renderLogList(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<DatabaseLogEntry> entries = PersonalDatabaseClient.getLogEntries(screen.logPanelScope);
        PersonalDatabaseLayout.Rect listRect = PersonalDatabaseScreenLogGeometry.logPanelListRect(screen);
        int visibleRows = PersonalDatabaseScreenLogGeometry.visibleLogRowCount(screen);
        int maxScrollIndex = Math.max(0, entries.size() - visibleRows);
        screen.logPanelScrollIndex = Math.min(screen.logPanelScrollIndex, maxScrollIndex);

        if (entries.isEmpty()) {
            String emptyText = Component.translatable("screen.infiniteinventory.log.empty").getString();
            int textWidth = screen.screenFont().width(emptyText);
            int textX = listRect.x() + (listRect.width() - textWidth) / 2;
            int textY = listRect.y() + (listRect.height() - screen.screenFont().lineHeight) / 2;
            guiGraphics.drawString(screen.screenFont(), emptyText, textX, textY, PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR, false);
            return;
        }

        int timeWidth = 64;
        int actionWidth = ACTION_CHIP_WIDTH;
        int amountWidth = AMOUNT_WIDTH;
        int itemWidth = Math.max(1, listRect.width() - timeWidth - actionWidth - amountWidth - ROW_GAP * 4);

        int endIndex = Math.min(entries.size(), screen.logPanelScrollIndex + visibleRows);
        for (int i = screen.logPanelScrollIndex; i < endIndex; i++) {
            DatabaseLogEntry entry = entries.get(entries.size() - 1 - i);
            int rowIndex = i - screen.logPanelScrollIndex;
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenLogGeometry.logPanelRowRect(screen, rowIndex);
            boolean hovered = rowRect.contains(mouseX, mouseY);
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, false);

            int y = rowRect.y() + (rowRect.height() - screen.screenFont().lineHeight) / 2 + 1;
            int x = rowRect.x() + ROW_GAP;

            // 时间
            String timeStr = TIME_FORMATTER.format(Instant.ofEpochMilli(entry.timestampMillis()));
            guiGraphics.drawString(screen.screenFont(), timeStr, x, y, PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR, false);
            x += timeWidth + ROW_GAP;

            // 操作类型（彩色标签）
            int actionColor = actionColor(entry.action());
            String actionLabel = Component.translatable(entry.action().translationKey()).getString();
            guiGraphics.drawString(screen.screenFont(), actionLabel, x, y, actionColor, false);
            x += actionWidth + ROW_GAP;

            // 物品图标
            if (!entry.stackSnapshot().isEmpty()) {
                ItemStack stack = entry.stackSnapshot();
                guiGraphics.renderItem(stack, x, rowRect.y() + 1);
                x += 18;
            }

            // 物品名称 + 页签信息
            String itemName = entry.stackSnapshot().getHoverName().getString();
            String tabInfo = formatTabInfo(screen, entry);
            String displayText = tabInfo.isEmpty() ? itemName : itemName + " " + tabInfo;
            displayText = screen.screenFont().plainSubstrByWidth(displayText, itemWidth - 20);
            guiGraphics.drawString(screen.screenFont(), displayText, x, y, PersonalDatabaseScreen.OVERLAY_TEXT_COLOR, false);
            x = rowRect.right() - amountWidth;

            // 数量
            String amountStr = "x" + entry.amount();
            guiGraphics.drawString(screen.screenFont(), amountStr, x, y, PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR, false);
        }

        if (listRect.contains(mouseX, mouseY)) {
            renderTooltip(screen, guiGraphics, mouseX, mouseY, entries, listRect);
        }
    }

    private static void renderTooltip(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY,
            List<DatabaseLogEntry> entries, PersonalDatabaseLayout.Rect listRect) {
        int relativeY = (int) (mouseY - listRect.y());
        int rowIndex = relativeY / PersonalDatabaseScreenLogGeometry.LOG_PANEL_ROW_HEIGHT;
        int entryIndex = screen.logPanelScrollIndex + rowIndex;
        if (entryIndex < 0 || entryIndex >= entries.size()) {
            return;
        }
        DatabaseLogEntry entry = entries.get(entries.size() - 1 - entryIndex);
        List<Component> tooltip = buildTooltip(entry);
        guiGraphics.renderComponentTooltip(screen.screenFont(), tooltip, mouseX, mouseY);
    }

    private static List<Component> buildTooltip(DatabaseLogEntry entry) {
        String timeStr = TOOLTIP_TIME_FORMATTER.format(Instant.ofEpochMilli(entry.timestampMillis()));
        String actionLabel = Component.translatable(entry.action().translationKey()).getString();
        String itemId = "";
        if (!entry.stackSnapshot().isEmpty()) {
            itemId = entry.stackSnapshot().getItem().toString();
        }
        return List.of(
                Component.literal(timeStr),
                Component.translatable("screen.infiniteinventory.log.tooltip.action", actionLabel),
                Component.translatable("screen.infiniteinventory.log.tooltip.amount", entry.amount()),
                Component.translatable("screen.infiniteinventory.log.tooltip.player", entry.playerNameSnapshot()),
                Component.literal(itemId).withStyle(net.minecraft.ChatFormatting.GRAY)
        );
    }

    private static void renderScrollbar(PersonalDatabaseScreen screen, GuiGraphics guiGraphics) {
        List<DatabaseLogEntry> entries = PersonalDatabaseClient.getLogEntries(screen.logPanelScope);
        int visibleRows = PersonalDatabaseScreenLogGeometry.visibleLogRowCount(screen);
        if (entries.size() <= visibleRows) {
            return;
        }
        PersonalDatabaseLayout.Rect trackRect = PersonalDatabaseScreenLogGeometry.logPanelScrollbarRect(screen);
        int thumbHeight = Math.max(8, trackRect.height() * visibleRows / entries.size());
        int maxScroll = entries.size() - visibleRows;
        int thumbY = trackRect.y() + (trackRect.height() - thumbHeight) * screen.logPanelScrollIndex / maxScroll;
        guiGraphics.fill(trackRect.x(), trackRect.y(), trackRect.right(), trackRect.bottom(), PersonalDatabaseScreen.SCROLLBAR_TRACK_COLOR);
        guiGraphics.fill(trackRect.x(), thumbY, trackRect.right(), thumbY + thumbHeight, PersonalDatabaseScreen.SCROLLBAR_THUMB_COLOR);
    }

    static boolean handleLogPanelClick(PersonalDatabaseScreen screen, double mouseX, double mouseY, int button) {
        if (!screen.logPanelExpanded) {
            return false;
        }
        if (button != 0) {
            return false;
        }
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(
                PersonalDatabaseScreenLogGeometry.logPanelRect(screen), mouseX, mouseY)) {
            screen.logPanelExpanded = false;
            return true;
        }
        for (DatabaseScope scope : DatabaseScope.values()) {
            PersonalDatabaseLayout.Rect toggleRect = PersonalDatabaseScreenLogGeometry.logPanelScopeToggleRect(screen, scope);
            if (toggleRect.contains(mouseX, mouseY)) {
                screen.logPanelScope = scope;
                screen.logPanelScrollIndex = 0;
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        new com.agguy.infiniteinventory.network.DatabaseLogRequestPayload(scope));
                return true;
            }
        }
        if (!PersonalDatabaseScreenLogGeometry.isWithinLogPanel(screen, mouseX, mouseY)) {
            screen.logPanelExpanded = false;
            return true;
        }
        return true;
    }

    static boolean handleLogPanelScroll(PersonalDatabaseScreen screen, double scrollY) {
        if (!screen.logPanelExpanded) {
            return false;
        }
        List<DatabaseLogEntry> entries = PersonalDatabaseClient.getLogEntries(screen.logPanelScope);
        int visibleRows = PersonalDatabaseScreenLogGeometry.visibleLogRowCount(screen);
        int maxScroll = Math.max(0, entries.size() - visibleRows);
        int delta = (int) -Math.signum(scrollY);
        int nextScroll = Math.max(0, Math.min(maxScroll, screen.logPanelScrollIndex + delta));
        if (nextScroll != screen.logPanelScrollIndex) {
            screen.logPanelScrollIndex = nextScroll;
            return true;
        }
        return false;
    }

    private static int actionColor(DatabaseLogAction action) {
        return switch (action) {
            case DEPOSIT -> 0xFF2E8B57;
            case EXTRACT -> 0xFFD2691E;
            case TRANSFER -> 0xFF4682B4;
            case DELETE -> 0xFFCD5C5C;
        };
    }

    private static String formatTabInfo(PersonalDatabaseScreen screen, DatabaseLogEntry entry) {
        String source = resolveTabName(screen, entry.sourceTabId());
        String target = resolveTabName(screen, entry.targetTabId());
        if (source.isEmpty() && target.isEmpty()) {
            return "";
        }
        if (source.isEmpty()) {
            return "-> " + target;
        }
        if (target.isEmpty()) {
            return source;
        }
        return source + " -> " + target;
    }

    private static String resolveTabName(PersonalDatabaseScreen screen, String tabId) {
        if (tabId == null || tabId.isEmpty()) {
            return "";
        }
        if (DatabaseTabs.DEFAULT_TAB_ID.equals(tabId)) {
            return Component.translatable(DatabaseTabs.DEFAULT_TAB_TRANSLATION_KEY).getString();
        }
        if (DatabaseTabs.ALL_TAB_ID.equals(tabId)) {
            return Component.translatable(DatabaseTabs.ALL_TAB_TRANSLATION_KEY).getString();
        }
        for (DatabasePanelView panel : PersonalDatabaseScreenCommonHelper.currentPanels(screen)) {
            if (panel.tab().id().equals(tabId)) {
                String displayName = panel.tab().displayName();
                if (!displayName.isBlank()) {
                    return displayName;
                }
                break;
            }
        }
        return tabId;
    }

    static void buildLogButton(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return;
        }
        PersonalDatabaseLayout.Rect logButtonRect = screen.layout.logButtonRect();
        screen.logButton = screen.addScreenButton(net.minecraft.client.gui.components.Button.builder(
                        Component.translatable("screen.infiniteinventory.log_button"),
                        button -> {
                            PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
                            screen.sortDropdownExpanded = false;
                            screen.pagePickerExpanded = false;
                            screen.advancedSearchExpanded = false;
                            screen.enhancementPanelExpanded = false;
                            screen.moreTabsExpanded = false;
                            screen.targetSelectorExpanded = false;
                            screen.viewSelectorExpanded = false;
                            screen.tabManagementExpanded = false;
                            PersonalDatabaseScreenTabHelper.closeTopTabPrompt(screen);
                            screen.logPanelExpanded = !screen.logPanelExpanded;
                            if (screen.logPanelExpanded) {
                                screen.logPanelScope = screen.databaseMenu.viewState().query().focusedTab().scope();
                                screen.logPanelScrollIndex = 0;
                                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                                        new com.agguy.infiniteinventory.network.DatabaseLogRequestPayload(screen.logPanelScope));
                            }
                        }
                )
                .bounds(logButtonRect.x(), logButtonRect.y(), logButtonRect.width(), logButtonRect.height())
                .build());
    }
}
