package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;

final class PersonalDatabaseScreenLogGeometry {
    static final int LOG_PANEL_WIDTH = 560;
    static final int LOG_PANEL_HEIGHT = 360;
    static final int LOG_PANEL_ROW_HEIGHT = 20;
    static final int LOG_PANEL_PADDING = 8;
    static final int LOG_SCOPE_TOGGLE_WIDTH = 76;
    static final int LOG_SCOPE_TOGGLE_GAP = 4;
    static final int LOG_TITLE_HEIGHT = 22;
    static final int LOG_HEADER_HEIGHT = 16;
    static final int LOG_SCROLLBAR_WIDTH = 6;
    static final int LOG_SCROLLBAR_MARGIN = 4;
    // 为右上角关闭按钮预留的宽度（按钮20 + 右间距4 + 间隙4）
    static final int LOG_CLOSE_BUTTON_RESERVED_WIDTH = 28;

    private PersonalDatabaseScreenLogGeometry() {
    }

    static PersonalDatabaseLayout.Rect logPanelRect(PersonalDatabaseScreen screen) {
        return PersonalDatabaseScreenGeometry.centeredOverlayRect(screen, LOG_PANEL_WIDTH, LOG_PANEL_HEIGHT);
    }

    static PersonalDatabaseLayout.Rect logPanelTitleRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = logPanelRect(screen);
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + LOG_PANEL_PADDING,
                panelRect.y() + LOG_PANEL_PADDING,
                panelRect.width() - LOG_PANEL_PADDING * 2,
                LOG_TITLE_HEIGHT
        );
    }

    static PersonalDatabaseLayout.Rect logPanelCloseButtonRect(PersonalDatabaseScreen screen) {
        return PersonalDatabaseScreenGeometry.overlayCloseButtonRect(logPanelRect(screen));
    }

    static PersonalDatabaseLayout.Rect logPanelScopeToggleRect(PersonalDatabaseScreen screen, DatabaseScope scope) {
        PersonalDatabaseLayout.Rect titleRect = logPanelTitleRect(screen);
        boolean isPersonal = scope == DatabaseScope.PERSONAL;
        int x = isPersonal
                ? titleRect.right() - LOG_SCOPE_TOGGLE_WIDTH * 2 - LOG_SCOPE_TOGGLE_GAP - LOG_CLOSE_BUTTON_RESERVED_WIDTH
                : titleRect.right() - LOG_SCOPE_TOGGLE_WIDTH - LOG_CLOSE_BUTTON_RESERVED_WIDTH;
        return new PersonalDatabaseLayout.Rect(
                x,
                titleRect.y() + (LOG_TITLE_HEIGHT - PersonalDatabaseLayout.CONTROL_HEIGHT) / 2,
                LOG_SCOPE_TOGGLE_WIDTH,
                PersonalDatabaseLayout.CONTROL_HEIGHT
        );
    }

    static PersonalDatabaseLayout.Rect logPanelListRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = logPanelRect(screen);
        int top = panelRect.y() + LOG_PANEL_PADDING + LOG_TITLE_HEIGHT + 4;
        int bottom = panelRect.bottom() - LOG_PANEL_PADDING;
        int left = panelRect.x() + LOG_PANEL_PADDING;
        int right = panelRect.right() - LOG_PANEL_PADDING - LOG_SCROLLBAR_WIDTH - LOG_SCROLLBAR_MARGIN;
        return new PersonalDatabaseLayout.Rect(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
    }

    static PersonalDatabaseLayout.Rect logPanelScrollbarRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = logPanelRect(screen);
        PersonalDatabaseLayout.Rect listRect = logPanelListRect(screen);
        int top = listRect.y();
        int bottom = listRect.bottom();
        int x = panelRect.right() - LOG_PANEL_PADDING - LOG_SCROLLBAR_WIDTH;
        return new PersonalDatabaseLayout.Rect(x, top, LOG_SCROLLBAR_WIDTH, Math.max(1, bottom - top));
    }

    static PersonalDatabaseLayout.Rect logPanelRowRect(PersonalDatabaseScreen screen, int rowIndex) {
        PersonalDatabaseLayout.Rect listRect = logPanelListRect(screen);
        return new PersonalDatabaseLayout.Rect(
                listRect.x(),
                listRect.y() + rowIndex * LOG_PANEL_ROW_HEIGHT,
                listRect.width(),
                LOG_PANEL_ROW_HEIGHT
        );
    }

    static int visibleLogRowCount(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect listRect = logPanelListRect(screen);
        return Math.max(0, listRect.height() / LOG_PANEL_ROW_HEIGHT);
    }

    static boolean isWithinLogPanel(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.logPanelExpanded || screen.layout == null) {
            return false;
        }
        return logPanelRect(screen).contains(mouseX, mouseY);
    }
}
