package com.agguy.infiniteinventory.compat.tests;

import com.agguy.infiniteinventory.compat.MenuSlotAdder;
import net.minecraft.world.inventory.Slot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MenuSlotAdderTest {

    @Test
    void shouldInvokeLambda() {
        MenuSlotAdder adder = slot -> 42;

        assertEquals(42, adder.add(null));
    }

    @Test
    void shouldTrackSlotIndex() {
        // Simulate a typical slot-adding loop
        int[] counter = {0};
        MenuSlotAdder adder = slot -> counter[0]++;

        for (int i = 0; i < 5; i++) {
            adder.add(null);
        }

        assertEquals(5, counter[0]);
    }
}
