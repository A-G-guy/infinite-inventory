package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseClickPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * DatabaseClickPayload 序列化对称性测试。
 */
class DatabaseClickPayloadTest {

    @Test
    void fullPayloadWithTargetScopeShouldRoundTripThroughStreamCodec() {
        DatabaseClickPayload payload = new DatabaseClickPayload(
                42,
                123456789L,
                3,
                7,
                DatabaseClickAction.TAKE_STACK_TO_INVENTORY,
                DatabaseScope.PUBLIC,
                "target_tab"
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseClickPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseClickPayload restored = DatabaseClickPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload, restored);
        assertEquals(DatabaseScope.PUBLIC, restored.targetScope());
        assertEquals(DatabaseClickAction.TAKE_STACK_TO_INVENTORY, restored.action());
    }

    @Test
    void payloadWithNullTargetScopeShouldRoundTripThroughStreamCodec() {
        DatabaseClickPayload payload = new DatabaseClickPayload(
                1,
                2L,
                0,
                0,
                DatabaseClickAction.STORE_STACK,
                null,
                ""
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseClickPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseClickPayload restored = DatabaseClickPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload, restored);
        assertNull(restored.targetScope());
        assertEquals("", restored.targetTabId());
    }

    @Test
    void payloadWithStoreSingleActionShouldRoundTripCorrectly() {
        DatabaseClickPayload payload = new DatabaseClickPayload(
                99,
                9876543210L,
                5,
                12,
                DatabaseClickAction.STORE_SINGLE,
                DatabaseScope.PERSONAL,
                "personal_gear"
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseClickPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseClickPayload restored = DatabaseClickPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload.containerId(), restored.containerId());
        assertEquals(payload.sessionId(), restored.sessionId());
        assertEquals(payload.panelIndex(), restored.panelIndex());
        assertEquals(payload.pageSlotIndex(), restored.pageSlotIndex());
        assertEquals(payload.action(), restored.action());
        assertEquals(payload.targetScope(), restored.targetScope());
        assertEquals(payload.targetTabId(), restored.targetTabId());
    }
}
