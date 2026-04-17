package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import com.agguy.infiniteinventory.network.DatabaseSelectionPayload;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseSelectionPayloadTest {
    @Test
    void customRequestedAmountShouldRoundTripThroughStreamCodec() {
        DatabaseSelectionPayload payload = new DatabaseSelectionPayload(
                12,
                34L,
                DatabaseSelectionAction.EXTRACT_CUSTOM_TO_INVENTORY,
                DatabaseTabs.DEFAULT_TAB_ID,
                7L,
                List.of()
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseSelectionPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseSelectionPayload restored = DatabaseSelectionPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload, restored);
        assertEquals(7L, restored.requestedAmount());
    }

    @Test
    void constructorShouldNormalizeRequestedAmountAndSelectionEntries() {
        DatabaseSelectionPayload customPayload = new DatabaseSelectionPayload(
                1,
                2L,
                DatabaseSelectionAction.EXTRACT_CUSTOM_TO_INVENTORY,
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
        assertEquals(0L, customPayload.requestedAmount());
        assertEquals(List.of(), customPayload.selectedEntries());
        assertEquals(0L, transferPayload.requestedAmount());
    }
}
