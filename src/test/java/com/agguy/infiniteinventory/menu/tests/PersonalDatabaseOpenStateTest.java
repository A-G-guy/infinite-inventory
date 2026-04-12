package com.agguy.infiniteinventory.menu.tests;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.menu.PersonalDatabaseOpenState;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalDatabaseOpenStateTest {
    @Test
    void shouldRoundTripQueriesForBothScopes() {
        PersonalDatabaseOpenState openState = new PersonalDatabaseOpenState(
                42L,
                DatabaseScope.PUBLIC,
                new DatabaseQuery(DatabaseScope.PERSONAL, DatabaseCategory.MATERIALS, DatabaseSortOption.NAME_ASC, "iron", 3, 81),
                new DatabaseQuery(DatabaseScope.PUBLIC, DatabaseCategory.BLOCKS, DatabaseSortOption.COUNT_DESC, "stone", 1, 96)
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        PersonalDatabaseOpenState.write(buffer, openState);
        PersonalDatabaseOpenState restored = PersonalDatabaseOpenState.read(buffer);

        assertEquals(openState, restored);
    }
}
