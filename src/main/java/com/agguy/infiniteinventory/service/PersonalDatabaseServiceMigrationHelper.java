package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseBackupManager;
import com.agguy.infiniteinventory.database.DatabaseStorageSavedData;
import com.agguy.infiniteinventory.database.LegacyMigrationState;
import com.agguy.infiniteinventory.database.PlayerDatabaseAttachment;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PersonalDatabaseServiceMigrationHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    private PersonalDatabaseServiceMigrationHelper() {
    }

    static void ensureLegacyPersonalMigration(PersonalDatabaseService service, ServerPlayer player) {
        ensureLegacyPersonalMigration(service, player, DatabaseStorageSavedData.get(player.server));
    }

    private static void ensureLegacyPersonalMigration(
            PersonalDatabaseService service,
            ServerPlayer player,
            DatabaseStorageSavedData storage
    ) {
        try {
            PlayerDatabaseAttachment legacyDatabase = service.getLegacyPersonalDatabase(player);
            if (legacyDatabase.entryCount() == 0 && legacyDatabase.unresolvedEntryCount() == 0) {
                pruneStaleMigrationState(storage, player.getUUID());
                return;
            }
            UUID playerId = player.getUUID();
            int legacyEntryCount = legacyDatabase.entryCount() + legacyDatabase.unresolvedEntryCount();
            if (!storage.hasPersonalDatabase(playerId)) {
                storage.personalDatabase(playerId).mergeFrom(legacyDatabase);
                storage.recordMigrationState(playerId, new LegacyMigrationState(
                        LegacyMigrationState.Status.PENDING_CLEANUP,
                        System.currentTimeMillis(),
                        legacyEntryCount
                ));
                storage.setDirty();
                LOGGER.info("已将玩家 {} 的旧个人数据库导入统一存储，等待迁移备份完成后清理旧附件", player.getGameProfile().getName());
            }
            tryFinalizeLegacyCleanup(player, storage, legacyDatabase, legacyEntryCount);
        } catch (Exception exception) {
            LOGGER.error("玩家 {} 的旧个人数据库迁移失败，已跳过以避免阻塞菜单打开", player.getGameProfile().getName(), exception);
        }
    }

    private static void tryFinalizeLegacyCleanup(
            ServerPlayer player,
            DatabaseStorageSavedData storage,
            PlayerDatabaseAttachment legacyDatabase,
            int legacyEntryCount
    ) {
        if (legacyDatabase.entryCount() == 0 && legacyDatabase.unresolvedEntryCount() == 0) {
            return;
        }
        UUID playerId = player.getUUID();
        LegacyMigrationState migrationState = storage.migrationState(playerId);
        if (!storage.hasPersonalDatabase(playerId)) {
            return;
        }
        if (migrationState == null) {
            storage.recordMigrationState(playerId, new LegacyMigrationState(
                    LegacyMigrationState.Status.SKIPPED_EXISTING_STORAGE,
                    System.currentTimeMillis(),
                    legacyEntryCount
            ));
            storage.setDirty();
            LOGGER.warn("玩家 {} 同时存在旧附件个人库与统一存储个人库，已跳过重复导入旧附件", player.getGameProfile().getName());
            return;
        }
        if (migrationState.status() == LegacyMigrationState.Status.SKIPPED_EXISTING_STORAGE) {
            return;
        }
        try {
            DatabaseBackupManager.createMigrationBackup(
                    player.server,
                    "legacy-personal-" + playerId,
                    storage.exportStorageTag(player.registryAccess())
            );
            legacyDatabase.clear();
            storage.clearMigrationState(playerId);
            storage.setDirty();
            LOGGER.info("已完成玩家 {} 的旧个人数据库迁移并清理旧附件", player.getGameProfile().getName());
        } catch (java.io.IOException exception) {
            LOGGER.error("为玩家 {} 生成旧个人数据库迁移备份失败，旧附件已保留", player.getGameProfile().getName(), exception);
        }
    }

    private static void pruneStaleMigrationState(DatabaseStorageSavedData storage, UUID playerId) {
        if (storage == null || playerId == null) {
            return;
        }
        if (storage.clearMigrationState(playerId)) {
            storage.setDirty();
        }
    }
}
