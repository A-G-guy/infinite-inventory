package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.compat.AccessorySlotGroup;
import java.util.List;

public record PersonalDatabaseLayout(
        Rect frameRect,
        Rect tabBarRect,
        Rect titleRect,
        Rect personalScopeButtonRect,
        Rect publicScopeButtonRect,
        Rect toolbarRect,
        Rect searchFieldRect,
        Rect settingsButtonRect,
        Rect sortButtonRect,
        Rect depositButtonRect,
        Rect depositExistingButtonRect,
        Rect viewSelectorButtonRect,
        Rect statisticsButtonRect,
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
        List<DatabaseViewportLayout> databaseViewportLayouts,
        List<AccessorySlotLayout> accessorySlotLayouts,
        List<AccessoryGroupLayout> accessoryGroupLayouts,
        int accessoryColumns,
        int accessoryVisibleRows,
        int accessoryTotalRows,
        int accessoryScrollRow
) {
    public static final int FRAME_MARGIN = 12;
    public static final int INNER_PADDING = 10;
    public static final int TAB_HEIGHT = 24;
    public static final int TAB_GAP = 4;
    public static final int CONTROL_HEIGHT = 20;
    public static final int TOOLBAR_GAP = 4;
    public static final int SECTION_GAP = 12;
    public static final int EQUIPMENT_BOTTOM_GAP = 4;
    public static final int SLOT_SIZE = 18;
    public static final int DATABASE_SLOT_SIZE = 20;
    public static final int GRID_PADDING = 10;
    public static final int FOOTER_HEIGHT = 20;
    public static final int PAGE_BUTTON_WIDTH = 20;
    public static final int PAGE_BUTTON_GAP = 4;
    public static final int PAGE_LABEL_WIDTH = 80;
    public static final int PAGE_CONTROLS_WIDTH = PAGE_BUTTON_WIDTH * 2 + PAGE_BUTTON_GAP * 2 + PAGE_LABEL_WIDTH;
    public static final int MAX_COLUMNS = 40;
    public static final int ACCESSORY_DRAWER_PADDING = 8;
    public static final int ACCESSORY_DRAWER_TITLE_HEIGHT = 12;
    public static final int ACCESSORY_DRAWER_TITLE_GAP = 6;
    public static final int VIEWPORT_HEADER_HEIGHT = 56;
    public static final int VIEWPORT_GAP = 8;
    static final int TITLE_HEIGHT = CONTROL_HEIGHT;
    static final int TITLE_GAP = 6;
    static final int SEARCH_MIN_WIDTH = 120;
    static final int ADVANCED_SEARCH_BUTTON_WIDTH = 56;
    static final int ENHANCEMENT_BUTTON_WIDTH = 56;
    static final int VIEW_SELECTOR_BUTTON_WIDTH = 56;
    static final int TAB_MANAGEMENT_BUTTON_WIDTH = 56;
    static final int LOG_BUTTON_WIDTH = 56;
    static final int SORT_BUTTON_WIDTH = 112;
    static final int DEPOSIT_BUTTON_WIDTH = 84;
    static final int SCOPE_BUTTON_WIDTH = 76;
    static final int ACCESSORY_DRAWER_MIN_WIDTH = 212;
    static final int ACCESSORY_DRAWER_TOP_GAP = 4;
    static final int HIDDEN_SLOT_X = -20_000;
    static final int HIDDEN_SLOT_Y = -20_000;

    public PersonalDatabaseLayout {
        frameRect = frameRect == null ? Rect.empty() : frameRect;
        tabBarRect = tabBarRect == null ? Rect.empty() : tabBarRect;
        titleRect = titleRect == null ? Rect.empty() : titleRect;
        personalScopeButtonRect = personalScopeButtonRect == null ? Rect.empty() : personalScopeButtonRect;
        publicScopeButtonRect = publicScopeButtonRect == null ? Rect.empty() : publicScopeButtonRect;
        toolbarRect = toolbarRect == null ? Rect.empty() : toolbarRect;
        searchFieldRect = searchFieldRect == null ? Rect.empty() : searchFieldRect;
        settingsButtonRect = settingsButtonRect == null ? Rect.empty() : settingsButtonRect;
        sortButtonRect = sortButtonRect == null ? Rect.empty() : sortButtonRect;
        depositButtonRect = depositButtonRect == null ? Rect.empty() : depositButtonRect;
        depositExistingButtonRect = depositExistingButtonRect == null ? Rect.empty() : depositExistingButtonRect;
        viewSelectorButtonRect = viewSelectorButtonRect == null ? Rect.empty() : viewSelectorButtonRect;
        statisticsButtonRect = statisticsButtonRect == null ? Rect.empty() : statisticsButtonRect;
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
        databaseViewportLayouts = databaseViewportLayouts == null ? List.of() : List.copyOf(databaseViewportLayouts);
        accessorySlotLayouts = accessorySlotLayouts == null ? List.of() : List.copyOf(accessorySlotLayouts);
        accessoryGroupLayouts = accessoryGroupLayouts == null ? List.of() : List.copyOf(accessoryGroupLayouts);
        accessoryColumns = Math.max(0, accessoryColumns);
        accessoryVisibleRows = Math.max(0, accessoryVisibleRows);
        accessoryTotalRows = Math.max(0, accessoryTotalRows);
        accessoryScrollRow = Math.max(0, accessoryScrollRow);
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
        return PersonalDatabaseLayoutFactory.create(
                screenWidth,
                screenHeight,
                equipmentWidth,
                equipmentHeight,
                bottomInventoryWidth,
                bottomInventoryHeight,
                accessoryGroups,
                1,
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
        return PersonalDatabaseLayoutFactory.create(
                screenWidth,
                screenHeight,
                equipmentWidth,
                equipmentHeight,
                bottomInventoryWidth,
                bottomInventoryHeight,
                accessoryGroups,
                1,
                accessoriesExpanded,
                accessoryScrollRow
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
            int visibleDatabasePanels,
            boolean accessoriesExpanded,
            int accessoryScrollRow
    ) {
        return PersonalDatabaseLayoutFactory.create(
                screenWidth,
                screenHeight,
                equipmentWidth,
                equipmentHeight,
                bottomInventoryWidth,
                bottomInventoryHeight,
                accessoryGroups,
                visibleDatabasePanels,
                accessoriesExpanded,
                accessoryScrollRow
        );
    }

    public int databaseViewportCount() {
        return this.databaseViewportLayouts.size();
    }

    public DatabaseViewportLayout databaseViewportLayout(int viewportIndex) {
        if (viewportIndex < 0 || viewportIndex >= this.databaseViewportLayouts.size()) {
            return DatabaseViewportLayout.empty();
        }
        return this.databaseViewportLayouts.get(viewportIndex);
    }

    public int databaseSlotCount(int viewportIndex) {
        DatabaseViewportLayout viewportLayout = this.databaseViewportLayout(viewportIndex);
        return viewportLayout.columns() * viewportLayout.rows();
    }

    public int visibleDatabaseSlotCount(int viewportIndex) {
        int visibleCount = 0;
        for (int slotIndex = 0; slotIndex < this.databaseSlotCount(viewportIndex); slotIndex++) {
            if (this.isDatabaseSlotVisible(viewportIndex, slotIndex)) {
                visibleCount++;
            }
        }
        return visibleCount;
    }

    public int visibleDatabaseSlotCount() {
        return this.visibleDatabaseSlotCount(0);
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

    public Rect databaseSlotBounds(int viewportIndex, int slotIndex) {
        DatabaseViewportLayout viewportLayout = this.databaseViewportLayout(viewportIndex);
        int column = slotIndex % viewportLayout.columns();
        int row = slotIndex / viewportLayout.columns();
        return new Rect(
                viewportLayout.gridRect().x() + column * DATABASE_SLOT_SIZE,
                viewportLayout.gridRect().y() + row * DATABASE_SLOT_SIZE,
                DATABASE_SLOT_SIZE,
                DATABASE_SLOT_SIZE
        );
    }

    public Rect visibleDatabaseSlotBounds(int viewportIndex, int visibleSlotIndex) {
        if (visibleSlotIndex < 0) {
            return Rect.empty();
        }
        int resolvedVisibleIndex = 0;
        for (int slotIndex = 0; slotIndex < this.databaseSlotCount(viewportIndex); slotIndex++) {
            if (!this.isDatabaseSlotVisible(viewportIndex, slotIndex)) {
                continue;
            }
            if (resolvedVisibleIndex == visibleSlotIndex) {
                return this.databaseSlotBounds(viewportIndex, slotIndex);
            }
            resolvedVisibleIndex++;
        }
        return Rect.empty();
    }

    public Rect visibleDatabaseSlotBounds(int visibleSlotIndex) {
        return this.visibleDatabaseSlotBounds(0, visibleSlotIndex);
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

    static Rect hiddenSlotRect() {
        return new Rect(HIDDEN_SLOT_X, HIDDEN_SLOT_Y, SLOT_SIZE, SLOT_SIZE);
    }

    static int countAccessorySlots(List<AccessorySlotGroup> accessoryGroups) {
        if (accessoryGroups == null || accessoryGroups.isEmpty()) {
            return 0;
        }
        int totalSlotCount = 0;
        for (AccessorySlotGroup accessoryGroup : accessoryGroups) {
            totalSlotCount += Math.max(0, accessoryGroup.slotCount());
        }
        return totalSlotCount;
    }

    private boolean isDatabaseSlotVisible(int viewportIndex, int slotIndex) {
        DatabaseViewportLayout viewportLayout = this.databaseViewportLayout(viewportIndex);
        if (viewportLayout.isEmpty()) {
            return false;
        }
        return !viewportLayout.accessoriesPanelRect().intersects(this.databaseSlotBounds(viewportIndex, slotIndex));
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record DatabaseViewportLayout(Rect panelRect, Rect headerRect, Rect gridRect, int columns, int rows, Rect accessoriesPanelRect) {
        public DatabaseViewportLayout {
            panelRect = panelRect == null ? Rect.empty() : panelRect;
            headerRect = headerRect == null ? Rect.empty() : headerRect;
            gridRect = gridRect == null ? Rect.empty() : gridRect;
            columns = Math.max(1, columns);
            rows = Math.max(1, rows);
            accessoriesPanelRect = accessoriesPanelRect == null ? Rect.empty() : accessoriesPanelRect;
        }

        public static DatabaseViewportLayout empty() {
            return new DatabaseViewportLayout(Rect.empty(), Rect.empty(), Rect.empty(), 1, 1, Rect.empty());
        }

        public boolean isEmpty() {
            return this.panelRect.height() <= 0 || this.panelRect.width() <= 0;
        }
    }

    public record AccessorySlotLayout(AccessorySlotGroup group, int slotIndex, int slotOffset, Rect slotRect, boolean visible) {
        public AccessorySlotLayout {
            group = group == null ? new AccessorySlotGroup("", "", -1, 0) : group;
            slotRect = slotRect == null ? hiddenSlotRect() : slotRect;
        }
    }

    public record AccessoryGroupLayout(AccessorySlotGroup group, Rect headerRect, Rect bodyRect, boolean visible) {
        public AccessoryGroupLayout {
            group = group == null ? new AccessorySlotGroup("", "", -1, 0) : group;
            headerRect = headerRect == null ? Rect.empty() : headerRect;
            bodyRect = bodyRect == null ? Rect.empty() : bodyRect;
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
