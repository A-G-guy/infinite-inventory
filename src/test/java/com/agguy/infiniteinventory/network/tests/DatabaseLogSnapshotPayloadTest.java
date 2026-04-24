package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseLogSnapshotPayload;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseLogSnapshotPayloadTest {

    @Test
    void shouldDefaultNullEntriesToEmptyList() {
        DatabaseLogSnapshotPayload payload = new DatabaseLogSnapshotPayload(DatabaseScope.PERSONAL, null);

        assertNotNull(payload.entries());
        assertTrue(payload.entries().isEmpty());
    }

    @Test
    void shouldPreserveScopeAndEntries() {
        DatabaseLogSnapshotPayload payload = new DatabaseLogSnapshotPayload(DatabaseScope.PUBLIC, List.of());

        assertEquals(DatabaseScope.PUBLIC, payload.scope());
        assertTrue(payload.entries().isEmpty());
    }

    @Test
    void typeShouldReturnCorrectType() {
        assertEquals(DatabaseLogSnapshotPayload.TYPE,
                new DatabaseLogSnapshotPayload(DatabaseScope.PERSONAL, List.of()).type());
    }
}
