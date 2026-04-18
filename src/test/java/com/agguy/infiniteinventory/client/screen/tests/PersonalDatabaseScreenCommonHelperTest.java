package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseScope;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalDatabaseScreenCommonHelperTest {
    @Test
    void clampVisibleTabsShouldKeepFocusedTabInsideCompactCapacity() {
        DatabaseScopedTabRef personalDefault = DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "__default");
        DatabaseScopedTabRef personalTools = DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "tools");
        DatabaseScopedTabRef publicFood = DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, "food");

        List<DatabaseScopedTabRef> clampedTabs = PersonalDatabaseScreenCommonHelper.clampVisibleTabs(
                List.of(personalDefault, personalTools, publicFood),
                publicFood,
                1
        );

        assertEquals(List.of(publicFood), clampedTabs);
    }

    @Test
    void clampVisibleTabsShouldPreserveOriginalOrderWhenFocusedTabAlreadyFits() {
        DatabaseScopedTabRef personalDefault = DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "__default");
        DatabaseScopedTabRef publicFood = DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, "food");
        DatabaseScopedTabRef personalTools = DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "tools");

        List<DatabaseScopedTabRef> clampedTabs = PersonalDatabaseScreenCommonHelper.clampVisibleTabs(
                List.of(personalDefault, publicFood, personalTools),
                publicFood,
                2
        );

        assertEquals(List.of(personalDefault, publicFood), clampedTabs);
    }
}
