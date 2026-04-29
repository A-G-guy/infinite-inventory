package com.agguy.infiniteinventory.compat.jei;

import net.minecraft.world.entity.player.Player;

/**
 * JEI 兼容桥接口。
 * 所有方法在 JEI 未安装时均返回空值/ false，保证模组在无 JEI 环境下正常运行。
 */
public interface JeiCompatBridge {
    boolean isAvailable();

    void onPlayerLogin(Player player);
}
