package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DepositExistingByTabPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DepositExistingByTabPayload 序列化对称性测试。
 */
class DepositExistingByTabPayloadTest {

    @Test
    void payloadWithTargetScopeShouldRoundTripThroughStreamCodec() {
        DepositExistingByTabPayload payload = new DepositExistingByTabPayload(1, 100L, DatabaseScope.PUBLIC);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DepositExistingByTabPayload.STREAM_CODEC.encode(buffer, payload);
        DepositExistingByTabPayload restored = DepositExistingByTabPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload.containerId(), restored.containerId());
        assertEquals(payload.sessionId(), restored.sessionId());
        assertEquals(payload.targetScope(), restored.targetScope());
    }

    @Test
    void payloadWithNullTargetScopeShouldRoundTripThroughStreamCodec() {
        DepositExistingByTabPayload payload = new DepositExistingByTabPayload(42, 999L, null);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DepositExistingByTabPayload.STREAM_CODEC.encode(buffer, payload);
        DepositExistingByTabPayload restored = DepositExistingByTabPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload.containerId(), restored.containerId());
        assertEquals(payload.sessionId(), restored.sessionId());
        assertNull(restored.targetScope());
    }
}
