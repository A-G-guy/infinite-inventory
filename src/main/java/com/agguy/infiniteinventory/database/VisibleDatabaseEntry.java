package com.agguy.infiniteinventory.database;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record VisibleDatabaseEntry(DatabaseScope scope, ItemStack stack, long amount, String tabId, String registryName, String note, boolean starred) {
    public VisibleDatabaseEntry {
        scope = DatabaseScope.normalize(scope);
        stack = stack.copyWithCount(1);
        amount = Math.max(0L, amount);
        tabId = DatabaseTabs.normalizeConcreteTarget(tabId);
        registryName = registryName == null ? "" : registryName;
        note = note == null ? "" : note;
    }

    public VisibleDatabaseEntry(DatabaseScope scope, ItemStack stack, long amount, String tabId, String registryName) {
        this(scope, stack, amount, tabId, registryName, "", false);
    }

    public VisibleDatabaseEntry(DatabaseScope scope, ItemStack stack, long amount, String tabId, String registryName, String note) {
        this(scope, stack, amount, tabId, registryName, note, false);
    }

    public DatabaseScopedTabRef scopedTab() {
        return DatabaseScopedTabRef.concreteTab(this.scope, this.tabId);
    }

    public static VisibleDatabaseEntry read(RegistryFriendlyByteBuf buffer) {
        return new VisibleDatabaseEntry(
                buffer.readEnum(DatabaseScope.class),
                ItemStack.STREAM_CODEC.decode(buffer),
                buffer.readVarLong(),
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH),
                buffer.readUtf(128),
                buffer.readUtf(256),
                buffer.readBoolean()
        );
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.scope);
        ItemStack.STREAM_CODEC.encode(buffer, this.stack);
        buffer.writeVarLong(this.amount);
        buffer.writeUtf(this.tabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
        buffer.writeUtf(this.registryName, 128);
        buffer.writeUtf(this.note, 256);
        buffer.writeBoolean(this.starred);
    }
}
