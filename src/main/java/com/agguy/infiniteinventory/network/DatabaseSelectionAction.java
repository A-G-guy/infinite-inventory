package com.agguy.infiniteinventory.network;

public enum DatabaseSelectionAction {
    EXTRACT_ONE_TO_INVENTORY,
    EXTRACT_HALF_STACK_TO_INVENTORY,
    EXTRACT_STACK_TO_INVENTORY,
    EXTRACT_ALL_TO_INVENTORY,
    TRANSFER_TO_TAB,
    EXTRACT_HALF_ENTRY_TO_INVENTORY,
    EXTRACT_CUSTOM_TO_INVENTORY;

    public boolean requiresTargetTab() {
        return this == TRANSFER_TO_TAB;
    }

    public boolean extractsToInventory() {
        return !this.requiresTargetTab();
    }

    public long resolveRequestedAmount(long entryAmount, int maxStackSize) {
        return this.resolveRequestedAmount(entryAmount, maxStackSize, 0L);
    }

    public long resolveRequestedAmount(long entryAmount, int maxStackSize, long requestedAmount) {
        long safeMaxStackSize = Math.max(1L, maxStackSize);
        return switch (this) {
            case EXTRACT_ONE_TO_INVENTORY -> 1L;
            case EXTRACT_HALF_STACK_TO_INVENTORY -> halfRoundedUp(safeMaxStackSize);
            case EXTRACT_STACK_TO_INVENTORY -> safeMaxStackSize;
            case EXTRACT_HALF_ENTRY_TO_INVENTORY -> halfRoundedUp(Math.max(1L, entryAmount));
            case EXTRACT_ALL_TO_INVENTORY -> Math.max(1L, entryAmount);
            case EXTRACT_CUSTOM_TO_INVENTORY -> Math.max(0L, requestedAmount);
            case TRANSFER_TO_TAB -> 0L;
        };
    }

    public long normalizeRequestedAmount(long requestedAmount) {
        return this == EXTRACT_CUSTOM_TO_INVENTORY ? Math.max(0L, requestedAmount) : 0L;
    }

    private static long halfRoundedUp(long value) {
        if (value <= 1L) {
            return 1L;
        }
        return value / 2L + value % 2L;
    }
}
