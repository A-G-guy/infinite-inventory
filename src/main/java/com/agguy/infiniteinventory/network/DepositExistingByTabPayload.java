package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseScope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record DepositExistingByTabPayload(
        int containerId,
        long sessionId,
        @Nullable DatabaseScope targetScope
) implements CustomPacketPayload {
    public static final Type<DepositExistingByTabPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "deposit_existing_by_tab")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DepositExistingByTabPayload> STREAM_CODEC = StreamCodec.of(
            DepositExistingByTabPayload::write,
            DepositExistingByTabPayload::read
    );

    @Override
    public Type<DepositExistingByTabPayload> type() {
        return TYPE;
    }

    private static DepositExistingByTabPayload read(RegistryFriendlyByteBuf buffer) {
        return new DepositExistingByTabPayload(
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readBoolean() ? buffer.readEnum(DatabaseScope.class) : null
        );
    }

    private static void write(RegistryFriendlyByteBuf buffer, DepositExistingByTabPayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarLong(payload.sessionId);
        buffer.writeBoolean(payload.targetScope != null);
        if (payload.targetScope != null) {
            buffer.writeEnum(payload.targetScope);
        }
    }
}
