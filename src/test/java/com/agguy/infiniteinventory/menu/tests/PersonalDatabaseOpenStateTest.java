package com.agguy.infiniteinventory.menu.tests;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.menu.PersonalDatabaseOpenState;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalDatabaseOpenStateTest {
    @Test
    void shouldRoundTripQueriesForBothScopes() {
        PersonalDatabaseOpenState openState = new PersonalDatabaseOpenState(
                42L,
                DatabaseScope.PUBLIC,
                new DatabaseQuery(
                        DatabaseScope.PERSONAL,
                        DatabaseTabs.DEFAULT_TAB_ID,
                        List.of(DatabaseTabs.DEFAULT_TAB_ID),
                        Map.of(DatabaseTabs.DEFAULT_TAB_ID, 3),
                        Map.of(DatabaseTabs.DEFAULT_TAB_ID, 81),
                        DatabaseSortOption.NAME_ASC,
                        "iron",
                        com.agguy.infiniteinventory.database.DatabaseSearchConfig.defaultConfig()
                ),
                new DatabaseQuery(
                        DatabaseScope.PUBLIC,
                        DatabaseTabs.ALL_TAB_ID,
                        List.of(DatabaseTabs.ALL_TAB_ID),
                        Map.of(DatabaseTabs.ALL_TAB_ID, 1),
                        Map.of(DatabaseTabs.ALL_TAB_ID, 96),
                        DatabaseSortOption.COUNT_DESC,
                        "stone",
                        com.agguy.infiniteinventory.database.DatabaseSearchConfig.defaultConfig()
                ),
                DatabaseEnhancementConfig.defaultConfig().withOption(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS, true),
                new DatabaseAutoStoreTarget(DatabaseScope.PUBLIC, DatabaseTabs.DEFAULT_TAB_ID)
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        PersonalDatabaseOpenState.write(buffer, openState);
        PersonalDatabaseOpenState restored = PersonalDatabaseOpenState.read(buffer);

        assertEquals(openState, restored);
    }
}
