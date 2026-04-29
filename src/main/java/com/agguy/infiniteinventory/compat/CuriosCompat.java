package com.agguy.infiniteinventory.compat;

import com.agguy.infiniteinventory.InfiniteInventory;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CuriosCompat {
    public static final String MOD_ID = "curios";

    private static final Logger LOGGER = LogManager.getLogger();
    private static final CuriosCompatBridge BRIDGE = createBridge();

    private CuriosCompat() {
    }

    public static boolean isAvailable() {
        return BRIDGE.isAvailable();
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!isAvailable()) {
            return;
        }
        event.enqueueWork(BRIDGE::registerDatabaseTerminalAccessory);
    }

    public static boolean isBackSlotEquipped(Player player, Item item) {
        return BRIDGE.isBackSlotEquipped(player, item);
    }

    public static List<AccessorySlotGroup> appendAccessorySlots(Player player, MenuSlotAdder slotAdder) {
        return BRIDGE.appendAccessorySlots(player, slotAdder);
    }

    private static CuriosCompatBridge createBridge() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return new NoOpCuriosCompatBridge();
        }
        try {
            Class<?> bridgeClass = Class.forName("com.agguy.infiniteinventory.compat.CuriosCompatBridgeImpl");
            return (CuriosCompatBridge) bridgeClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            LOGGER.error("无法初始化 Curios 兼容桥接，已回退为禁用状态。mod={}", InfiniteInventory.MODID, exception);
            return new NoOpCuriosCompatBridge();
        } catch (LinkageError error) {
            LOGGER.error("Curios 兼容桥接加载失败，可能是 API 版本不兼容，已回退为禁用状态。mod={}", InfiniteInventory.MODID, error);
            return new NoOpCuriosCompatBridge();
        }
    }
}
