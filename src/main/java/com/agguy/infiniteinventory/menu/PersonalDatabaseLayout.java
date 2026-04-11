package com.agguy.infiniteinventory.menu;

public final class PersonalDatabaseLayout {
    public static final int SCREEN_WIDTH = 364;
    public static final int SCREEN_HEIGHT = 324;
    public static final int PLAYER_PANEL_X = 0;
    public static final int PLAYER_PANEL_Y = 102;
    public static final int PLAYER_PANEL_WIDTH = 176;
    public static final int PLAYER_PANEL_HEIGHT = 166;
    public static final int DATABASE_PANEL_X = 188;
    public static final int DATABASE_PANEL_Y = 102;
    public static final int DATABASE_PANEL_WIDTH = 176;
    public static final int DATABASE_PANEL_HEIGHT = 222;
    public static final int DATABASE_COLUMNS = 9;
    public static final int DATABASE_ROWS = 6;
    public static final int DATABASE_SLOT_COUNT = DATABASE_COLUMNS * DATABASE_ROWS;
    public static final int DATABASE_SLOT_SPACING = 18;

    private PersonalDatabaseLayout() {
    }

    public static int databaseSlotX(int slotIndex) {
        return DATABASE_PANEL_X + 8 + slotIndex % DATABASE_COLUMNS * DATABASE_SLOT_SPACING;
    }

    public static int databaseSlotY(int slotIndex) {
        return DATABASE_PANEL_Y + 18 + slotIndex / DATABASE_COLUMNS * DATABASE_SLOT_SPACING;
    }
}
