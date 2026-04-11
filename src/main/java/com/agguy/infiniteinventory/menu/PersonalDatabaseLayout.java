package com.agguy.infiniteinventory.menu;

public record PersonalDatabaseLayout(
        Rect frameRect,
        Rect tabBarRect,
        Rect titleRect,
        Rect searchFieldRect,
        Rect sortButtonRect,
        Rect depositButtonRect,
        Rect equipmentPanelRect,
        Rect bottomInventoryRect,
        Rect databasePanelRect,
        Rect databaseGridRect,
        Rect databaseFooterRect,
        Rect previousPageButtonRect,
        Rect nextPageButtonRect,
        int databaseColumns,
        int databaseRows
) {
    public static final int FRAME_MARGIN = 12;
    public static final int INNER_PADDING = 10;
    public static final int TAB_HEIGHT = 24;
    public static final int TAB_GAP = 4;
    public static final int CONTROL_HEIGHT = 20;
    public static final int TOOLBAR_GAP = 6;
    public static final int SECTION_GAP = 12;
    public static final int SLOT_SIZE = 18;
    public static final int GRID_PADDING = 8;
    public static final int FOOTER_HEIGHT = 24;
    public static final int PAGE_BUTTON_WIDTH = 20;
    public static final int PAGE_CONTROLS_WIDTH = 108;
    public static final int MAX_COLUMNS = 14;
    public static final int MAX_ROWS = 8;
    private static final int TITLE_HEIGHT = 12;
    private static final int TITLE_GAP = 8;
    private static final int SEARCH_MIN_WIDTH = 96;
    private static final int SEARCH_MAX_WIDTH = 280;
    private static final int SORT_BUTTON_WIDTH = 110;
    private static final int DEPOSIT_BUTTON_WIDTH = 86;

    public PersonalDatabaseLayout {
        frameRect = frameRect == null ? Rect.empty() : frameRect;
        tabBarRect = tabBarRect == null ? Rect.empty() : tabBarRect;
        titleRect = titleRect == null ? Rect.empty() : titleRect;
        searchFieldRect = searchFieldRect == null ? Rect.empty() : searchFieldRect;
        sortButtonRect = sortButtonRect == null ? Rect.empty() : sortButtonRect;
        depositButtonRect = depositButtonRect == null ? Rect.empty() : depositButtonRect;
        equipmentPanelRect = equipmentPanelRect == null ? Rect.empty() : equipmentPanelRect;
        bottomInventoryRect = bottomInventoryRect == null ? Rect.empty() : bottomInventoryRect;
        databasePanelRect = databasePanelRect == null ? Rect.empty() : databasePanelRect;
        databaseGridRect = databaseGridRect == null ? Rect.empty() : databaseGridRect;
        databaseFooterRect = databaseFooterRect == null ? Rect.empty() : databaseFooterRect;
        previousPageButtonRect = previousPageButtonRect == null ? Rect.empty() : previousPageButtonRect;
        nextPageButtonRect = nextPageButtonRect == null ? Rect.empty() : nextPageButtonRect;
        databaseColumns = Math.max(1, databaseColumns);
        databaseRows = Math.max(1, databaseRows);
    }

    public static PersonalDatabaseLayout create(
            int screenWidth,
            int screenHeight,
            int equipmentWidth,
            int equipmentHeight,
            int bottomInventoryWidth,
            int bottomInventoryHeight
    ) {
        int frameY = FRAME_MARGIN + TAB_HEIGHT - 1;
        Rect frameRect = new Rect(
                FRAME_MARGIN,
                frameY,
                Math.max(1, screenWidth - FRAME_MARGIN * 2),
                Math.max(1, screenHeight - FRAME_MARGIN - frameY)
        );
        Rect tabBarRect = new Rect(
                frameRect.x() + INNER_PADDING,
                frameRect.y() - TAB_HEIGHT + 1,
                Math.max(1, frameRect.width() - INNER_PADDING * 2),
                TAB_HEIGHT
        );
        Rect titleRect = new Rect(
                frameRect.x() + INNER_PADDING,
                frameRect.y() + INNER_PADDING,
                Math.max(1, frameRect.width() - INNER_PADDING * 2),
                TITLE_HEIGHT
        );

        int toolbarLeft = frameRect.x() + INNER_PADDING;
        int toolbarRight = frameRect.right() - INNER_PADDING;
        int searchWidth = Math.min(
                SEARCH_MAX_WIDTH,
                Math.max(SEARCH_MIN_WIDTH, toolbarRight - toolbarLeft - SORT_BUTTON_WIDTH - DEPOSIT_BUTTON_WIDTH - TOOLBAR_GAP * 2)
        );
        int searchY = titleRect.bottom() + TITLE_GAP;
        Rect searchFieldRect = new Rect(toolbarLeft, searchY, searchWidth, CONTROL_HEIGHT);
        Rect sortButtonRect = new Rect(searchFieldRect.right() + TOOLBAR_GAP, searchY, SORT_BUTTON_WIDTH, CONTROL_HEIGHT);
        Rect depositButtonRect = new Rect(sortButtonRect.right() + TOOLBAR_GAP, searchY, DEPOSIT_BUTTON_WIDTH, CONTROL_HEIGHT);

        int contentTop = searchFieldRect.bottom() + SECTION_GAP;
        Rect bottomInventoryRect = new Rect(
                frameRect.x() + (frameRect.width() - bottomInventoryWidth) / 2,
                frameRect.bottom() - INNER_PADDING - bottomInventoryHeight,
                bottomInventoryWidth,
                bottomInventoryHeight
        );
        Rect equipmentPanelRect = new Rect(frameRect.x() + INNER_PADDING, contentTop, equipmentWidth, equipmentHeight);

        int databasePanelX = equipmentPanelRect.right() + SECTION_GAP;
        int databasePanelWidth = Math.max(1, frameRect.right() - INNER_PADDING - databasePanelX);
        int databasePanelHeight = Math.max(1, bottomInventoryRect.y() - SECTION_GAP - contentTop);
        Rect databasePanelRect = new Rect(databasePanelX, contentTop, databasePanelWidth, databasePanelHeight);

        int availableGridWidth = Math.max(SLOT_SIZE, databasePanelRect.width() - GRID_PADDING * 2);
        int availableGridHeight = Math.max(SLOT_SIZE, databasePanelRect.height() - GRID_PADDING * 2 - FOOTER_HEIGHT);
        int databaseColumns = clamp(availableGridWidth / SLOT_SIZE, 1, MAX_COLUMNS);
        int databaseRows = clamp(availableGridHeight / SLOT_SIZE, 1, MAX_ROWS);
        int gridWidth = databaseColumns * SLOT_SIZE;
        int gridHeight = databaseRows * SLOT_SIZE;
        Rect databaseGridRect = new Rect(
                databasePanelRect.x() + Math.max(0, (databasePanelRect.width() - gridWidth) / 2),
                databasePanelRect.y() + GRID_PADDING,
                gridWidth,
                gridHeight
        );
        Rect databaseFooterRect = new Rect(
                databasePanelRect.x() + GRID_PADDING,
                databasePanelRect.bottom() - GRID_PADDING - FOOTER_HEIGHT,
                Math.max(1, databasePanelRect.width() - GRID_PADDING * 2),
                FOOTER_HEIGHT
        );
        int pageButtonsX = Math.max(databaseFooterRect.x(), databaseFooterRect.right() - PAGE_CONTROLS_WIDTH);
        Rect previousPageButtonRect = new Rect(pageButtonsX, databaseFooterRect.y() + 2, PAGE_BUTTON_WIDTH, CONTROL_HEIGHT);
        Rect nextPageButtonRect = new Rect(databaseFooterRect.right() - PAGE_BUTTON_WIDTH, databaseFooterRect.y() + 2, PAGE_BUTTON_WIDTH, CONTROL_HEIGHT);

        return new PersonalDatabaseLayout(
                frameRect,
                tabBarRect,
                titleRect,
                searchFieldRect,
                sortButtonRect,
                depositButtonRect,
                equipmentPanelRect,
                bottomInventoryRect,
                databasePanelRect,
                databaseGridRect,
                databaseFooterRect,
                previousPageButtonRect,
                nextPageButtonRect,
                databaseColumns,
                databaseRows
        );
    }

    public int databaseSlotCount() {
        return this.databaseColumns * this.databaseRows;
    }

    public int databaseSlotX(int slotIndex) {
        return this.databaseGridRect.x() + slotIndex % this.databaseColumns * SLOT_SIZE;
    }

    public int databaseSlotY(int slotIndex) {
        return this.databaseGridRect.y() + slotIndex / this.databaseColumns * SLOT_SIZE;
    }

    public Rect databaseSlotBounds(int slotIndex) {
        return new Rect(this.databaseSlotX(slotIndex), this.databaseSlotY(slotIndex), SLOT_SIZE, SLOT_SIZE);
    }

    public Rect tabBounds(int index, int totalTabs) {
        if (totalTabs <= 0) {
            return Rect.empty();
        }
        int totalGap = Math.max(0, totalTabs - 1) * TAB_GAP;
        int tabWidth = Math.max(1, (this.tabBarRect.width() - totalGap) / totalTabs);
        int x = this.tabBarRect.x() + index * (tabWidth + TAB_GAP);
        int width = index == totalTabs - 1 ? this.tabBarRect.right() - x : tabWidth;
        return new Rect(x, this.tabBarRect.y(), width, this.tabBarRect.height());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Rect(int x, int y, int width, int height) {
        public Rect {
            width = Math.max(0, width);
            height = Math.max(0, height);
        }

        public static Rect empty() {
            return new Rect(0, 0, 0, 0);
        }

        public int right() {
            return this.x + this.width;
        }

        public int bottom() {
            return this.y + this.height;
        }

        public int centerX() {
            return this.x + this.width / 2;
        }

        public int centerY() {
            return this.y + this.height / 2;
        }

        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.right() && mouseY >= this.y && mouseY < this.bottom();
        }

        public boolean intersects(Rect other) {
            return this.x < other.right()
                    && this.right() > other.x
                    && this.y < other.bottom()
                    && this.bottom() > other.y;
        }
    }
}
