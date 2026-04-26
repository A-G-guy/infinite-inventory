package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.network.DatabaseDepositConflictPayload;
import com.agguy.infiniteinventory.network.DepositConflictAction;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 存款冲突检测与解析的委托辅助类。
 *
 * <p>将冲突检测、服务端冲突载荷发送与解析逻辑从 {@link PersonalDatabaseMenu} 抽取，
 * 控制主类规模并保持单一职责。
 */
final class PersonalDatabaseMenuConflictHelper {

    private final PersonalDatabaseMenu menu;

    PersonalDatabaseMenuConflictHelper(PersonalDatabaseMenu menu) {
        this.menu = menu;
    }

    void sendDepositConflict(DatabaseScope scope, String targetTabId, String existingTabId, ItemStack stack, int slotIndex) {
        if (!(this.menu.owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        PacketDistributor.sendToPlayer(serverPlayer, new DatabaseDepositConflictPayload(
                scope, targetTabId, existingTabId, stack, slotIndex
        ));
    }

    void resolveDepositConflict(
            DatabaseScope scope,
            String targetTabId,
            String existingTabId,
            DepositConflictAction action,
            int slotIndex,
            ItemStack originalStack
    ) {
        if (!(this.menu.owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (action == DepositConflictAction.CANCEL) {
            return;
        }
        StoredStackKey key = StoredStackKey.of(originalStack);
        if (key == null || originalStack.isEmpty()) {
            return;
        }
        if (action == DepositConflictAction.MOVE_TO_TARGET) {
            PersonalDatabaseService.INSTANCE.resolveDepositConflictMoveEntry(serverPlayer, scope, key, existingTabId, targetTabId);
        }
        String depositTabId = action == DepositConflictAction.KEEP_IN_SOURCE ? existingTabId : targetTabId;
        if (slotIndex == -2) {
            resolveCarriedConflict(serverPlayer, scope, key, depositTabId, action);
        } else if (slotIndex >= 0 && slotIndex < this.menu.slots.size()) {
            resolveSlotConflict(serverPlayer, scope, key, depositTabId, action, slotIndex);
        }
    }

    private void resolveCarriedConflict(ServerPlayer serverPlayer, DatabaseScope scope, StoredStackKey key, String depositTabId, DepositConflictAction action) {
        ItemStack carried = this.menu.getCarried();
        if (carried.isEmpty() || !StoredStackKey.of(carried).equals(key)) {
            if (action == DepositConflictAction.MOVE_TO_TARGET) {
                this.menu.broadcastChanges();
                this.menu.syncAfterScopeMutation(serverPlayer, scope);
            }
            return;
        }
        if (PersonalDatabaseService.INSTANCE.storeStack(serverPlayer, scope, depositTabId, carried.copyAndClear())) {
            this.menu.setCarried(ItemStack.EMPTY);
            this.menu.broadcastChanges();
            this.menu.syncAfterScopeMutation(serverPlayer, scope);
        }
    }

    private void resolveSlotConflict(ServerPlayer serverPlayer, DatabaseScope scope, StoredStackKey key, String depositTabId, DepositConflictAction action, int slotIndex) {
        net.minecraft.world.inventory.Slot slot = this.menu.slots.get(slotIndex);
        if (!slot.hasItem() || !StoredStackKey.of(slot.getItem()).equals(key)) {
            if (action == DepositConflictAction.MOVE_TO_TARGET) {
                this.menu.broadcastChanges();
                this.menu.syncAfterScopeMutation(serverPlayer, scope);
            }
            return;
        }
        if (PersonalDatabaseService.INSTANCE.depositSlot(serverPlayer, scope, depositTabId, slot)) {
            this.menu.broadcastChanges();
            this.menu.syncAfterScopeMutation(serverPlayer, scope);
        }
    }
}
