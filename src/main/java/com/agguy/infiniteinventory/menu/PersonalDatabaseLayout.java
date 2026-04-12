package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.compat.AccessorySlotGroup;
import java.util.ArrayList;
import java.util.List;

public record PersonalDatabaseLayout(
        Rect frameRect,
        Rect tabBarRect,
        Rect titleRect,
        Rect personalScopeButtonRect,
        Rect publicScopeButtonRect,
        Rect toolbarRect,
        Rect searchFieldRect,
        Rect advancedSearchButtonRect,
        Rect sortButtonRect,
        Rect depositButtonRect,
        Rect pageLabelRect,
        Rect equipmentPanelRect,
        Rect accessoryToggleRect,
        Rect accessoriesPanelRect,
        Rect bottomInventoryRect,
        Rect databasePanelRect,
        Rect databaseGridRect,
        Rect databaseFooterRect,
        Rect previousPageButtonRect,
        Rect nextPageButtonRect,
        List<AccessorySlotLayout> accessorySlotLayouts,
        int accessoryColumns,
        int accessoryVisibleRows,
        int accessoryTotalRows,
        int accessoryScrollRow,
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
    public static final int DATABASE_SLOT_SIZE = 20;
    public static final int GRID_PADDING = 10;
    public static final int FOOTER_HEIGHT = 20;
    public static final int PAGE_BUTTON_WIDTH = 20;
    public static final int PAGE_BUTTON_GAP = 4;
    public static final int PAGE_LABEL_WIDTH = 80;
    public static final int PAGE_CONTROLS_WIDTH = PAGE_BUTTON_WIDTH * 2 + PAGE_BUTTON_GAP * 2 + PAGE_LABEL_WIDTH;
    public static final int MAX_COLUMNS = 40;
    public static final int MAX_ROWS = 16;
    public static final int ACCESSORY_DRAWER_PADDING = 8;
    public static final int ACCESSORY_DRAWER_TITLE_HEIGHT = 12;
    public static final int ACCESSORY_DRAWER_TITLE_GAP = 8;
    private static final int TITLE_HEIGHT = CONTROL_HEIGHT;
    private static final int TITLE_GAP = 6;
    private static final int SEARCH_MIN_WIDTH = 140;
    private static final int ADVANCED_SEARCH_BUTTON_WIDTH = 40;
    private static final int SORT_BUTTON_WIDTH = 128;
    private static final int DEPOSIT_BUTTON_WIDTH = 88;
    private static final int SCOPE_BUTTON_WIDTH = 72;
    private static final int ACCESSORY_DRAWER_MIN_WIDTH = 212;
    private static final int ACCESSORY_DRAWER_TOP_GAP = 4;
    private static final int HIDDEN_SLOT_X = -20_000;
    private static final int HIDDEN_SLOT_Y = -20_000;

    public PersonalDatabaseLayout {
        frameRect = frameRect == null ? Rect.empty() : frameRect;
        tabBarRect = tabBarRect == null ? Rect.empty() : tabBarRect;
        titleRect = titleRect == null ? Rect.empty() : titleRect;
        personalScopeButtonRect = personalScopeButtonRect == null ? Rect.empty() : personalScopeButtonRect;
        publicScopeButtonRect = publicScopeButtonRect == null ? Rect.empty() : publicScopeButtonRect;
        toolbarRect = toolbarRect == null ? Rect.empty() : toolbarRect;
        searchFieldRect = searchFieldRect == null ? Rect.empty() : searchFieldRect;
        advancedSearchButtonRect = advancedSearchButtonRect == null ? Rect.empty() : advancedSearchButtonRect;
        sortButtonRect = sortButtonRect == null ? Rect.empty() : sortButtonRect;
        depositButtonRect = depositButtonRect == null ? Rect.empty() : depositButtonRect;
        pageLabelRect = pageLabelRect == null ? Rect.empty() : pageLabelRect;
        equipmentPanelRect = equipmentPanelRect == null ? Rect.empty() : equipmentPanelRect;
        accessoryToggleRect = accessoryToggleRect == null ? Rect.empty() : accessoryToggleRect;
        accessoriesPanelRect = accessoriesPanelRect == null ? Rect.empty() : accessoriesPanelRect;
        bottomInventoryRect = bottomInventoryRect == null ? Rect.empty() : bottomInventoryRect;
        databasePanelRect = databasePanelRect == null ? Rect.empty() : databasePanelRect;
        databaseGridRect = databaseGridRect == null ? Rect.empty() : databaseGridRect;
        databaseFooterRect = databaseFooterRect == null ? Rect.empty() : databaseFooterRect;
        previousPageButtonRect = previousPageButtonRect == null ? Rect.empty() : previousPageButtonRect;
        nextPageButtonRect = nextPageButtonRect == null ? Rect.empty() : nextPageButtonRect;
        accessorySlotLayouts = accessorySlotLayouts == null ? List.of() : List.copyOf(accessorySlotLayouts);
        accessoryColumns = Math.max(0, accessoryColumns);
        accessoryVisibleRows = Math.max(0, accessoryVisibleRows);
        accessoryTotalRows = Math.max(0, accessoryTotalRows);
        accessoryScrollRow = Math.max(0, accessoryScrollRow);
        databaseColumns = Math.max(1, databaseColumns);
        databaseRows = Math.max(1, databaseRows);
    }

    public static PersonalDatabaseLayout create(
            int screenWidth,
            int screenHeight,
            int equipmentWidth,
            int equipmentHeight,
            int bottomInventoryWidth,
            int bottomInventoryHeight,
            List<AccessorySlotGroup> accessoryGroups
    ) {
        return create(
                screenWidth,
                screenHeight,
                equipmentWidth,
                equipmentHeight,
                bottomInventoryWidth,
                bottomInventoryHeight,
                accessoryGroups,
                false,
                0
        );
    }

    public static PersonalDatabaseLayout create(
            int screenWidth,
            int screenHeight,
            int equipmentWidth,
            int equipmentHeight,
            int bottomInventoryWidth,
            int bottomInventoryHeight,
            List<AccessorySlotGroup> accessoryGroups,
            boolean accessoriesExpanded,
            int accessoryScrollRow
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
        int scopeButtonsX = Math.max(titleRect.x(), frameRect.right() - INNER_PADDING - SCOPE_BUTTON_WIDTH * 2);
        Rect personalScopeButtonRect = new Rect(scopeButtonsX, titleRect.y(), SCOPE_BUTTON_WIDTH, CONTROL_HEIGHT);
        Rect publicScopeButtonRect = new Rect(personalScopeButtonRect.right(), titleRect.y(), SCOPE_BUTTON_WIDTH, CONTROL_HEIGHT);
        int toolbarY = titleRect.bottom() + TITLE_GAP;
        Rect toolbarRect = new Rect(
                frameRect.x() + INNER_PADDING,
                toolbarY,
                Math.max(1, frameRect.width() - INNER_PADDING * 2),
                CONTROL_HEIGHT
        );

        int toolbarLeft = toolbarRect.x();
        int toolbarRight = toolbarRect.right();
        Rect previousPageButtonRect = new Rect(toolbarRight - PAGE_CONTROLS_WIDTH, toolbarY, PAGE_BUTTON_WIDTH, CONTROL_HEIGHT);
        Rect pageLabelRect = new Rect(previousPageButtonRect.right() + PAGE_BUTTON_GAP, toolbarY, PAGE_LABEL_WIDTH, CONTROL_HEIGHT);
        Rect nextPageButtonRect = new Rect(pageLabelRect.right() + PAGE_BUTTON_GAP, toolbarY, PAGE_BUTTON_WIDTH, CONTROL_HEIGHT);
        Rect depositButtonRect = new Rect(previousPageButtonRect.x() - TOOLBAR_GAP - DEPOSIT_BUTTON_WIDTH, toolbarY, DEPOSIT_BUTTON_WIDTH, CONTROL_HEIGHT);
        Rect sortButtonRect = new Rect(depositButtonRect.x() - TOOLBAR_GAP - SORT_BUTTON_WIDTH, toolbarY, SORT_BUTTON_WIDTH, CONTROL_HEIGHT);
        Rect advancedSearchButtonRect = new Rect(sortButtonRect.x() - TOOLBAR_GAP - ADVANCED_SEARCH_BUTTON_WIDTH, toolbarY, ADVANCED_SEARCH_BUTTON_WIDTH, CONTROL_HEIGHT);
        int searchWidth = Math.max(SEARCH_MIN_WIDTH, advancedSearchButtonRect.x() - TOOLBAR_GAP - toolbarLeft);
        Rect searchFieldRect = new Rect(toolbarLeft, toolbarY, searchWidth, CONTROL_HEIGHT);

        int contentTop = toolbarRect.bottom() + SECTION_GAP;
        int playerColumnX = frameRect.x() + INNER_PADDING;
        int playerColumnWidth = Math.max(equipmentWidth, bottomInventoryWidth);
        Rect equipmentPanelRect = new Rect(playerColumnX, contentTop, equipmentWidth, equipmentHeight);
        Rect accessoryToggleRect = accessoryGroups == null || accessoryGroups.isEmpty()
                ? Rect.empty()
                : new Rect(playerColumnX, equipmentPanelRect.bottom() + SECTION_GAP, playerColumnWidth, CONTROL_HEIGHT);
        int bottomInventoryTop = accessoryToggleRect.height() > 0
                ? accessoryToggleRect.bottom() + SECTION_GAP
                : equipmentPanelRect.bottom() + SECTION_GAP;
        Rect bottomInventoryRect = new Rect(playerColumnX, bottomInventoryTop, bottomInventoryWidth, bottomInventoryHeight);
        Rect accessoriesPanelRect = createAccessoriesPanelRect(frameRect, accessoryToggleRect, accessoryGroups, accessoriesExpanded);
        AccessorySlotLayoutResult accessorySlotLayoutResult = buildAccessorySlotLayouts(
                accessoryGroups,
                accessoriesPanelRect,
                accessoryScrollRow
        );

        int databasePanelX = playerColumnX + playerColumnWidth + SECTION_GAP;
        int databasePanelWidth = Math.max(1, frameRect.right() - INNER_PADDING - databasePanelX);
        Rect databaseFooterRect = new Rect(
                databasePanelX,
                frameRect.bottom() - INNER_PADDING - FOOTER_HEIGHT,
                databasePanelWidth,
                FOOTER_HEIGHT
        );
        int databasePanelHeight = Math.max(1, databaseFooterRect.y() - SECTION_GAP - contentTop);
        Rect databasePanelRect = new Rect(databasePanelX, contentTop, databasePanelWidth, databasePanelHeight);

        int availableGridWidth = Math.max(DATABASE_SLOT_SIZE, databasePanelRect.width() - GRID_PADDING * 2);
        int availableGridHeight = Math.max(DATABASE_SLOT_SIZE, databasePanelRect.height() - GRID_PADDING * 2);
        int databaseColumns = clamp(availableGridWidth / DATABASE_SLOT_SIZE, 1, MAX_COLUMNS);
        int databaseRows = clamp(availableGridHeight / DATABASE_SLOT_SIZE, 1, MAX_ROWS);
        int gridWidth = databaseColumns * DATABASE_SLOT_SIZE;
        int gridHeight = databaseRows * DATABASE_SLOT_SIZE;
        Rect databaseGridRect = new Rect(
                databasePanelRect.x() + GRID_PADDING,
                databasePanelRect.y() + GRID_PADDING,
                gridWidth,
                gridHeight
        );

        return new PersonalDatabaseLayout(
                frameRect,
                tabBarRect,
                titleRect,
                personalScopeButtonRect,
                publicScopeButtonRect,
                toolbarRect,
                searchFieldRect,
                advancedSearchButtonRect,
                sortButtonRect,
                depositButtonRect,
                pageLabelRect,
                equipmentPanelRect,
                accessoryToggleRect,
                accessoriesPanelRect,
                bottomInventoryRect,
                databasePanelRect,
                databaseGridRect,
                databaseFooterRect,
                previousPageButtonRect,
                nextPageButtonRect,
                accessorySlotLayoutResult.slotLayouts(),
                accessorySlotLayoutResult.columns(),
                accessorySlotLayoutResult.visibleRows(),
                accessorySlotLayoutResult.totalRows(),
                accessorySlotLayoutResult.scrollRow(),
                databaseColumns,
                databaseRows
        );
    }

    public int databaseSlotCount() {
        return this.databaseColumns * this.databaseRows;
    }

    public int visibleDatabaseSlotCount() {
        int visibleCount = 0;
        for (int slotIndex = 0; slotIndex < this.databaseSlotCount(); slotIndex++) {
            if (this.isDatabaseSlotVisible(slotIndex)) {
                visibleCount++;
            }
        }
        return visibleCount;
    }

    public int accessoryMaxScrollRow() {
        return Math.max(0, this.accessoryTotalRows - this.accessoryVisibleRows);
    }

    public int accessoryVisibleSlotCount() {
        int count = 0;
        for (AccessorySlotLayout slotLayout : this.accessorySlotLayouts) {
            if (slotLayout.visible()) {
                count++;
            }
        }
        return count;
    }

    public int databaseSlotX(int slotIndex) {
        return this.databaseGridRect.x() + slotIndex % this.databaseColumns * DATABASE_SLOT_SIZE;
    }

    public int databaseSlotY(int slotIndex) {
        return this.databaseGridRect.y() + slotIndex / this.databaseColumns * DATABASE_SLOT_SIZE;
    }

    public Rect databaseSlotBounds(int slotIndex) {
        return new Rect(this.databaseSlotX(slotIndex), this.databaseSlotY(slotIndex), DATABASE_SLOT_SIZE, DATABASE_SLOT_SIZE);
    }

    public Rect visibleDatabaseSlotBounds(int visibleSlotIndex) {
        if (visibleSlotIndex < 0) {
            return Rect.empty();
        }
        int resolvedVisibleIndex = 0;
        for (int slotIndex = 0; slotIndex < this.databaseSlotCount(); slotIndex++) {
            if (!this.isDatabaseSlotVisible(slotIndex)) {
                continue;
            }
            if (resolvedVisibleIndex == visibleSlotIndex) {
                return this.databaseSlotBounds(slotIndex);
            }
            resolvedVisibleIndex++;
        }
        return Rect.empty();
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

    private static Rect createAccessoriesPanelRect(
            Rect frameRect,
            Rect accessoryToggleRect,
            List<AccessorySlotGroup> accessoryGroups,
            boolean accessoriesExpanded
    ) {
        if (!accessoriesExpanded || accessoryToggleRect.height() <= 0) {
            return Rect.empty();
        }
        int totalSlotCount = countAccessorySlots(accessoryGroups);
        if (totalSlotCount <= 0) {
            return Rect.empty();
        }
        int drawerX = accessoryToggleRect.right() + SECTION_GAP;
        int drawerY = accessoryToggleRect.bottom() + ACCESSORY_DRAWER_TOP_GAP;
        int maxDrawerWidth = Math.max(1, frameRect.right() - INNER_PADDING - drawerX);
        int drawerWidth = Math.min(maxDrawerWidth, Math.max(accessoryToggleRect.width(), ACCESSORY_DRAWER_MIN_WIDTH));
        int contentWidth = Math.max(SLOT_SIZE, drawerWidth - ACCESSORY_DRAWER_PADDING * 2);
        int columns = Math.max(1, contentWidth / SLOT_SIZE);
        int totalRows = Math.max(1, (totalSlotCount + columns - 1) / columns);
        int maxDrawerBottom = Math.max(drawerY + 1, frameRect.bottom() - INNER_PADDING - FOOTER_HEIGHT - SECTION_GAP);
        int maxGridHeight = Math.max(
                0,
                maxDrawerBottom
                        - drawerY
                        - ACCESSORY_DRAWER_PADDING * 2
                        - ACCESSORY_DRAWER_TITLE_HEIGHT
                        - ACCESSORY_DRAWER_TITLE_GAP
        );
        int visibleRows = Math.max(1, Math.min(totalRows, maxGridHeight / SLOT_SIZE));
        int drawerHeight = ACCESSORY_DRAWER_PADDING * 2
                + ACCESSORY_DRAWER_TITLE_HEIGHT
                + ACCESSORY_DRAWER_TITLE_GAP
                + visibleRows * SLOT_SIZE;
        int drawerBottom = Math.min(maxDrawerBottom, drawerY + drawerHeight);
        return new Rect(drawerX, drawerY, drawerWidth, Math.max(1, drawerBottom - drawerY));
    }

    private static AccessorySlotLayoutResult buildAccessorySlotLayouts(
            List<AccessorySlotGroup> accessoryGroups,
            Rect accessoriesPanelRect,
            int requestedScrollRow
    ) {
        if (accessoryGroups == null || accessoryGroups.isEmpty()) {
            return AccessorySlotLayoutResult.empty();
        }

        int totalSlotCount = countAccessorySlots(accessoryGroups);
        if (totalSlotCount <= 0) {
            return AccessorySlotLayoutResult.empty();
        }

        int columns = 1;
        int visibleRows = 0;
        int totalRows = 0;
        int scrollRow = 0;
        boolean panelVisible = accessoriesPanelRect.height() > 0;
        int gridLeft = HIDDEN_SLOT_X;
        int gridTop = HIDDEN_SLOT_Y;
        if (panelVisible) {
            int contentWidth = Math.max(SLOT_SIZE, accessoriesPanelRect.width() - ACCESSORY_DRAWER_PADDING * 2);
            columns = Math.max(1, contentWidth / SLOT_SIZE);
            totalRows = Math.max(1, (totalSlotCount + columns - 1) / columns);
            int gridHeight = Math.max(0, accessoriesPanelRect.height()
                    - ACCESSORY_DRAWER_PADDING * 2
                    - ACCESSORY_DRAWER_TITLE_HEIGHT
                    - ACCESSORY_DRAWER_TITLE_GAP);
            visibleRows = Math.max(1, gridHeight / SLOT_SIZE);
            scrollRow = clamp(requestedScrollRow, 0, Math.max(0, totalRows - visibleRows));
            gridLeft = accessoriesPanelRect.x() + ACCESSORY_DRAWER_PADDING;
            gridTop = accessoriesPanelRect.y() + ACCESSORY_DRAWER_PADDING + ACCESSORY_DRAWER_TITLE_HEIGHT + ACCESSORY_DRAWER_TITLE_GAP;
        }

        List<AccessorySlotLayout> layouts = new ArrayList<>(totalSlotCount);
        int displayIndex = 0;
        for (AccessorySlotGroup group : accessoryGroups) {
            for (int slotOffset = 0; slotOffset < group.slotCount(); slotOffset++) {
                int row = displayIndex / columns - scrollRow;
                int column = displayIndex % columns;
                boolean visible = panelVisible && row >= 0 && row < visibleRows;
                Rect slotRect = visible
                        ? new Rect(gridLeft + column * SLOT_SIZE, gridTop + row * SLOT_SIZE, SLOT_SIZE, SLOT_SIZE)
                        : hiddenSlotRect();
                layouts.add(new AccessorySlotLayout(group, group.firstSlotIndex() + slotOffset, slotOffset, slotRect, visible));
                displayIndex++;
            }
        }
        return new AccessorySlotLayoutResult(
                List.copyOf(layouts),
                columns,
                visibleRows,
                totalRows,
                scrollRow
        );
    }

    private static Rect hiddenSlotRect() {
        return new Rect(HIDDEN_SLOT_X, HIDDEN_SLOT_Y, SLOT_SIZE, SLOT_SIZE);
    }

    private static int countAccessorySlots(List<AccessorySlotGroup> accessoryGroups) {
        if (accessoryGroups == null || accessoryGroups.isEmpty()) {
            return 0;
        }
        int totalSlotCount = 0;
        for (AccessorySlotGroup accessoryGroup : accessoryGroups) {
            totalSlotCount += Math.max(0, accessoryGroup.slotCount());
        }
        return totalSlotCount;
    }

    private boolean isDatabaseSlotVisible(int slotIndex) {
        return !this.accessoriesPanelCovers(this.databaseSlotBounds(slotIndex));
    }

    private boolean accessoriesPanelCovers(Rect rect) {
        return this.accessoriesPanelRect.height() > 0 && this.accessoriesPanelRect.intersects(rect);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record AccessorySlotLayout(AccessorySlotGroup group, int slotIndex, int slotOffset, Rect slotRect, boolean visible) {
        public AccessorySlotLayout {
            group = group == null ? new AccessorySlotGroup("", "", -1, 0) : group;
            slotRect = slotRect == null ? hiddenSlotRect() : slotRect;
        }
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

    private record AccessorySlotLayoutResult(
            List<AccessorySlotLayout> slotLayouts,
            int columns,
            int visibleRows,
            int totalRows,
            int scrollRow
    ) {
        private static AccessorySlotLayoutResult empty() {
            return new AccessorySlotLayoutResult(List.of(), 0, 0, 0, 0);
        }
    }
}
