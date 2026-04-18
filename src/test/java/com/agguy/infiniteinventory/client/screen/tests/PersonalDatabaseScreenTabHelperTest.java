package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseScope;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalDatabaseScreenTabHelperTest {
    @Test
    void filterTopTabsByScopeShouldKeepOnlyRequestedScopeAndPreserveOrder() {
        List<DatabaseScopedTabRef> tabs = List.of(
                DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, "public_food"),
                DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "personal_tools"),
                DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, "public_blocks"),
                DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "personal_misc")
        );

        assertEquals(
                List.of(
                        DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "personal_tools"),
                        DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "personal_misc")
                ),
                PersonalDatabaseScreenTabHelper.filterTopTabsByScope(tabs, DatabaseScope.PERSONAL)
        );
        assertEquals(
                List.of(
                        DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, "public_food"),
                        DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, "public_blocks")
                ),
                PersonalDatabaseScreenTabHelper.filterTopTabsByScope(tabs, DatabaseScope.PUBLIC)
        );
    }
}
