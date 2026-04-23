package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseTabMutationAction;
import com.agguy.infiniteinventory.network.DatabaseTabMutationPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * DatabaseTabMutationPayload 序列化对称性测试。
 */
class DatabaseTabMutationPayloadTest {

    @Test
    void explicitTargetScopeShouldRoundTripThroughStreamCodec() {
        DatabaseTabMutationPayload payload = new DatabaseTabMutationPayload(
                4,
                9L,
                DatabaseScope.PERSONAL,
                DatabaseScope.PUBLIC,
                DatabaseTabMutationAction.TRANSFER,
                "source_tab",
                "target_tab",
                "",
                ""
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseTabMutationPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseTabMutationPayload restored = DatabaseTabMutationPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload, restored);
        assertEquals(DatabaseScope.PUBLIC, restored.resolvedTargetScope());
    }

    @Test
    void legacyConstructorShouldKeepTargetScopeEmptyAndFallbackToSourceScope() {
        DatabaseTabMutationPayload payload = new DatabaseTabMutationPayload(
                4,
                9L,
                DatabaseScope.PUBLIC,
                DatabaseTabMutationAction.TRANSFER,
                "source_tab",
                "target_tab",
                "",
                ""
        );

        assertNull(payload.targetScope());
        assertEquals(DatabaseScope.PUBLIC, payload.resolvedTargetScope());
    }

    @Test
    void payloadWithAllFieldsShouldRoundTripThroughStreamCodec() {
        DatabaseTabMutationPayload payload = new DatabaseTabMutationPayload(
                42,
                123456789L,
                DatabaseScope.PERSONAL,
                DatabaseScope.PERSONAL,
                DatabaseTabMutationAction.RENAME,
                "old_tab",
                "new_tab",
                "New Tab Name",
                "minecraft:diamond"
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseTabMutationPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseTabMutationPayload restored = DatabaseTabMutationPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload.containerId(), restored.containerId());
        assertEquals(payload.sessionId(), restored.sessionId());
        assertEquals(payload.scope(), restored.scope());
        assertEquals(payload.targetScope(), restored.targetScope());
        assertEquals(payload.action(), restored.action());
        assertEquals(payload.tabId(), restored.tabId());
        assertEquals(payload.targetTabId(), restored.targetTabId());
        assertEquals(payload.name(), restored.name());
        assertEquals(payload.iconItemId(), restored.iconItemId());
    }

    @Test
    void nullTargetScopeShouldRoundTripAndFallbackToSourceScope() {
        DatabaseTabMutationPayload payload = new DatabaseTabMutationPayload(
                1,
                2L,
                DatabaseScope.PUBLIC,
                null,
                DatabaseTabMutationAction.DELETE,
                "tab_to_delete",
                "",
                "",
                ""
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseTabMutationPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseTabMutationPayload restored = DatabaseTabMutationPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload, restored);
        assertNull(restored.targetScope());
        assertEquals(DatabaseScope.PUBLIC, restored.resolvedTargetScope());
    }

    @Test
    void compactConstructorShouldNormalizeNullFields() {
        DatabaseTabMutationPayload payload = new DatabaseTabMutationPayload(
                1,
                1L,
                null,
                null,
                DatabaseTabMutationAction.ADD,
                null,
                null,
                null,
                null
        );

        assertEquals(DatabaseScope.PERSONAL, payload.scope());
        assertNull(payload.targetScope());
        assertEquals("", payload.tabId());
        assertEquals("", payload.targetTabId());
        assertEquals("", payload.name());
        assertEquals("", payload.iconItemId());
    }
}
