package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseStatisticsRequestPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DatabaseStatisticsRequestPayload 序列化对称性测试。
 */
class DatabaseStatisticsRequestPayloadTest {

    @Test
    void personalScopePayloadShouldRoundTripThroughStreamCodec() {
        DatabaseStatisticsRequestPayload payload = new DatabaseStatisticsRequestPayload(DatabaseScope.PERSONAL);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DatabaseStatisticsRequestPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseStatisticsRequestPayload restored = DatabaseStatisticsRequestPayload.STREAM_CODEC.decode(buffer);

        assertEquals(DatabaseScope.PERSONAL, restored.scope());
    }

    @Test
    void publicScopePayloadShouldRoundTripThroughStreamCodec() {
        DatabaseStatisticsRequestPayload payload = new DatabaseStatisticsRequestPayload(DatabaseScope.PUBLIC);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DatabaseStatisticsRequestPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseStatisticsRequestPayload restored = DatabaseStatisticsRequestPayload.STREAM_CODEC.decode(buffer);

        assertEquals(DatabaseScope.PUBLIC, restored.scope());
    }
}
