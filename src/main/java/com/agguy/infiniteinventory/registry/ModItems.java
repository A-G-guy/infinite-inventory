package com.agguy.infiniteinventory.registry;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.item.DatabaseAccessItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(InfiniteInventory.MODID);
    public static final DeferredItem<Item> DATABASE_ACCESS_ITEM = REGISTER.register(
            "database_access_item",
            () -> new DatabaseAccessItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON))
    );

    private ModItems() {
    }

    public static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(DATABASE_ACCESS_ITEM.get());
        }
    }
}
