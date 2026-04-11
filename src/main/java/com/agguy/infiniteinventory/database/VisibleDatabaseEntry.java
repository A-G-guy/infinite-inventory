package com.agguy.infiniteinventory.database;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record VisibleDatabaseEntry(ItemStack stack, long amount, DatabaseCategory category, String registryName) {
    public VisibleDatabaseEntry {
        stack = stack.copyWithCount(1);
        amount = Math.max(0L, amount);
        category = category == null ? DatabaseCategory.OTHER : category;
        registryName = registryName == null ? "" : registryName;
    }

    public static VisibleDatabaseEntry read(RegistryFriendlyByteBuf buffer) {
        return new VisibleDatabaseEntry(
                ItemStack.STREAM_CODEC.decode(buffer),
                buffer.readVarLong(),
                buffer.readEnum(DatabaseCategory.class),
                buffer.readUtf(128)
        );
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        ItemStack.STREAM_CODEC.encode(buffer, this.stack);
        buffer.writeVarLong(this.amount);
        buffer.writeEnum(this.category);
        buffer.writeUtf(this.registryName, 128);
    }
}
