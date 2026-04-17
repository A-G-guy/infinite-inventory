package com.agguy.infiniteinventory.database;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public final class DatabaseSelectionEntry {
    private final DatabaseScope scope;
    private final String sourceTabId;
    private final ItemStack displayStack;
    private final int hashCode;

    public DatabaseSelectionEntry(DatabaseScope scope, String sourceTabId, ItemStack displayStack) {
        this.scope = DatabaseScope.normalize(scope);
        this.sourceTabId = DatabaseTabs.normalizeConcreteTarget(sourceTabId);
        this.displayStack = displayStack == null || displayStack.isEmpty()
                ? ItemStack.EMPTY
                : displayStack.copyWithCount(1);
        int calculatedHashCode = 31 * this.scope.hashCode() + this.sourceTabId.hashCode();
        this.hashCode = 31 * calculatedHashCode + ItemStack.hashItemAndComponents(this.displayStack);
    }

    public DatabaseScope scope() {
        return this.scope;
    }

    public String sourceTabId() {
        return this.sourceTabId;
    }

    public DatabaseScopedTabRef scopedTab() {
        return DatabaseScopedTabRef.concreteTab(this.scope, this.sourceTabId);
    }

    public ItemStack displayStack() {
        return this.displayStack.copy();
    }

    public boolean isEmpty() {
        return this.displayStack.isEmpty();
    }

    public static DatabaseSelectionEntry read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseSelectionEntry(
                buffer.readEnum(DatabaseScope.class),
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH),
                ItemStack.STREAM_CODEC.decode(buffer)
        );
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.scope);
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
        return this.scope == entry.scope
                && this.sourceTabId.equals(entry.sourceTabId)
                && ItemStack.isSameItemSameComponents(this.displayStack, entry.displayStack);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
}
