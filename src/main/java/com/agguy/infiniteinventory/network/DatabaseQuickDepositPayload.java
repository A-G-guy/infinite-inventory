package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record DatabaseQuickDepositPayload(
        int containerId,
        long sessionId,
        int slotIndex,
        @Nullable DatabaseScope targetScope,
        String targetTabId
) implements CustomPacketPayload {
    public static final Type<DatabaseQuickDepositPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "database_quick_deposit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseQuickDepositPayload> STREAM_CODEC = StreamCodec.of(
            DatabaseQuickDepositPayload::write,
            DatabaseQuickDepositPayload::read
    );

    @Override
    public Type<DatabaseQuickDepositPayload> type() {
        return TYPE;
    }

    private static DatabaseQuickDepositPayload read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseQuickDepositPayload(
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readVarInt(),
                buffer.readBoolean() ? buffer.readEnum(DatabaseScope.class) : null,
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH)
        );
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseQuickDepositPayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarLong(payload.sessionId);
        buffer.writeVarInt(payload.slotIndex);
        buffer.writeBoolean(payload.targetScope != null);
        if (payload.targetScope != null) {
            buffer.writeEnum(payload.targetScope);
        }
        buffer.writeUtf(payload.targetTabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
    }
}
