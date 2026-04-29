package com.agguy.infiniteinventory.compat.jei;

import net.minecraft.world.entity.player.Player;

final class JeiCompatBridgeImpl implements JeiCompatBridge {

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onPlayerLogin(Player player) {
    }
}
