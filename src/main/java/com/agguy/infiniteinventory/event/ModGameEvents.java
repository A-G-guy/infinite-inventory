package com.agguy.infiniteinventory.event;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.compat.jei.JeiCompat;
import com.agguy.infiniteinventory.database.DatabaseBackupManager;
import com.agguy.infiniteinventory.registry.ModItems;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.level.ServerPlayer;

@EventBusSubscriber(modid = InfiniteInventory.MODID)
public final class ModGameEvents {
    private ModGameEvents() {
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        if (event.getOriginal().level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        Inventory originalInventory = event.getOriginal().getInventory();
        for (int slotIndex = 0; slotIndex < originalInventory.getContainerSize(); slotIndex++) {
            ItemStack stack = originalInventory.getItem(slotIndex);
            if (stack.is(ModItems.DATABASE_ACCESS_ITEM.get())) {
                event.getEntity().getInventory().placeItemBackInInventory(stack.copy());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        event.getDrops().removeIf(itemEntity -> itemEntity.getItem().is(ModItems.DATABASE_ACCESS_ITEM.get()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemEntityPickup(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (event.canPickup().isFalse()) {
            return;
        }
        if (PersonalDatabaseService.INSTANCE.tryAutoStorePickedUpItem(player, event.getItemEntity())) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        DatabaseBackupManager.maybeCreateRollingBackup(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        JeiCompat.onPlayerLogin(player);
        PersonalDatabaseService.INSTANCE.syncJeiAmountsToPlayer(player);
    }
}
