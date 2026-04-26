package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import com.agguy.infiniteinventory.service.PersonalDatabaseServiceDepositHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 快捷移动（Shift+单击）逻辑的委托辅助类。
 *
 * <p>将 {@link PersonalDatabaseMenu#quickMoveStack} 及其辅助方法抽取至此，
 * 控制主类规模。
 */
final class PersonalDatabaseMenuQuickMoveHelper {

    private final PersonalDatabaseMenu menu;

    PersonalDatabaseMenuQuickMoveHelper(PersonalDatabaseMenu menu) {
        this.menu = menu;
    }

    ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.menu.slots.size()) {
            return ItemStack.EMPTY;
        }
        net.minecraft.world.inventory.Slot slot = this.menu.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack rawStack = slot.getItem();
        ItemStack copy = rawStack.copy();

        DatabaseScopedTabRef quickMoveTargetTab = this.menu.resolveSingleStoreTarget();
        if (this.menu.shouldDepositQuickMovedSlot(slotIndex)
                && player instanceof ServerPlayer serverPlayer
                && quickMoveTargetTab != null) {
            PersonalDatabaseServiceDepositHelper.DepositConflict conflict = PersonalDatabaseService.INSTANCE.checkDepositConflict(
                    serverPlayer, quickMoveTargetTab.scope(), quickMoveTargetTab.tabId(), rawStack
            );
            if (conflict != null) {
                this.menu.sendDepositConflict(conflict.scope(), conflict.targetTabId(), conflict.existingTabId(), conflict.stack(), slotIndex);
                return copy;
            }
            if (PersonalDatabaseService.INSTANCE.depositSlot(
                    serverPlayer,
                    quickMoveTargetTab.scope(),
                    quickMoveTargetTab.tabId(),
                    slot
            )) {
                this.menu.broadcastChanges();
                this.menu.syncAfterScopeMutation(serverPlayer, quickMoveTargetTab.scope());
                return copy;
            }
        }

        boolean moved = false;
        if (slotIndex == this.menu.resultSlotIndex) {
            moved = this.handleCraftingSlotMove(player, slot, rawStack, copy);
        } else if (this.menu.craftingSlotRange.contains(slotIndex)
                || this.menu.armorSlotRange.contains(slotIndex)
                || slotIndex == this.menu.offhandSlotIndex
                || this.menu.accessorySlotRange.contains(slotIndex)) {
            moved = this.handleArmorSlotMove(rawStack);
        } else if (this.menu.mainInventorySlotRange.contains(slotIndex) || this.menu.hotbarSlotRange.contains(slotIndex)) {
            moved = this.handleInventorySlotMove(slotIndex, rawStack, player);
        } else {
            moved = this.menu.moveToPlayerStorage(rawStack, false);
        }

        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (rawStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, copy);
        } else {
            slot.setChanged();
        }
        if (rawStack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, rawStack);
        return copy;
    }

    private boolean handleCraftingSlotMove(Player player, net.minecraft.world.inventory.Slot slot, ItemStack rawStack, ItemStack copy) {
        boolean moved = this.menu.moveToPlayerStorage(rawStack, true);
        if (moved) {
            slot.onQuickCraft(rawStack, copy);
        }
        return moved;
    }

    private boolean handleArmorSlotMove(ItemStack rawStack) {
        return this.menu.moveToPlayerStorage(rawStack, false);
    }

    private boolean handleInventorySlotMove(int slotIndex, ItemStack rawStack, Player player) {
        boolean moved = this.menu.tryMoveToAccessorySlots(rawStack);
        EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(rawStack);
        if (!rawStack.isEmpty() && equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            int armorSlotOffset = PersonalDatabaseMenuSupport.armorSlotOffset(equipmentSlot);
            int armorSlotIndex = this.menu.armorSlotRange.firstIndex() + armorSlotOffset;
            if (armorSlotOffset >= 0 && !this.menu.slots.get(armorSlotIndex).hasItem()) {
                moved = this.menu.invokeMoveItemStackTo(rawStack, armorSlotIndex, armorSlotIndex + 1, false) || moved;
            }
        } else if (!rawStack.isEmpty() && equipmentSlot == EquipmentSlot.OFFHAND && !this.menu.slots.get(this.menu.offhandSlotIndex).hasItem()) {
            moved = this.menu.invokeMoveItemStackTo(rawStack, this.menu.offhandSlotIndex, this.menu.offhandSlotIndex + 1, false) || moved;
        }
        if (!rawStack.isEmpty() && this.menu.mainInventorySlotRange.contains(slotIndex)) {
            moved = this.menu.invokeMoveItemStackTo(rawStack, this.menu.hotbarSlotRange.firstIndex(), this.menu.hotbarSlotRange.lastIndexExclusive(), false) || moved;
        } else if (!rawStack.isEmpty() && this.menu.hotbarSlotRange.contains(slotIndex)) {
            moved = this.menu.invokeMoveItemStackTo(rawStack, this.menu.mainInventorySlotRange.firstIndex(), this.menu.mainInventorySlotRange.lastIndexExclusive(), false) || moved;
        }
        return moved;
    }
}
