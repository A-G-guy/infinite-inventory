package com.agguy.infiniteinventory;

import com.agguy.infiniteinventory.compat.AccessoriesCompat;
import com.agguy.infiniteinventory.compat.jei.JeiCompat;
import com.agguy.infiniteinventory.network.ModNetwork;
import com.agguy.infiniteinventory.registry.ModAttachments;
import com.agguy.infiniteinventory.registry.ModItems;
import com.agguy.infiniteinventory.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(InfiniteInventory.MODID)
public final class InfiniteInventory {
    public static final String MODID = "infiniteinventory";

    public InfiniteInventory(IEventBus modEventBus) {
        ModItems.REGISTER.register(modEventBus);
        ModMenus.REGISTER.register(modEventBus);
        ModAttachments.REGISTER.register(modEventBus);

        modEventBus.addListener(ModNetwork::register);
        modEventBus.addListener(ModItems::addCreativeTabContents);
        modEventBus.addListener(AccessoriesCompat::onCommonSetup);
        modEventBus.addListener(JeiCompat::onCommonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(InfiniteInventoryClient::registerScreens);
            modEventBus.addListener(InfiniteInventoryClient::registerKeyMappings);
            NeoForge.EVENT_BUS.addListener(InfiniteInventoryClient::onClientTick);
            NeoForge.EVENT_BUS.addListener(InfiniteInventoryClient::onItemTooltip);
        }
    }
}
