package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DatabaseViewerLocalePayload(int containerId, long sessionId, String languageCode) implements CustomPacketPayload {
    private static final int MAX_LANGUAGE_CODE_LENGTH = 16;

    public static final Type<DatabaseViewerLocalePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "database_viewer_locale")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseViewerLocalePayload> STREAM_CODEC = StreamCodec.of(
            DatabaseViewerLocalePayload::write,
            DatabaseViewerLocalePayload::read
    );

    public DatabaseViewerLocalePayload {
        languageCode = languageCode == null ? "" : languageCode.trim();
    }

    @Override
    public Type<DatabaseViewerLocalePayload> type() {
        return TYPE;
    }

    private static DatabaseViewerLocalePayload read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseViewerLocalePayload(
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readUtf(MAX_LANGUAGE_CODE_LENGTH)
        );
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseViewerLocalePayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarLong(payload.sessionId);
        buffer.writeUtf(payload.languageCode, MAX_LANGUAGE_CODE_LENGTH);
    }
}
