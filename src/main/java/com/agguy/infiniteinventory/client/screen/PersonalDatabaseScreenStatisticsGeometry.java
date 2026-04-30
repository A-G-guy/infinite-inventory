package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;

/**
 * 统计面板几何尺寸与布局计算。
 */
final class PersonalDatabaseScreenStatisticsGeometry {
    static final int STATISTICS_PANEL_WIDTH = 560;
    static final int STATISTICS_PANEL_HEIGHT = 380;
    static final int NAV_WIDTH = 120;
    static final int NAV_ITEM_HEIGHT = 26;
    static final int NAV_PADDING = 8;
    static final int CONTENT_PADDING = 12;
    static final int SCOPE_TOGGLE_WIDTH = 60;
    static final int CLOSE_BUTTON_RESERVED_WIDTH = 28;

    private PersonalDatabaseScreenStatisticsGeometry() {
    }

    static PersonalDatabaseLayout.Rect statisticsPanelRect(PersonalDatabaseScreen screen) {
        return PersonalDatabaseScreenGeometry.centeredOverlayRect(
                screen, STATISTICS_PANEL_WIDTH, STATISTICS_PANEL_HEIGHT
        );
    }

    static PersonalDatabaseLayout.Rect statisticsNavRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = statisticsPanelRect(screen);
        return new PersonalDatabaseLayout.Rect(
                panelRect.x(), panelRect.y(),
                NAV_WIDTH, panelRect.height()
        );
    }

    static PersonalDatabaseLayout.Rect statisticsContentRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = statisticsPanelRect(screen);
        PersonalDatabaseLayout.Rect navRect = statisticsNavRect(screen);
        return new PersonalDatabaseLayout.Rect(
                navRect.right() + 1, panelRect.y(),
                Math.max(1, panelRect.right() - navRect.right() - 1),
                panelRect.height()
        );
    }

    static PersonalDatabaseLayout.Rect statisticsContentInnerRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect contentRect = statisticsContentRect(screen);
        return new PersonalDatabaseLayout.Rect(
                contentRect.x() + CONTENT_PADDING,
                contentRect.y() + CONTENT_PADDING,
                Math.max(1, contentRect.width() - CONTENT_PADDING * 2),
                Math.max(1, contentRect.height() - CONTENT_PADDING * 2)
        );
    }

    static PersonalDatabaseLayout.Rect statisticsNavItemRect(PersonalDatabaseScreen screen, int index) {
        PersonalDatabaseLayout.Rect navRect = statisticsNavRect(screen);
        int titleBottom = navRect.y() + NAV_PADDING + NAV_ITEM_HEIGHT;
        int itemY = titleBottom + 4 + index * (NAV_ITEM_HEIGHT + 2);
        return new PersonalDatabaseLayout.Rect(
                navRect.x() + NAV_PADDING,
                itemY,
                Math.max(1, navRect.width() - NAV_PADDING * 2),
                NAV_ITEM_HEIGHT
        );
    }

    static PersonalDatabaseLayout.Rect statisticsScopeToggleRect(PersonalDatabaseScreen screen, DatabaseScope scope) {
        PersonalDatabaseLayout.Rect panelRect = statisticsPanelRect(screen);
        boolean isPersonal = scope == DatabaseScope.PERSONAL;
        int x = isPersonal
                ? panelRect.right() - SCOPE_TOGGLE_WIDTH * 2 - 8 - CLOSE_BUTTON_RESERVED_WIDTH
                : panelRect.right() - SCOPE_TOGGLE_WIDTH - 8 - CLOSE_BUTTON_RESERVED_WIDTH;
        return new PersonalDatabaseLayout.Rect(x, panelRect.y() + 8, SCOPE_TOGGLE_WIDTH, 20);
    }

    static int clickedNavItemIndex(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        PersonalDatabaseLayout.Rect navRect = statisticsNavRect(screen);
        if (!navRect.contains(mouseX, mouseY)) {
            return -1;
        }
        for (int i = 0; i < PersonalDatabaseScreenEnums.StatisticsPanelTab.values().length; i++) {
            if (statisticsNavItemRect(screen, i).contains(mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }

    static boolean isWithinStatisticsPanel(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.statisticsPanelExpanded || screen.layout == null) {
            return false;
        }
        return statisticsPanelRect(screen).contains(mouseX, mouseY);
    }
}
