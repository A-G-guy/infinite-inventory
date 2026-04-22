package com.agguy.infiniteinventory.compat.jei;

import com.agguy.infiniteinventory.database.DatabaseScope;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class JeiCompatBridgeImpl implements JeiCompatBridge {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onPlayerLogin(Player player) {
    }

    @Override
    public void syncAmounts(Player player, Map<ItemStack, Long> personalAmounts, Map<ItemStack, Long> publicAmounts) {
        JeiAmountCache.INSTANCE.update(
                new it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap<>(personalAmounts),
                new it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap<>(publicAmounts)
        );
    }
}
