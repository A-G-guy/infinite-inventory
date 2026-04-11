package com.agguy.infiniteinventory.compat;

import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public interface AccessoriesCompatBridge {
    boolean isAvailable();

    void registerDatabaseTerminalAccessory();

    boolean isBackSlotEquipped(Player player, Item item);

    List<AccessorySlotGroup> appendAccessorySlots(Player player, MenuSlotAdder slotAdder);
}
