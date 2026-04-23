package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import com.agguy.infiniteinventory.network.DatabaseSelectionPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DatabaseSelectionPayload 序列化对称性测试。
 *
 * 注意：包含 DatabaseSelectionEntry（内含 ItemStack）的序列化需要同步注册表，
 * 在单元测试环境中无法完整模拟，因此 Entry 的序列化测试在集成环境中进行。
 * 此处重点覆盖 Payload 本身的字段序列化与构造函数规范化行为。
 */
class DatabaseSelectionPayloadTest {

    @Test
    void customRequestedAmountShouldRoundTripThroughStreamCodec() {
        DatabaseSelectionPayload payload = new DatabaseSelectionPayload(
                12,
                34L,
                DatabaseSelectionAction.EXTRACT_CUSTOM_TO_INVENTORY,
                DatabaseScope.PUBLIC,
                "target_tab",
                7L,
                List.of()
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseSelectionPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseSelectionPayload restored = DatabaseSelectionPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload, restored);
        assertEquals(DatabaseScope.PUBLIC, restored.targetScope());
        assertEquals(7L, restored.requestedAmount());
    }

    @Test
    void constructorShouldNormalizeRequestedAmountAndSelectionEntries() {
        DatabaseSelectionPayload customPayload = new DatabaseSelectionPayload(
                1,
                2L,
                DatabaseSelectionAction.EXTRACT_CUSTOM_TO_INVENTORY,
                null,
                null,
                -9L,
                null
        );
        DatabaseSelectionPayload transferPayload = new DatabaseSelectionPayload(
                1,
                2L,
                DatabaseSelectionAction.TRANSFER_TO_TAB,
                "target_tab",
                99L,
                List.of()
        );

        assertEquals("", customPayload.targetTabId());
        assertNull(customPayload.targetScope());
        assertEquals(0L, customPayload.requestedAmount());
        assertEquals(List.of(), customPayload.selectedEntries());
        assertNull(transferPayload.targetScope());
        assertEquals(0L, transferPayload.requestedAmount());
    }

    @Test
    void payloadWithEmptyEntriesAndNullTargetScopeShouldRoundTripCorrectly() {
        DatabaseSelectionPayload payload = new DatabaseSelectionPayload(
                1,
                2L,
                DatabaseSelectionAction.EXTRACT_ALL_TO_INVENTORY,
                null,
                "dest_tab",
                0L,
                List.of()
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseSelectionPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseSelectionPayload restored = DatabaseSelectionPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload, restored);
        assertNull(restored.targetScope());
        assertEquals("dest_tab", restored.targetTabId());
        assertTrue(restored.selectedEntries().isEmpty());
    }

    @Test
    void payloadWithDifferentActionTypesShouldRoundTripCorrectly() {
        DatabaseSelectionPayload extractOnePayload = new DatabaseSelectionPayload(
                7,
                100L,
                DatabaseSelectionAction.EXTRACT_ONE_TO_INVENTORY,
                null,
                "",
                0L,
                List.of()
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseSelectionPayload.STREAM_CODEC.encode(buffer, extractOnePayload);
        DatabaseSelectionPayload restored = DatabaseSelectionPayload.STREAM_CODEC.decode(buffer);

        assertEquals(extractOnePayload, restored);
        assertEquals(DatabaseSelectionAction.EXTRACT_ONE_TO_INVENTORY, restored.action());
    }

    @Test
    void legacyConstructorWithoutTargetScopeShouldNormalizeCorrectly() {
        DatabaseSelectionPayload payload = new DatabaseSelectionPayload(
                3,
                55L,
                DatabaseSelectionAction.TRANSFER_TO_TAB,
                "source_tab",
                List.of()
        );

        assertNull(payload.targetScope());
        assertEquals("source_tab", payload.targetTabId());
        assertEquals(0L, payload.requestedAmount());
    }

    @Test
    void resolvedTargetScopeShouldFallbackToGivenScope() {
        DatabaseSelectionPayload payload = new DatabaseSelectionPayload(
                1,
                2L,
                DatabaseSelectionAction.EXTRACT_ALL_TO_INVENTORY,
                null,
                "tab",
                0L,
                List.of()
        );

        assertEquals(DatabaseScope.PERSONAL, payload.resolvedTargetScope(DatabaseScope.PERSONAL));
        assertEquals(DatabaseScope.PUBLIC, payload.resolvedTargetScope(DatabaseScope.PUBLIC));
    }
}
