package com.agguy.infiniteinventory;

import com.agguy.infiniteinventory.network.ModNetwork;
import com.agguy.infiniteinventory.registry.ModAttachments;
import com.agguy.infiniteinventory.registry.ModItems;
import com.agguy.infiniteinventory.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(InfiniteInventory.MODID)
public final class InfiniteInventory {
    public static final String MODID = "infiniteinventory";

    public InfiniteInventory(IEventBus modEventBus) {
        ModItems.REGISTER.register(modEventBus);
        ModMenus.REGISTER.register(modEventBus);
        ModAttachments.REGISTER.register(modEventBus);

        modEventBus.addListener(ModNetwork::register);
        modEventBus.addListener(ModItems::addCreativeTabContents);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(InfiniteInventoryClient::registerScreens);
        }
    }
}
