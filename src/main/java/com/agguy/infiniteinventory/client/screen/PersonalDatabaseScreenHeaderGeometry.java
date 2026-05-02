package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import net.minecraft.util.Mth;

final class PersonalDatabaseScreenHeaderGeometry {
    private static final int PANEL_HEADER_TITLE_HEIGHT = 12;
    private static final int PANEL_HEADER_ROW_GAP = 2;
    private static final int PANEL_SORT_BUTTON_WIDTH = 80;

    private PersonalDatabaseScreenHeaderGeometry() {
    }

    static PanelHeaderLayout panelHeaderLayout(PersonalDatabaseScreen screen, int panelIndex) {
        if (screen.layout == null || panelIndex < 0 || panelIndex >= screen.layout.databaseViewportCount()) {
            return PanelHeaderLayout.empty();
        }
        PersonalDatabaseLayout.DatabaseViewportLayout viewportLayout = screen.layout.databaseViewportLayout(panelIndex);
        PersonalDatabaseLayout.Rect headerRect = viewportLayout.headerRect();
        if (headerRect.height() <= 0 || headerRect.width() <= 0) {
            return PanelHeaderLayout.empty();
        }

        int controlHeight = PersonalDatabaseLayout.CONTROL_HEIGHT;
        int headerX = headerRect.x();
        int titleY = headerRect.y();
        int searchY = titleY + PANEL_HEADER_TITLE_HEIGHT + PANEL_HEADER_ROW_GAP;
        int controlsY = searchY + controlHeight + PANEL_HEADER_ROW_GAP;
        PersonalDatabaseLayout.Rect searchRect = new PersonalDatabaseLayout.Rect(
                headerX,
                searchY,
                headerRect.width(),
                controlHeight
        );

        int pagerButtonWidth = PersonalDatabaseLayout.PAGE_BUTTON_WIDTH;
        int minPageWidth = 52;
        int maxPageWidth = 64;
        int pageWidth = Mth.clamp(headerRect.width() / 6, minPageWidth, maxPageWidth);
        int pagerWidth = pagerButtonWidth * 2 + pageWidth + PersonalDatabaseLayout.PAGE_BUTTON_GAP * 2;
        int availableSortWidth = headerRect.width() - pagerWidth - PersonalDatabaseLayout.PAGE_BUTTON_GAP;
        int sortWidth = Math.max(64, Math.min(PANEL_SORT_BUTTON_WIDTH, availableSortWidth));
        if (sortWidth + PersonalDatabaseLayout.PAGE_BUTTON_GAP + pagerWidth > headerRect.width()) {
            sortWidth = Math.max(
                    48,
                    headerRect.width() - pagerWidth - PersonalDatabaseLayout.PAGE_BUTTON_GAP
            );
        }
        int pagerX = headerRect.right() - pagerWidth;
        PersonalDatabaseLayout.Rect sortRect = new PersonalDatabaseLayout.Rect(
                headerX,
                controlsY,
                Math.max(1, Math.min(sortWidth, pagerX - headerX - PersonalDatabaseLayout.PAGE_BUTTON_GAP)),
                controlHeight
        );
        int previousX = Math.max(sortRect.right() + PersonalDatabaseLayout.PAGE_BUTTON_GAP, pagerX);
        PersonalDatabaseLayout.Rect previousRect = new PersonalDatabaseLayout.Rect(
                previousX,
                controlsY,
                pagerButtonWidth,
                controlHeight
        );
        PersonalDatabaseLayout.Rect pageRect = new PersonalDatabaseLayout.Rect(
                previousRect.right() + PersonalDatabaseLayout.PAGE_BUTTON_GAP,
                controlsY,
                pageWidth,
                controlHeight
        );
        PersonalDatabaseLayout.Rect nextRect = new PersonalDatabaseLayout.Rect(
                pageRect.right() + PersonalDatabaseLayout.PAGE_BUTTON_GAP,
                controlsY,
                pagerButtonWidth,
                controlHeight
        );
        return new PanelHeaderLayout(searchRect, sortRect, previousRect, pageRect, nextRect);
    }

    record PanelHeaderLayout(
            PersonalDatabaseLayout.Rect searchRect,
            PersonalDatabaseLayout.Rect sortRect,
            PersonalDatabaseLayout.Rect previousRect,
            PersonalDatabaseLayout.Rect pageRect,
            PersonalDatabaseLayout.Rect nextRect
    ) {
        private static PanelHeaderLayout empty() {
            PersonalDatabaseLayout.Rect empty = PersonalDatabaseLayout.Rect.empty();
            return new PanelHeaderLayout(empty, empty, empty, empty, empty);
        }
    }
}
