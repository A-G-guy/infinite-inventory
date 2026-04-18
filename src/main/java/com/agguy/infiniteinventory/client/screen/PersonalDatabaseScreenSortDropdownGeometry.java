package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseSortDirection;
import com.agguy.infiniteinventory.database.DatabaseSortMethod;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class PersonalDatabaseScreenSortDropdownGeometry {
    private PersonalDatabaseScreenSortDropdownGeometry() {
    }

    @Nullable
    static PersonalDatabaseLayout.Rect dropdownRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null || !screen.sortDropdownExpanded || screen.activeSortPanelIndex < 0) {
            return null;
        }
        PersonalDatabaseLayout.Rect sortButtonRect = PersonalDatabaseScreenGeometry.panelSortButtonRect(
                screen,
                screen.activeSortPanelIndex
        );
        int maxWidth = Math.max(1, screen.layout.frameRect().width() - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN * 2);
        int width = Math.min(maxWidth, Math.max(sortButtonRect.width(), preferredDropdownWidth(screen)));
        int height = dropdownHeight();
        int minX = screen.layout.frameRect().x() + PersonalDatabaseScreen.CONTEXT_MENU_MARGIN;
        int maxX = Math.max(minX, screen.layout.frameRect().right() - width - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN);
        int x = Mth.clamp(sortButtonRect.x(), minX, maxX);
        int y = anchoredPopupY(screen.layout.frameRect(), sortButtonRect, height);
        return new PersonalDatabaseLayout.Rect(x, y, width, height);
    }

    static PersonalDatabaseLayout.Rect directionButtonRect(PersonalDatabaseScreen screen, DatabaseSortDirection direction) {
        PersonalDatabaseLayout.Rect dropdownRect = dropdownRect(screen);
        if (dropdownRect == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int innerX = dropdownRect.x() + 2;
        int innerWidth = dropdownRect.width() - 4;
        int leftWidth = Math.max(1, (innerWidth - PersonalDatabaseScreen.SORT_DIRECTION_BUTTON_GAP) / 2);
        int rightX = innerX + leftWidth + PersonalDatabaseScreen.SORT_DIRECTION_BUTTON_GAP;
        int rightWidth = Math.max(1, innerWidth - leftWidth - PersonalDatabaseScreen.SORT_DIRECTION_BUTTON_GAP);
        if (direction == DatabaseSortDirection.ASC) {
            return new PersonalDatabaseLayout.Rect(
                    innerX,
                    dropdownRect.y() + 2,
                    leftWidth,
                    PersonalDatabaseScreen.DROPDOWN_ROW_HEIGHT - 1
            );
        }
        return new PersonalDatabaseLayout.Rect(
                rightX,
                dropdownRect.y() + 2,
                rightWidth,
                PersonalDatabaseScreen.DROPDOWN_ROW_HEIGHT - 1
        );
    }

    static PersonalDatabaseLayout.Rect methodRowRect(PersonalDatabaseScreen screen, int rowIndex) {
        PersonalDatabaseLayout.Rect dropdownRect = dropdownRect(screen);
        if (dropdownRect == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        return new PersonalDatabaseLayout.Rect(
                dropdownRect.x() + 2,
                dropdownRect.y() + 2
                        + PersonalDatabaseScreen.DROPDOWN_ROW_HEIGHT
                        + PersonalDatabaseScreen.SORT_DROPDOWN_SECTION_GAP
                        + rowIndex * PersonalDatabaseScreen.DROPDOWN_ROW_HEIGHT,
                dropdownRect.width() - 4,
                PersonalDatabaseScreen.DROPDOWN_ROW_HEIGHT - 1
        );
    }

    private static int preferredDropdownWidth(PersonalDatabaseScreen screen) {
        int directionLabelWidth = Math.max(
                screen.screenFont().width(PersonalDatabaseScreenCommonHelper.sortDirectionLabel(DatabaseSortDirection.ASC)),
                screen.screenFont().width(PersonalDatabaseScreenCommonHelper.sortDirectionLabel(DatabaseSortDirection.DESC))
        ) + 16;
        int width = Math.max(
                PersonalDatabaseScreen.SORT_DROPDOWN_MIN_WIDTH,
                directionLabelWidth * 2 + PersonalDatabaseScreen.SORT_DIRECTION_BUTTON_GAP + 4
        );
        for (DatabaseSortMethod method : DatabaseSortDropdownModel.methodOptions()) {
            width = Math.max(
                    width,
                    screen.screenFont().width(PersonalDatabaseScreenCommonHelper.sortMethodLabel(method)) + 20
            );
        }
        return width;
    }

    private static int dropdownHeight() {
        return 4
                + PersonalDatabaseScreen.DROPDOWN_ROW_HEIGHT
                + PersonalDatabaseScreen.SORT_DROPDOWN_SECTION_GAP
                + DatabaseSortDropdownModel.methodOptions().size() * PersonalDatabaseScreen.DROPDOWN_ROW_HEIGHT;
    }

    private static int anchoredPopupY(
            PersonalDatabaseLayout.Rect frameRect,
            PersonalDatabaseLayout.Rect anchorRect,
            int height
    ) {
        int minY = frameRect.y() + PersonalDatabaseScreen.CONTEXT_MENU_MARGIN;
        int maxY = Math.max(minY, frameRect.bottom() - height - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN);
        int belowY = anchorRect.bottom() + 2;
        if (belowY + height <= frameRect.bottom() - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN) {
            return belowY;
        }
        int aboveY = anchorRect.y() - 2 - height;
        if (aboveY >= minY) {
            return aboveY;
        }
        return Mth.clamp(belowY, minY, maxY);
    }
}
