package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.compat.AccessorySlotGroup;
import java.util.ArrayList;
import java.util.List;

final class AccessoryDrawerLayoutHelper {
    private static final int COMPACT_DRAWER_MIN_WIDTH = 128;

    private AccessoryDrawerLayoutHelper() {
    }

    static PersonalDatabaseLayout.Rect createAccessoriesPanelRect(
            PersonalDatabaseLayout.Rect frameRect,
            PersonalDatabaseLayout.Rect equipmentPanelRect,
            PersonalDatabaseLayout.Rect accessoryToggleRect,
            PersonalDatabaseLayout.Rect bottomInventoryRect,
            int playerColumnWidth,
            List<AccessorySlotGroup> accessoryGroups,
            boolean accessoriesExpanded,
            boolean compactOverlayFallback
    ) {
        if (!accessoriesExpanded || accessoryToggleRect.height() <= 0) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int totalSlotCount = PersonalDatabaseLayout.countAccessorySlots(accessoryGroups);
        if (totalSlotCount <= 0) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        boolean inlineToggle = accessoryToggleRect.y() <= equipmentPanelRect.y() + 1;
        boolean overlayMode = compactOverlayFallback || inlineToggle;
        int drawerWidth = resolveDrawerWidth(playerColumnWidth, accessoryGroups);
        int drawerX = equipmentPanelRect.x();
        int drawerY = overlayMode
                ? equipmentPanelRect.bottom() + PersonalDatabaseLayout.ACCESSORY_DRAWER_TOP_GAP
                : accessoryToggleRect.bottom() + PersonalDatabaseLayout.ACCESSORY_DRAWER_TOP_GAP;
        int contentWidth = Math.max(
                PersonalDatabaseLayout.SLOT_SIZE,
                drawerWidth - PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING * 2
        );
        int columns = Math.max(1, contentWidth / PersonalDatabaseLayout.SLOT_SIZE);
        int totalRows = calculateAccessoryContentRows(accessoryGroups, columns);
        int maxDrawerBottom = Math.max(
                drawerY + 1,
                overlayMode
                        ? frameRect.bottom() - PersonalDatabaseLayout.INNER_PADDING
                        : bottomInventoryRect.y() - PersonalDatabaseLayout.SECTION_GAP
        );
        int maxGridHeight = Math.max(
                0,
                maxDrawerBottom
                        - drawerY
                        - PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING * 2
                        - PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT
                        - PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_GAP
        );
        if (maxGridHeight < PersonalDatabaseLayout.SLOT_SIZE) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int visibleRows = Math.max(1, Math.min(totalRows, maxGridHeight / PersonalDatabaseLayout.SLOT_SIZE));
        int drawerHeight = PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING * 2
                + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT
                + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_GAP
                + visibleRows * PersonalDatabaseLayout.SLOT_SIZE;
        int drawerBottom = Math.min(maxDrawerBottom, drawerY + drawerHeight);
        return new PersonalDatabaseLayout.Rect(drawerX, drawerY, drawerWidth, Math.max(1, drawerBottom - drawerY));
    }

    private static int resolveDrawerWidth(int playerColumnWidth, List<AccessorySlotGroup> accessoryGroups) {
        int maxWidth = Math.max(1, playerColumnWidth);
        int minWidth = Math.min(maxWidth, COMPACT_DRAWER_MIN_WIDTH);
        int widestGroupSlots = 1;
        if (accessoryGroups != null) {
            for (AccessorySlotGroup group : accessoryGroups) {
                widestGroupSlots = Math.max(widestGroupSlots, Math.max(1, group.slotCount()));
            }
        }
        int preferredWidth = PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING * 2
                + widestGroupSlots * PersonalDatabaseLayout.SLOT_SIZE;
        return PersonalDatabaseLayout.clamp(preferredWidth, minWidth, maxWidth);
    }

