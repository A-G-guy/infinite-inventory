package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseLogAction;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class PersonalDatabaseServiceDepositHelper {
    private PersonalDatabaseServiceDepositHelper() {
    }

    static boolean depositSlot(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, String targetTabId, Slot slot) {
        ItemStack stack = slot.getItem();
        if (!service.canStore(stack)) {
            return false;
        }
        String resolvedTargetTabId = service.resolveConcreteTargetTabId(player, scope, targetTabId);
        service.resolveDatabaseForMutation(player, scope).store(stack.copy(), resolvedTargetTabId);
        service.markScopeDirty(player, scope);
        slot.setByPlayer(ItemStack.EMPTY, stack.copy());
        slot.setChanged();
        PersonalDatabaseServiceLogHelper.recordLog(service, player, scope, DatabaseLogAction.DEPOSIT, stack, stack.getCount(), "", resolvedTargetTabId, null);
        return true;
    }

    static long depositMainInventory(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, String targetTabId) {
        Inventory inventory = player.getInventory();
        StoredItemDatabase database = service.resolveDatabaseForMutation(player, scope);
        String resolvedTargetTabId = service.resolveConcreteTargetTabId(player, scope, targetTabId);
        long movedItems = 0L;
        boolean movedAny = false;
        for (int slotIndex = 0; slotIndex < inventory.items.size(); slotIndex++) {
            if (!PersonalDatabaseServiceHelper.isPrimaryStorageSlot(slotIndex)) {
                continue;
            }
            ItemStack stack = inventory.items.get(slotIndex);
            if (!service.canStore(stack)) {
                continue;
            }
            movedItems = PersonalDatabaseServiceStorageHelper.safeAddMovedItems(movedItems, stack);
            movedAny = true;
            database.store(stack.copy(), resolvedTargetTabId);
            PersonalDatabaseServiceLogHelper.recordLog(service, player, scope, DatabaseLogAction.DEPOSIT, stack, stack.getCount(), "", resolvedTargetTabId, null);
            inventory.items.set(slotIndex, ItemStack.EMPTY);
        }
        if (movedAny) {
            service.markScopeDirty(player, scope);
            inventory.setChanged();
        }
        return movedItems;
    }

    static boolean storeStack(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, String targetTabId, ItemStack stack) {
        if (!service.canStore(stack)) {
            return false;
        }
        String resolvedTargetTabId = service.resolveConcreteTargetTabId(player, scope, targetTabId);
        service.resolveDatabaseForMutation(player, scope).store(stack, resolvedTargetTabId);
        service.markScopeDirty(player, scope);
        PersonalDatabaseServiceLogHelper.recordLog(service, player, scope, DatabaseLogAction.DEPOSIT, stack, stack.getCount(), "", resolvedTargetTabId, null);
        return true;
    }

    static boolean tryAutoStorePickedUpItem(PersonalDatabaseService service, ServerPlayer player, ItemEntity itemEntity) {
        if (player == null || itemEntity == null) {
            return false;
        }
        if (!service.getEnhancementConfig(player).isEnabled(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS)) {
            return false;
        }
        if (itemEntity.hasPickUpDelay()) {
            return false;
        }
        if (itemEntity.getTarget() != null && !itemEntity.getTarget().equals(player.getUUID())) {
            return false;
        }
        ItemStack stack = itemEntity.getItem();
        if (!service.canStore(stack)) {
            return false;
        }
        int pickedUpAmount = stack.getCount();
        if (pickedUpAmount <= 0) {
            return false;
        }
        DatabaseAutoStoreTarget autoStoreTarget = service.resolveAutoStoreTarget(player);
        service.resolveDatabaseForMutation(player, autoStoreTarget.scope()).store(stack.copy(), autoStoreTarget.tabId());
        service.markScopeDirty(player, autoStoreTarget.scope());
        player.take(itemEntity, pickedUpAmount);
        player.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), pickedUpAmount);
        player.onItemPickup(itemEntity);
        itemEntity.discard();
        PersonalDatabaseServiceLogHelper.recordLog(service, player, autoStoreTarget.scope(), DatabaseLogAction.DEPOSIT, stack, pickedUpAmount, "", autoStoreTarget.tabId(), null);
        if (autoStoreTarget.scope() == DatabaseScope.PUBLIC) {
            service.syncPublicViewers(player.server);
        } else if (player.containerMenu instanceof com.agguy.infiniteinventory.menu.PersonalDatabaseMenu menu && menu.activeScope() == DatabaseScope.PERSONAL) {
            menu.syncViewToClient();
        }
        return true;
    }
}
