package com.agguy.infiniteinventory.database;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public final class DatabaseSelectionEntry {
    private final String sourceTabId;
    private final ItemStack displayStack;
    private final int hashCode;

    public DatabaseSelectionEntry(String sourceTabId, ItemStack displayStack) {
        this.sourceTabId = DatabaseTabs.normalizeConcreteTarget(sourceTabId);
        this.displayStack = displayStack == null || displayStack.isEmpty()
                ? ItemStack.EMPTY
                : displayStack.copyWithCount(1);
        this.hashCode = 31 * this.sourceTabId.hashCode() + ItemStack.hashItemAndComponents(this.displayStack);
    }

    public String sourceTabId() {
        return this.sourceTabId;
    }

    public ItemStack displayStack() {
        return this.displayStack.copy();
    }

    public boolean isEmpty() {
        return this.displayStack.isEmpty();
    }

    public static DatabaseSelectionEntry read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseSelectionEntry(
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH),
                ItemStack.STREAM_CODEC.decode(buffer)
        );
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(this.sourceTabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
        ItemStack.STREAM_CODEC.encode(buffer, this.displayStack);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DatabaseSelectionEntry entry)) {
            return false;
        }
        return this.sourceTabId.equals(entry.sourceTabId)
                && ItemStack.isSameItemSameComponents(this.displayStack, entry.displayStack);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
}
