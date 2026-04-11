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
        assertEquals(1, DatabaseClickAction.TAKE_SINGLE.resolveRequestedAmount(64));
        assertEquals(64, DatabaseClickAction.TAKE_STACK.resolveRequestedAmount(64));
        assertEquals(64, DatabaseClickAction.TAKE_STACK_TO_INVENTORY.resolveRequestedAmount(64));
        assertEquals(1, DatabaseClickAction.TAKE_STACK.resolveRequestedAmount(0));
        assertEquals(0, DatabaseClickAction.TAKE_ALL.resolveRequestedAmount(64));
    }

    @Test
    void inventoryExtractionFlagsShouldMatchActions() {
        assertTrue(DatabaseClickAction.TAKE_STACK_TO_INVENTORY.extractsToInventory());
        assertTrue(DatabaseClickAction.TAKE_ALL.extractsToInventory());
        assertTrue(DatabaseClickAction.TAKE_ALL.extractsEntireEntry());
        assertFalse(DatabaseClickAction.TAKE_STACK.extractsToInventory());
        assertFalse(DatabaseClickAction.TAKE_STACK_TO_INVENTORY.extractsEntireEntry());
    }
}
