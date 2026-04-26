package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;

/**
 * 设置面板几何计算类，负责计算设置面板内各区域的位置与尺寸。
 */
final class PersonalDatabaseScreenSettingsGeometry {
    private static final int NAV_WIDTH = PersonalDatabaseScreen.SETTINGS_NAV_WIDTH;
    private static final int NAV_ITEM_HEIGHT = PersonalDatabaseScreen.SETTINGS_NAV_ITEM_HEIGHT;
    private static final int NAV_PADDING = PersonalDatabaseScreen.SETTINGS_NAV_PADDING;
    private static final int CONTENT_PADDING = PersonalDatabaseScreen.SETTINGS_CONTENT_PADDING;
    private static final int NAV_ITEM_GAP = 2;
    private static final int DIVIDER_WIDTH = 1;

    private PersonalDatabaseScreenSettingsGeometry() {
    }

    /**
     * 设置面板整体矩形，占满 frameRect。
     */
    static PersonalDatabaseLayout.Rect settingsPanelRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        return screen.layout.frameRect();
    }

    /**
     * 左侧导航栏矩形。
     */
    static PersonalDatabaseLayout.Rect settingsNavRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = settingsPanelRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int navWidth = resolveNavWidth(panelRect.width());
        return new PersonalDatabaseLayout.Rect(
                panelRect.x(),
                panelRect.y(),
                navWidth,
                panelRect.height()
        );
    }

    /**
     * 右侧内容区矩形。
     */
    static PersonalDatabaseLayout.Rect settingsContentRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = settingsPanelRect(screen);
        PersonalDatabaseLayout.Rect navRect = settingsNavRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int contentX = navRect.right() + DIVIDER_WIDTH;
        int contentWidth = Math.max(1, panelRect.right() - contentX);
        return new PersonalDatabaseLayout.Rect(
                contentX,
                panelRect.y(),
                contentWidth,
                panelRect.height()
        );
    }

    /**
     * 内容区内边距后的实际可用矩形。
     */
    static PersonalDatabaseLayout.Rect settingsContentInnerRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect contentRect = settingsContentRect(screen);
        if (contentRect.width() <= 0 || contentRect.height() <= 0) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        return new PersonalDatabaseLayout.Rect(
                contentRect.x() + CONTENT_PADDING,
                contentRect.y() + CONTENT_PADDING,
                Math.max(1, contentRect.width() - CONTENT_PADDING * 2),
                Math.max(1, contentRect.height() - CONTENT_PADDING * 2)
        );
    }

    /**
     * 导航项矩形。
     */
    static PersonalDatabaseLayout.Rect settingsNavItemRect(PersonalDatabaseScreen screen, int index) {
        PersonalDatabaseLayout.Rect navRect = settingsNavRect(screen);
        if (navRect.width() <= 0 || navRect.height() <= 0) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int itemX = navRect.x() + NAV_PADDING;
        int itemY = navRect.y() + NAV_PADDING + index * (NAV_ITEM_HEIGHT + NAV_ITEM_GAP);
        int itemWidth = Math.max(1, navRect.width() - NAV_PADDING * 2);
        return new PersonalDatabaseLayout.Rect(itemX, itemY, itemWidth, NAV_ITEM_HEIGHT);
    }

    /**
     * 导航栏标题区域矩形（设置面板标题）。
     */
    static PersonalDatabaseLayout.Rect settingsNavTitleRect(PersonalDatabaseLayout.Rect navRect) {
        if (navRect.width() <= 0 || navRect.height() <= 0) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        return new PersonalDatabaseLayout.Rect(
                navRect.x() + NAV_PADDING,
                navRect.y() + NAV_PADDING,
                Math.max(1, navRect.width() - NAV_PADDING * 2),
                NAV_ITEM_HEIGHT
        );
    }

    /**
     * 判断点是否在设置面板内。
     */
    static boolean isWithinSettingsPanel(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        return settingsPanelRect(screen).contains(mouseX, mouseY);
    }

    /**
     * 判断点是否在内容区内。
     */
    static boolean isWithinSettingsContent(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        return settingsContentRect(screen).contains(mouseX, mouseY);
    }

    /**
     * 获取点击的导航项索引，-1 表示未点击。
     */
    static int clickedNavItemIndex(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        PersonalDatabaseLayout.Rect navRect = settingsNavRect(screen);
        if (!navRect.contains(mouseX, mouseY)) {
            return -1;
        }
        PersonalDatabaseLayout.Rect titleRect = settingsNavTitleRect(navRect);
        if (mouseY < titleRect.bottom() + NAV_ITEM_GAP) {
            return -1;
        }
        for (int i = 0; i < PersonalDatabaseScreenEnums.SettingsPanelTab.values().length; i++) {
            PersonalDatabaseLayout.Rect itemRect = settingsNavItemRect(screen, i);
            if (itemRect.contains(mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }

    private static int resolveNavWidth(int panelWidth) {
        if (panelWidth < 520) {
            return Math.min(NAV_WIDTH, Math.max(100, panelWidth / 4));
        }
        return NAV_WIDTH;
    }
}