    static AccessorySlotLayoutResult buildAccessorySlotLayouts(
            List<AccessorySlotGroup> accessoryGroups,
            PersonalDatabaseLayout.Rect accessoriesPanelRect,
            int requestedScrollRow
    ) {
        if (accessoryGroups == null || accessoryGroups.isEmpty()) {
            return AccessorySlotLayoutResult.empty();
        }

        int totalSlotCount = PersonalDatabaseLayout.countAccessorySlots(accessoryGroups);
        if (totalSlotCount <= 0) {
            return AccessorySlotLayoutResult.empty();
        }

        int columns = 1;
        int visibleRows = 0;
        int totalRows = 0;
        int scrollRow = 0;
        boolean panelVisible = accessoriesPanelRect.height() > 0;
        int gridLeft = PersonalDatabaseLayout.HIDDEN_SLOT_X;
        int gridTop = PersonalDatabaseLayout.HIDDEN_SLOT_Y;
        int contentWidth = PersonalDatabaseLayout.SLOT_SIZE;
        if (panelVisible) {
            contentWidth = Math.max(
                    PersonalDatabaseLayout.SLOT_SIZE,
                    accessoriesPanelRect.width() - PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING * 2
            );
            columns = Math.max(1, contentWidth / PersonalDatabaseLayout.SLOT_SIZE);
            totalRows = calculateAccessoryContentRows(accessoryGroups, columns);
            int gridHeight = Math.max(
                    0,
                    accessoriesPanelRect.height()
                            - PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING * 2
                            - PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT
                            - PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_GAP
            );
            visibleRows = Math.max(1, gridHeight / PersonalDatabaseLayout.SLOT_SIZE);
            scrollRow = PersonalDatabaseLayout.clamp(requestedScrollRow, 0, Math.max(0, totalRows - visibleRows));
            gridLeft = accessoriesPanelRect.x() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING;
            gridTop = accessoriesPanelRect.y()
                    + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING
                    + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT
                    + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_GAP;
        }

        List<PersonalDatabaseLayout.AccessorySlotLayout> layouts = new ArrayList<>(totalSlotCount);
        List<PersonalDatabaseLayout.AccessoryGroupLayout> groupLayouts = new ArrayList<>(accessoryGroups.size());
        int currentRow = 0;
        for (AccessorySlotGroup group : accessoryGroups) {
            int slotRows = Math.max(1, (Math.max(0, group.slotCount()) + columns - 1) / columns);
            int headerRow = currentRow;
            int slotStartRow = headerRow + 1;
            int bodyLeft = Integer.MAX_VALUE;
            int bodyTop = Integer.MAX_VALUE;
            int bodyRight = Integer.MIN_VALUE;
            int bodyBottom = Integer.MIN_VALUE;
            for (int slotOffset = 0; slotOffset < group.slotCount(); slotOffset++) {
                int absoluteRow = slotStartRow + slotOffset / columns;
                int row = absoluteRow - scrollRow;
                int column = slotOffset % columns;
                boolean visible = panelVisible && row >= 0 && row < visibleRows;
                PersonalDatabaseLayout.Rect slotRect = visible
                        ? new PersonalDatabaseLayout.Rect(
                                gridLeft + column * PersonalDatabaseLayout.SLOT_SIZE,
                                gridTop + row * PersonalDatabaseLayout.SLOT_SIZE,
                                PersonalDatabaseLayout.SLOT_SIZE,
                                PersonalDatabaseLayout.SLOT_SIZE
                        )
                        : PersonalDatabaseLayout.hiddenSlotRect();
                if (visible) {
                    bodyLeft = Math.min(bodyLeft, slotRect.x());
                    bodyTop = Math.min(bodyTop, slotRect.y());
                    bodyRight = Math.max(bodyRight, slotRect.right());
                    bodyBottom = Math.max(bodyBottom, slotRect.bottom());
                }
                layouts.add(new PersonalDatabaseLayout.AccessorySlotLayout(
                        group,
                        group.firstSlotIndex() + slotOffset,
                        slotOffset,
                        slotRect,
                        visible
                ));
            }
            PersonalDatabaseLayout.Rect headerRect = panelVisible && headerRow >= scrollRow && headerRow < scrollRow + visibleRows
                    ? new PersonalDatabaseLayout.Rect(
                            gridLeft,
                            gridTop + (headerRow - scrollRow) * PersonalDatabaseLayout.SLOT_SIZE,
                            contentWidth,
                            PersonalDatabaseLayout.SLOT_SIZE
                    )
                    : PersonalDatabaseLayout.Rect.empty();
            PersonalDatabaseLayout.Rect bodyRect = bodyRight > bodyLeft && bodyBottom > bodyTop
                    ? new PersonalDatabaseLayout.Rect(bodyLeft, bodyTop, bodyRight - bodyLeft, bodyBottom - bodyTop)
                    : PersonalDatabaseLayout.Rect.empty();
            groupLayouts.add(new PersonalDatabaseLayout.AccessoryGroupLayout(
                    group,
                    headerRect,
                    bodyRect,
                    headerRect.height() > 0 || bodyRect.height() > 0
            ));
            currentRow = slotStartRow + slotRows;
        }
        return new AccessorySlotLayoutResult(
                List.copyOf(layouts),
                List.copyOf(groupLayouts),
                columns,
                visibleRows,
                totalRows,
                scrollRow
        );
    }

    private static int calculateAccessoryContentRows(List<AccessorySlotGroup> accessoryGroups, int columns) {
        if (accessoryGroups == null || accessoryGroups.isEmpty()) {
            return 0;
        }
        int totalRows = 0;
        int resolvedColumns = Math.max(1, columns);
        for (AccessorySlotGroup group : accessoryGroups) {
            int slotRows = Math.max(1, (Math.max(0, group.slotCount()) + resolvedColumns - 1) / resolvedColumns);
            totalRows += 1 + slotRows;
        }
        return totalRows;
    }

    record AccessorySlotLayoutResult(
            List<PersonalDatabaseLayout.AccessorySlotLayout> slotLayouts,
            List<PersonalDatabaseLayout.AccessoryGroupLayout> groupLayouts,
            int columns,
            int visibleRows,
            int totalRows,
            int scrollRow
    ) {
        static AccessorySlotLayoutResult empty() {
            return new AccessorySlotLayoutResult(List.of(), List.of(), 0, 0, 0, 0);
        }
    }
}
