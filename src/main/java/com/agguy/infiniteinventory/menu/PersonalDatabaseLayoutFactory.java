package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.compat.AccessorySlotGroup;
import java.util.ArrayList;
import java.util.List;

final class PersonalDatabaseLayoutFactory {
    private PersonalDatabaseLayoutFactory() {
    }

    static PersonalDatabaseLayout create(
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
        int frameY = PersonalDatabaseLayout.FRAME_MARGIN + PersonalDatabaseLayout.TAB_HEIGHT - 1;
        PersonalDatabaseLayout.Rect frameRect = new PersonalDatabaseLayout.Rect(
                PersonalDatabaseLayout.FRAME_MARGIN,
                frameY,
                Math.max(1, screenWidth - PersonalDatabaseLayout.FRAME_MARGIN * 2),
                Math.max(1, screenHeight - PersonalDatabaseLayout.FRAME_MARGIN - frameY)
        );
        PersonalDatabaseLayout.Rect tabBarRect = new PersonalDatabaseLayout.Rect(
                frameRect.x() + PersonalDatabaseLayout.INNER_PADDING,
                frameRect.y() - PersonalDatabaseLayout.TAB_HEIGHT + 1,
                Math.max(1, frameRect.width() - PersonalDatabaseLayout.INNER_PADDING * 2),
                PersonalDatabaseLayout.TAB_HEIGHT
        );
        PersonalDatabaseLayout.Rect titleRect = new PersonalDatabaseLayout.Rect(
                frameRect.x() + PersonalDatabaseLayout.INNER_PADDING,
                frameRect.y() + PersonalDatabaseLayout.INNER_PADDING,
                Math.max(1, frameRect.width() - PersonalDatabaseLayout.INNER_PADDING * 2),
                PersonalDatabaseLayout.TITLE_HEIGHT
        );
        boolean compactTopBar = frameRect.width() <= 520;
        boolean compactAccessoryLayout = isCompactAccessoryLayout(screenWidth, screenHeight);
        int settingsButtonWidth = compactTopBar ? 48 : 56;
        int depositButtonWidth = compactTopBar ? 76 : PersonalDatabaseLayout.DEPOSIT_BUTTON_WIDTH;
        int defaultScopeButtonWidth = compactTopBar ? 76 : PersonalDatabaseLayout.SCOPE_BUTTON_WIDTH;
        int toolbarRight = frameRect.right() - PersonalDatabaseLayout.INNER_PADDING;
        int titleReservedWidth = compactTopBar ? 72 : Math.max(132, Math.min(172, frameRect.width() / 5));
        PersonalDatabaseLayout.Rect settingsButtonRect = new PersonalDatabaseLayout.Rect(
                toolbarRight - settingsButtonWidth,
                titleRect.y(),
                settingsButtonWidth,
                PersonalDatabaseLayout.CONTROL_HEIGHT
        );
        int scopeButtonWidth = Math.max(
                1,
                Math.min(
                        defaultScopeButtonWidth,
                        Math.max(
                                1,
                                settingsButtonRect.x()
                                        - titleRect.x()
                                        - titleReservedWidth
                                        - PersonalDatabaseLayout.TOOLBAR_GAP * 2
                                        - depositButtonWidth
                        )
                )
        );
        PersonalDatabaseLayout.Rect depositButtonRect = new PersonalDatabaseLayout.Rect(
                settingsButtonRect.x() - PersonalDatabaseLayout.TOOLBAR_GAP - depositButtonWidth,
                titleRect.y(),
                depositButtonWidth,
                PersonalDatabaseLayout.CONTROL_HEIGHT
        );
        PersonalDatabaseLayout.Rect personalScopeButtonRect = new PersonalDatabaseLayout.Rect(
                depositButtonRect.x() - PersonalDatabaseLayout.TOOLBAR_GAP - scopeButtonWidth,
                titleRect.y(),
                scopeButtonWidth,
                PersonalDatabaseLayout.CONTROL_HEIGHT
        );
        PersonalDatabaseLayout.Rect publicScopeButtonRect = PersonalDatabaseLayout.Rect.empty();
        int toolbarLeft = titleRect.x() + titleReservedWidth;
        int toolbarWidth = Math.max(
                0,
                personalScopeButtonRect.x() - PersonalDatabaseLayout.TOOLBAR_GAP - toolbarLeft
        );
        PersonalDatabaseLayout.Rect toolbarRect = new PersonalDatabaseLayout.Rect(
                toolbarLeft,
                titleRect.y(),
                toolbarWidth,
                PersonalDatabaseLayout.CONTROL_HEIGHT
        );
        PersonalDatabaseLayout.Rect searchFieldRect = PersonalDatabaseLayout.Rect.empty();
        PersonalDatabaseLayout.Rect sortButtonRect = PersonalDatabaseLayout.Rect.empty();
        PersonalDatabaseLayout.Rect previousPageButtonRect = PersonalDatabaseLayout.Rect.empty();
        PersonalDatabaseLayout.Rect pageLabelRect = PersonalDatabaseLayout.Rect.empty();
        PersonalDatabaseLayout.Rect nextPageButtonRect = PersonalDatabaseLayout.Rect.empty();

        int contentTop = titleRect.bottom() + PersonalDatabaseLayout.SECTION_GAP;
        int playerColumnX = frameRect.x() + PersonalDatabaseLayout.INNER_PADDING;
        int playerColumnWidth = Math.max(equipmentWidth, bottomInventoryWidth);
        PersonalDatabaseLayout.Rect equipmentPanelRect = new PersonalDatabaseLayout.Rect(playerColumnX, contentTop, equipmentWidth, equipmentHeight);
        boolean hasAccessorySlots = accessoryGroups != null && !accessoryGroups.isEmpty();
        boolean reserveAccessoryToggleRow = hasAccessorySlots
                && canReserveAccessoryToggleRow(frameRect, equipmentPanelRect, bottomInventoryHeight);
        PersonalDatabaseLayout.Rect accessoryToggleRect = !hasAccessorySlots
                ? PersonalDatabaseLayout.Rect.empty()
                : reserveAccessoryToggleRow
                        ? new PersonalDatabaseLayout.Rect(
                                playerColumnX,
                                equipmentPanelRect.bottom() + PersonalDatabaseLayout.SECTION_GAP,
                                playerColumnWidth,
                                PersonalDatabaseLayout.CONTROL_HEIGHT
                        )
                        : new PersonalDatabaseLayout.Rect(
                                equipmentPanelRect.right() - defaultScopeButtonWidth,
                                equipmentPanelRect.y(),
                                defaultScopeButtonWidth,
                                PersonalDatabaseLayout.CONTROL_HEIGHT
                        );
        int collapsedBottomInventoryY = reserveAccessoryToggleRow
                ? accessoryToggleRect.bottom() + PersonalDatabaseLayout.SECTION_GAP
                : equipmentPanelRect.bottom() + PersonalDatabaseLayout.SECTION_GAP;
        PersonalDatabaseLayout.Rect bottomInventoryRect = new PersonalDatabaseLayout.Rect(
                playerColumnX,
                collapsedBottomInventoryY,
                bottomInventoryWidth,
                bottomInventoryHeight
        );
        boolean inlineAccessoryOverlay = accessoryToggleRect.height() > 0
                && accessoryToggleRect.y() <= equipmentPanelRect.y() + 1;
        if (accessoriesExpanded
                && hasAccessorySlots
                && reserveAccessoryToggleRow
                && !compactAccessoryLayout
                && !inlineAccessoryOverlay) {
            bottomInventoryRect = new PersonalDatabaseLayout.Rect(
                    playerColumnX,
                    frameRect.bottom() - PersonalDatabaseLayout.INNER_PADDING - bottomInventoryHeight,
                    bottomInventoryWidth,
                    bottomInventoryHeight
            );
        }
        PersonalDatabaseLayout.Rect accessoriesPanelRect = AccessoryDrawerLayoutHelper.createAccessoriesPanelRect(
                frameRect,
                equipmentPanelRect,
                accessoryToggleRect,
                bottomInventoryRect,
                playerColumnWidth,
                accessoryGroups,
                accessoriesExpanded,
                compactAccessoryLayout
        );
        AccessoryDrawerLayoutHelper.AccessorySlotLayoutResult accessorySlotLayoutResult = AccessoryDrawerLayoutHelper.buildAccessorySlotLayouts(
                accessoryGroups,
                accessoriesPanelRect,
                accessoryScrollRow
        );

        int databasePanelX = playerColumnX + playerColumnWidth + PersonalDatabaseLayout.SECTION_GAP;
        int databasePanelWidth = Math.max(1, frameRect.right() - PersonalDatabaseLayout.INNER_PADDING - databasePanelX);
        PersonalDatabaseLayout.Rect databaseFooterRect = new PersonalDatabaseLayout.Rect(
                databasePanelX,
                frameRect.bottom() - PersonalDatabaseLayout.INNER_PADDING,
                databasePanelWidth,
                0
        );
        int databasePanelHeight = Math.max(1, databaseFooterRect.y() - contentTop);
        PersonalDatabaseLayout.Rect databasePanelRect = new PersonalDatabaseLayout.Rect(
                databasePanelX,
                contentTop,
                databasePanelWidth,
                databasePanelHeight
        );
        List<PersonalDatabaseLayout.DatabaseViewportLayout> databaseViewports = buildDatabaseViewports(
                databasePanelRect,
                visibleDatabasePanels,
                accessoriesPanelRect
        );
        PersonalDatabaseLayout.Rect databaseGridRect = databaseViewports.isEmpty()
                ? PersonalDatabaseLayout.Rect.empty()
                : databaseViewports.getFirst().gridRect();

        return new PersonalDatabaseLayout(
                frameRect,
                tabBarRect,
                titleRect,
                personalScopeButtonRect,
                publicScopeButtonRect,
                toolbarRect,
                searchFieldRect,
                settingsButtonRect,
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
                databaseViewports,
                accessorySlotLayoutResult.slotLayouts(),
                accessorySlotLayoutResult.groupLayouts(),
                accessorySlotLayoutResult.columns(),
                accessorySlotLayoutResult.visibleRows(),
                accessorySlotLayoutResult.totalRows(),
                accessorySlotLayoutResult.scrollRow()
        );
    }

