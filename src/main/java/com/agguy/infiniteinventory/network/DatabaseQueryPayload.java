package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DatabaseQueryPayload(int containerId, long sessionId, DatabaseQuery query) implements CustomPacketPayload {
    public static final Type<DatabaseQueryPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "database_query"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseQueryPayload> STREAM_CODEC = StreamCodec.of(
            DatabaseQueryPayload::write,
            DatabaseQueryPayload::read
    );

    @Override
    public Type<DatabaseQueryPayload> type() {
        return TYPE;
    }

    private static DatabaseQueryPayload read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseQueryPayload(buffer.readVarInt(), buffer.readVarLong(), DatabaseQuery.read(buffer));
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseQueryPayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarLong(payload.sessionId);
        DatabaseQuery.write(buffer, payload.query);
    }
}
