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
                .withOption(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS, true)
                .withOption(DatabaseEnhancementOption.SHOW_JEI_AMOUNT_IN_TOOLTIP, true)
                .withOption(DatabaseEnhancementOption.JEI_AUTO_EXTRACT_FOR_CRAFTING, true);

        DatabaseEnhancementConfig restoredFromTag = DatabaseEnhancementConfig.fromTag(config.toTag());
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        DatabaseEnhancementConfig.write(buffer, config);
        DatabaseEnhancementConfig restoredFromBuffer = DatabaseEnhancementConfig.read(buffer);

        assertEquals(config, restoredFromTag);
        assertEquals(config, restoredFromBuffer);
        assertTrue(restoredFromBuffer.isEnabled(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS));
        assertTrue(restoredFromBuffer.isEnabled(DatabaseEnhancementOption.SHOW_JEI_AMOUNT_IN_TOOLTIP));
        assertTrue(restoredFromBuffer.isEnabled(DatabaseEnhancementOption.JEI_AUTO_EXTRACT_FOR_CRAFTING));
    }

    @Test
    void shouldDisableOptionWithoutAffectingDefaultConfigInstance() {
        DatabaseEnhancementConfig enabledConfig = DatabaseEnhancementConfig.defaultConfig()
                .withOption(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS, true)
                .withOption(DatabaseEnhancementOption.SHOW_JEI_AMOUNT_IN_TOOLTIP, true)
                .withOption(DatabaseEnhancementOption.JEI_AUTO_EXTRACT_FOR_CRAFTING, true);
        DatabaseEnhancementConfig disabledConfig = enabledConfig
                .withOption(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS, false)
                .withOption(DatabaseEnhancementOption.SHOW_JEI_AMOUNT_IN_TOOLTIP, false);

        assertTrue(enabledConfig.isEnabled(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS));
        assertTrue(enabledConfig.isEnabled(DatabaseEnhancementOption.SHOW_JEI_AMOUNT_IN_TOOLTIP));
        assertTrue(enabledConfig.isEnabled(DatabaseEnhancementOption.JEI_AUTO_EXTRACT_FOR_CRAFTING));
        assertFalse(disabledConfig.isEnabled(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS));
        assertFalse(disabledConfig.isEnabled(DatabaseEnhancementOption.SHOW_JEI_AMOUNT_IN_TOOLTIP));
        assertTrue(disabledConfig.isEnabled(DatabaseEnhancementOption.JEI_AUTO_EXTRACT_FOR_CRAFTING));
    }

    @Test
    void shouldSupportAllEnhancementOptionsIndependently() {
        DatabaseEnhancementConfig config = DatabaseEnhancementConfig.defaultConfig()
                .withOption(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS, true)
                .withOption(DatabaseEnhancementOption.SHOW_JEI_AMOUNT_IN_TOOLTIP, false)
                .withOption(DatabaseEnhancementOption.JEI_AUTO_EXTRACT_FOR_CRAFTING, true);

        assertTrue(config.isEnabled(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS));
        assertFalse(config.isEnabled(DatabaseEnhancementOption.SHOW_JEI_AMOUNT_IN_TOOLTIP));
        assertTrue(config.isEnabled(DatabaseEnhancementOption.JEI_AUTO_EXTRACT_FOR_CRAFTING));
    }

    @Test
    void shouldReturnDefaultConfigForNullOption() {
        DatabaseEnhancementConfig config = DatabaseEnhancementConfig.defaultConfig()
                .withOption(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS, true);

        assertEquals(config, config.withOption(null, true));
        assertFalse(config.isEnabled(null));
    }
}
