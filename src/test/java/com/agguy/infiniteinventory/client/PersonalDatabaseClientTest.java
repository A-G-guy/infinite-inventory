package com.agguy.infiniteinventory.client;

import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.localization.ViewerLanguage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalDatabaseClientTest {
    @Test
    void shouldNotSendViewerLanguageUpdateWhenServerDoesNotSupportChannel() {
        boolean shouldSend = PersonalDatabaseClient.shouldSendViewerLanguageUpdate(
                7,
                19L,
                ViewerLanguage.ZH_CN,
                Integer.MIN_VALUE,
                Long.MIN_VALUE,
                null,
                false
        );

        assertFalse(shouldSend);
    }

    @Test
    void shouldNotSendViewerLanguageUpdateWhenStateIsAlreadySynced() {
        boolean shouldSend = PersonalDatabaseClient.shouldSendViewerLanguageUpdate(
                7,
                19L,
                ViewerLanguage.ZH_CN,
                7,
                19L,
                ViewerLanguage.ZH_CN,
                true
        );

        assertFalse(shouldSend);
    }

    @Test
    void shouldSendViewerLanguageUpdateWhenSessionChangesAndChannelIsAvailable() {
        boolean shouldSend = PersonalDatabaseClient.shouldSendViewerLanguageUpdate(
                7,
                20L,
                ViewerLanguage.ZH_CN,
                7,
                19L,
                ViewerLanguage.ZH_CN,
                true
        );

        assertTrue(shouldSend);
    }

    @Test
    void shouldReturnDefaultEnhancementConfigInitially() {
        DatabaseEnhancementConfig config = PersonalDatabaseClient.lastKnownEnhancementConfig();

        assertFalse(config.isEnabled(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS));
        assertTrue(config.isEnabled(DatabaseEnhancementOption.SHOW_AMOUNT_IN_TOOLTIP));
    }

    @Test
    void shouldReturnSameDefaultConfigInstanceInitially() {
        DatabaseEnhancementConfig config1 = PersonalDatabaseClient.lastKnownEnhancementConfig();
        DatabaseEnhancementConfig config2 = PersonalDatabaseClient.lastKnownEnhancementConfig();

        assertSame(config1, config2);
    }
}