    private static List<PersonalDatabaseLayout.DatabaseViewportLayout> buildDatabaseViewports(
            PersonalDatabaseLayout.Rect databasePanelRect,
            int visibleDatabasePanels,
            PersonalDatabaseLayout.Rect accessoriesPanelRect
    ) {
        int panelCount = PersonalDatabaseLayout.clamp(visibleDatabasePanels, 1, 4);
        List<PersonalDatabaseLayout.Rect> panelRects = switch (panelCount) {
            case 1 -> List.of(databasePanelRect);
            case 2 -> buildTwoPanelRects(databasePanelRect);
            case 3 -> buildThreePanelRects(databasePanelRect);
            case 4 -> buildFourPanelRects(databasePanelRect);
            default -> List.of(databasePanelRect);
        };
        List<PersonalDatabaseLayout.DatabaseViewportLayout> viewports = new ArrayList<>(panelRects.size());
        for (PersonalDatabaseLayout.Rect panelRect : panelRects) {
            int headerHeight = Math.min(
                    PersonalDatabaseLayout.VIEWPORT_HEADER_HEIGHT,
                    Math.max(
                            PersonalDatabaseLayout.CONTROL_HEIGHT,
                            panelRect.height()
                                    - PersonalDatabaseLayout.GRID_PADDING * 2
                                    - PersonalDatabaseLayout.DATABASE_SLOT_SIZE
                    )
            );
            PersonalDatabaseLayout.Rect headerRect = new PersonalDatabaseLayout.Rect(
                    panelRect.x() + PersonalDatabaseLayout.GRID_PADDING,
                    panelRect.y() + PersonalDatabaseLayout.GRID_PADDING / 2,
                    Math.max(1, panelRect.width() - PersonalDatabaseLayout.GRID_PADDING * 2),
                    headerHeight
            );
            int availableGridWidth = Math.max(
                    PersonalDatabaseLayout.DATABASE_SLOT_SIZE,
                    panelRect.width() - PersonalDatabaseLayout.GRID_PADDING * 2
            );
            int availableGridHeight = Math.max(
                    PersonalDatabaseLayout.DATABASE_SLOT_SIZE,
                    panelRect.height() - headerHeight - PersonalDatabaseLayout.GRID_PADDING * 2
            );
            int columns = PersonalDatabaseLayout.clamp(
                    availableGridWidth / PersonalDatabaseLayout.DATABASE_SLOT_SIZE,
                    1,
                    PersonalDatabaseLayout.MAX_COLUMNS
            );
            int rows = Math.max(1, availableGridHeight / PersonalDatabaseLayout.DATABASE_SLOT_SIZE);
            PersonalDatabaseLayout.Rect gridRect = new PersonalDatabaseLayout.Rect(
                    panelRect.x() + PersonalDatabaseLayout.GRID_PADDING,
                    headerRect.bottom() + 2,
                    columns * PersonalDatabaseLayout.DATABASE_SLOT_SIZE,
                    rows * PersonalDatabaseLayout.DATABASE_SLOT_SIZE
            );
            viewports.add(new PersonalDatabaseLayout.DatabaseViewportLayout(
                    panelRect,
                    headerRect,
                    gridRect,
                    columns,
                    rows,
                    accessoriesPanelRect
            ));
        }
        return List.copyOf(viewports);
    }

