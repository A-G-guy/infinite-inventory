package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.network.DatabaseClickAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseClickActionTest {
    @Test
    void storeActionsShouldExposeExpectedFlags() {
        assertTrue(DatabaseClickAction.STORE_STACK.isStoreAction());
        assertTrue(DatabaseClickAction.STORE_SINGLE.isStoreAction());
        assertTrue(DatabaseClickAction.STORE_SINGLE.storesSingleItem());
        assertFalse(DatabaseClickAction.STORE_STACK.storesSingleItem());
        assertFalse(DatabaseClickAction.TAKE_SINGLE.isStoreAction());
    }

    @Test
    void takeActionsShouldResolveExpectedAmounts() {
        assertEquals(1L, DatabaseClickAction.TAKE_SINGLE.resolveRequestedAmount(9L, 64));
        assertEquals(64L, DatabaseClickAction.TAKE_STACK.resolveRequestedAmount(9L, 64));
        assertEquals(64L, DatabaseClickAction.TAKE_STACK_TO_INVENTORY.resolveRequestedAmount(9L, 64));
        assertEquals(32L, DatabaseClickAction.TAKE_HALF_STACK_TO_INVENTORY.resolveRequestedAmount(9L, 64));
        assertEquals(1L, DatabaseClickAction.TAKE_HALF_STACK_TO_INVENTORY.resolveRequestedAmount(9L, 1));
        assertEquals(3L, DatabaseClickAction.TAKE_HALF_ENTRY_TO_INVENTORY.resolveRequestedAmount(5L, 64));
        assertEquals(1L, DatabaseClickAction.TAKE_STACK.resolveRequestedAmount(9L, 0));
        assertEquals(5L, DatabaseClickAction.TAKE_ALL.resolveRequestedAmount(5L, 64));
        assertEquals(1L, DatabaseClickAction.DROP_SINGLE.resolveRequestedAmount(128L, 64));
    }

    @Test
    void inventoryExtractionFlagsShouldMatchActions() {
        assertTrue(DatabaseClickAction.TAKE_STACK_TO_INVENTORY.extractsToInventory());
        assertTrue(DatabaseClickAction.TAKE_HALF_ENTRY_TO_INVENTORY.extractsToInventory());
        assertTrue(DatabaseClickAction.TAKE_ALL.extractsToInventory());
        assertTrue(DatabaseClickAction.TAKE_ALL.extractsEntireEntry());
        assertFalse(DatabaseClickAction.TAKE_STACK.extractsToInventory());
        assertFalse(DatabaseClickAction.TAKE_HALF_STACK_TO_INVENTORY.extractsToInventory());
        assertFalse(DatabaseClickAction.TAKE_HALF_STACK_TO_INVENTORY.extractsEntireEntry());
        assertFalse(DatabaseClickAction.TAKE_STACK_TO_INVENTORY.extractsEntireEntry());
        assertTrue(DatabaseClickAction.DROP_SINGLE.dropsToWorld());
        assertFalse(DatabaseClickAction.DROP_SINGLE.extractsToInventory());
        assertFalse(DatabaseClickAction.DROP_SINGLE.extractsEntireEntry());
    }
}
