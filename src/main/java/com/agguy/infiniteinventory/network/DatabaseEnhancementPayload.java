package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DatabaseEnhancementPayload(int containerId, long sessionId, DatabaseEnhancementConfig enhancementConfig) implements CustomPacketPayload {
    public static final Type<DatabaseEnhancementPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "database_enhancement"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseEnhancementPayload> STREAM_CODEC = StreamCodec.of(
            DatabaseEnhancementPayload::write,
            DatabaseEnhancementPayload::read
    );

    @Override
    public Type<DatabaseEnhancementPayload> type() {
        return TYPE;
    }

    private static DatabaseEnhancementPayload read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseEnhancementPayload(buffer.readVarInt(), buffer.readVarLong(), DatabaseEnhancementConfig.read(buffer));
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseEnhancementPayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarLong(payload.sessionId);
        DatabaseEnhancementConfig.write(buffer, payload.enhancementConfig);
    }
}
