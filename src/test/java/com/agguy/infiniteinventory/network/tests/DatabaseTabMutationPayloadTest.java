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
}
