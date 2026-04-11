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
        Rect accessoriesPanelRect,
        Rect bottomInventoryRect,
        Rect databasePanelRect,
        Rect databaseGridRect,
        Rect databaseFooterRect,
        Rect previousPageButtonRect,
        Rect nextPageButtonRect,
        List<AccessoryGroupLayout> accessoryGroupLayouts,
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
    public static final int PAGE_LABEL_WIDTH = 64;
    public static final int PAGE_CONTROLS_WIDTH = PAGE_BUTTON_WIDTH * 2 + PAGE_BUTTON_GAP * 2 + PAGE_LABEL_WIDTH;
    public static final int MAX_COLUMNS = 40;
    public static final int MAX_ROWS = 16;
    public static final int ACCESSORY_PANEL_PADDING = 8;
    public static final int ACCESSORY_GROUP_LABEL_HEIGHT = 12;
    public static final int ACCESSORY_GROUP_LABEL_GAP = 4;
    public static final int ACCESSORY_GROUP_GAP = 6;
    private static final int TITLE_HEIGHT = CONTROL_HEIGHT;
    private static final int TITLE_GAP = 6;
    private static final int SEARCH_MIN_WIDTH = 140;
    private static final int ADVANCED_SEARCH_BUTTON_WIDTH = 40;
    private static final int SORT_BUTTON_WIDTH = 128;
    private static final int DEPOSIT_BUTTON_WIDTH = 88;
    private static final int SCOPE_BUTTON_WIDTH = 72;

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
        accessoriesPanelRect = accessoriesPanelRect == null ? Rect.empty() : accessoriesPanelRect;
        bottomInventoryRect = bottomInventoryRect == null ? Rect.empty() : bottomInventoryRect;
        databasePanelRect = databasePanelRect == null ? Rect.empty() : databasePanelRect;
        databaseGridRect = databaseGridRect == null ? Rect.empty() : databaseGridRect;
        databaseFooterRect = databaseFooterRect == null ? Rect.empty() : databaseFooterRect;
        previousPageButtonRect = previousPageButtonRect == null ? Rect.empty() : previousPageButtonRect;
        nextPageButtonRect = nextPageButtonRect == null ? Rect.empty() : nextPageButtonRect;
        accessoryGroupLayouts = accessoryGroupLayouts == null ? List.of() : List.copyOf(accessoryGroupLayouts);
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
        List<AccessoryGroupLayout> accessoryGroupLayouts = buildAccessoryGroupLayouts(
                playerColumnX,
                equipmentPanelRect.bottom() + SECTION_GAP,
                playerColumnWidth,
                accessoryGroups
        );
        Rect accessoriesPanelRect = resolveAccessoriesPanelRect(playerColumnX, equipmentPanelRect.bottom() + SECTION_GAP, playerColumnWidth, accessoryGroupLayouts);
        int bottomInventoryTop = accessoriesPanelRect.height() > 0
                ? accessoriesPanelRect.bottom() + SECTION_GAP
                : equipmentPanelRect.bottom() + SECTION_GAP;
        Rect bottomInventoryRect = new Rect(
                playerColumnX,
                bottomInventoryTop,
                bottomInventoryWidth,
                bottomInventoryHeight
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
                accessoriesPanelRect,
                bottomInventoryRect,
                databasePanelRect,
                databaseGridRect,
                databaseFooterRect,
                previousPageButtonRect,
                nextPageButtonRect,
                accessoryGroupLayouts,
                databaseColumns,
                databaseRows
        );
    }

    public int databaseSlotCount() {
        return this.databaseColumns * this.databaseRows;
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

    private static List<AccessoryGroupLayout> buildAccessoryGroupLayouts(
            int panelX,
            int panelY,
            int panelWidth,
            List<AccessorySlotGroup> accessoryGroups
    ) {
        if (accessoryGroups == null || accessoryGroups.isEmpty()) {
            return List.of();
        }
        List<AccessoryGroupLayout> layouts = new ArrayList<>();
        int contentX = panelX + ACCESSORY_PANEL_PADDING;
        int contentWidth = Math.max(SLOT_SIZE, panelWidth - ACCESSORY_PANEL_PADDING * 2);
        int columns = Math.max(1, contentWidth / SLOT_SIZE);
        int currentY = panelY + ACCESSORY_PANEL_PADDING;
        for (AccessorySlotGroup group : accessoryGroups) {
            int rows = Math.max(1, (group.slotCount() + columns - 1) / columns);
            Rect labelRect = new Rect(contentX, currentY, contentWidth, ACCESSORY_GROUP_LABEL_HEIGHT);
            Rect slotsRect = new Rect(
                    contentX,
                    labelRect.bottom() + ACCESSORY_GROUP_LABEL_GAP,
                    columns * SLOT_SIZE,
                    rows * SLOT_SIZE
            );
            layouts.add(new AccessoryGroupLayout(group, labelRect, slotsRect, columns));
            currentY = slotsRect.bottom() + ACCESSORY_GROUP_GAP;
        }
        return List.copyOf(layouts);
    }

    private static Rect resolveAccessoriesPanelRect(
            int panelX,
            int panelY,
            int panelWidth,
            List<AccessoryGroupLayout> layouts
    ) {
        if (layouts == null || layouts.isEmpty()) {
            return Rect.empty();
        }
        int panelBottom = layouts.getLast().slotsRect().bottom() + ACCESSORY_PANEL_PADDING;
        return new Rect(panelX, panelY, panelWidth, panelBottom - panelY);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record AccessoryGroupLayout(AccessorySlotGroup group, Rect labelRect, Rect slotsRect, int columns) {
        public AccessoryGroupLayout {
            group = group == null ? new AccessorySlotGroup("", "", -1, 0) : group;
            labelRect = labelRect == null ? Rect.empty() : labelRect;
            slotsRect = slotsRect == null ? Rect.empty() : slotsRect;
            columns = Math.max(1, columns);
        }

        public Rect slotBounds(int slotOffset) {
            return new Rect(
                    this.slotsRect.x() + slotOffset % this.columns * SLOT_SIZE,
                    this.slotsRect.y() + slotOffset / this.columns * SLOT_SIZE,
                    SLOT_SIZE,
                    SLOT_SIZE
            );
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
}
