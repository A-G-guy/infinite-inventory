package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.network.JeiCraftingTabSourcePayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeiCraftingTabSourcePayloadTest {

    @Test
    void shouldRoundTripThroughStreamCodec() {
        JeiCraftingTabSourcePayload payload = new JeiCraftingTabSourcePayload(
                DatabaseScope.PUBLIC,
                "custom_blocks",
                true
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        JeiCraftingTabSourcePayload.STREAM_CODEC.encode(buffer, payload);
        JeiCraftingTabSourcePayload restored = JeiCraftingTabSourcePayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload, restored);
        assertEquals(DatabaseScope.PUBLIC, restored.scope());
        assertEquals("custom_blocks", restored.tabId());
        assertTrue(restored.enabled());
    }

    @Test
    void shouldRoundTripDisablePayload() {
        JeiCraftingTabSourcePayload payload = new JeiCraftingTabSourcePayload(
                DatabaseScope.PERSONAL,
                DatabaseTabs.ALL_TAB_ID,
                false
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        JeiCraftingTabSourcePayload.STREAM_CODEC.encode(buffer, payload);
        JeiCraftingTabSourcePayload restored = JeiCraftingTabSourcePayload.STREAM_CODEC.decode(buffer);

        assertFalse(restored.enabled());
        assertEquals(DatabaseScope.PERSONAL, restored.scope());
    }
}
