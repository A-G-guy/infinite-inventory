package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseScope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端向服务端请求统计数据 (C2S)。
 * 复用日志请求的模式：仅携带 scope。
 */
public record DatabaseStatisticsRequestPayload(DatabaseScope scope) implements CustomPacketPayload {
    public static final Type<DatabaseStatisticsRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "database_statistics_request")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseStatisticsRequestPayload> STREAM_CODEC =
            StreamCodec.of(DatabaseStatisticsRequestPayload::write, DatabaseStatisticsRequestPayload::read);

    @Override
    public Type<DatabaseStatisticsRequestPayload> type() {
        return TYPE;
    }

    private static DatabaseStatisticsRequestPayload read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseStatisticsRequestPayload(DatabaseScope.read(buffer.readUtf(), DatabaseScope.PERSONAL));
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseStatisticsRequestPayload payload) {
        buffer.writeUtf(payload.scope.name());
    }
}
