package com.agguy.infiniteinventory.database.tests;
import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseItemClassifier;
import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.database.UnresolvedStoredEntry;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class StoredItemDatabaseTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }
    @Test
    void shouldPreserveLegacyInvalidEntriesWhenProviderUnavailable() {
        CompoundTag legacyRoot = new CompoundTag();
        ListTag entries = new ListTag();
        entries.add(this.entryTag(this.invalidStackTag("missing:ghost_item"), 9L, DatabaseCategory.OTHER, 11L));
        legacyRoot.put("entries", entries);
        legacyRoot.putLong("next_sequence", 20L);
        StoredItemDatabase restored = new StoredItemDatabase();
        restored.deserializeNBT(null, legacyRoot);
        assertEquals(0, restored.entryCount());
        assertEquals(1, restored.unresolvedEntryCount());
        StoredItemDatabase roundTripped = new StoredItemDatabase();
        roundTripped.deserializeNBT(null, restored.serializeNBT(null));
        assertEquals(0, roundTripped.entryCount());
        assertEquals(1, roundTripped.unresolvedEntryCount());
    }
    @Test
    void shouldPreserveCurrentFormatUnresolvedEntriesWithoutDroppingThem() {
        CompoundTag currentRoot = new CompoundTag();
        currentRoot.putInt("schema_version", StoredItemDatabase.CURRENT_SCHEMA_VERSION);
        currentRoot.putInt("classifier_version", DatabaseItemClassifier.CURRENT_VERSION);
        currentRoot.put("entries", new ListTag());
        ListTag unresolvedEntries = new ListTag();
        unresolvedEntries.add(this.entryTag(this.invalidStackTag("missing:ghost_apple"), 5L, DatabaseCategory.CONSUMABLES, 7L));
        currentRoot.put("unresolved_entries", unresolvedEntries);
        currentRoot.putLong("next_sequence", 10L);
        StoredItemDatabase restored = new StoredItemDatabase();
        restored.deserializeNBT(null, currentRoot);
        assertEquals(0, restored.entryCount());
        assertEquals(1, restored.unresolvedEntryCount());
        assertEquals(10L, restored.serializeNBT(null).getLong("next_sequence"));
    }
    @Test
    void serializeShouldWriteCurrentSchemaVersion() {
        StoredItemDatabase database = new StoredItemDatabase();
        assertEquals(StoredItemDatabase.CURRENT_SCHEMA_VERSION, database.serializeNBT(null).getInt("schema_version"));
    }
    @Test
    void storeShouldStackOnlyCompletelyIdenticalItems() {
        StoredItemDatabase database = new StoredItemDatabase();
        ItemStack namedStone = new ItemStack(Items.STONE, 4);
        namedStone.set(DataComponents.CUSTOM_NAME, Component.literal("仓库A"));
        ItemStack sameNamedStone = new ItemStack(Items.STONE, 7);
        sameNamedStone.set(DataComponents.CUSTOM_NAME, Component.literal("仓库A"));
        ItemStack differentNamedStone = new ItemStack(Items.STONE, 5);
        differentNamedStone.set(DataComponents.CUSTOM_NAME, Component.literal("仓库B"));
        database.store(namedStone);
        database.store(sameNamedStone);
        database.store(differentNamedStone);
        assertEquals(2, database.entryCount());
        assertEquals(11L, database.getAmount(StoredStackKey.of(namedStone)));
        assertEquals(5L, database.getAmount(StoredStackKey.of(differentNamedStone)));
    }
    @Test
    void storeShouldKeepOriginalFirstAddedWhenExistingEntryGetsMoreItems() {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.store(new ItemStack(Items.STONE, 4));
        database.store(new ItemStack(Items.STONE, 2));
        StoredStackEntry entry = database.entries().get(key);
        assertEquals(6L, entry.amount());
        assertEquals(1L, entry.firstAdded());
        assertEquals(2L, entry.lastModified());
    }
    @Test
    void storeAfterEntryWasClearedShouldAssignNewFirstAddedSequence() {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.store(new ItemStack(Items.STONE, 1));
        database.extract(key, 1);
        database.store(new ItemStack(Items.STONE, 1));
        StoredStackEntry entry = database.entries().get(key);
        assertEquals(1L, entry.amount());
        assertEquals(3L, entry.firstAdded());
        assertEquals(3L, entry.lastModified());
    }
    @Test
    void serializeAndDeserializeShouldPreserveStackComponents() {
        StoredItemDatabase database = new StoredItemDatabase();
        ItemStack namedSword = new ItemStack(Items.DIAMOND_SWORD);
        namedSword.set(DataComponents.CUSTOM_NAME, Component.literal("无限之刃"));
        namedSword.set(DataComponents.DAMAGE, 17);
        StoredStackKey originalKey = StoredStackKey.of(namedSword);
        database.store(namedSword);
        CompoundTag serialized = database.serializeNBT(null);
        StoredItemDatabase restored = new StoredItemDatabase();
        restored.deserializeNBT(null, serialized);
        assertEquals(1, restored.entryCount());
        StoredStackKey restoredKey = restored.entries().keySet().iterator().next();
        assertEquals(1L, restored.getAmount(restoredKey));
        assertEquals(originalKey, restoredKey);
        assertEquals(originalKey.displayStack().getHoverName().getString(), restoredKey.displayStack().getHoverName().getString());
        assertEquals(originalKey.displayStack().getOrDefault(DataComponents.DAMAGE, 0), restoredKey.displayStack().getOrDefault(DataComponents.DAMAGE, 0));
    }
    @Test
    void mergeFromShouldPreferTabAssignmentFromNewerEntryAndPreserveStackedAmount() throws ReflectiveOperationException {
        StoredItemDatabase targetDatabase = new StoredItemDatabase();
        StoredItemDatabase sourceDatabase = new StoredItemDatabase();
        var key = DatabaseTestReflectionHelper.fakeKey("test:material");
        DatabaseTestReflectionHelper.forceEntry(targetDatabase, key, new StoredStackEntry("target_tab", 16L, 4L));
        DatabaseTestReflectionHelper.forceEntry(sourceDatabase, key, new StoredStackEntry("source_tab", 8L, 7L));
        targetDatabase.mergeFrom(sourceDatabase);
        StoredStackEntry mergedEntry = targetDatabase.entries().get(key);
        assertEquals("source_tab", mergedEntry.tabId());
        assertEquals(24L, mergedEntry.amount());
        assertEquals(7L, mergedEntry.lastModified());
    }
    @Test
    void mergeFromShouldAdvanceNextSequenceWhenSourceSequenceIsStale() throws ReflectiveOperationException {
        StoredItemDatabase targetDatabase = new StoredItemDatabase();
        StoredItemDatabase sourceDatabase = new StoredItemDatabase();
        var key = DatabaseTestReflectionHelper.fakeKey("test:sequence_item");
        DatabaseTestReflectionHelper.forceEntry(sourceDatabase, key, new StoredStackEntry(DatabaseTabs.DEFAULT_TAB_ID, 6L, 25L));
        DatabaseTestReflectionHelper.forceNextSequence(sourceDatabase, 1L);
        targetDatabase.mergeFrom(sourceDatabase);
        assertEquals(26L, DatabaseTestReflectionHelper.readNextSequence(targetDatabase));
    }
    @Test
    void extractShouldClampOversizedRequestToLegalStackSize() {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.store(new ItemStack(Items.STONE, 64));
        database.store(new ItemStack(Items.STONE, 6));
        ItemStack extracted = database.extract(key, Integer.MAX_VALUE);
        assertEquals(64, extracted.getCount());
        assertEquals(6L, database.getAmount(key));
    }
    @Test
    void oldSchemaShouldRequestResaveAfterDeserialize() {
        CompoundTag oldSchemaRoot = new CompoundTag();
        oldSchemaRoot.putInt("schema_version", 1);
        oldSchemaRoot.put("entries", new ListTag());
        oldSchemaRoot.put("unresolved_entries", new ListTag());
        oldSchemaRoot.putLong("next_sequence", 1L);
        StoredItemDatabase restored = new StoredItemDatabase();
        restored.deserializeNBT(null, oldSchemaRoot);
        assertTrue(restored.needsResave());
        assertEquals(1L, restored.revision());
    }
    @Test
    void deserializeShouldBackfillFirstAddedWhenOldEntriesDoNotStoreIt() {
        CompoundTag oldSchemaRoot = new CompoundTag();
        oldSchemaRoot.putInt("schema_version", 2);
        oldSchemaRoot.putInt("classifier_version", DatabaseItemClassifier.CURRENT_VERSION);
        ListTag entries = new ListTag();
        entries.add(this.entryTag(new ItemStack(Items.STONE), 4L, DatabaseCategory.BLOCKS, 9L));
        oldSchemaRoot.put("entries", entries);
        oldSchemaRoot.put("unresolved_entries", new ListTag());
        oldSchemaRoot.putLong("next_sequence", 10L);
        StoredItemDatabase restored = new StoredItemDatabase();
        restored.deserializeNBT(null, oldSchemaRoot);
        StoredStackEntry entry = restored.entries().values().iterator().next();
        assertEquals(9L, entry.firstAdded());
        assertEquals(9L, entry.lastModified());
        assertTrue(restored.needsResave());
    }
    @Test
    void ensureTabAssignmentsShouldFallbackMissingTabsToDefaultTab() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.MINECART));
        DatabaseTestReflectionHelper.forceEntry(database, key, new StoredStackEntry("missing_tab", 4L, 12L));
        assertTrue(database.ensureTabAssignments(new DatabaseTabDirectory()));
        StoredStackEntry reassignedEntry = database.entries().get(key);
        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, reassignedEntry.tabId());
        assertEquals(4L, reassignedEntry.amount());
        assertEquals(12L, reassignedEntry.lastModified());
    }
    @Test
    void moveEntryToTabShouldOnlyMoveMatchingSourceEntry() {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.store(new ItemStack(Items.STONE, 8), "blocks");
        assertTrue(database.moveEntryToTab(key, "blocks", "building"));
        assertEquals("building", database.entries().get(key).tabId());
        assertEquals(8L, database.entries().get(key).amount());
    }
    @Test
    void moveEntryToTabShouldIgnoreMismatchedSourceTab() {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.store(new ItemStack(Items.STONE, 8), "blocks");
        assertFalse(database.moveEntryToTab(key, "ores", "building"));
        assertEquals("blocks", database.entries().get(key).tabId());
    }
    @Test
    void roundTripShouldMatchOriginalState() {
        StoredItemDatabase original = new StoredItemDatabase();
        original.store(new ItemStack(Items.STONE, 64));
        original.store(new ItemStack(Items.DIRT, 32));
        original.store(new ItemStack(Items.DIAMOND, 16));
        original.store(new ItemStack(Items.APPLE, 8));
        StoredStackKey stoneKey = StoredStackKey.of(new ItemStack(Items.STONE));
        StoredStackKey dirtKey = StoredStackKey.of(new ItemStack(Items.DIRT));
        original.setNote(stoneKey, "建筑方块");
        original.toggleStar(dirtKey);
        CompoundTag serialized = original.serializeNBT(null);
        StoredItemDatabase restored = new StoredItemDatabase();
        restored.deserializeNBT(null, serialized);
        assertEquals(original.entryCount(), restored.entryCount());
        assertEquals(original.notes().size(), restored.notes().size());
        assertEquals(original.starredEntries().size(), restored.starredEntries().size());
        for (StoredStackKey key : original.entries().keySet()) {
            assertEquals(original.getAmount(key), restored.getAmount(key));
        }
        assertEquals("建筑方块", restored.noteFor(stoneKey));
        assertTrue(restored.isStarred(dirtKey));
    }
    @Test
    void deserializeShouldPreserveNotesAndStars() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 8));
        database.store(new ItemStack(Items.DIRT, 4));
        StoredStackKey stoneKey = StoredStackKey.of(new ItemStack(Items.STONE));
        database.setNote(stoneKey, "备注测试");
        database.toggleStar(stoneKey);
        CompoundTag serialized = database.serializeNBT(null);
        StoredItemDatabase restored = new StoredItemDatabase();
        restored.deserializeNBT(null, serialized);
        assertEquals("备注测试", restored.noteFor(stoneKey));
        assertTrue(restored.isStarred(stoneKey));
    }
    @Test
    void storeExtractMergeClearAndDeserializeShouldAdvanceRevision() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredItemDatabase otherDatabase = new StoredItemDatabase();
        assertEquals(0L, DatabaseTestReflectionHelper.readRevision(database));
        database.store(new ItemStack(Items.STONE, 8));
        assertEquals(1L, DatabaseTestReflectionHelper.readRevision(database));
        database.extract(StoredStackKey.of(new ItemStack(Items.STONE)), 2);
        assertEquals(2L, DatabaseTestReflectionHelper.readRevision(database));
        otherDatabase.store(new ItemStack(Items.DIRT, 3));
        database.mergeFrom(otherDatabase);
        assertEquals(3L, DatabaseTestReflectionHelper.readRevision(database));
        database.clear();
        assertEquals(4L, DatabaseTestReflectionHelper.readRevision(database));
        CompoundTag oldSchemaRoot = new CompoundTag();
        oldSchemaRoot.putInt("schema_version", 1);
        oldSchemaRoot.put("entries", new ListTag());
        oldSchemaRoot.put("unresolved_entries", new ListTag());
        oldSchemaRoot.putLong("next_sequence", 1L);
        database.deserializeNBT(null, oldSchemaRoot);
        assertEquals(5L, DatabaseTestReflectionHelper.readRevision(database));
        assertTrue(database.needsResave());
    }
    // ---------- batch update tests ----------
    @Test
    void batchUpdateShouldDelayRevisionIncrement() {
        StoredItemDatabase database = new StoredItemDatabase();
        long initialRevision = database.revision();
        database.beginBatchUpdate();
        database.store(new ItemStack(Items.STONE, 4));
        database.store(new ItemStack(Items.DIRT, 2));
        database.store(new ItemStack(Items.APPLE, 1));
        assertEquals(initialRevision, database.revision());
        database.endBatchUpdate();
        assertEquals(initialRevision + 1, database.revision());
    }
    @Test
    void nestedBatchUpdateShouldOnlyIncrementOnce() {
        StoredItemDatabase database = new StoredItemDatabase();
        long initialRevision = database.revision();
        database.beginBatchUpdate();
        database.store(new ItemStack(Items.STONE, 4));
        database.beginBatchUpdate();
        database.store(new ItemStack(Items.DIRT, 2));
        database.endBatchUpdate();
        assertEquals(initialRevision, database.revision());
        database.endBatchUpdate();
        assertEquals(initialRevision + 1, database.revision());
    }
    @Test
    void batchDepositShouldPreserveSequenceMonotonicity() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.beginBatchUpdate();
        for (int i = 0; i < 36; i++) {
            database.store(new ItemStack(Items.STONE, 1));
        }
        database.endBatchUpdate();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        StoredStackEntry entry = database.entries().get(key);
        assertEquals(36L, entry.amount());
        assertEquals(1L, entry.firstAdded());
        assertEquals(36L, entry.lastModified());
    }
    @Test
    void batchUpdateShouldNotAffectDataIntegrity() {
        StoredItemDatabase batchDatabase = new StoredItemDatabase();
        StoredItemDatabase normalDatabase = new StoredItemDatabase();
        batchDatabase.beginBatchUpdate();
        batchDatabase.store(new ItemStack(Items.STONE, 8));
        batchDatabase.store(new ItemStack(Items.DIRT, 4));
        batchDatabase.store(new ItemStack(Items.APPLE, 2));
        batchDatabase.endBatchUpdate();
        normalDatabase.store(new ItemStack(Items.STONE, 8));
        normalDatabase.store(new ItemStack(Items.DIRT, 4));
        normalDatabase.store(new ItemStack(Items.APPLE, 2));
        assertEquals(normalDatabase.entryCount(), batchDatabase.entryCount());
        for (StoredStackKey key : normalDatabase.entries().keySet()) {
            assertEquals(normalDatabase.getAmount(key), batchDatabase.getAmount(key));
        }
    }
    @Test
    void batchUpdateWithExceptionShouldStillEndBatch() {
        StoredItemDatabase database = new StoredItemDatabase();
        long initialRevision = database.revision();
        try {
            database.beginBatchUpdate();
            database.store(new ItemStack(Items.STONE, 4));
            throw new RuntimeException("模拟异常");
        } catch (RuntimeException e) {
            database.endBatchUpdate();
        }
        assertEquals(initialRevision + 1, database.revision());
        database.beginBatchUpdate();
        database.store(new ItemStack(Items.DIRT, 2));
        database.endBatchUpdate();
        assertEquals(initialRevision + 2, database.revision());
    }
    @Test
    void batchExtractShouldDelayRevisionIncrement() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 64));
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        long initialRevision = database.revision();
        database.beginBatchUpdate();
        database.extract(key, 16);
        database.extract(key, 16);
        database.extract(key, 16);
        assertEquals(initialRevision, database.revision());
        database.endBatchUpdate();
        assertEquals(initialRevision + 1, database.revision());
        assertEquals(16L, database.getAmount(key));
    }
    @Test
    void concurrentBatchUpdateShouldBeThreadSafe() throws Exception {
        StoredItemDatabase database = new StoredItemDatabase();
        int threadCount = 8;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int thread = 0; thread < threadCount; thread++) {
            tasks.add(() -> {
                database.beginBatchUpdate();
                for (int i = 0; i < operationsPerThread; i++) {
                    database.store(new ItemStack(Items.STONE, 1));
                }
                database.endBatchUpdate();
                return null;
            });
        }
        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();
        assertEquals((long) threadCount, database.revision());
        assertEquals((long) threadCount * operationsPerThread, database.getAmount(StoredStackKey.of(new ItemStack(Items.STONE))));
    }

    private CompoundTag entryTag(CompoundTag stackTag, long count, DatabaseCategory category, long lastModified) {
        CompoundTag entryTag = new CompoundTag();
        entryTag.put("stack", stackTag);
        entryTag.putLong("count", count);
        entryTag.putString("category", category.name());
        entryTag.putLong("last_modified", lastModified);
        return entryTag;
    }
    private CompoundTag entryTag(ItemStack stack, long count, DatabaseCategory category, long lastModified) {
        CompoundTag stackTag = new CompoundTag();
        stackTag.putString("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        stackTag.putInt("count", Math.max(1, stack.getCount()));
        return this.entryTag(stackTag, count, category, lastModified);
    }
    private CompoundTag invalidStackTag(String itemId) {
        CompoundTag stackTag = new CompoundTag();
        stackTag.putString("id", itemId);
        stackTag.putInt("count", 1);
        return stackTag;
    }
}
