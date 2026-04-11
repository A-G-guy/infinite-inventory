package com.agguy.infiniteinventory.network;

public enum DatabaseClickAction {
    STORE_STACK,
    STORE_SINGLE,
    TAKE_SINGLE,
    TAKE_STACK,
    TAKE_STACK_TO_INVENTORY,
    TAKE_ALL;

    public boolean isStoreAction() {
        return this == STORE_STACK || this == STORE_SINGLE;
    }

    public boolean storesSingleItem() {
        return this == STORE_SINGLE;
    }

    public boolean extractsToInventory() {
        return this == TAKE_STACK_TO_INVENTORY || this == TAKE_ALL;
    }

    public boolean extractsEntireEntry() {
        return this == TAKE_ALL;
    }

    public int resolveRequestedAmount(int maxStackSize) {
        return switch (this) {
            case TAKE_SINGLE -> 1;
            case TAKE_STACK, TAKE_STACK_TO_INVENTORY -> Math.max(1, maxStackSize);
            default -> 0;
        };
    }
}
