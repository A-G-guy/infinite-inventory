package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DatabaseTabMutationPayload(
        int containerId,
        long sessionId,
        DatabaseScope scope,
        DatabaseTabMutationAction action,
        String tabId,
        String targetTabId,
        String name,
        String iconItemId
) implements CustomPacketPayload {
    public static final Type<DatabaseTabMutationPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "database_tab_mutation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseTabMutationPayload> STREAM_CODEC = StreamCodec.of(
            DatabaseTabMutationPayload::write,
            DatabaseTabMutationPayload::read
    );

    @Override
    public Type<DatabaseTabMutationPayload> type() {
        return TYPE;
    }

    private static DatabaseTabMutationPayload read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseTabMutationPayload(
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readEnum(DatabaseScope.class),
                buffer.readEnum(DatabaseTabMutationAction.class),
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH),
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH),
                buffer.readUtf(128),
                buffer.readUtf(128)
        );
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseTabMutationPayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarLong(payload.sessionId);
        buffer.writeEnum(payload.scope);
        buffer.writeEnum(payload.action);
        buffer.writeUtf(payload.tabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
        buffer.writeUtf(payload.targetTabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
        buffer.writeUtf(payload.name, 128);
        buffer.writeUtf(payload.iconItemId, 128);
    }
}
