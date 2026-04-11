package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DepositAllPayload(int containerId) implements CustomPacketPayload {
    public static final Type<DepositAllPayload> TYPE = CustomPacketPayload.createType(InfiniteInventory.MODID + ":deposit_all");
    public static final StreamCodec<RegistryFriendlyByteBuf, DepositAllPayload> STREAM_CODEC = StreamCodec.of(
            DepositAllPayload::write,
            DepositAllPayload::read
    );

    @Override
    public Type<DepositAllPayload> type() {
        return TYPE;
    }

    private static DepositAllPayload read(RegistryFriendlyByteBuf buffer) {
        return new DepositAllPayload(buffer.readVarInt());
    }

    private static void write(RegistryFriendlyByteBuf buffer, DepositAllPayload payload) {
        buffer.writeVarInt(payload.containerId);
    }
}
