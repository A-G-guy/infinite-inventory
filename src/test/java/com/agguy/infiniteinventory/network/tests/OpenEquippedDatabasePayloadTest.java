package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.network.OpenEquippedDatabasePayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenEquippedDatabasePayloadTest {

    @Test
    void shouldConstructWithNoArgs() {
        OpenEquippedDatabasePayload payload = new OpenEquippedDatabasePayload();

        assertNotNull(payload);
    }

    @Test
    void typeShouldReturnCorrectType() {
        assertEquals(OpenEquippedDatabasePayload.TYPE, new OpenEquippedDatabasePayload().type());
    }
}