    private static List<PersonalDatabaseLayout.Rect> buildTwoPanelRects(PersonalDatabaseLayout.Rect databasePanelRect) {
        int width = Math.max(1, (databasePanelRect.width() - PersonalDatabaseLayout.VIEWPORT_GAP) / 2);
        PersonalDatabaseLayout.Rect left = new PersonalDatabaseLayout.Rect(
                databasePanelRect.x(),
                databasePanelRect.y(),
                width,
                databasePanelRect.height()
        );
        PersonalDatabaseLayout.Rect right = new PersonalDatabaseLayout.Rect(
                left.right() + PersonalDatabaseLayout.VIEWPORT_GAP,
                databasePanelRect.y(),
                databasePanelRect.right() - left.right() - PersonalDatabaseLayout.VIEWPORT_GAP,
                databasePanelRect.height()
        );
        return List.of(left, right);
    }

    private static List<PersonalDatabaseLayout.Rect> buildThreePanelRects(PersonalDatabaseLayout.Rect databasePanelRect) {
        int leftWidth = Math.max(1, (databasePanelRect.width() - PersonalDatabaseLayout.VIEWPORT_GAP) * 3 / 5);
        int rightWidth = Math.max(1, databasePanelRect.width() - leftWidth - PersonalDatabaseLayout.VIEWPORT_GAP);
        PersonalDatabaseLayout.Rect left = new PersonalDatabaseLayout.Rect(
                databasePanelRect.x(),
                databasePanelRect.y(),
                leftWidth,
                databasePanelRect.height()
        );
        int stackedHeight = Math.max(1, (databasePanelRect.height() - PersonalDatabaseLayout.VIEWPORT_GAP) / 2);
        PersonalDatabaseLayout.Rect topRight = new PersonalDatabaseLayout.Rect(
                left.right() + PersonalDatabaseLayout.VIEWPORT_GAP,
                databasePanelRect.y(),
                rightWidth,
                stackedHeight
        );
        PersonalDatabaseLayout.Rect bottomRight = new PersonalDatabaseLayout.Rect(
                topRight.x(),
                topRight.bottom() + PersonalDatabaseLayout.VIEWPORT_GAP,
                rightWidth,
                databasePanelRect.bottom() - topRight.bottom() - PersonalDatabaseLayout.VIEWPORT_GAP
        );
        return List.of(left, topRight, bottomRight);
    }

