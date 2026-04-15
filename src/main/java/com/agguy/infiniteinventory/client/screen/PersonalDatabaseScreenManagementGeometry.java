package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;

final class PersonalDatabaseScreenManagementGeometry {
    private PersonalDatabaseScreenManagementGeometry() {
    }

    static PersonalDatabaseLayout.Rect tabManagementPanelRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int desiredHeight = Math.max(
                PersonalDatabaseScreen.MANAGEMENT_PANEL_HEIGHT,
                56 + PersonalDatabaseScreenCommonHelper.currentTabs(screen).size() * PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT
        );
        return PersonalDatabaseScreenGeometry.centeredOverlayRect(
                screen,
                PersonalDatabaseScreen.MANAGEMENT_PANEL_WIDTH,
                desiredHeight
        );
    }

    static PersonalDatabaseLayout.Rect managementListRowRect(PersonalDatabaseScreen screen, int index) {
        PersonalDatabaseLayout.Rect panelRect = tabManagementPanelRect(screen);
        int rowY = panelRect.y()
                + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                + 8
                + index * PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT;
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                rowY,
                PersonalDatabaseScreen.MANAGEMENT_LIST_WIDTH,
                PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT - 1
        );
    }

    static PersonalDatabaseLayout.Rect managementNameFieldRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = tabManagementPanelRect(screen);
        int x = panelRect.x()
                + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                + PersonalDatabaseScreen.MANAGEMENT_LIST_WIDTH
                + 14;
        return new PersonalDatabaseLayout.Rect(
                x,
                panelRect.y() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT + 18,
                Math.max(1, panelRect.right() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING - x),
                20
        );
    }

    static PersonalDatabaseLayout.Rect managementIconFieldRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect nameFieldRect = managementNameFieldRect(screen);
        return new PersonalDatabaseLayout.Rect(nameFieldRect.x(), nameFieldRect.bottom() + 22, nameFieldRect.width(), 20);
    }

    static PersonalDatabaseLayout.Rect managementActionButtonRect(PersonalDatabaseScreen screen, int row, int column) {
        PersonalDatabaseLayout.Rect iconFieldRect = managementIconFieldRect(screen);
        int x = iconFieldRect.x() + column * (PersonalDatabaseScreen.MANAGEMENT_BUTTON_WIDTH + 8);
        int y = iconFieldRect.bottom() + 18 + row * (PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT + 6);
        return new PersonalDatabaseLayout.Rect(
                x,
                y,
                PersonalDatabaseScreen.MANAGEMENT_BUTTON_WIDTH,
                PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT
        );
    }
}
