package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import com.agguy.infiniteinventory.network.DatabaseSelectionPayload;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DatabaseSelectionPayloadTest {
    @Test
    void customRequestedAmountShouldRoundTripThroughStreamCodec() {
        DatabaseSelectionPayload payload = new DatabaseSelectionPayload(
                12,
                34L,
                DatabaseSelectionAction.EXTRACT_CUSTOM_TO_INVENTORY,
                DatabaseScope.PUBLIC,
                DatabaseTabs.DEFAULT_TAB_ID,
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
                DatabaseTabs.DEFAULT_TAB_ID,
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
}
