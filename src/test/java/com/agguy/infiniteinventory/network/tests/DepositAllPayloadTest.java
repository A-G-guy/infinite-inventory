package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DepositAllPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DepositAllPayload 序列化对称性测试。
 */
class DepositAllPayloadTest {

    @Test
    void payloadWithTargetScopeShouldRoundTripThroughStreamCodec() {
        DepositAllPayload payload = new DepositAllPayload(1, 100L, DatabaseScope.PUBLIC, "target_tab");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DepositAllPayload.STREAM_CODEC.encode(buffer, payload);
        DepositAllPayload restored = DepositAllPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload.containerId(), restored.containerId());
        assertEquals(payload.sessionId(), restored.sessionId());
        assertEquals(payload.targetScope(), restored.targetScope());
        assertEquals(payload.targetTabId(), restored.targetTabId());
    }

    @Test
    void payloadWithNullTargetScopeShouldRoundTripThroughStreamCodec() {
        DepositAllPayload payload = new DepositAllPayload(42, 999L, null, "");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DepositAllPayload.STREAM_CODEC.encode(buffer, payload);
        DepositAllPayload restored = DepositAllPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload.containerId(), restored.containerId());
        assertNull(restored.targetScope());
        assertEquals("", restored.targetTabId());
    }
}
