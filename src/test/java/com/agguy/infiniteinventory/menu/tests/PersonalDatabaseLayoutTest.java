package com.agguy.infiniteinventory.menu.tests;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalDatabaseLayoutTest {
    private static final int INVENTORY_WIDTH = 176;
    private static final int INVENTORY_HEIGHT = 83;

    @Test
    void layoutShouldKeepDatabaseGridInsidePanelAtDesktopResolution() {
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1920, 1080, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT);

        assertTrue(layout.databaseGridRect().x() >= layout.databasePanelRect().x());
        assertTrue(layout.databaseGridRect().right() <= layout.databasePanelRect().right());
        assertTrue(layout.databaseGridRect().y() >= layout.databasePanelRect().y());
        assertTrue(layout.databaseGridRect().bottom() <= layout.databasePanelRect().bottom());
    }

    @Test
    void layoutShouldExposeMoreDatabaseSlotsOnLargerScreens() {
        PersonalDatabaseLayout compactLayout = PersonalDatabaseLayout.create(960, 540, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT);
        PersonalDatabaseLayout largeLayout = PersonalDatabaseLayout.create(1920, 1080, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT);

        assertTrue(largeLayout.databaseSlotCount() >= compactLayout.databaseSlotCount());
        assertTrue(largeLayout.databaseSlotCount() > 112);
    }

    @Test
    void tabBoundsShouldStayInsideTabBar() {
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT);
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
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT);

        assertTrue(layout.equipmentPanelRect().x() == layout.bottomInventoryRect().x());
        assertTrue(layout.databasePanelRect().x() >= layout.equipmentPanelRect().right() + PersonalDatabaseLayout.SECTION_GAP);
        assertTrue(layout.databaseFooterRect().x() == layout.databasePanelRect().x());
    }

    @Test
    void gridShouldPinToDatabasePanelOriginInsteadOfCentering() {
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT);

        assertTrue(layout.databaseGridRect().x() == layout.databasePanelRect().x() + PersonalDatabaseLayout.GRID_PADDING);
        assertTrue(layout.databaseGridRect().y() == layout.databasePanelRect().y() + PersonalDatabaseLayout.GRID_PADDING);
    }

    @Test
    void pageControlsShouldStayInsideToolbar() {
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT);

        assertTrue(layout.previousPageButtonRect().x() >= layout.toolbarRect().x());
        assertTrue(layout.pageLabelRect().x() >= layout.toolbarRect().x());
        assertTrue(layout.nextPageButtonRect().right() <= layout.toolbarRect().right());
        assertTrue(layout.previousPageButtonRect().y() == layout.toolbarRect().y());
    }

    @Test
    void scopeButtonsShouldStayInsideFrameTitleArea() {
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(1280, 720, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT);

        assertTrue(layout.personalScopeButtonRect().x() >= layout.frameRect().x());
        assertTrue(layout.personalScopeButtonRect().right() <= layout.frameRect().right());
        assertTrue(layout.publicScopeButtonRect().x() == layout.personalScopeButtonRect().right());
        assertTrue(layout.publicScopeButtonRect().right() <= layout.frameRect().right());
        assertTrue(layout.personalScopeButtonRect().y() == layout.titleRect().y());
    }
}
