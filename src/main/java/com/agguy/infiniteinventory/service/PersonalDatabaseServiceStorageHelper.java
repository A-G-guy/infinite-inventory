package com.agguy.infiniteinventory.service;

import net.minecraft.world.item.ItemStack;

final class PersonalDatabaseServiceStorageHelper {
    private PersonalDatabaseServiceStorageHelper() {
    }

    static long safeAddMovedItems(long currentTotal, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return currentTotal;
        }
        int stackCount = stack.getCount();
        if (stackCount <= 0) {
            return currentTotal;
        }
        if (Long.MAX_VALUE - currentTotal < stackCount) {
            return Long.MAX_VALUE;
        }
        return currentTotal + stackCount;
    }

    static boolean isPrimaryStorageSlot(int slotIndex) {
        return slotIndex >= PersonalDatabaseService.HOTBAR_SLOT_COUNT;
    }
}
