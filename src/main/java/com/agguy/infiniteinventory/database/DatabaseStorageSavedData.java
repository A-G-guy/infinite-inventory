package com.agguy.infiniteinventory.database;

import com.agguy.infiniteinventory.InfiniteInventory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class DatabaseStorageSavedData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final String DATA_NAME = "infiniteinventory_public_database";
    private static final String SCHEMA_VERSION_KEY = "schema_version";
    private static final String PUBLIC_DATABASE_KEY = "public_database";
    private static final String LEGACY_PUBLIC_DATABASE_KEY = "database";
    private static final String PERSONAL_DATABASES_KEY = "personal_databases";
    private static final String PERSONAL_DATABASE_PLAYER_ID_KEY = "player_uuid";
    private static final String PERSONAL_DATABASE_DATA_KEY = "database";
    private static final String MIGRATION_STATES_KEY = "migration_states";
    private static final String MIGRATION_STATE_PLAYER_ID_KEY = "player_uuid";
    private static final String MIGRATION_STATE_DATA_KEY = "state";

    private final StoredItemDatabase publicDatabase = new StoredItemDatabase();
    private final Map<UUID, StoredItemDatabase> personalDatabases = new LinkedHashMap<>();
    private final Map<UUID, LegacyMigrationState> migrationStates = new LinkedHashMap<>();

    private long lastAutomaticBackupAtMillis;
    private PendingMigrationBackup pendingMigrationBackup;

    private DatabaseStorageSavedData() {
    }

    private DatabaseStorageSavedData(CompoundTag tag, HolderLookup.Provider provider) {
        this.loadFromStorageTag(tag, provider);
    }

    public static DatabaseStorageSavedData fromTag(CompoundTag tag, HolderLookup.Provider provider) {
        return new DatabaseStorageSavedData(tag, provider);
    }

    public static DatabaseStorageSavedData get(MinecraftServer server) {
        DatabaseBackupManager.markStorageAccessed(server);
        ServerLevel overworld = Objects.requireNonNull(server.overworld(), "The overworld must exist before database access");
        SavedData.Factory<DatabaseStorageSavedData> factory = new SavedData.Factory<>(
                DatabaseStorageSavedData::new,
                DatabaseStorageSavedData::new
        );
        DatabaseStorageSavedData data = overworld.getDataStorage().computeIfAbsent(factory, DATA_NAME);
        DatabaseBackupManager.flushPendingMigrationBackup(server, data);
        return data;
    }

    public StoredItemDatabase publicDatabase() {
        return this.publicDatabase;
    }

    public StoredItemDatabase personalDatabase(UUID playerId) {
        return this.personalDatabases.computeIfAbsent(playerId, ignored -> new StoredItemDatabase());
    }

    public StoredItemDatabase personalDatabaseView(UUID playerId) {
        StoredItemDatabase database = this.personalDatabases.get(playerId);
        return database == null ? new StoredItemDatabase() : database;
    }

    public boolean hasPersonalDatabase(UUID playerId) {
        return this.personalDatabases.containsKey(playerId);
    }

    public void prunePersonalDatabase(UUID playerId) {
        StoredItemDatabase database = this.personalDatabases.get(playerId);
        if (database != null && database.entryCount() == 0 && database.unresolvedEntryCount() == 0) {
            this.personalDatabases.remove(playerId);
        }
    }

    public int unresolvedEntryCount(DatabaseScope scope, UUID playerId) {
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            return this.publicDatabase.unresolvedEntryCount();
        }
        StoredItemDatabase personalDatabase = this.personalDatabases.get(playerId);
        return personalDatabase == null ? 0 : personalDatabase.unresolvedEntryCount();
    }

    public LegacyMigrationState migrationState(UUID playerId) {
        return this.migrationStates.get(playerId);
    }

    public void recordMigrationState(UUID playerId, LegacyMigrationState migrationState) {
        if (playerId == null || migrationState == null) {
            return;
        }
        this.migrationStates.put(playerId, migrationState);
    }

    public CompoundTag exportStorageTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SCHEMA_VERSION_KEY, CURRENT_SCHEMA_VERSION);
        tag.put(PUBLIC_DATABASE_KEY, this.publicDatabase.serializeNBT(provider));

        ListTag serializedPersonalDatabases = new ListTag();
        for (Map.Entry<UUID, StoredItemDatabase> entry : this.personalDatabases.entrySet()) {
            StoredItemDatabase database = entry.getValue();
            if (database.entryCount() == 0 && database.unresolvedEntryCount() == 0) {
                continue;
            }
            CompoundTag personalDatabaseTag = new CompoundTag();
            personalDatabaseTag.putUUID(PERSONAL_DATABASE_PLAYER_ID_KEY, entry.getKey());
            personalDatabaseTag.put(PERSONAL_DATABASE_DATA_KEY, database.serializeNBT(provider));
            serializedPersonalDatabases.add(personalDatabaseTag);
        }
        tag.put(PERSONAL_DATABASES_KEY, serializedPersonalDatabases);

        ListTag serializedMigrationStates = new ListTag();
        for (Map.Entry<UUID, LegacyMigrationState> entry : this.migrationStates.entrySet()) {
            CompoundTag migrationStateTag = new CompoundTag();
            migrationStateTag.putUUID(MIGRATION_STATE_PLAYER_ID_KEY, entry.getKey());
            migrationStateTag.put(MIGRATION_STATE_DATA_KEY, entry.getValue().toTag());
            serializedMigrationStates.add(migrationStateTag);
        }
        tag.put(MIGRATION_STATES_KEY, serializedMigrationStates);
        return tag;
    }

    public void restoreFromSnapshot(CompoundTag storageTag, HolderLookup.Provider provider) {
        this.loadFromStorageTag(storageTag, provider);
        this.pendingMigrationBackup = null;
    }

    public boolean shouldCreateRollingBackup(long nowMillis) {
        return nowMillis - this.lastAutomaticBackupAtMillis >= DatabaseBackupManager.ROLLING_BACKUP_INTERVAL_MILLIS;
    }

    public void markAutomaticBackupCreated(long nowMillis) {
        this.lastAutomaticBackupAtMillis = Math.max(this.lastAutomaticBackupAtMillis, nowMillis);
    }

    public PendingMigrationBackup consumePendingMigrationBackup() {
        PendingMigrationBackup backup = this.pendingMigrationBackup;
        this.pendingMigrationBackup = null;
        return backup;
    }

    public void restorePendingMigrationBackup(PendingMigrationBackup backup) {
        if (backup != null) {
            this.pendingMigrationBackup = backup;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag storageTag = this.exportStorageTag(provider);
        for (String key : storageTag.getAllKeys()) {
            tag.put(key, storageTag.get(key));
        }
        return tag;
    }

    private void loadFromStorageTag(CompoundTag tag, HolderLookup.Provider provider) {
        this.publicDatabase.clear();
        this.personalDatabases.clear();
        this.migrationStates.clear();
        this.pendingMigrationBackup = null;
        if (tag == null || tag.isEmpty()) {
            return;
        }
        if (!tag.contains(SCHEMA_VERSION_KEY)) {
            this.loadLegacyPublicFormat(tag, provider);
            return;
        }
        this.loadCurrentFormat(tag, provider);
    }

    private void loadCurrentFormat(CompoundTag tag, HolderLookup.Provider provider) {
        int storedSchemaVersion = Math.max(0, tag.getInt(SCHEMA_VERSION_KEY));
        this.publicDatabase.deserializeNBT(provider, this.resolvePublicDatabaseTag(tag));

        for (Tag entry : tag.getList(PERSONAL_DATABASES_KEY, Tag.TAG_COMPOUND)) {
            if (!(entry instanceof CompoundTag personalDatabaseTag) || !personalDatabaseTag.hasUUID(PERSONAL_DATABASE_PLAYER_ID_KEY)) {
                continue;
            }
            StoredItemDatabase database = new StoredItemDatabase();
            database.deserializeNBT(provider, personalDatabaseTag.getCompound(PERSONAL_DATABASE_DATA_KEY));
            if (database.entryCount() == 0 && database.unresolvedEntryCount() == 0) {
                continue;
            }
            this.personalDatabases.put(personalDatabaseTag.getUUID(PERSONAL_DATABASE_PLAYER_ID_KEY), database);
        }

        for (Tag entry : tag.getList(MIGRATION_STATES_KEY, Tag.TAG_COMPOUND)) {
            if (!(entry instanceof CompoundTag migrationStateTag) || !migrationStateTag.hasUUID(MIGRATION_STATE_PLAYER_ID_KEY)) {
                continue;
            }
            this.migrationStates.put(
                    migrationStateTag.getUUID(MIGRATION_STATE_PLAYER_ID_KEY),
                    LegacyMigrationState.fromTag(migrationStateTag.getCompound(MIGRATION_STATE_DATA_KEY))
            );
        }

        if (storedSchemaVersion < CURRENT_SCHEMA_VERSION) {
            this.pendingMigrationBackup = new PendingMigrationBackup(
                    "schema-upgrade-v" + storedSchemaVersion,
                    this.exportStorageTag(provider)
            );
        }
    }

    private CompoundTag resolvePublicDatabaseTag(CompoundTag rootTag) {
        if (rootTag.contains(PUBLIC_DATABASE_KEY, Tag.TAG_COMPOUND)) {
            return rootTag.getCompound(PUBLIC_DATABASE_KEY);
        }
        return rootTag.getCompound(LEGACY_PUBLIC_DATABASE_KEY);
    }

    private void loadLegacyPublicFormat(CompoundTag tag, HolderLookup.Provider provider) {
        this.publicDatabase.deserializeNBT(provider, tag.getCompound(LEGACY_PUBLIC_DATABASE_KEY));
        this.pendingMigrationBackup = new PendingMigrationBackup(
                "legacy-public-format",
                this.exportStorageTag(provider)
        );
    }

    public record PendingMigrationBackup(String reason, CompoundTag storageSnapshot) {
        public PendingMigrationBackup {
            reason = reason == null || reason.isBlank() ? "migration" : reason;
            storageSnapshot = storageSnapshot == null ? new CompoundTag() : storageSnapshot.copy();
        }
    }
}
