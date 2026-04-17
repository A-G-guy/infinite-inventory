package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseCrossTransferHelper;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.database.UnresolvedStoredEntry;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseCrossTransferHelperTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void transferSelectionShouldPreserveMetadataAndForceExplicitTargetTab() throws ReflectiveOperationException {
        StoredItemDatabase sourceDatabase = new StoredItemDatabase();
        StoredItemDatabase targetDatabase = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));

        DatabaseTestReflectionHelper.forceEntry(sourceDatabase, key, new StoredStackEntry("source_tab", 5L, 12L, 3L));
        DatabaseTestReflectionHelper.forceEntry(targetDatabase, key, new StoredStackEntry("existing_tab", 2L, 9L, 6L));
        DatabaseTestReflectionHelper.forceNextSequence(targetDatabase, 1L);

        boolean changed = DatabaseCrossTransferHelper.transferSelection(
                null,
                sourceDatabase,
                targetDatabase,
                List.of(new DatabaseSelectionEntry("source_tab", new ItemStack(Items.STONE))),
                "target_tab"
        );

        assertTrue(changed);
        assertFalse(sourceDatabase.entries().containsKey(key));
        StoredStackEntry mergedEntry = targetDatabase.entries().get(key);
        assertEquals("target_tab", mergedEntry.tabId());
        assertEquals(7L, mergedEntry.amount());
        assertEquals(12L, mergedEntry.lastModified());
        assertEquals(3L, mergedEntry.firstAdded());
        assertEquals(13L, DatabaseTestReflectionHelper.readNextSequence(targetDatabase));
    }

    @Test
    void transferTabShouldMoveResolvedAndUnresolvedEntriesAcrossDatabases() throws ReflectiveOperationException {
        StoredItemDatabase sourceDatabase = new StoredItemDatabase();
        StoredItemDatabase targetDatabase = new StoredItemDatabase();
        StoredStackKey sourceKey = StoredStackKey.of(new ItemStack(Items.OAK_LOG));
        StoredStackKey unaffectedKey = StoredStackKey.of(new ItemStack(Items.STONE));

        DatabaseTestReflectionHelper.forceEntry(sourceDatabase, sourceKey, new StoredStackEntry("logs", 8L, 17L, 4L));
        DatabaseTestReflectionHelper.forceEntry(sourceDatabase, unaffectedKey, new StoredStackEntry("stone", 2L, 6L, 2L));
        DatabaseTestReflectionHelper.forceUnresolvedEntry(
                sourceDatabase,
                new UnresolvedStoredEntry(DatabaseTestReflectionHelper.invalidStackTag("missing:ghost_log"), 3L, "logs", 19L, 5L)
        );
        DatabaseTestReflectionHelper.forceNextSequence(targetDatabase, 2L);

        boolean changed = DatabaseCrossTransferHelper.transferTab(null, sourceDatabase, targetDatabase, "logs", "shared");

        assertTrue(changed);
        assertFalse(sourceDatabase.entries().containsKey(sourceKey));
        assertEquals(1, sourceDatabase.entryCount());
        assertEquals("stone", sourceDatabase.entries().get(unaffectedKey).tabId());
        assertEquals(0, sourceDatabase.unresolvedEntryCount());

        StoredStackEntry movedEntry = targetDatabase.entries().get(sourceKey);
        assertEquals("shared", movedEntry.tabId());
        assertEquals(8L, movedEntry.amount());
        assertEquals(17L, movedEntry.lastModified());
        assertEquals(4L, movedEntry.firstAdded());
        assertEquals(1, targetDatabase.unresolvedEntryCount());
        assertEquals("shared", targetDatabase.unresolvedEntries().get(0).tabId());
        assertEquals(20L, DatabaseTestReflectionHelper.readNextSequence(targetDatabase));
    }
}
