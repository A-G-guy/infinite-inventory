package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseItemClassifier;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseStorageSavedData;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.LegacyMigrationState;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseStorageSavedDataTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

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
    void shouldFallbackToLegacyPublicKeyWhenCurrentSchemaSnapshotMissesNewKey() {
        CompoundTag currentRoot = new CompoundTag();
        currentRoot.putInt("schema_version", DatabaseStorageSavedData.CURRENT_SCHEMA_VERSION);
        currentRoot.put("database", this.unresolvedDatabase().serializeNBT(null));
        currentRoot.put("personal_databases", new ListTag());
        currentRoot.put("migration_states", new ListTag());

        DatabaseStorageSavedData restored = DatabaseStorageSavedData.fromTag(currentRoot, null);

        assertEquals(0, restored.publicDatabase().entryCount());
        assertEquals(1, restored.publicDatabase().unresolvedEntryCount());
    }

    @Test
    void shouldPersistPersonalDatabasesAndPendingMigrationStates() {
        UUID playerId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.fromTag(new CompoundTag(), null);
        storage.personalDatabase(playerId).mergeFrom(this.unresolvedDatabase());
        storage.recordMigrationState(playerId, new LegacyMigrationState(
                LegacyMigrationState.Status.PENDING_CLEANUP,
                123456789L,
                1
        ));

        CompoundTag serialized = storage.exportStorageTag(null);
        DatabaseStorageSavedData restored = DatabaseStorageSavedData.fromTag(serialized, null);

        assertTrue(restored.hasPersonalDatabase(playerId));
        assertEquals(1, restored.personalDatabaseView(playerId).unresolvedEntryCount());
        assertEquals(LegacyMigrationState.Status.PENDING_CLEANUP, restored.migrationState(playerId).status());
    }

    @Test
    void shouldPruneCompletedMigrationStatesOnExport() {
        UUID playerId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.fromTag(new CompoundTag(), null);
        storage.recordMigrationState(playerId, new LegacyMigrationState(
                LegacyMigrationState.Status.MIGRATED,
                123456789L,
                1
        ));

        CompoundTag serialized = storage.exportStorageTag(null);
        DatabaseStorageSavedData restored = DatabaseStorageSavedData.fromTag(serialized, null);

        assertNull(restored.migrationState(playerId));
    }

    @Test
    void shouldMarkStorageDirtyWhenNestedDatabaseRequiresResave() {
        UUID playerId = UUID.fromString("99999999-8888-7777-6666-555555555555");
        CompoundTag currentRoot = new CompoundTag();
        currentRoot.putInt("schema_version", DatabaseStorageSavedData.CURRENT_SCHEMA_VERSION);
        currentRoot.put("public_database", this.currentSchemaDatabaseTag());

        CompoundTag personalDatabaseTag = new CompoundTag();
        personalDatabaseTag.putUUID("player_uuid", playerId);
        personalDatabaseTag.put("database", this.oldSchemaDatabaseTag());
        ListTag personalDatabases = new ListTag();
        personalDatabases.add(personalDatabaseTag);
        currentRoot.put("personal_databases", personalDatabases);
        currentRoot.put("migration_states", new ListTag());

        DatabaseStorageSavedData restored = DatabaseStorageSavedData.fromTag(currentRoot, null);

        assertFalse(restored.publicDatabase().needsResave());
        assertTrue(restored.hasPersonalDatabase(playerId));
        assertTrue(restored.isDirty());
    }

    @Test
    void personalAndPublicDatabasesShouldStoreAndStackIndependently() throws ReflectiveOperationException {
        UUID playerId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.fromTag(new CompoundTag(), null);
        var key = DatabaseTestReflectionHelper.fakeKey("test:material");

        DatabaseTestReflectionHelper.forceEntry(storage.publicDatabase(), key, new StoredStackEntry(DatabaseTabs.DEFAULT_TAB_ID, 12L, 4L));
        storage.publicDatabase().mergeFrom(this.databaseWithEntry(key, new StoredStackEntry(DatabaseTabs.DEFAULT_TAB_ID, 5L, 6L)));
        DatabaseTestReflectionHelper.forceEntry(storage.personalDatabase(playerId), key, new StoredStackEntry(DatabaseTabs.DEFAULT_TAB_ID, 3L, 8L));

        assertEquals(17L, storage.publicDatabase().getAmount(key));
        assertEquals(3L, storage.personalDatabaseView(playerId).getAmount(key));
        assertEquals(0, storage.unresolvedEntryCount(DatabaseScope.PUBLIC, playerId));
        assertEquals(0, storage.unresolvedEntryCount(DatabaseScope.PERSONAL, playerId));
    }

    @Test
    void exportAndRestoreWithoutProviderShouldPreserveResolvedItemData() {
        UUID playerId = UUID.fromString("12345678-90ab-cdef-1234-567890abcdef");
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.fromTag(new CompoundTag(), null);
        ItemStack namedPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        namedPickaxe.set(DataComponents.CUSTOM_NAME, Component.literal("矿工一号"));
        namedPickaxe.set(DataComponents.DAMAGE, 9);
        StoredStackKey expectedKey = StoredStackKey.of(namedPickaxe);

        storage.personalDatabase(playerId).store(namedPickaxe);

        CompoundTag serialized = storage.exportStorageTag(null);
        DatabaseStorageSavedData restored = DatabaseStorageSavedData.fromTag(serialized, null);

        assertTrue(restored.hasPersonalDatabase(playerId));
        assertEquals(1, restored.personalDatabaseView(playerId).entryCount());
        assertEquals(0, restored.personalDatabaseView(playerId).unresolvedEntryCount());
        assertEquals(1L, restored.personalDatabaseView(playerId).getAmount(expectedKey));
    }

    @Test
    void clearedMigrationStateShouldNotBePersisted() {
        UUID playerId = UUID.fromString("fedcba98-7654-3210-fedc-ba9876543210");
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.fromTag(new CompoundTag(), null);
        storage.recordMigrationState(playerId, new LegacyMigrationState(
                LegacyMigrationState.Status.MIGRATED,
                456L,
                3
        ));

        assertTrue(storage.clearMigrationState(playerId));

        CompoundTag serialized = storage.exportStorageTag(null);
        DatabaseStorageSavedData restored = DatabaseStorageSavedData.fromTag(serialized, null);

        assertNull(restored.migrationState(playerId));
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

    private CompoundTag oldSchemaDatabaseTag() {
        CompoundTag databaseTag = new CompoundTag();
        databaseTag.putInt("schema_version", 1);
        databaseTag.put("entries", new ListTag());
        databaseTag.put("unresolved_entries", new ListTag());
        databaseTag.putLong("next_sequence", 1L);
        return databaseTag;
    }

    private CompoundTag currentSchemaDatabaseTag() {
        CompoundTag databaseTag = new CompoundTag();
        databaseTag.putInt("schema_version", StoredItemDatabase.CURRENT_SCHEMA_VERSION);
        databaseTag.putInt("classifier_version", DatabaseItemClassifier.CURRENT_VERSION);
        databaseTag.put("entries", new ListTag());
        databaseTag.put("unresolved_entries", new ListTag());
        databaseTag.putLong("next_sequence", 1L);
        return databaseTag;
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
