package com.agguy.infiniteinventory.compat;

import net.minecraft.world.inventory.Slot;

@FunctionalInterface
public interface MenuSlotAdder {
    int add(Slot slot);
}
