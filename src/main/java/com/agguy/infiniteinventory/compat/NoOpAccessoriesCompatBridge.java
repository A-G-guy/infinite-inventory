package com.agguy.infiniteinventory.compat;

import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

final class NoOpAccessoriesCompatBridge implements AccessoriesCompatBridge {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void registerDatabaseTerminalAccessory() {
    }

    @Override
    public boolean isBackSlotEquipped(Player player, Item item) {
        return false;
    }

    @Override
    public List<AccessorySlotGroup> appendAccessorySlots(Player player, MenuSlotAdder slotAdder) {
        return List.of();
    }
}
