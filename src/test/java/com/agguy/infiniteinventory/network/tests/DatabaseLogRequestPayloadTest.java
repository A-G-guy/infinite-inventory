package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseLogRequestPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DatabaseLogRequestPayload 序列化对称性测试。
 */
class DatabaseLogRequestPayloadTest {

    @Test
    void personalScopePayloadShouldRoundTripThroughStreamCodec() {
        DatabaseLogRequestPayload payload = new DatabaseLogRequestPayload(DatabaseScope.PERSONAL);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DatabaseLogRequestPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseLogRequestPayload restored = DatabaseLogRequestPayload.STREAM_CODEC.decode(buffer);

        assertEquals(DatabaseScope.PERSONAL, restored.scope());
    }

    @Test
    void publicScopePayloadShouldRoundTripThroughStreamCodec() {
        DatabaseLogRequestPayload payload = new DatabaseLogRequestPayload(DatabaseScope.PUBLIC);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DatabaseLogRequestPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseLogRequestPayload restored = DatabaseLogRequestPayload.STREAM_CODEC.decode(buffer);

        assertEquals(DatabaseScope.PUBLIC, restored.scope());
    }
}
