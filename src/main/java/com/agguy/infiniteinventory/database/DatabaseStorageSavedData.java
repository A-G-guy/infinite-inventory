package com.agguy.infiniteinventory.database;

import com.agguy.infiniteinventory.InfiniteInventory;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    public static final int CURRENT_SCHEMA_VERSION = 2;

    private static final String DATA_NAME = "infiniteinventory_public_database";
    private static final String SCHEMA_VERSION_KEY = "schema_version";
    private static final String PUBLIC_DATABASE_KEY = "public_database";
    private static final String PUBLIC_TABS_KEY = "public_tabs";
    private static final String LEGACY_PUBLIC_DATABASE_KEY = "database";
    private static final String PERSONAL_DATABASES_KEY = "personal_databases";
    private static final String PERSONAL_DATABASE_PLAYER_ID_KEY = "player_uuid";
    private static final String PERSONAL_DATABASE_DATA_KEY = "database";
    private static final String PERSONAL_TAB_DIRECTORIES_KEY = "personal_tab_directories";
    private static final String PERSONAL_TAB_DIRECTORY_PLAYER_ID_KEY = "player_uuid";
    private static final String PERSONAL_TAB_DIRECTORY_DATA_KEY = "tabs";
    private static final String MIGRATION_STATES_KEY = "migration_states";
    private static final String MIGRATION_STATE_PLAYER_ID_KEY = "player_uuid";
    private static final String MIGRATION_STATE_DATA_KEY = "state";

    private final StoredItemDatabase publicDatabase = new StoredItemDatabase();
    private DatabaseTabDirectory publicTabs = new DatabaseTabDirectory();
    private final Map<UUID, StoredItemDatabase> personalDatabases = new LinkedHashMap<>();
    private final Map<UUID, DatabaseTabDirectory> personalTabDirectories = new LinkedHashMap<>();
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

    public DatabaseTabDirectory publicTabs() {
        return this.publicTabs;
    }

    public StoredItemDatabase personalDatabase(UUID playerId) {
        this.personalTabs(playerId);
        return this.personalDatabases.computeIfAbsent(playerId, ignored -> new StoredItemDatabase());
    }

    public StoredItemDatabase personalDatabaseView(UUID playerId) {
        StoredItemDatabase database = this.personalDatabases.get(playerId);
        return database == null ? new StoredItemDatabase() : database;
    }

    public DatabaseTabDirectory personalTabs(UUID playerId) {
        return this.personalTabDirectories.computeIfAbsent(playerId, ignored -> new DatabaseTabDirectory());
    }

    public DatabaseTabDirectory personalTabsView(UUID playerId) {
        DatabaseTabDirectory tabDirectory = this.personalTabDirectories.get(playerId);
        return tabDirectory == null ? new DatabaseTabDirectory() : tabDirectory;
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

    public boolean clearMigrationState(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return this.migrationStates.remove(playerId) != null;
    }

    public CompoundTag exportStorageTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SCHEMA_VERSION_KEY, CURRENT_SCHEMA_VERSION);
        tag.put(PUBLIC_DATABASE_KEY, this.publicDatabase.serializeNBT(provider));
        tag.put(PUBLIC_TABS_KEY, this.publicTabs.toTag());

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

        ListTag serializedTabDirectories = new ListTag();
        LinkedHashSet<UUID> playerIds = new LinkedHashSet<>();
        playerIds.addAll(this.personalDatabases.keySet());
        playerIds.addAll(this.personalTabDirectories.keySet());
        for (UUID playerId : playerIds) {
            CompoundTag tabDirectoryTag = new CompoundTag();
            tabDirectoryTag.putUUID(PERSONAL_TAB_DIRECTORY_PLAYER_ID_KEY, playerId);
            tabDirectoryTag.put(PERSONAL_TAB_DIRECTORY_DATA_KEY, this.personalTabsView(playerId).toTag());
            serializedTabDirectories.add(tabDirectoryTag);
        }
        tag.put(PERSONAL_TAB_DIRECTORIES_KEY, serializedTabDirectories);

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
        this.publicTabs = new DatabaseTabDirectory();
        this.personalDatabases.clear();
        this.personalTabDirectories.clear();
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
        boolean needsResave = storedSchemaVersion < CURRENT_SCHEMA_VERSION;
        this.publicDatabase.deserializeNBT(provider, this.resolvePublicDatabaseTag(tag));
        this.publicTabs = tag.contains(PUBLIC_TABS_KEY, Tag.TAG_COMPOUND)
                ? DatabaseTabDirectory.fromTag(tag.getCompound(PUBLIC_TABS_KEY))
                : new DatabaseTabDirectory();
        needsResave = needsResave || !tag.contains(PUBLIC_TABS_KEY, Tag.TAG_COMPOUND) || this.publicDatabase.needsResave();
        needsResave = this.publicDatabase.ensureTabAssignments(this.publicTabs) || needsResave;

        for (Tag entry : tag.getList(PERSONAL_DATABASES_KEY, Tag.TAG_COMPOUND)) {
            if (!(entry instanceof CompoundTag personalDatabaseTag) || !personalDatabaseTag.hasUUID(PERSONAL_DATABASE_PLAYER_ID_KEY)) {
                continue;
            }
            StoredItemDatabase database = new StoredItemDatabase();
            database.deserializeNBT(provider, personalDatabaseTag.getCompound(PERSONAL_DATABASE_DATA_KEY));
            this.personalDatabases.put(personalDatabaseTag.getUUID(PERSONAL_DATABASE_PLAYER_ID_KEY), database);
            needsResave = needsResave || database.needsResave();
        }

        for (Tag entry : tag.getList(PERSONAL_TAB_DIRECTORIES_KEY, Tag.TAG_COMPOUND)) {
            if (!(entry instanceof CompoundTag tabDirectoryTag) || !tabDirectoryTag.hasUUID(PERSONAL_TAB_DIRECTORY_PLAYER_ID_KEY)) {
                continue;
            }
            this.personalTabDirectories.put(
                    tabDirectoryTag.getUUID(PERSONAL_TAB_DIRECTORY_PLAYER_ID_KEY),
                    DatabaseTabDirectory.fromTag(tabDirectoryTag.getCompound(PERSONAL_TAB_DIRECTORY_DATA_KEY))
            );
        }

        LinkedHashSet<UUID> playerIds = new LinkedHashSet<>();
        playerIds.addAll(this.personalDatabases.keySet());
        playerIds.addAll(this.personalTabDirectories.keySet());
        for (UUID playerId : playerIds) {
            DatabaseTabDirectory tabDirectory = this.personalTabs(playerId);
            StoredItemDatabase database = this.personalDatabases.get(playerId);
            if (database != null) {
                needsResave = database.ensureTabAssignments(tabDirectory) || needsResave;
            } else if (!this.personalTabDirectories.containsKey(playerId)) {
                needsResave = true;
            }
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
        if (needsResave) {
            this.setDirty();
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
        this.publicTabs = new DatabaseTabDirectory();
        this.publicDatabase.ensureTabAssignments(this.publicTabs);
        this.pendingMigrationBackup = new PendingMigrationBackup(
                "legacy-public-format",
                this.exportStorageTag(provider)
        );
        this.setDirty();
    }

    public record PendingMigrationBackup(String reason, CompoundTag storageSnapshot) {
        public PendingMigrationBackup {
            reason = reason == null || reason.isBlank() ? "migration" : reason;
            storageSnapshot = storageSnapshot == null ? new CompoundTag() : storageSnapshot.copy();
        }
    }
}
