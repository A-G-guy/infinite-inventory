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
        assertTrue(layout.databaseGridRect().bottom() <= layout.databaseFooterRect().y());
    }

    @Test
    void layoutShouldExposeMoreDatabaseSlotsOnLargerScreens() {
        PersonalDatabaseLayout compactLayout = PersonalDatabaseLayout.create(960, 540, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT);
        PersonalDatabaseLayout largeLayout = PersonalDatabaseLayout.create(1920, 1080, INVENTORY_WIDTH, INVENTORY_HEIGHT, INVENTORY_WIDTH, INVENTORY_HEIGHT);

        assertTrue(largeLayout.databaseSlotCount() >= compactLayout.databaseSlotCount());
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
}
