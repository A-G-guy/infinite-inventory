package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenEquippedDatabasePayload() implements CustomPacketPayload {
    public static final Type<OpenEquippedDatabasePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "open_equipped_database"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenEquippedDatabasePayload> STREAM_CODEC = StreamCodec.of(
            OpenEquippedDatabasePayload::write,
            OpenEquippedDatabasePayload::read
    );

    @Override
    public Type<OpenEquippedDatabasePayload> type() {
        return TYPE;
    }

    private static OpenEquippedDatabasePayload read(RegistryFriendlyByteBuf buffer) {
        return new OpenEquippedDatabasePayload();
    }

    private static void write(RegistryFriendlyByteBuf buffer, OpenEquippedDatabasePayload payload) {
    }
}
