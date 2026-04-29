package com.agguy.infiniteinventory.compat.jei;

import net.minecraft.world.entity.player.Player;

final class NoOpJeiCompatBridge implements JeiCompatBridge {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void onPlayerLogin(Player player) {
    }
}
