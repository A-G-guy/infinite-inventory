package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseQuickDepositPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseQuickDepositPayloadTest {

    @Test
    void shouldPreserveNullableTargetScope() {
        DatabaseQuickDepositPayload payload = new DatabaseQuickDepositPayload(0, 1L, 5, null, "target_tab", false);

        assertNull(payload.targetScope());
        assertEquals("target_tab", payload.targetTabId());
        assertEquals(5, payload.slotIndex());
        assertFalse(payload.storeSingleOnly());
    }

    @Test
    void shouldPreserveNonNullTargetScope() {
        DatabaseQuickDepositPayload payload = new DatabaseQuickDepositPayload(0, 1L, 10, DatabaseScope.PUBLIC,
                "my_tab", true);

        assertEquals(DatabaseScope.PUBLIC, payload.targetScope());
        assertEquals("my_tab", payload.targetTabId());
        assertEquals(10, payload.slotIndex());
        assertTrue(payload.storeSingleOnly());
    }

    @Test
    void typeShouldReturnCorrectType() {
        assertEquals(DatabaseQuickDepositPayload.TYPE,
                new DatabaseQuickDepositPayload(0, 0L, 0, null, "", false).type());
    }
}
