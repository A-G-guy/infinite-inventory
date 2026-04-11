package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseStorageSavedData;
import com.agguy.infiniteinventory.database.LegacyMigrationState;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseStorageSavedDataTest {
    @Test
    void shouldLoadLegacyPublicDatabaseIntoUnifiedStorage() {
        CompoundTag legacyRoot = new CompoundTag();
        legacyRoot.put("database", this.unresolvedDatabase().serializeNBT(null));

        DatabaseStorageSavedData restored = DatabaseStorageSavedData.fromTag(legacyRoot, null);

        assertEquals(0, restored.publicDatabase().entryCount());
        assertEquals(1, restored.publicDatabase().unresolvedEntryCount());
        assertNotNull(restored.consumePendingMigrationBackup());
    }

    @Test
    void shouldPersistPersonalDatabasesAndMigrationStates() {
        UUID playerId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.fromTag(new CompoundTag(), null);
        storage.personalDatabase(playerId).mergeFrom(this.unresolvedDatabase());
        storage.recordMigrationState(playerId, new LegacyMigrationState(
                LegacyMigrationState.Status.MIGRATED,
                123456789L,
                1
        ));

        CompoundTag serialized = storage.exportStorageTag(null);
        DatabaseStorageSavedData restored = DatabaseStorageSavedData.fromTag(serialized, null);

        assertTrue(restored.hasPersonalDatabase(playerId));
        assertEquals(1, restored.personalDatabaseView(playerId).unresolvedEntryCount());
        assertEquals(LegacyMigrationState.Status.MIGRATED, restored.migrationState(playerId).status());
    }

    @Test
    void personalAndPublicDatabasesShouldStoreAndStackIndependently() throws ReflectiveOperationException {
        UUID playerId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.fromTag(new CompoundTag(), null);
        var key = DatabaseTestReflectionHelper.fakeKey("test:material");

        DatabaseTestReflectionHelper.forceEntry(storage.publicDatabase(), key, new StoredStackEntry(DatabaseCategory.MATERIALS, 12L, 4L));
        storage.publicDatabase().mergeFrom(this.databaseWithEntry(key, new StoredStackEntry(DatabaseCategory.MATERIALS, 5L, 6L)));
        DatabaseTestReflectionHelper.forceEntry(storage.personalDatabase(playerId), key, new StoredStackEntry(DatabaseCategory.MATERIALS, 3L, 8L));

        assertEquals(17L, storage.publicDatabase().getAmount(key));
        assertEquals(3L, storage.personalDatabaseView(playerId).getAmount(key));
        assertEquals(0, storage.unresolvedEntryCount(DatabaseScope.PUBLIC, playerId));
        assertEquals(0, storage.unresolvedEntryCount(DatabaseScope.PERSONAL, playerId));
    }

    private StoredItemDatabase databaseWithEntry(StoredStackKey key, StoredStackEntry entry) {
        StoredItemDatabase database = new StoredItemDatabase();
        try {
            DatabaseTestReflectionHelper.forceEntry(database, key, entry);
            return database;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法构造数据库测试数据", exception);
        }
    }

    private StoredItemDatabase unresolvedDatabase() {
        CompoundTag currentRoot = new CompoundTag();
        currentRoot.putInt("schema_version", StoredItemDatabase.CURRENT_SCHEMA_VERSION);
        currentRoot.put("entries", new ListTag());

        ListTag unresolvedEntries = new ListTag();
        unresolvedEntries.add(this.entryTag("missing:ghost_ingot", 24L, DatabaseCategory.MATERIALS, 5L));
        currentRoot.put("unresolved_entries", unresolvedEntries);
        currentRoot.putLong("next_sequence", 8L);

        StoredItemDatabase database = new StoredItemDatabase();
        database.deserializeNBT(null, currentRoot);
        return database;
    }

    private CompoundTag entryTag(String itemId, long count, DatabaseCategory category, long lastModified) {
        CompoundTag stackTag = new CompoundTag();
        stackTag.putString("id", itemId);
        stackTag.putInt("count", 1);

        CompoundTag entryTag = new CompoundTag();
        entryTag.put("stack", stackTag);
        entryTag.putLong("count", count);
        entryTag.putString("category", category.name());
        entryTag.putLong("last_modified", lastModified);
        return entryTag;
    }
}
