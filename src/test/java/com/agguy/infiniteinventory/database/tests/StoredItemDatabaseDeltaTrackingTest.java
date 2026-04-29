package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link StoredItemDatabase} 增量数量追踪测试。
 */
class StoredItemDatabaseDeltaTrackingTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void storeShouldTrackAmountDelta() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 8), "blocks");
        List<StoredItemDatabase.AmountDelta> deltas = database.drainPendingAmountDeltas();
        assertEquals(1, deltas.size());
        assertEquals("blocks", deltas.get(0).tabId());
        assertEquals(8L, deltas.get(0).amount());
        assertFalse(deltas.get(0).removed());
    }

    @Test
    void extractShouldTrackRemovalDelta() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 4), "blocks");
        database.drainPendingAmountDeltas();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.extract(key, 4);
        List<StoredItemDatabase.AmountDelta> deltas = database.drainPendingAmountDeltas();
        assertEquals(1, deltas.size());
        assertTrue(deltas.get(0).removed());
        assertEquals(0L, deltas.get(0).amount());
    }

    @Test
    void extractShouldTrackReducedAmountDelta() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 8), "blocks");
        database.drainPendingAmountDeltas();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.extract(key, 3);
        List<StoredItemDatabase.AmountDelta> deltas = database.drainPendingAmountDeltas();
        assertEquals(1, deltas.size());
        assertEquals(5L, deltas.get(0).amount());
        assertFalse(deltas.get(0).removed());
    }

    @Test
    void drainShouldClearQueue() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 1));
        assertEquals(1, database.drainPendingAmountDeltas().size());
        assertTrue(database.drainPendingAmountDeltas().isEmpty());
    }

    @Test
    void batchUpdateShouldAccumulateDeltas() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.beginBatchUpdate();
        database.store(new ItemStack(Items.STONE, 4), "blocks");
        database.store(new ItemStack(Items.DIRT, 2), "building");
        database.endBatchUpdate();
        List<StoredItemDatabase.AmountDelta> deltas = database.drainPendingAmountDeltas();
        assertEquals(2, deltas.size());
    }

    @Test
    void clearShouldTrackRemovedDeltaForAllEntries() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 4), "blocks");
        database.store(new ItemStack(Items.DIRT, 2), "building");
        database.drainPendingAmountDeltas();
        database.clear();
        List<StoredItemDatabase.AmountDelta> deltas = database.drainPendingAmountDeltas();
        assertEquals(2, deltas.size());
        assertTrue(deltas.stream().allMatch(StoredItemDatabase.AmountDelta::removed));
    }

    @Test
    void duplicateKeyDeltasShouldMergeOnDrain() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.beginBatchUpdate();
        database.store(new ItemStack(Items.STONE, 4), "blocks");
        database.store(new ItemStack(Items.STONE, 2), "blocks");
        database.endBatchUpdate();
        List<StoredItemDatabase.AmountDelta> deltas = database.drainPendingAmountDeltas();
        assertEquals(1, deltas.size());
        assertEquals(6L, deltas.get(0).amount());
    }

    @Test
    void ensureTabAssignmentsShouldTrackAmountDelta() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.MINECART));
        DatabaseTestReflectionHelper.forceEntry(database, key, new StoredStackEntry("missing_tab", 4L, 12L));
        database.drainPendingAmountDeltas();
        database.ensureTabAssignments(new DatabaseTabDirectory());
        List<StoredItemDatabase.AmountDelta> deltas = database.drainPendingAmountDeltas();
        assertEquals(1, deltas.size());
        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, deltas.get(0).tabId());
        assertEquals(4L, deltas.get(0).amount());
        assertFalse(deltas.get(0).removed());
    }

    @Test
    void moveEntryToTabShouldTrackAmountDelta() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 8), "blocks");
        database.drainPendingAmountDeltas();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.moveEntryToTab(key, "blocks", "building");
        List<StoredItemDatabase.AmountDelta> deltas = database.drainPendingAmountDeltas();
        assertEquals(1, deltas.size());
        assertEquals("building", deltas.get(0).tabId());
        assertEquals(8L, deltas.get(0).amount());
        assertFalse(deltas.get(0).removed());
    }

    @Test
    void transferTabShouldTrackAmountDelta() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 8), "blocks");
        database.store(new ItemStack(Items.DIRT, 4), "blocks");
        database.drainPendingAmountDeltas();
        database.transferTab("blocks", "building");
        List<StoredItemDatabase.AmountDelta> deltas = database.drainPendingAmountDeltas();
        assertEquals(2, deltas.size());
        assertTrue(deltas.stream().allMatch(d -> d.tabId().equals("building")));
        assertTrue(deltas.stream().allMatch(d -> !d.removed()));
        assertTrue(deltas.stream().anyMatch(d -> d.amount() == 8L));
        assertTrue(deltas.stream().anyMatch(d -> d.amount() == 4L));
    }
}
