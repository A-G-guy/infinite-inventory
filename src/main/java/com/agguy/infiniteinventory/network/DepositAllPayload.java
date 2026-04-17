package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record DepositAllPayload(
        int containerId,
        long sessionId,
        @Nullable DatabaseScope targetScope,
        String targetTabId
) implements CustomPacketPayload {
    public static final Type<DepositAllPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "deposit_all"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DepositAllPayload> STREAM_CODEC = StreamCodec.of(
            DepositAllPayload::write,
            DepositAllPayload::read
    );

    @Override
    public Type<DepositAllPayload> type() {
        return TYPE;
    }

    private static DepositAllPayload read(RegistryFriendlyByteBuf buffer) {
        return new DepositAllPayload(
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readBoolean() ? buffer.readEnum(DatabaseScope.class) : null,
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH)
        );
    }

    private static void write(RegistryFriendlyByteBuf buffer, DepositAllPayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarLong(payload.sessionId);
        buffer.writeBoolean(payload.targetScope != null);
        if (payload.targetScope != null) {
            buffer.writeEnum(payload.targetScope);
        }
        buffer.writeUtf(payload.targetTabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
    }
}
