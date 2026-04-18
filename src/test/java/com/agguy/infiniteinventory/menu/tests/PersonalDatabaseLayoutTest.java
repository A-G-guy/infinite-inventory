package com.agguy.infiniteinventory.menu.tests;

import com.agguy.infiniteinventory.compat.AccessorySlotGroup;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalDatabaseLayoutTest {
    private static final int INVENTORY_WIDTH = 176;
    private static final int INVENTORY_HEIGHT = 83;

    @Test
    void layoutShouldKeepDatabaseGridInsidePanelAtDesktopResolution() {
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1920, 1080, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, List.of());

        assertTrue(layout.databaseGridRect().x() >= layout.databasePanelRect().x());
        assertTrue(layout.databaseGridRect().right() <= layout.databasePanelRect().right());
        assertTrue(layout.databaseGridRect().y() >= layout.databasePanelRect().y());
        assertTrue(layout.databaseGridRect().bottom() <= layout.databasePanelRect().bottom());
    }

    @Test
    void layoutShouldExposeMoreDatabaseSlotsOnLargerScreens() {
        PersonalDatabaseLayout compactLayout = PersonalDatabaseLayout.create(960, 540, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, List.of());
        PersonalDatabaseLayout largeLayout = PersonalDatabaseLayout.create(1920, 1080, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, List.of());

        assertTrue(largeLayout.databaseSlotCount(0) >= compactLayout.databaseSlotCount(0));
        assertTrue(largeLayout.databaseSlotCount(0) > 112);
    }

    @Test
    void tabBoundsShouldStayInsideTabBar() {
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, List.of());
        int previousRight = layout.tabBarRect().x();
        for (int index = 0; index < 7; index++) {
            PersonalDatabaseLayout.Rect tabRect = layout.tabBounds(index, 7);
            assertTrue(tabRect.x() >= layout.tabBarRect().x());
            assertTrue(tabRect.right() <= layout.tabBarRect().right());
            assertTrue(tabRect.x() >= previousRight);
            previousRight = tabRect.right();
        }
    }

    @Test
    void layoutShouldDockPlayerInventoryAndDatabaseIntoSeparateColumns() {
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, List.of());

        assertTrue(layout.equipmentPanelRect().x() == layout.bottomInventoryRect().x());
        assertTrue(layout.databasePanelRect().x() >= layout.equipmentPanelRect().right() + PersonalDatabaseLayout.SECTION_GAP);
        assertTrue(layout.databaseFooterRect().x() == layout.databasePanelRect().x());
    }

    @Test
    void gridShouldPinToDatabasePanelOriginInsteadOfCentering() {
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, List.of());
        PersonalDatabaseLayout.DatabaseViewportLayout viewportLayout = layout.databaseViewportLayout(0);

        assertTrue(layout.databaseGridRect().x() == viewportLayout.panelRect().x() + PersonalDatabaseLayout.GRID_PADDING);
        assertTrue(layout.databaseGridRect().y() == viewportLayout.headerRect().bottom() + 2);
    }

    @Test
    void titleToolbarShouldKeepSummaryAndDepositControlsAligned() {
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, List.of());

        assertTrue(layout.toolbarRect().x() >= layout.titleRect().x());
        assertTrue(layout.toolbarRect().right() <= layout.personalScopeButtonRect().x());
        assertTrue(layout.toolbarRect().y() == layout.titleRect().y());
        assertTrue(layout.depositButtonRect().y() == layout.titleRect().y());
    }

    @Test
    void scopeToggleShouldMoveOutOfTopTabBar() {
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, List.of());

        assertTrue(layout.personalScopeButtonRect().width() > 0);
        assertEquals(0, layout.publicScopeButtonRect().width());
        assertTrue(layout.personalScopeButtonRect().y() == layout.titleRect().y());
        assertTrue(layout.personalScopeButtonRect().x() >= layout.toolbarRect().right());
        assertTrue(!layout.personalScopeButtonRect().intersects(layout.tabBarRect()));
    }

    @Test
    void titleAreaShouldKeepViewAndManageButtonsInsideFrame() {
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, List.of());

        assertTrue(layout.viewSelectorButtonRect().x() >= layout.frameRect().x());
        assertTrue(layout.viewSelectorButtonRect().right() <= layout.frameRect().right());
        assertTrue(layout.tabManagementButtonRect().x() >= layout.viewSelectorButtonRect().right());
        assertTrue(layout.tabManagementButtonRect().right() <= layout.frameRect().right());
        assertTrue(layout.viewSelectorButtonRect().y() == layout.titleRect().y());
    }

    @Test
    void collapsedAccessoriesShouldReserveOnlyToggleRow() {
        List<AccessorySlotGroup> accessoryGroups = List.of(
                new AccessorySlotGroup("back", "accessories.slot.back", 46, 1),
                new AccessorySlotGroup("ring", "accessories.slot.ring", 47, 2)
        );
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, accessoryGroups);

        assertTrue(layout.accessoryToggleRect().height() == PersonalDatabaseLayout.CONTROL_HEIGHT);
        assertTrue(layout.accessoriesPanelRect().height() == 0);
        assertTrue(layout.bottomInventoryRect().y() == layout.accessoryToggleRect().bottom() + PersonalDatabaseLayout.SECTION_GAP);
        assertTrue(layout.accessorySlotLayouts().stream().noneMatch(PersonalDatabaseLayout.AccessorySlotLayout::visible));
    }

    @Test
    void expandedAccessoriesPanelShouldOverlayDatabaseAreaWithoutMovingBottomInventory() {
        List<AccessorySlotGroup> accessoryGroups = List.of(
                new AccessorySlotGroup("ring", "accessories.slot.ring", 46, 10)
        );
        PersonalDatabaseLayout collapsedLayout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, accessoryGroups);
        PersonalDatabaseLayout expandedLayout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, accessoryGroups, true, 0);

        assertTrue(expandedLayout.accessoriesPanelRect().height() > 0);
        assertTrue(expandedLayout.accessoriesPanelRect().x() >= collapsedLayout.bottomInventoryRect().right() + PersonalDatabaseLayout.SECTION_GAP);
        assertTrue(expandedLayout.bottomInventoryRect().y() == collapsedLayout.bottomInventoryRect().y());
        assertTrue(expandedLayout.accessoriesPanelRect().intersects(expandedLayout.databasePanelRect()));
    }

    @Test
    void expandedAccessoriesPanelShouldShrinkToContentHeight() {
        List<AccessorySlotGroup> accessoryGroups = List.of(
                new AccessorySlotGroup("back", "accessories.slot.back", 46, 3)
        );
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, accessoryGroups, true, 0);

        int expectedHeight = PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING * 2
                + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT
                + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_GAP
                + PersonalDatabaseLayout.SLOT_SIZE * 2;
        assertEquals(expectedHeight, layout.accessoriesPanelRect().height());
    }

    @Test
    void expandedAccessoriesPanelShouldOnlyCountVisibleDatabaseSlotsForPaging() {
        List<AccessorySlotGroup> accessoryGroups = List.of(
                new AccessorySlotGroup("ring", "accessories.slot.ring", 46, 24)
        );
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, accessoryGroups, true, 0);

        assertTrue(layout.visibleDatabaseSlotCount() > 0);
        assertTrue(layout.visibleDatabaseSlotCount() < layout.databaseSlotCount(0));
        for (int slotIndex = 0; slotIndex < layout.visibleDatabaseSlotCount(); slotIndex++) {
            assertTrue(!layout.accessoriesPanelRect().intersects(layout.visibleDatabaseSlotBounds(slotIndex)));
        }
    }

    @Test
    void visibleAccessorySlotsShouldStayInsideExpandedAccessoriesPanel() {
        List<AccessorySlotGroup> accessoryGroups = List.of(
                new AccessorySlotGroup("ring", "accessories.slot.ring", 46, 24)
        );
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, accessoryGroups, true, 0);

        assertTrue(layout.accessoryVisibleSlotCount() > 0);
        for (PersonalDatabaseLayout.AccessorySlotLayout slotLayout : layout.accessorySlotLayouts()) {
            if (!slotLayout.visible()) {
                continue;
            }
            PersonalDatabaseLayout.Rect slotRect = slotLayout.slotRect();
            assertTrue(slotRect.x() >= layout.accessoriesPanelRect().x());
            assertTrue(slotRect.right() <= layout.accessoriesPanelRect().right());
            assertTrue(slotRect.y() >= layout.accessoriesPanelRect().y());
            assertTrue(slotRect.bottom() <= layout.accessoriesPanelRect().bottom());
        }
    }

    @Test
    void expandedAccessoriesPanelShouldExposeVisibleGroupLayouts() {
        List<AccessorySlotGroup> accessoryGroups = List.of(
                new AccessorySlotGroup("back", "accessories.slot.back", 46, 1),
                new AccessorySlotGroup("ring", "accessories.slot.ring", 47, 2)
        );
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, accessoryGroups, true, 0);

        assertEquals(2, layout.accessoryGroupLayouts().size());
        assertTrue(layout.accessoryGroupLayouts().stream().allMatch(PersonalDatabaseLayout.AccessoryGroupLayout::visible));
        assertTrue(layout.accessoryGroupLayouts().getFirst().headerRect().height() == PersonalDatabaseLayout.SLOT_SIZE);
        assertTrue(layout.accessoryGroupLayouts().get(1).headerRect().y() >= layout.accessoryGroupLayouts().getFirst().bodyRect().bottom());
    }

    @Test
    void scrollingAccessoriesShouldClampAndRevealLaterRows() {
        List<AccessorySlotGroup> accessoryGroups = List.of(
                new AccessorySlotGroup("ring", "accessories.slot.ring", 46, 260)
        );
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT, accessoryGroups, true, 1);

        assertTrue(layout.accessoryMaxScrollRow() > 0);
        assertTrue(layout.accessoryScrollRow() == 1);
        assertTrue(layout.accessoryGroupLayouts().getFirst().headerRect().height() == 0);
        assertTrue(layout.accessorySlotLayouts().getFirst().visible());
        assertTrue(layout.accessorySlotLayouts().stream().anyMatch(slotLayout -> slotLayout.visible() && slotLayout.slotOffset() >= layout.accessoryColumns()));
    }

    @Test
    void compactLayoutShouldKeepBottomInventoryInsideFrameWhenAccessoriesExist() {
        List<AccessorySlotGroup> accessoryGroups = List.of(
                new AccessorySlotGroup("ring", "accessories.slot.ring", 46, 4)
        );
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(
                480,
                270,
                INVENTORY_WIDTH,
                INVENTORY_HEIGHT,
                INVENTORY_WIDTH,
                INVENTORY_HEIGHT,
                accessoryGroups
        );

        assertTrue(layout.bottomInventoryRect().bottom() <= layout.frameRect().bottom());
        assertTrue(layout.databaseFooterRect().bottom() <= layout.frameRect().bottom());
        assertTrue(layout.accessoryToggleRect().bottom() <= layout.bottomInventoryRect().y());
    }
}
