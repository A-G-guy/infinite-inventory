package com.agguy.infiniteinventory.menu.tests;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTabQueryState;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.menu.PersonalDatabaseOpenState;
import io.netty.buffer.Unpooled;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalDatabaseOpenStateTest {
    @Test
    void shouldRoundTripUnifiedQueryState() {
        DatabaseScopedTabRef personalDefault = DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, DatabaseTabs.DEFAULT_TAB_ID);
        DatabaseScopedTabRef publicAll = DatabaseScopedTabRef.allTab(DatabaseScope.PUBLIC);
        PersonalDatabaseOpenState openState = new PersonalDatabaseOpenState(
                42L,
                new DatabaseQuery(
                        publicAll,
                        java.util.List.of(publicAll, personalDefault),
                        Map.of(
                                personalDefault,
                                new DatabaseTabQueryState(
                                        DatabaseSortOption.NAME_ASC,
                                        "iron",
                                        com.agguy.infiniteinventory.database.DatabaseSearchConfig.defaultConfig(),
                                        3,
                                        81
                                ),
                                publicAll,
                                new DatabaseTabQueryState(
                                        DatabaseSortOption.COUNT_DESC,
                                        "stone",
                                        com.agguy.infiniteinventory.database.DatabaseSearchConfig.defaultConfig(),
                                        1,
                                        96
                                )
                        ),
                        java.util.List.of()
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
