package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseTabs;

enum PersonalDatabaseScreenFitProfile {
    UNSUPPORTED(false, false, 1),
    COMPACT(true, true, 1),
    STANDARD(true, false, DatabaseTabs.MAX_VISIBLE_TAB_COUNT);

    private static final int MIN_SUPPORTED_WIDTH = 480;
    private static final int MIN_SUPPORTED_HEIGHT = 270;
    private static final int MIN_STANDARD_WIDTH = 640;
    private static final int MIN_STANDARD_HEIGHT = 360;

    private final boolean fullUiSupported;
    private final boolean compactLayout;
    private final int maxVisiblePanels;

    PersonalDatabaseScreenFitProfile(boolean fullUiSupported, boolean compactLayout, int maxVisiblePanels) {
        this.fullUiSupported = fullUiSupported;
        this.compactLayout = compactLayout;
        this.maxVisiblePanels = maxVisiblePanels;
    }

    static PersonalDatabaseScreenFitProfile resolve(int screenWidth, int screenHeight) {
        if (screenWidth < MIN_SUPPORTED_WIDTH || screenHeight < MIN_SUPPORTED_HEIGHT) {
            return UNSUPPORTED;
        }
        if (screenWidth < MIN_STANDARD_WIDTH || screenHeight < MIN_STANDARD_HEIGHT) {
            return COMPACT;
        }
        return STANDARD;
    }

    boolean supportsFullUi() {
        return this.fullUiSupported;
    }

    boolean compactLayout() {
        return this.compactLayout;
    }

    boolean showsViewSelectorPreview() {
        return this == STANDARD;
    }

    boolean usesCompactManagementLayout() {
        return this == COMPACT;
    }

    int maxVisiblePanels() {
        return this.maxVisiblePanels;
    }
}
