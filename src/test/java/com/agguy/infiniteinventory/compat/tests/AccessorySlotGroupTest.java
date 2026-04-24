package com.agguy.infiniteinventory.compat.tests;

import com.agguy.infiniteinventory.compat.AccessorySlotGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AccessorySlotGroup record 纯逻辑测试。
 */
class AccessorySlotGroupTest {

    @Test
    void lastSlotIndexExclusiveShouldReturnFirstSlotIndexPlusSlotCount() {
        AccessorySlotGroup group = new AccessorySlotGroup("back", "Back Slot", 10, 4);
        assertEquals(14, group.lastSlotIndexExclusive());
    }

    @Test
    void lastSlotIndexExclusiveShouldEqualFirstSlotIndexWhenSlotCountIsZero() {
        AccessorySlotGroup group = new AccessorySlotGroup("empty", "Empty", 5, 0);
        assertEquals(5, group.lastSlotIndexExclusive());
    }

    @Test
    void containsSlotIndexShouldReturnTrueForFirstSlot() {
        AccessorySlotGroup group = new AccessorySlotGroup("back", "Back Slot", 10, 4);
        assertTrue(group.containsSlotIndex(10));
    }

    @Test
    void containsSlotIndexShouldReturnTrueForLastSlot() {
        AccessorySlotGroup group = new AccessorySlotGroup("back", "Back Slot", 10, 4);
        assertTrue(group.containsSlotIndex(13));
    }

    @Test
    void containsSlotIndexShouldReturnFalseForOutOfRange() {
        AccessorySlotGroup group = new AccessorySlotGroup("back", "Back Slot", 10, 4);
        assertFalse(group.containsSlotIndex(9));
        assertFalse(group.containsSlotIndex(14));
    }

    @Test
    void containsSlotIndexShouldReturnFalseForEmptyGroup() {
        AccessorySlotGroup group = new AccessorySlotGroup("empty", "Empty", 5, 0);
        assertFalse(group.containsSlotIndex(5));
        assertFalse(group.containsSlotIndex(4));
    }

    @Test
    void recordComponentsShouldBeAccessible() {
        AccessorySlotGroup group = new AccessorySlotGroup("ring", "Rings", 20, 2);
        assertEquals("ring", group.slotName());
        assertEquals("Rings", group.translationKey());
        assertEquals(20, group.firstSlotIndex());
        assertEquals(2, group.slotCount());
    }
}
