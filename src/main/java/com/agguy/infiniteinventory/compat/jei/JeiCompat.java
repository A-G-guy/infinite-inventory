package com.agguy.infiniteinventory.compat.jei;

import com.agguy.infiniteinventory.InfiniteInventory;
import java.lang.reflect.InvocationTargetException;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class JeiCompat {
    public static final String MOD_ID = "jei";

    private static final Logger LOGGER = LogManager.getLogger();
    private static final JeiCompatBridge BRIDGE = createBridge();

    private JeiCompat() {
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!isAvailable()) {
            return;
        }
        LOGGER.info("JEI 兼容层已加载。");
    }

    public static boolean isAvailable() {
        return BRIDGE.isAvailable();
    }

    public static void onPlayerLogin(Player player) {
        BRIDGE.onPlayerLogin(player);
    }

    private static JeiCompatBridge createBridge() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return new NoOpJeiCompatBridge();
        }
        try {
            Class<?> bridgeClass = Class.forName("com.agguy.infiniteinventory.compat.jei.JeiCompatBridgeImpl");
            return (JeiCompatBridge) bridgeClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            LOGGER.error("无法初始化 JEI 兼容桥接，已回退为禁用状态。mod={}", InfiniteInventory.MODID, exception);
            return new NoOpJeiCompatBridge();
        } catch (LinkageError error) {
            LOGGER.error("JEI 兼容桥接加载失败，可能是 API 版本不兼容，已回退为禁用状态。mod={}", InfiniteInventory.MODID, error);
            return new NoOpJeiCompatBridge();
        }
    }
}
