package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;

final class PersonalDatabaseScreenListHelper {
    private PersonalDatabaseScreenListHelper() {
    }

    static VisibleRange visibleRange(int totalRows, int requestedScrollIndex, int maxVisibleRows) {
        int resolvedTotalRows = Math.max(0, totalRows);
        int resolvedMaxVisibleRows = Math.max(1, maxVisibleRows);
        int maxScrollIndex = Math.max(0, resolvedTotalRows - resolvedMaxVisibleRows);
        int scrollIndex = Math.max(0, Math.min(maxScrollIndex, requestedScrollIndex));
        int fromIndex = Math.min(resolvedTotalRows, scrollIndex);
        int toIndex = Math.min(resolvedTotalRows, fromIndex + resolvedMaxVisibleRows);
        return new VisibleRange(fromIndex, toIndex, scrollIndex, resolvedMaxVisibleRows, resolvedTotalRows);
    }

    static int maxVisibleRows(PersonalDatabaseLayout.Rect clipRect, int rowHeight) {
        if (clipRect.height() <= 0 || rowHeight <= 0) {
            return 1;
        }
        return Math.max(1, clipRect.height() / rowHeight);
    }

    static int clampScrollIndex(VisibleRange visibleRange, int deltaRows) {
        int maxScrollIndex = Math.max(0, visibleRange.totalRows() - visibleRange.maxVisibleRows());
        return Math.max(0, Math.min(maxScrollIndex, visibleRange.scrollIndex() + deltaRows));
    }

    static void enableScissor(GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect clipRect) {
        if (clipRect.width() <= 0 || clipRect.height() <= 0) {
            return;
        }
        guiGraphics.enableScissor(clipRect.x(), clipRect.y(), clipRect.right(), clipRect.bottom());
    }

    static boolean isAccessoryScrollbarThumbHit(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (screen.layout == null || !screen.accessoriesExpanded) {
            return false;
        }
        var panelRect = screen.layout.accessoriesPanelRect();
        var bodyClipRect = new PersonalDatabaseLayout.Rect(
                panelRect.x() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                panelRect.y() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING
                        + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT
                        + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_GAP,
                Math.max(1, panelRect.width() - PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING * 2),
                Math.max(
                        1,
                        panelRect.height()
                                - PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING * 2
                                - PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT
                                - PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_GAP
                )
        );
        var thumb = scrollbarThumbRect(bodyClipRect, visibleRange(
                screen.layout.accessoryTotalRows(),
                screen.accessoryScrollRow,
                screen.layout.accessoryVisibleRows()
        ));
        return thumb != null && thumb.contains(mouseX, mouseY);
    }

    @Nullable
    static PersonalDatabaseLayout.Rect scrollbarThumbRect(PersonalDatabaseLayout.Rect clipRect, VisibleRange visibleRange) {
        if (clipRect.width() <= 0 || clipRect.height() <= 0) {
            return null;
        }
        if (!visibleRange.hasRowsAbove() && !visibleRange.hasRowsBelow()) {
            return null;
        }
        int trackWidth = 4;
        int trackLeft = clipRect.right() - trackWidth - 1;
        int trackTop = clipRect.y() + 2;
        int trackBottom = clipRect.bottom() - 2;
        int trackHeight = Math.max(1, trackBottom - trackTop);
        int maxScrollIndex = Math.max(1, visibleRange.totalRows() - visibleRange.maxVisibleRows());
        int thumbHeight = Math.max(10, trackHeight * visibleRange.maxVisibleRows() / Math.max(1, visibleRange.totalRows()));
        thumbHeight = Math.min(trackHeight, thumbHeight);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbTop = trackTop + thumbTravel * visibleRange.scrollIndex() / maxScrollIndex;
        return new PersonalDatabaseLayout.Rect(trackLeft, thumbTop, trackWidth, thumbHeight);
    }

    static void renderScrollIndicators(
            PersonalDatabaseScreen screen,
            GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect clipRect,
            VisibleRange visibleRange
    ) {
        if (clipRect.width() <= 0 || clipRect.height() <= 0) {
            return;
        }
        if (!visibleRange.hasRowsAbove() && !visibleRange.hasRowsBelow()) {
            return;
        }
        int trackWidth = 4;
        int trackLeft = clipRect.right() - trackWidth - 1;
        int trackTop = clipRect.y() + 2;
        int trackBottom = clipRect.bottom() - 2;
        guiGraphics.fill(trackLeft, trackTop, trackLeft + trackWidth, trackBottom, PersonalDatabaseScreen.SCROLLBAR_TRACK_COLOR);
        var thumb = scrollbarThumbRect(clipRect, visibleRange);
        if (thumb != null) {
            guiGraphics.fill(thumb.x(), thumb.y(), thumb.right(), thumb.bottom(), PersonalDatabaseScreen.SCROLLBAR_THUMB_COLOR);
            if (thumb.height() >= 12) {
                guiGraphics.fill(thumb.x(), thumb.y(), thumb.right(), thumb.y() + 1, PersonalDatabaseScreen.SCROLLBAR_THUMB_HOVERED_COLOR);
            }
        }
    }

    record VisibleRange(int fromIndex, int toIndex, int scrollIndex, int maxVisibleRows, int totalRows) {
        boolean hasRowsAbove() {
            return this.fromIndex > 0;
        }

        boolean hasRowsBelow() {
            return this.toIndex < this.totalRows;
        }
    }
}
