package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseEnhancementConfigTest {
    @Test
    void shouldRoundTripEnabledOptionsThroughTagAndBuffer() {
        DatabaseEnhancementConfig config = DatabaseEnhancementConfig.defaultConfig()
                .withOption(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS, true);

        DatabaseEnhancementConfig restoredFromTag = DatabaseEnhancementConfig.fromTag(config.toTag());
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        DatabaseEnhancementConfig.write(buffer, config);
        DatabaseEnhancementConfig restoredFromBuffer = DatabaseEnhancementConfig.read(buffer);

        assertEquals(config, restoredFromTag);
        assertEquals(config, restoredFromBuffer);
        assertTrue(restoredFromBuffer.isEnabled(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS));
    }

    @Test
    void shouldDisableOptionWithoutAffectingDefaultConfigInstance() {
        DatabaseEnhancementConfig enabledConfig = DatabaseEnhancementConfig.defaultConfig()
                .withOption(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS, true);
        DatabaseEnhancementConfig disabledConfig = enabledConfig.withOption(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS, false);

        assertTrue(enabledConfig.isEnabled(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS));
        assertFalse(disabledConfig.isEnabled(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS));
    }
}
