package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseStarPayload;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabaseStarPayloadTest {

    @Test
    void shouldNormalizeScopeInCompactConstructor() {
        DatabaseStarPayload payload = new DatabaseStarPayload(0, 1L, null, List.of(),
                DatabaseStarPayload.StarAction.TOGGLE);

        assertEquals(DatabaseScope.PERSONAL, payload.scope());
    }

    @Test
    void shouldDefaultNullTargetStacksToEmptyList() {
        DatabaseStarPayload payload = new DatabaseStarPayload(0, 1L, DatabaseScope.PERSONAL, null,
                DatabaseStarPayload.StarAction.TOGGLE);

        assertNotNull(payload.targetStacks());
        assertEquals(0, payload.targetStacks().size());
    }

    @Test
    void shouldDefaultNullActionToToggle() {
        DatabaseStarPayload payload = new DatabaseStarPayload(0, 1L, DatabaseScope.PERSONAL, List.of(), null);

        assertEquals(DatabaseStarPayload.StarAction.TOGGLE, payload.action());
    }

    @Test
    void shouldPreserveAllEnumValues() {
        for (DatabaseStarPayload.StarAction action : DatabaseStarPayload.StarAction.values()) {
            DatabaseStarPayload payload = new DatabaseStarPayload(1, 100L, DatabaseScope.PUBLIC,
                    List.of(new ItemStack(Items.STONE)), action);

            assertEquals(action, payload.action());
            assertEquals(DatabaseScope.PUBLIC, payload.scope());
            assertEquals(1, payload.targetStacks().size());
        }
    }

    @Test
    void typeShouldReturnCorrectType() {
        assertEquals(DatabaseStarPayload.TYPE, new DatabaseStarPayload(0, 0L, DatabaseScope.PERSONAL, List.of(),
                DatabaseStarPayload.StarAction.TOGGLE).type());
    }
}
