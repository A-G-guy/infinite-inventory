package com.agguy.infiniteinventory.client.screen.tests;

import com.agguy.infiniteinventory.client.screen.DatabasePagePickerModel;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabasePagePickerModelTest {
    @Test
    void smallPageCountsShouldExposeAllPagesDirectly() {
        List<DatabasePagePickerModel.PageOption> options = DatabasePagePickerModel.build(5, 2);

        assertEquals(List.of(0, 1, 2, 3, 4), options.stream().map(DatabasePagePickerModel.PageOption::pageIndex).toList());
        assertEquals(List.of(
                DatabasePagePickerModel.ShortcutType.PAGE,
                DatabasePagePickerModel.ShortcutType.PAGE,
                DatabasePagePickerModel.ShortcutType.PAGE,
                DatabasePagePickerModel.ShortcutType.PAGE,
                DatabasePagePickerModel.ShortcutType.PAGE
        ), options.stream().map(DatabasePagePickerModel.PageOption::shortcutType).toList());
    }

    @Test
    void largePageCountsShouldExposeFirstLastNeighborsAndMidJumps() {
        List<DatabasePagePickerModel.PageOption> options = DatabasePagePickerModel.build(40, 20);

        assertEquals(List.of(0, 9, 18, 19, 20, 21, 22, 30, 39), options.stream().map(DatabasePagePickerModel.PageOption::pageIndex).toList());
        assertEquals(DatabasePagePickerModel.ShortcutType.FIRST, options.getFirst().shortcutType());
        assertEquals(DatabasePagePickerModel.ShortcutType.LAST, options.getLast().shortcutType());
    }

    @Test
    void nearStartShouldStillOfferDirectJumpToFarPages() {
        List<DatabasePagePickerModel.PageOption> options = DatabasePagePickerModel.build(10, 0);

        assertEquals(List.of(0, 1, 2, 5, 9), options.stream().map(DatabasePagePickerModel.PageOption::pageIndex).toList());
        assertEquals(DatabasePagePickerModel.ShortcutType.FIRST, options.getFirst().shortcutType());
        assertEquals(DatabasePagePickerModel.ShortcutType.LAST, options.getLast().shortcutType());
    }
}
