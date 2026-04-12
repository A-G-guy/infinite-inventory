package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseItemClassifier;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void serializeShouldWriteClassifierVersion() {
        StoredItemDatabase database = new StoredItemDatabase();

        assertEquals(DatabaseItemClassifier.CURRENT_VERSION, database.serializeNBT(null).getInt("classifier_version"));
    }

    @Test
    void mergeFromShouldNormalizeCategoryAndPreserveStackedAmount() throws ReflectiveOperationException {
        StoredItemDatabase targetDatabase = new StoredItemDatabase();
        StoredItemDatabase sourceDatabase = new StoredItemDatabase();
        var key = DatabaseTestReflectionHelper.fakeKey("test:material");

        DatabaseTestReflectionHelper.forceEntry(targetDatabase, key, new StoredStackEntry(DatabaseCategory.OTHER, 16L, 4L));
        DatabaseTestReflectionHelper.forceEntry(sourceDatabase, key, new StoredStackEntry(DatabaseCategory.MATERIALS, 8L, 7L));

        targetDatabase.mergeFrom(sourceDatabase);

        StoredStackEntry mergedEntry = targetDatabase.entries().get(key);
        assertEquals(DatabaseCategory.MATERIALS, mergedEntry.category());
        assertEquals(24L, mergedEntry.amount());
        assertEquals(7L, mergedEntry.lastModified());
    }

    @Test
    void mergeFromShouldAdvanceNextSequenceWhenSourceSequenceIsStale() throws ReflectiveOperationException {
        StoredItemDatabase targetDatabase = new StoredItemDatabase();
        StoredItemDatabase sourceDatabase = new StoredItemDatabase();
        var key = DatabaseTestReflectionHelper.fakeKey("test:sequence_item");

        DatabaseTestReflectionHelper.forceEntry(sourceDatabase, key, new StoredStackEntry(DatabaseCategory.MATERIALS, 6L, 25L));
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
    void recategorizeShouldApplyLatestClassifierRulesToResolvedEntries() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.MINECART));
        DatabaseTestReflectionHelper.forceEntry(database, key, new StoredStackEntry(DatabaseCategory.MATERIALS, 4L, 12L));

        assertTrue(DatabaseTestReflectionHelper.invokeRecategorizeResolvedEntriesIfNeeded(database, 0));

        StoredStackEntry recategorizedEntry = database.entries().get(key);
        assertEquals(DatabaseCategory.OTHER, recategorizedEntry.category());
        assertEquals(4L, recategorizedEntry.amount());
        assertEquals(12L, recategorizedEntry.lastModified());
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

    private CompoundTag entryTag(CompoundTag stackTag, long count, DatabaseCategory category, long lastModified) {
        CompoundTag entryTag = new CompoundTag();
        entryTag.put("stack", stackTag);
        entryTag.putLong("count", count);
        entryTag.putString("category", category.name());
        entryTag.putLong("last_modified", lastModified);
        return entryTag;
    }

    private CompoundTag invalidStackTag(String itemId) {
        CompoundTag stackTag = new CompoundTag();
        stackTag.putString("id", itemId);
        stackTag.putInt("count", 1);
        return stackTag;
    }
}
