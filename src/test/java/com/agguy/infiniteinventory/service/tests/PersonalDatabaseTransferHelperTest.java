package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.database.tests.DatabaseTestReflectionHelper;
import com.agguy.infiniteinventory.service.PersonalDatabaseTransferHelper;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalDatabaseTransferHelperTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void resolveTargetScopeShouldFallbackToSourceScopeWhenMissing() {
        assertEquals(DatabaseScope.PUBLIC, PersonalDatabaseTransferHelper.resolveTargetScope(DatabaseScope.PUBLIC, null));
        assertEquals(DatabaseScope.PERSONAL, PersonalDatabaseTransferHelper.resolveTargetScope(DatabaseScope.PUBLIC, DatabaseScope.PERSONAL));
    }

    @Test
    void transferSelectionShouldMoveAcrossDatabasesAndHonorExplicitTargetTab() throws ReflectiveOperationException {
        StoredItemDatabase sourceDatabase = new StoredItemDatabase();
        StoredItemDatabase targetDatabase = new StoredItemDatabase();
        ItemStack diamondStack = new ItemStack(Items.DIAMOND);
        StoredStackKey key = StoredStackKey.of(diamondStack);

        DatabaseTestReflectionHelper.forceEntry(sourceDatabase, key, new StoredStackEntry("ores", 5L, 7L, 3L));
        DatabaseTestReflectionHelper.forceEntry(targetDatabase, key, new StoredStackEntry("vault", 2L, 9L, 1L));

        assertTrue(PersonalDatabaseTransferHelper.transferSelection(
                sourceDatabase,
                targetDatabase,
                List.of(
                        new DatabaseSelectionEntry(DatabaseScope.PERSONAL, "ores", diamondStack),
                        new DatabaseSelectionEntry(DatabaseScope.PERSONAL, "ores", diamondStack)
                ),
                "gems"
        ));
        assertEquals(0, sourceDatabase.entryCount());

        StoredStackEntry mergedEntry = targetDatabase.entries().get(key);
        assertEquals("gems", mergedEntry.tabId());
        assertEquals(7L, mergedEntry.amount());
        assertEquals(9L, mergedEntry.lastModified());
        assertEquals(1L, mergedEntry.firstAdded());
    }
}