    private static List<PersonalDatabaseLayout.Rect> buildFourPanelRects(PersonalDatabaseLayout.Rect databasePanelRect) {
        int width = Math.max(1, (databasePanelRect.width() - PersonalDatabaseLayout.VIEWPORT_GAP) / 2);
        int height = Math.max(1, (databasePanelRect.height() - PersonalDatabaseLayout.VIEWPORT_GAP) / 2);
        PersonalDatabaseLayout.Rect topLeft = new PersonalDatabaseLayout.Rect(
                databasePanelRect.x(),
                databasePanelRect.y(),
                width,
                height
        );
        PersonalDatabaseLayout.Rect topRight = new PersonalDatabaseLayout.Rect(
                topLeft.right() + PersonalDatabaseLayout.VIEWPORT_GAP,
                databasePanelRect.y(),
                databasePanelRect.right() - topLeft.right() - PersonalDatabaseLayout.VIEWPORT_GAP,
                height
        );
        PersonalDatabaseLayout.Rect bottomLeft = new PersonalDatabaseLayout.Rect(
                databasePanelRect.x(),
                topLeft.bottom() + PersonalDatabaseLayout.VIEWPORT_GAP,
                width,
                databasePanelRect.bottom() - topLeft.bottom() - PersonalDatabaseLayout.VIEWPORT_GAP
        );
        PersonalDatabaseLayout.Rect bottomRight = new PersonalDatabaseLayout.Rect(
                topRight.x(),
                topRight.bottom() + PersonalDatabaseLayout.VIEWPORT_GAP,
                topRight.width(),
                databasePanelRect.bottom() - topRight.bottom() - PersonalDatabaseLayout.VIEWPORT_GAP
        );
        return List.of(topLeft, topRight, bottomLeft, bottomRight);
    }

    private static boolean canReserveAccessoryToggleRow(
            PersonalDatabaseLayout.Rect frameRect,
            PersonalDatabaseLayout.Rect equipmentPanelRect,
            int bottomInventoryHeight
    ) {
        int requiredBottom = equipmentPanelRect.bottom()
                + PersonalDatabaseLayout.SECTION_GAP
                + PersonalDatabaseLayout.CONTROL_HEIGHT
                + PersonalDatabaseLayout.SECTION_GAP
                + bottomInventoryHeight;
        return requiredBottom <= frameRect.bottom() - PersonalDatabaseLayout.INNER_PADDING;
    }

    private static boolean isCompactAccessoryLayout(int screenWidth, int screenHeight) {
        return screenWidth < 640 || screenHeight < 360;
    }

}
