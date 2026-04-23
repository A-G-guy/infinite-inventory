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

/**
 * 世界存档级数据库持久化管理器，统管公共/个人数据库与标签目录的存储、加载、迁移备份及滚动备份策略。
 *
 * <p>设计意图：Minecraft 的 {@link SavedData} 机制天然适合作为跨会话的全局状态容器。
 * 本类将模组中所有需要持久化的核心数据（物品数据库、标签目录、迁移状态）收敛到单一事实来源，
 * 避免数据散落在多个文件或内存对象中导致的同步与一致性难题。</p>
 *
 * <p>在系统中的位置：位于数据持久化层最顶层，向下委托给 {@link StoredItemDatabase}、
 * {@link DatabaseTabDirectory} 等对象完成具体序列化，向上通过 {@link #get(MinecraftServer)}
 * 为服务端逻辑提供统一入口。滚动备份与迁移备份的触发由 {@link DatabaseBackupManager} 协调。</p>
 */
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

    /**
     * 从已持久化的 NBT 标签重建数据实例。
     *
     * @param tag      存档中读取到的原始 NBT 数据，可能为空或包含旧版格式
     * @param provider 用于物品栈反序列化的注册表上下文
     * @return 重建后的数据实例，内部会自动处理版本迁移与旧格式兼容
     */
    public static DatabaseStorageSavedData fromTag(CompoundTag tag, HolderLookup.Provider provider) {
        return new DatabaseStorageSavedData(tag, provider);
    }

    /**
     * 获取（或惰性创建）与指定服务端实例绑定的存档数据。
     *
     * <p>业务约束：必须在主世界（Overworld）已加载完成后调用，因为 Minecraft 的
     * {@code SavedData} 依附于维度级别的 {@code DataStorage}。</p>
     *
     * @param server 当前运行的服务端实例
     * @return 与该服务端绑定的唯一数据实例
     * @throws NullPointerException 若主世界尚未生成
     */
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

    /**
     * 返回公共数据库的可变引用。
     *
     * @return 所有玩家共享的全局物品数据库
     */
    public StoredItemDatabase publicDatabase() {
        return this.publicDatabase;
    }

    /**
     * 返回公共标签目录的可变引用。
     *
     * @return 所有玩家共享的全局标签页配置
     */
    public DatabaseTabDirectory publicTabs() {
        return this.publicTabs;
    }

    /**
     * 获取指定玩家的个人数据库，若不存在则自动创建。
     *
     * <p>设计决策：在创建数据库的同时会隐式初始化该玩家的标签目录，
     * 确保后续查询不会因标签目录缺失而抛出异常。</p>
     *
     * @param playerId 玩家唯一标识
     * @return 该玩家的个人物品数据库
     */
    public StoredItemDatabase personalDatabase(UUID playerId) {
        this.personalTabs(playerId);
        return this.personalDatabases.computeIfAbsent(playerId, ignored -> new StoredItemDatabase());
    }

    /**
     * 以只读视角获取指定玩家的个人数据库，若不存在则返回空实例。
     *
     * <p>与 {@link #personalDatabase(UUID)} 的区别：不会触发自动创建，
     * 适合仅做数据查看而不希望污染持久化存储的场景。</p>
     *
     * @param playerId 玩家唯一标识
     * @return 该玩家的个人物品数据库；若从未创建过则返回空数据库
     */
    public StoredItemDatabase personalDatabaseView(UUID playerId) {
        StoredItemDatabase database = this.personalDatabases.get(playerId);
        return database == null ? new StoredItemDatabase() : database;
    }

    /**
     * 获取指定玩家的个人标签目录，若不存在则自动创建。
     *
     * @param playerId 玩家唯一标识
     * @return 该玩家的个人标签页配置
     */
    public DatabaseTabDirectory personalTabs(UUID playerId) {
        return this.personalTabDirectories.computeIfAbsent(playerId, ignored -> new DatabaseTabDirectory());
    }

    /**
     * 以只读视角获取指定玩家的个人标签目录，若不存在则返回空实例。
     *
     * @param playerId 玩家唯一标识
     * @return 该玩家的个人标签页配置；若从未创建过则返回空目录
     */
    public DatabaseTabDirectory personalTabsView(UUID playerId) {
        DatabaseTabDirectory tabDirectory = this.personalTabDirectories.get(playerId);
        return tabDirectory == null ? new DatabaseTabDirectory() : tabDirectory;
    }

    /**
     * 判断指定玩家是否已拥有个人数据库。
     *
     * @param playerId 玩家唯一标识
     * @return 若该玩家的个人数据库已创建则返回 {@code true}
     */
    public boolean hasPersonalDatabase(UUID playerId) {
        return this.personalDatabases.containsKey(playerId);
    }

    /**
     * 清理指定玩家的空个人数据库，防止存档中残留无意义条目。
     *
     * <p>业务约束：仅当数据库中既无已解析条目也无未解析条目时才会执行删除，
     * 避免误删包含待恢复数据的数据库。</p>
     *
     * @param playerId 玩家唯一标识
     */
    public void prunePersonalDatabase(UUID playerId) {
        StoredItemDatabase database = this.personalDatabases.get(playerId);
        if (database != null && database.entryCount() == 0 && database.unresolvedEntryCount() == 0) {
            this.personalDatabases.remove(playerId);
        }
    }

    /**
     * 查询指定作用域下未解析条目的数量。
     *
     * @param scope    数据库作用域（公共或个人）
     * @param playerId 当作用域为个人时必须提供玩家标识；公共作用域下该参数被忽略
     * @return 未解析条目的数量
     */
    public int unresolvedEntryCount(DatabaseScope scope, UUID playerId) {
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            return this.publicDatabase.unresolvedEntryCount();
        }
        StoredItemDatabase personalDatabase = this.personalDatabases.get(playerId);
        return personalDatabase == null ? 0 : personalDatabase.unresolvedEntryCount();
    }

    /**
     * 获取指定玩家的旧版数据迁移状态。
     *
     * @param playerId 玩家唯一标识
     * @return 迁移状态；若该玩家无迁移记录则返回 {@code null}
     */
    public LegacyMigrationState migrationState(UUID playerId) {
        return this.migrationStates.get(playerId);
    }

    /**
     * 记录指定玩家的迁移状态。
     *
     * <p>业务约束：空值参数会被静默忽略，避免无效状态污染存储。</p>
     *
     * @param playerId       玩家唯一标识
     * @param migrationState 迁移状态
     */
    public void recordMigrationState(UUID playerId, LegacyMigrationState migrationState) {
        if (playerId == null || migrationState == null) {
            return;
        }
        this.migrationStates.put(playerId, migrationState);
    }

    /**
     * 清除指定玩家的迁移状态记录。
     *
     * @param playerId 玩家唯一标识
     * @return 若确实存在并被清除则返回 {@code true}，否则返回 {@code false}
     */
    public boolean clearMigrationState(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return this.migrationStates.remove(playerId) != null;
    }

    /**
     * 将当前全部数据导出为可持久化的 NBT 标签。
     *
     * <p>设计决策：空个人数据库不会被序列化，以减少存档体积；
     * 标签目录则会为所有拥有数据库或目录的玩家统一写出，确保 UI 状态不丢失。</p>
     *
     * @param provider 用于物品栈序列化的注册表上下文
     * @return 包含完整存档数据的复合标签
     */
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

    /**
     * 从快照完全恢复数据状态，并清除任何待处理的迁移备份。
     *
     * <p>业务约束：通常由管理员手动回滚或备份恢复流程调用，
     * 恢复后会将 {@code pendingMigrationBackup} 置空，防止旧备份在下次保存时被误写。</p>
     *
     * @param storageTag 包含完整数据的快照标签
     * @param provider   用于物品栈反序列化的注册表上下文
     */
    public void restoreFromSnapshot(CompoundTag storageTag, HolderLookup.Provider provider) {
        this.loadFromStorageTag(storageTag, provider);
        this.pendingMigrationBackup = null;
    }

    /**
     * 判断当前是否满足创建滚动备份的时间条件。
     *
     * @param nowMillis 当前时间戳（毫秒）
     * @return 若距离上次自动备份已超过 {@link DatabaseBackupManager#ROLLING_BACKUP_INTERVAL_MILLIS} 则返回 {@code true}
     */
    public boolean shouldCreateRollingBackup(long nowMillis) {
        return nowMillis - this.lastAutomaticBackupAtMillis >= DatabaseBackupManager.ROLLING_BACKUP_INTERVAL_MILLIS;
    }

    /**
     * 标记已创建自动备份的时间戳。
     *
     * <p>设计决策：使用 {@code Math.max} 防止因时间回拨导致备份间隔被异常拉长。</p>
     *
     * @param nowMillis 备份创建时的时间戳（毫秒）
     */
    public void markAutomaticBackupCreated(long nowMillis) {
        this.lastAutomaticBackupAtMillis = Math.max(this.lastAutomaticBackupAtMillis, nowMillis);
    }

    /**
     * 消费（取出并清空）当前待处理的迁移备份。
     *
     * @return 待处理的迁移备份；若不存在则返回 {@code null}
     */
    public PendingMigrationBackup consumePendingMigrationBackup() {
        PendingMigrationBackup backup = this.pendingMigrationBackup;
        this.pendingMigrationBackup = null;
        return backup;
    }

    /**
     * 恢复一个待处理的迁移备份到当前实例。
     *
     * @param backup 迁移备份；若为 {@code null} 则不做任何操作
     */
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

    /**
     * 迁移过程中生成的待处理备份快照，用于在发生异常时回滚到升级前的完整状态。
     *
     * <p>设计决策：将迁移原因与完整数据快照封装为不可变记录，
     * 确保备份信息在传递过程中不会被意外篡改。构造函数会对空值做防御性处理，
     * 避免 NBT 拷贝时抛出异常。</p>
     *
     * @param reason         触发备份的原因标识，例如 {@code "schema-upgrade-v1"}
     * @param storageSnapshot 备份时刻的完整存档数据快照
     */
    public record PendingMigrationBackup(String reason, CompoundTag storageSnapshot) {
        public PendingMigrationBackup {
            reason = reason == null || reason.isBlank() ? "migration" : reason;
            storageSnapshot = storageSnapshot == null ? new CompoundTag() : storageSnapshot.copy();
        }
    }
}
