package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseLogAction;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class PersonalDatabaseServiceDepositHelper {
    private PersonalDatabaseServiceDepositHelper() {
    }

    public record DepositConflict(
            DatabaseScope scope,
            String targetTabId,
            String existingTabId,
            ItemStack stack
    ) {
    }

    @Nullable
    static DepositConflict checkDepositConflict(
            PersonalDatabaseService service,
            ServerPlayer player,
            DatabaseScope scope,
            String targetTabId,
            ItemStack stack
    ) {
        if (!service.canStore(stack)) {
            return null;
        }
        StoredItemDatabase database = service.resolveDatabaseForMutation(player, scope);
        StoredStackKey key = StoredStackKey.of(stack);
        StoredStackEntry entry = database.entries().get(key);
        if (entry == null) {
            return null;
        }
        String resolvedTargetTabId = service.resolveConcreteTargetTabId(player, scope, targetTabId);
        if (entry.tabId().equals(resolvedTargetTabId)) {
            return null;
        }
        return new DepositConflict(scope, resolvedTargetTabId, entry.tabId(), stack.copy());
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

    static long depositExistingByTab(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope) {
        Inventory inventory = player.getInventory();
        StoredItemDatabase database = service.resolveDatabaseForMutation(player, scope);
        long movedItems = 0L;
        boolean movedAny = false;
        for (int slotIndex = 0; slotIndex < inventory.items.size(); slotIndex++) {
            if (!PersonalDatabaseServiceStorageHelper.isPrimaryStorageSlot(slotIndex)) {
                continue;
            }
            ItemStack stack = inventory.items.get(slotIndex);
            if (!service.canStore(stack)) {
                continue;
            }
            StoredStackKey key = StoredStackKey.of(stack);
            StoredStackEntry entry = database.entries().get(key);
            if (entry == null) {
                continue;
            }
            movedItems = PersonalDatabaseServiceStorageHelper.safeAddMovedItems(movedItems, stack);
            movedAny = true;
            database.store(stack.copy(), entry.tabId());
            PersonalDatabaseServiceLogHelper.recordLog(service, player, scope, DatabaseLogAction.DEPOSIT, stack, stack.getCount(), "", entry.tabId(), null);
            inventory.items.set(slotIndex, ItemStack.EMPTY);
        }
        if (movedAny) {
            service.markScopeDirty(player, scope);
            inventory.setChanged();
        }
        return movedItems;
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
