package com.agguy.infiniteinventory.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MenuSlotRangeTest {
    @Test
    void spanOfMainInventoryAndHotbarStopsBeforeOffhandSlot() {
        MenuSlotRange mainInventory = MenuSlotRange.of(9, 27);
        MenuSlotRange hotbar = MenuSlotRange.of(36, 9);
        MenuSlotRange playerStorage = MenuSlotRange.span(mainInventory, hotbar);

        assertEquals(9, playerStorage.firstIndex());
        assertEquals(45, playerStorage.lastIndexExclusive());
        assertTrue(playerStorage.contains(44));
        assertFalse(playerStorage.contains(45));
    }

    @Test
    void spanReturnsExistingRangeWhenOnlyOneSideIsPresent() {
        MenuSlotRange hotbarOnly = MenuSlotRange.of(36, 9);

        MenuSlotRange playerStorage = MenuSlotRange.span(MenuSlotRange.empty(), hotbarOnly);

        assertEquals(hotbarOnly.firstIndex(), playerStorage.firstIndex());
        assertEquals(hotbarOnly.slotCount(), playerStorage.slotCount());
    }
}
