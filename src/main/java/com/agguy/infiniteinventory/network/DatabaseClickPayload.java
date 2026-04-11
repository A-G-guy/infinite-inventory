package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DatabaseClickPayload(int containerId, int pageSlotIndex, DatabaseClickAction action) implements CustomPacketPayload {
    public static final Type<DatabaseClickPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "database_click"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseClickPayload> STREAM_CODEC = StreamCodec.of(
            DatabaseClickPayload::write,
            DatabaseClickPayload::read
    );

    @Override
    public Type<DatabaseClickPayload> type() {
        return TYPE;
    }

    private static DatabaseClickPayload read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseClickPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readEnum(DatabaseClickAction.class));
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseClickPayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarInt(payload.pageSlotIndex);
        buffer.writeEnum(payload.action);
    }
}
