package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseScope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DatabaseLogRequestPayload(DatabaseScope scope) implements CustomPacketPayload {
    public static final Type<DatabaseLogRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "database_log_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseLogRequestPayload> STREAM_CODEC = StreamCodec.of(
            DatabaseLogRequestPayload::write,
            DatabaseLogRequestPayload::read
    );

    @Override
    public Type<DatabaseLogRequestPayload> type() {
        return TYPE;
    }

    private static DatabaseLogRequestPayload read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseLogRequestPayload(DatabaseScope.read(buffer.readUtf(), DatabaseScope.PERSONAL));
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseLogRequestPayload payload) {
        buffer.writeUtf(payload.scope.name());
    }
}
