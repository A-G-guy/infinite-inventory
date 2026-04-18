package com.agguy.infiniteinventory.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalDatabaseScreenIconPickerGeometryTest {
    @Test
    void standardGridSpecShouldExposeSixByFourPageSize() {
        PersonalDatabaseScreenIconPickerGeometry.IconPickerGridSpec gridSpec =
                PersonalDatabaseScreenIconPickerGeometry.resolveGridSpec(720, 260, false);

        assertEquals(6, gridSpec.columns());
        assertEquals(4, gridSpec.rows());
        assertEquals(24, gridSpec.columns() * gridSpec.rows());
        assertTrue(gridSpec.cellWidth() > 0);
        assertTrue(gridSpec.cellHeight() >= PersonalDatabaseScreen.ICON_PICKER_CELL_MIN_HEIGHT);
    }

    @Test
    void compactGridSpecShouldExposeFourByFourPageSize() {
        PersonalDatabaseScreenIconPickerGeometry.IconPickerGridSpec gridSpec =
                PersonalDatabaseScreenIconPickerGeometry.resolveGridSpec(420, 220, true);

        assertEquals(4, gridSpec.columns());
        assertEquals(4, gridSpec.rows());
        assertEquals(16, gridSpec.columns() * gridSpec.rows());
        assertTrue(gridSpec.cellWidth() > 0);
        assertTrue(gridSpec.cellHeight() > 29);
    }
}
