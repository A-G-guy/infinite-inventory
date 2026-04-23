package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseQueryPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DatabaseQueryPayload 序列化对称性测试。
 */
class DatabaseQueryPayloadTest {

    @Test
    void defaultQueryShouldRoundTripThroughStreamCodec() {
        DatabaseQueryPayload payload = new DatabaseQueryPayload(
                7,
                42L,
                DatabaseQuery.defaultQuery(DatabaseScope.PERSONAL)
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseQueryPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseQueryPayload restored = DatabaseQueryPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload, restored);
        assertEquals(payload.query().focusedTab(), restored.query().focusedTab());
        assertEquals(payload.query().visibleTabs(), restored.query().visibleTabs());
    }

    @Test
    void publicScopeQueryShouldRoundTripThroughStreamCodec() {
        DatabaseQueryPayload payload = new DatabaseQueryPayload(
                3,
                99L,
                DatabaseQuery.defaultQuery(DatabaseScope.PUBLIC)
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseQueryPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseQueryPayload restored = DatabaseQueryPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload.containerId(), restored.containerId());
        assertEquals(payload.sessionId(), restored.sessionId());
        assertEquals(DatabaseScope.PUBLIC, restored.query().scope());
    }

    @Test
    void queryWithCustomTabStateShouldRoundTripCorrectly() {
        DatabaseQuery query = DatabaseQuery.defaultQuery(DatabaseScope.PERSONAL)
                .withSearchText("diamond")
                .withPageIndex(2)
                .withPageSize(36);
        DatabaseQueryPayload payload = new DatabaseQueryPayload(1, 100L, query);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseQueryPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseQueryPayload restored = DatabaseQueryPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload.containerId(), restored.containerId());
        assertEquals(payload.sessionId(), restored.sessionId());
        assertEquals("diamond", restored.query().searchText());
        assertEquals(2, restored.query().pageIndex());
        assertEquals(36, restored.query().pageSize());
    }
}
