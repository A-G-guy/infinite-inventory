package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalDatabaseSelectionServiceTest {
    @Test
    void extractSelectionToInventoryShouldExposeRequestedAmountOverload() throws ReflectiveOperationException {
        var method = PersonalDatabaseService.class.getDeclaredMethod(
                "extractSelectionToInventory",
                ServerPlayer.class,
                DatabaseScope.class,
                List.class,
                DatabaseSelectionAction.class,
                long.class
        );

        assertEquals(long.class, method.getReturnType());
    }
}
