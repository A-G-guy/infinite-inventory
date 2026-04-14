package com.agguy.infiniteinventory.database;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record VisibleDatabaseEntry(ItemStack stack, long amount, String tabId, String registryName) {
    public VisibleDatabaseEntry {
        stack = stack.copyWithCount(1);
        amount = Math.max(0L, amount);
        tabId = DatabaseTabs.normalizeConcreteTarget(tabId);
        registryName = registryName == null ? "" : registryName;
    }

    public static VisibleDatabaseEntry read(RegistryFriendlyByteBuf buffer) {
        return new VisibleDatabaseEntry(
                ItemStack.STREAM_CODEC.decode(buffer),
                buffer.readVarLong(),
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH),
                buffer.readUtf(128)
        );
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        ItemStack.STREAM_CODEC.encode(buffer, this.stack);
        buffer.writeVarLong(this.amount);
        buffer.writeUtf(this.tabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
        buffer.writeUtf(this.registryName, 128);
    }
}
