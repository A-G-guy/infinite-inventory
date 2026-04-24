package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.network.DatabaseViewerLocalePayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseViewerLocalePayloadTest {

    @Test
    void shouldTrimLanguageCodeInCompactConstructor() {
        DatabaseViewerLocalePayload payload = new DatabaseViewerLocalePayload(0, 1L, "  zh_cn  ");

        assertEquals("zh_cn", payload.languageCode());
    }

    @Test
    void shouldDefaultNullLanguageCodeToEmptyString() {
        DatabaseViewerLocalePayload payload = new DatabaseViewerLocalePayload(0, 1L, null);

        assertEquals("", payload.languageCode());
    }

    @Test
    void shouldPreserveValidLanguageCode() {
        DatabaseViewerLocalePayload payload = new DatabaseViewerLocalePayload(5, 100L, "zh_cn");

        assertEquals(5, payload.containerId());
        assertEquals(100L, payload.sessionId());
        assertEquals("zh_cn", payload.languageCode());
    }

    @Test
    void typeShouldReturnCorrectType() {
        assertEquals(DatabaseViewerLocalePayload.TYPE,
                new DatabaseViewerLocalePayload(0, 0L, "").type());
    }
}
