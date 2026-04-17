package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseEnhancementPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseEnhancementPayloadTest {
    @Test
    void shouldRoundTripScopeAndTargetTabThroughStreamCodec() {
        DatabaseEnhancementPayload payload = new DatabaseEnhancementPayload(
                9,
                27L,
                DatabaseEnhancementConfig.defaultConfig()
                        .withOption(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS, true),
                new DatabaseAutoStoreTarget(DatabaseScope.PUBLIC, "public_food")
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseEnhancementPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseEnhancementPayload restored = DatabaseEnhancementPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload, restored);
        assertEquals(DatabaseScope.PUBLIC, restored.autoStoreTarget().scope());
        assertEquals("public_food", restored.autoStoreTarget().tabId());
    }
}
