package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseSortDirection;
import com.agguy.infiniteinventory.database.DatabaseSortMethod;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSortDropdownModelTest {
    @Test
    void methodOptionsShouldStayInExpectedMenuOrder() {
        assertEquals(
                List.of(
                        DatabaseSortMethod.RECENTLY_CHANGED,
                        DatabaseSortMethod.RECENTLY_ADDED,
                        DatabaseSortMethod.NAME,
                        DatabaseSortMethod.COUNT,
                        DatabaseSortMethod.MOD_NAMESPACE,
                        DatabaseSortMethod.ITEM_ID,
                        DatabaseSortMethod.STARRED
                ),
                DatabaseSortDropdownModel.methodOptions()
        );
    }

    @Test
    void directionActionShouldKeepDropdownOpen() {
        DatabaseSortDropdownModel.SortAction action = DatabaseSortDropdownModel.actionForDirection(
                DatabaseSortOption.NAME_DESC,
                DatabaseSortDirection.ASC
        );

        assertEquals(DatabaseSortOption.NAME_ASC, action.nextSort());
        assertFalse(action.closeMenu());
    }

    @Test
    void methodActionShouldPreserveDirectionAndCloseDropdown() {
        DatabaseSortDropdownModel.SortAction action = DatabaseSortDropdownModel.actionForMethod(
                DatabaseSortOption.COUNT_ASC,
                DatabaseSortMethod.ITEM_ID
        );

        assertEquals(DatabaseSortOption.ITEM_ID_ASC, action.nextSort());
        assertTrue(action.closeMenu());
    }
}
