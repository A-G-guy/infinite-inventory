package com.agguy.infiniteinventory.compat.jei;

import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * JEI 兼容桥接口。
 * 所有方法在 JEI 未安装时均返回空值/ false，保证模组在无 JEI 环境下正常运行。
 */
public interface JeiCompatBridge {
    boolean isAvailable();

    void onPlayerLogin(Player player);

    void syncAmounts(Player player, Map<ItemStack, Long> personalAmounts, Map<ItemStack, Long> publicAmounts);

    Set<String> getEnabledCraftingTabIds(Player player, com.agguy.infiniteinventory.database.DatabaseScope scope);

    void setEnabledCraftingTabId(Player player, com.agguy.infiniteinventory.database.DatabaseScope scope, String tabId, boolean enabled);
}
