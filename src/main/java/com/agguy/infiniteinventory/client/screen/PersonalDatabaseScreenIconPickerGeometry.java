package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;

final class PersonalDatabaseScreenIconPickerGeometry {
    private PersonalDatabaseScreenIconPickerGeometry() {
    }

    static PersonalDatabaseLayout.Rect iconPickerRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int width = Math.min(
                PersonalDatabaseScreen.ICON_PICKER_WIDTH,
                Math.max(1, screen.layout.frameRect().width() - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN * 2)
        );
        int height = Math.min(
                PersonalDatabaseScreen.ICON_PICKER_HEIGHT,
                Math.max(1, screen.layout.frameRect().height() - PersonalDatabaseScreen.CONTEXT_MENU_MARGIN * 2)
        );
        return PersonalDatabaseScreenGeometry.centeredOverlayRect(screen, width, height);
    }

    static PersonalDatabaseLayout.Rect iconPickerSearchFieldRect(PersonalDatabaseScreen screen) {
        return iconPickerLayout(screen).searchRect();
    }

    static PersonalDatabaseLayout.Rect iconPickerCategoryRect(PersonalDatabaseScreen screen, int index) {
        IconPickerLayout layout = iconPickerLayout(screen);
        if (index < 0 || index >= DatabaseCategory.values().length) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int tabGap = 4;
        int totalGap = Math.max(0, DatabaseCategory.values().length - 1) * tabGap;
        int tabWidth = Math.max(1, (layout.categoryRowRect().width() - totalGap) / DatabaseCategory.values().length);
        int x = layout.categoryRowRect().x() + index * (tabWidth + tabGap);
        int width = index == DatabaseCategory.values().length - 1
                ? layout.categoryRowRect().right() - x
                : tabWidth;
        return new PersonalDatabaseLayout.Rect(x, layout.categoryRowRect().y(), width, layout.categoryRowRect().height());
    }

    static PersonalDatabaseLayout.Rect iconPickerGridRect(PersonalDatabaseScreen screen) {
        return iconPickerLayout(screen).gridRect();
    }

    static int iconPickerPageSize(PersonalDatabaseScreen screen) {
        IconPickerLayout layout = iconPickerLayout(screen);
        return Math.max(1, layout.columns() * layout.rows());
    }

    static PersonalDatabaseLayout.Rect iconPickerCellRect(PersonalDatabaseScreen screen, int index) {
        IconPickerLayout layout = iconPickerLayout(screen);
        if (index < 0) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int column = index % layout.columns();
        int row = index / layout.columns();
        if (row >= layout.rows()) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int x = layout.gridRect().x() + column * (layout.cellWidth() + layout.cellGap());
        int y = layout.gridRect().y() + row * (layout.cellHeight() + layout.cellGap());
        return new PersonalDatabaseLayout.Rect(x, y, layout.cellWidth(), layout.cellHeight());
    }

    static PersonalDatabaseLayout.Rect iconPickerCellIconRect(PersonalDatabaseScreen screen, int index) {
        PersonalDatabaseLayout.Rect cellRect = iconPickerCellRect(screen, index);
        if (cellRect.width() <= 0 || cellRect.height() <= 0) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        return new PersonalDatabaseLayout.Rect(
                cellRect.x() + Math.max(0, (cellRect.width() - 16) / 2),
                cellRect.y() + 5,
                16,
                16
        );
    }

    static PersonalDatabaseLayout.Rect iconPickerCellLabelRect(PersonalDatabaseScreen screen, int index) {
        PersonalDatabaseLayout.Rect cellRect = iconPickerCellRect(screen, index);
        if (cellRect.width() <= 0 || cellRect.height() <= 0) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        return new PersonalDatabaseLayout.Rect(
                cellRect.x() + 4,
                cellRect.y() + 25,
                Math.max(1, cellRect.width() - 8),
                Math.max(1, cellRect.height() - 29)
        );
    }

    static PersonalDatabaseLayout.Rect iconPickerSelectedPreviewRect(PersonalDatabaseScreen screen) {
        return iconPickerLayout(screen).previewRect();
    }

    static PersonalDatabaseLayout.Rect iconPickerPreviousPageButtonRect(PersonalDatabaseScreen screen) {
        return iconPickerLayout(screen).previousRect();
    }

    static PersonalDatabaseLayout.Rect iconPickerPageLabelRect(PersonalDatabaseScreen screen) {
        return iconPickerLayout(screen).pageRect();
    }

    static PersonalDatabaseLayout.Rect iconPickerNextPageButtonRect(PersonalDatabaseScreen screen) {
        return iconPickerLayout(screen).nextRect();
    }

    static PersonalDatabaseLayout.Rect iconPickerCancelButtonRect(PersonalDatabaseScreen screen) {
        return iconPickerLayout(screen).cancelRect();
    }

    static PersonalDatabaseLayout.Rect iconPickerApplyButtonRect(PersonalDatabaseScreen screen) {
        return iconPickerLayout(screen).applyRect();
    }

    private static IconPickerLayout iconPickerLayout(PersonalDatabaseScreen screen) {
        PersonalDatabaseLayout.Rect panelRect = iconPickerRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return IconPickerLayout.empty();
        }
        int padding = PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING;
        PersonalDatabaseLayout.Rect searchRect = new PersonalDatabaseLayout.Rect(
                panelRect.x() + padding,
                panelRect.y() + padding + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT + 8,
                panelRect.width() - padding * 2,
                20
        );
        PersonalDatabaseLayout.Rect categoryRowRect = new PersonalDatabaseLayout.Rect(
                searchRect.x(),
                searchRect.bottom() + 8,
                searchRect.width(),
                20
        );
        int footerButtonWidth = PersonalDatabaseScreen.MANAGEMENT_BUTTON_WIDTH;
        int pagerButtonWidth = PersonalDatabaseLayout.PAGE_BUTTON_WIDTH;
        int footerY = panelRect.bottom() - padding - PersonalDatabaseLayout.CONTROL_HEIGHT;
        PersonalDatabaseLayout.Rect applyRect = new PersonalDatabaseLayout.Rect(
                panelRect.right() - padding - footerButtonWidth,
                footerY,
                footerButtonWidth,
                PersonalDatabaseLayout.CONTROL_HEIGHT
        );
        PersonalDatabaseLayout.Rect cancelRect = new PersonalDatabaseLayout.Rect(
                applyRect.x() - 8 - footerButtonWidth,
                footerY,
                footerButtonWidth,
                PersonalDatabaseLayout.CONTROL_HEIGHT
        );
        PersonalDatabaseLayout.Rect pageRect = new PersonalDatabaseLayout.Rect(
                cancelRect.x() - 8 - 84,
                footerY,
                84,
                PersonalDatabaseLayout.CONTROL_HEIGHT
        );
        PersonalDatabaseLayout.Rect nextRect = new PersonalDatabaseLayout.Rect(
                pageRect.x() - 4 - pagerButtonWidth,
                footerY,
                pagerButtonWidth,
                PersonalDatabaseLayout.CONTROL_HEIGHT
        );
        PersonalDatabaseLayout.Rect previousRect = new PersonalDatabaseLayout.Rect(
                nextRect.x() - 4 - pagerButtonWidth,
                footerY,
                pagerButtonWidth,
                PersonalDatabaseLayout.CONTROL_HEIGHT
        );
        PersonalDatabaseLayout.Rect previewRect = new PersonalDatabaseLayout.Rect(
                panelRect.x() + padding,
                footerY,
                Math.max(1, previousRect.x() - panelRect.x() - padding - 12),
                PersonalDatabaseLayout.CONTROL_HEIGHT
        );
        int gridTop = categoryRowRect.bottom() + 8;
        int gridBottom = footerY - 8;
        int gridHeight = Math.max(1, gridBottom - gridTop);
        int gridWidth = Math.max(1, searchRect.width());
        IconPickerGridSpec gridSpec = resolveGridSpec(
                gridWidth,
                gridHeight,
                PersonalDatabaseScreenCommonHelper.fitProfile(screen).compactLayout()
        );
        int contentWidth = gridSpec.columns() * gridSpec.cellWidth() + Math.max(0, gridSpec.columns() - 1) * gridSpec.cellGap();
        int contentHeight = gridSpec.rows() * gridSpec.cellHeight() + Math.max(0, gridSpec.rows() - 1) * gridSpec.cellGap();
        PersonalDatabaseLayout.Rect gridRect = new PersonalDatabaseLayout.Rect(
                searchRect.x() + Math.max(0, (gridWidth - contentWidth) / 2),
                gridTop + Math.max(0, (gridHeight - contentHeight) / 2),
                contentWidth,
                contentHeight
        );
        return new IconPickerLayout(
                searchRect,
                categoryRowRect,
                gridRect,
                gridSpec.columns(),
                gridSpec.rows(),
                gridSpec.cellWidth(),
                gridSpec.cellHeight(),
                gridSpec.cellGap(),
                previewRect,
                previousRect,
                pageRect,
                nextRect,
                cancelRect,
                applyRect
        );
    }

    static IconPickerGridSpec resolveGridSpec(int gridWidth, int gridHeight, boolean compactLayout) {
        int columns = compactLayout
                ? PersonalDatabaseScreen.COMPACT_ICON_PICKER_COLUMNS
                : PersonalDatabaseScreen.STANDARD_ICON_PICKER_COLUMNS;
        int rows = PersonalDatabaseScreen.ICON_PICKER_ROWS;
        int cellGap = PersonalDatabaseScreen.ICON_PICKER_CELL_GAP;
        int cellWidth = Math.max(1, (gridWidth - Math.max(0, columns - 1) * cellGap) / columns);
        int maxCellHeight = Math.max(1, (gridHeight - Math.max(0, rows - 1) * cellGap) / rows);
        int cellHeight = Math.min(PersonalDatabaseScreen.ICON_PICKER_CELL_MAX_HEIGHT, maxCellHeight);
        if (maxCellHeight >= PersonalDatabaseScreen.ICON_PICKER_CELL_MIN_HEIGHT) {
            cellHeight = Math.max(PersonalDatabaseScreen.ICON_PICKER_CELL_MIN_HEIGHT, cellHeight);
        }
        return new IconPickerGridSpec(columns, rows, cellWidth, cellHeight, cellGap);
    }

    private record IconPickerLayout(
            PersonalDatabaseLayout.Rect searchRect,
            PersonalDatabaseLayout.Rect categoryRowRect,
            PersonalDatabaseLayout.Rect gridRect,
            int columns,
            int rows,
            int cellWidth,
            int cellHeight,
            int cellGap,
            PersonalDatabaseLayout.Rect previewRect,
            PersonalDatabaseLayout.Rect previousRect,
            PersonalDatabaseLayout.Rect pageRect,
            PersonalDatabaseLayout.Rect nextRect,
            PersonalDatabaseLayout.Rect cancelRect,
            PersonalDatabaseLayout.Rect applyRect
    ) {
        private static IconPickerLayout empty() {
            PersonalDatabaseLayout.Rect empty = PersonalDatabaseLayout.Rect.empty();
            return new IconPickerLayout(empty, empty, empty, 1, 1, 44, 44, 6, empty, empty, empty, empty, empty, empty);
        }
    }

    record IconPickerGridSpec(int columns, int rows, int cellWidth, int cellHeight, int cellGap) {
    }
}
