package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;

final class PersonalDatabaseScreenManagementGeometry {
    private static final int COLUMN_GAP = 12;
    private static final int ADD_BUTTON_WIDTH = 54;
    private static final int ACTION_BUTTON_GAP = 8;
    private static final int ACTION_ROW_GAP = 6;
    private static final int EDITOR_SECTION_GAP = 18;
    private static final int FIELD_BLOCK_GAP = 22;

    private PersonalDatabaseScreenManagementGeometry() {
    }

    static PersonalDatabaseLayout.Rect tabManagementPanelRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int desiredHeight = editorTopOffset(screen)
                + 18
                + 20
                + FIELD_BLOCK_GAP
                + 20
                + EDITOR_SECTION_GAP
                + 3 * PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT
                + 2 * ACTION_ROW_GAP
                + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING;
        return PersonalDatabaseScreenGeometry.centeredOverlayRect(
                screen,
                PersonalDatabaseScreen.MANAGEMENT_PANEL_WIDTH,
                Math.max(PersonalDatabaseScreen.MANAGEMENT_PANEL_HEIGHT, desiredHeight)
        );
    }

    static PersonalDatabaseLayout.Rect managementScopeColumnRect(PersonalDatabaseScreen screen, boolean personalColumn) {
        PersonalDatabaseLayout.Rect panelRect = tabManagementPanelRect(screen);
        int width = Math.max(
                1,
                (panelRect.width() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2 - COLUMN_GAP) / 2
        );
        int x = personalColumn
                ? panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                : panelRect.right() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING - width;
        int y = panelRect.y()
                + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                + 8;
        int height = managementListBottom(screen) - y;
        return new PersonalDatabaseLayout.Rect(x, y, width, Math.max(1, height));
    }

    static PersonalDatabaseLayout.Rect managementScopeHeaderRect(PersonalDatabaseScreen screen, boolean personalColumn) {
        PersonalDatabaseLayout.Rect columnRect = managementScopeColumnRect(screen, personalColumn);
        return new PersonalDatabaseLayout.Rect(
                columnRect.x(),
                columnRect.y(),
                columnRect.width(),
                PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT
        );
    }

    static PersonalDatabaseLayout.Rect managementScopeAddButtonRect(PersonalDatabaseScreen screen, boolean personalColumn) {
        PersonalDatabaseLayout.Rect headerRect = managementScopeHeaderRect(screen, personalColumn);
        return new PersonalDatabaseLayout.Rect(
                headerRect.right() - ADD_BUTTON_WIDTH,
                headerRect.y(),
                ADD_BUTTON_WIDTH,
                headerRect.height()
        );
    }

    static PersonalDatabaseLayout.Rect managementListRowRect(PersonalDatabaseScreen screen, boolean personalColumn, int index) {
        PersonalDatabaseLayout.Rect columnRect = managementScopeColumnRect(screen, personalColumn);
        return new PersonalDatabaseLayout.Rect(
                columnRect.x(),
                columnRect.y() + PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT + 6 + index * PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT,
                columnRect.width(),
                PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT - 1
        );
    }

    static PersonalDatabaseLayout.Rect managementNameFieldRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = tabManagementPanelRect(screen);
        int x = panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING;
        int y = panelRect.y() + editorTopOffset(screen) + 18;
        return new PersonalDatabaseLayout.Rect(
                x,
                y,
                panelRect.width() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2,
                20
        );
    }

    static PersonalDatabaseLayout.Rect managementIconFieldRect(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect nameFieldRect = managementNameFieldRect(screen);
        return new PersonalDatabaseLayout.Rect(
                nameFieldRect.x(),
                nameFieldRect.bottom() + FIELD_BLOCK_GAP,
                nameFieldRect.width(),
                20
        );
    }

    static PersonalDatabaseLayout.Rect managementActionButtonRect(PersonalDatabaseScreen screen, int row, int column) {
        PersonalDatabaseLayout.Rect iconFieldRect = managementIconFieldRect(screen);
        int totalWidth = PersonalDatabaseScreen.MANAGEMENT_BUTTON_WIDTH * 2 + ACTION_BUTTON_GAP;
        int startX = iconFieldRect.x() + Math.max(0, (iconFieldRect.width() - totalWidth) / 2);
        int x = startX + column * (PersonalDatabaseScreen.MANAGEMENT_BUTTON_WIDTH + ACTION_BUTTON_GAP);
        int y = iconFieldRect.bottom() + EDITOR_SECTION_GAP + row * (PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT + ACTION_ROW_GAP);
        return new PersonalDatabaseLayout.Rect(
                x,
                y,
                PersonalDatabaseScreen.MANAGEMENT_BUTTON_WIDTH,
                PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT
        );
    }

    static int managementEditorTop(PersonalDatabaseScreen screen) {
        return tabManagementPanelRect(screen).y() + editorTopOffset(screen);
    }

    private static int managementListBottom(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect columnRect = managementScopeColumnRect(screen, true);
        return columnRect.y() + listHeight(screen);
    }

    private static int editorTopOffset(PersonalDatabaseScreen screen) {
        return topContentOffset() + listHeight(screen) + EDITOR_SECTION_GAP;
    }

    private static int listHeight(PersonalDatabaseScreen screen) {
        return PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT
                + 6
                + managementRowCount(screen) * PersonalDatabaseScreen.MANAGEMENT_ROW_HEIGHT;
    }

    private static int managementRowCount(PersonalDatabaseScreen screen) {
        return Math.max(
                PersonalDatabaseScreenCommonHelper.tabsForScope(screen, com.agguy.infiniteinventory.database.DatabaseScope.PERSONAL).size(),
                PersonalDatabaseScreenCommonHelper.tabsForScope(screen, com.agguy.infiniteinventory.database.DatabaseScope.PUBLIC).size()
        );
    }

    private static int topContentOffset() {
        return PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING
                + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                + 8;
    }
}
