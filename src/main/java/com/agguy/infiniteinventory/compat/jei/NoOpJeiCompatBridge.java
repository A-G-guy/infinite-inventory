package com.agguy.infiniteinventory.compat.jei;

import com.agguy.infiniteinventory.database.DatabaseScope;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

final class NoOpJeiCompatBridge implements JeiCompatBridge {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void onPlayerLogin(Player player) {
    }

    @Override
    public void syncAmounts(Player player, Map<ItemStack, Long> personalAmounts, Map<ItemStack, Long> publicAmounts) {
    }
}
