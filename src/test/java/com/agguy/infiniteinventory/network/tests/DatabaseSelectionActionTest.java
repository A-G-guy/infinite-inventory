package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSelectionActionTest {
    @Test
    void extractionActionsShouldResolveExpectedAmounts() {
        assertEquals(1L, DatabaseSelectionAction.EXTRACT_ONE_TO_INVENTORY.resolveRequestedAmount(9L, 64));
        assertEquals(32L, DatabaseSelectionAction.EXTRACT_HALF_STACK_TO_INVENTORY.resolveRequestedAmount(9L, 64));
        assertEquals(1L, DatabaseSelectionAction.EXTRACT_HALF_STACK_TO_INVENTORY.resolveRequestedAmount(9L, 1));
        assertEquals(64L, DatabaseSelectionAction.EXTRACT_STACK_TO_INVENTORY.resolveRequestedAmount(9L, 64));
        assertEquals(3L, DatabaseSelectionAction.EXTRACT_HALF_ENTRY_TO_INVENTORY.resolveRequestedAmount(5L, 64));
        assertEquals(9L, DatabaseSelectionAction.EXTRACT_ALL_TO_INVENTORY.resolveRequestedAmount(9L, 64));
        assertEquals(17L, DatabaseSelectionAction.EXTRACT_CUSTOM_TO_INVENTORY.resolveRequestedAmount(9L, 64, 17L));
        assertEquals(0L, DatabaseSelectionAction.EXTRACT_CUSTOM_TO_INVENTORY.resolveRequestedAmount(9L, 64, 0L));
        assertEquals(0L, DatabaseSelectionAction.EXTRACT_CUSTOM_TO_INVENTORY.resolveRequestedAmount(9L, 64, -5L));
        assertEquals(0L, DatabaseSelectionAction.TRANSFER_TO_TAB.normalizeRequestedAmount(23L));
        assertEquals(7L, DatabaseSelectionAction.EXTRACT_CUSTOM_TO_INVENTORY.normalizeRequestedAmount(7L));
        assertEquals(0L, DatabaseSelectionAction.EXTRACT_CUSTOM_TO_INVENTORY.normalizeRequestedAmount(-1L));
    }

    @Test
    void targetRequirementShouldMatchActionType() {
        assertTrue(DatabaseSelectionAction.TRANSFER_TO_TAB.requiresTargetTab());
        assertFalse(DatabaseSelectionAction.TRANSFER_TO_TAB.extractsToInventory());
        assertFalse(DatabaseSelectionAction.EXTRACT_ONE_TO_INVENTORY.requiresTargetTab());
        assertTrue(DatabaseSelectionAction.EXTRACT_ALL_TO_INVENTORY.extractsToInventory());
        assertTrue(DatabaseSelectionAction.EXTRACT_HALF_ENTRY_TO_INVENTORY.extractsToInventory());
        assertTrue(DatabaseSelectionAction.EXTRACT_CUSTOM_TO_INVENTORY.extractsToInventory());
    }
}
