package com.agguy.infiniteinventory.network;

public enum DatabaseClickAction {
    STORE_STACK,
    STORE_SINGLE,
    TAKE_SINGLE,
    TAKE_STACK,
    TAKE_STACK_TO_INVENTORY,
    TAKE_HALF_STACK_TO_INVENTORY,
    TAKE_HALF_ENTRY_TO_INVENTORY,
    TAKE_ALL,
    DROP_SINGLE;

    public boolean isStoreAction() {
        return this == STORE_STACK || this == STORE_SINGLE;
    }

    public boolean storesSingleItem() {
        return this == STORE_SINGLE;
    }

    public boolean extractsToInventory() {
        return this == TAKE_STACK_TO_INVENTORY
                || this == TAKE_HALF_ENTRY_TO_INVENTORY
                || this == TAKE_ALL;
    }

    public boolean extractsEntireEntry() {
        return this == TAKE_ALL;
    }

    public boolean dropsToWorld() {
        return this == DROP_SINGLE;
    }

    public long resolveRequestedAmount(long entryAmount, int maxStackSize) {
        long safeMaxStackSize = Math.max(1L, maxStackSize);
        return switch (this) {
            case TAKE_SINGLE -> 1L;
            case TAKE_STACK, TAKE_STACK_TO_INVENTORY -> safeMaxStackSize;
            case TAKE_HALF_STACK_TO_INVENTORY -> halfRoundedUp(safeMaxStackSize);
            case TAKE_HALF_ENTRY_TO_INVENTORY -> halfRoundedUp(Math.max(1L, entryAmount));
            case TAKE_ALL -> Math.max(1L, entryAmount);
            case DROP_SINGLE -> 1L;
            default -> 0L;
        };
    }

    private static long halfRoundedUp(long value) {
        if (value <= 1L) {
            return 1L;
        }
        return value / 2L + value % 2L;
    }
}
