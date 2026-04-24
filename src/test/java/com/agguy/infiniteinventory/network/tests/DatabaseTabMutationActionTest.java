package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.network.DatabaseTabMutationAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseTabMutationActionTest {

    @Test
    void shouldHaveAllExpectedActions() {
        assertEquals(8, DatabaseTabMutationAction.values().length);
    }

    @Test
    void shouldResolveAddByValueOf() {
        assertEquals(DatabaseTabMutationAction.ADD, DatabaseTabMutationAction.valueOf("ADD"));
    }

    @Test
    void shouldResolveDeleteByValueOf() {
        assertEquals(DatabaseTabMutationAction.DELETE, DatabaseTabMutationAction.valueOf("DELETE"));
    }

    @Test
    void shouldResolveTransferByValueOf() {
        assertEquals(DatabaseTabMutationAction.TRANSFER, DatabaseTabMutationAction.valueOf("TRANSFER"));
    }

    @Test
    void shouldResolveToggleTopVisibilityByValueOf() {
        assertEquals(DatabaseTabMutationAction.TOGGLE_TOP_VISIBILITY,
                DatabaseTabMutationAction.valueOf("TOGGLE_TOP_VISIBILITY"));
    }
}
