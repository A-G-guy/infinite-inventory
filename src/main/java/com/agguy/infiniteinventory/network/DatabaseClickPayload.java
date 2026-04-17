package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record DatabaseClickPayload(
        int containerId,
        long sessionId,
        int panelIndex,
        int pageSlotIndex,
        DatabaseClickAction action,
        @Nullable DatabaseScope targetScope,
        String targetTabId
) implements CustomPacketPayload {
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
        return new DatabaseClickPayload(
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readEnum(DatabaseClickAction.class),
                buffer.readBoolean() ? buffer.readEnum(DatabaseScope.class) : null,
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH)
        );
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseClickPayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarLong(payload.sessionId);
        buffer.writeVarInt(payload.panelIndex);
        buffer.writeVarInt(payload.pageSlotIndex);
        buffer.writeEnum(payload.action);
        buffer.writeBoolean(payload.targetScope != null);
        if (payload.targetScope != null) {
            buffer.writeEnum(payload.targetScope);
        }
        buffer.writeUtf(payload.targetTabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
    }
}
