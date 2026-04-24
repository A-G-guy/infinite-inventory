package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.network.DatabaseSnapshotPayload;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabaseSnapshotPayloadTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void shouldConstructWithViewState() {
        DatabaseViewState viewState = DatabaseViewState.empty(0);
        DatabaseSnapshotPayload payload = new DatabaseSnapshotPayload(viewState);

        assertNotNull(payload.viewState());
        assertEquals(viewState, payload.viewState());
    }

    @Test
    void typeShouldReturnCorrectType() {
        assertEquals(DatabaseSnapshotPayload.TYPE,
                new DatabaseSnapshotPayload(DatabaseViewState.empty(0)).type());
    }
}
