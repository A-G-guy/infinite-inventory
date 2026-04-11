package com.agguy.infiniteinventory.database;

import com.agguy.infiniteinventory.InfiniteInventory;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DatabaseBackupManager {
    public static final long ROLLING_BACKUP_INTERVAL_MILLIS = 15L * 60L * 1000L;
    private static final int BACKUP_FORMAT_VERSION = 1;
    private static final int AUTO_MANAGED_BACKUP_LIMIT = 12;

    private static final String BACKUP_FORMAT_VERSION_KEY = "backup_format_version";
    private static final String TYPE_KEY = "type";
    private static final String REASON_KEY = "reason";
    private static final String CREATED_AT_MILLIS_KEY = "created_at_millis";
    private static final String STORAGE_KEY = "storage";
    private static final String FILE_EXTENSION = ".nbt.gz";
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneId.of("UTC"));
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Set<MinecraftServer> ACCESSED_SERVERS = Collections.newSetFromMap(new WeakHashMap<>());

    private DatabaseBackupManager() {
    }

    public static void markStorageAccessed(MinecraftServer server) {
        if (server != null) {
            ACCESSED_SERVERS.add(server);
        }
    }

    public static void flushPendingMigrationBackup(MinecraftServer server, DatabaseStorageSavedData storage) {
        if (server == null || storage == null) {
            return;
        }
        DatabaseStorageSavedData.PendingMigrationBackup pendingMigrationBackup = storage.consumePendingMigrationBackup();
        if (pendingMigrationBackup == null) {
            return;
        }
        try {
            createBackup(server, DatabaseBackupType.MIGRATION, pendingMigrationBackup.reason(), pendingMigrationBackup.storageSnapshot());
        } catch (IOException exception) {
            storage.restorePendingMigrationBackup(pendingMigrationBackup);
            LOGGER.error("写入数据库迁移备份失败: {}", pendingMigrationBackup.reason(), exception);
        }
    }

    public static Path createManualBackup(MinecraftServer server, String reason) throws IOException {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(server);
        return createBackup(server, DatabaseBackupType.MANUAL, reason, storage.exportStorageTag(server.registryAccess()));
    }

    public static Path createMigrationBackup(MinecraftServer server, String reason, CompoundTag storageSnapshot) throws IOException {
        return createBackup(server, DatabaseBackupType.MIGRATION, reason, storageSnapshot);
    }

    public static Path createPreRestoreBackup(MinecraftServer server, String reason) throws IOException {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(server);
        return createBackup(server, DatabaseBackupType.PRE_RESTORE, reason, storage.exportStorageTag(server.registryAccess()));
    }

    public static void maybeCreateRollingBackup(MinecraftServer server) {
        if (server == null || !ACCESSED_SERVERS.contains(server)) {
            return;
        }
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(server);
        if (!storage.isDirty()) {
            return;
        }
        long nowMillis = System.currentTimeMillis();
        if (!storage.shouldCreateRollingBackup(nowMillis)) {
            return;
        }
        try {
            createBackup(server, DatabaseBackupType.ROLLING, "dirty-window", storage.exportStorageTag(server.registryAccess()));
            storage.markAutomaticBackupCreated(nowMillis);
        } catch (IOException exception) {
            LOGGER.error("写入数据库轮换备份失败", exception);
        }
    }

    public static List<DatabaseBackupInfo> listBackups(MinecraftServer server) {
        Path backupDirectory = backupDirectory(server);
        if (!Files.isDirectory(backupDirectory)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(backupDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(FILE_EXTENSION))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .map(DatabaseBackupManager::readBackupInfo)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (IOException exception) {
            LOGGER.error("列出数据库备份失败", exception);
            return List.of();
        }
    }

    public static DatabaseBackupInfo restoreBackup(MinecraftServer server, String fileName) throws IOException {
        Path backupFile = resolveBackupFile(server, fileName);
        CompoundTag backupTag = NbtIo.readCompressed(backupFile, NbtAccounter.unlimitedHeap());
        CompoundTag storageSnapshot = backupTag.getCompound(STORAGE_KEY);
        if (storageSnapshot.isEmpty()) {
            throw new IOException("备份文件不包含可恢复的数据库快照: " + fileName);
        }
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(server);
        storage.restoreFromSnapshot(storageSnapshot, server.registryAccess());
        storage.setDirty();
        ACCESSED_SERVERS.add(server);
        return parseBackupInfo(backupFile.getFileName().toString(), backupTag);
    }

    private static Path createBackup(MinecraftServer server, DatabaseBackupType type, String reason, CompoundTag storageSnapshot) throws IOException {
        Path backupDirectory = backupDirectory(server);
        Files.createDirectories(backupDirectory);

        String fileName = FILE_TIME_FORMATTER.format(Instant.ofEpochMilli(System.currentTimeMillis()))
                + "_" + type.name().toLowerCase(Locale.ROOT)
                + "_" + sanitizeReason(reason)
                + FILE_EXTENSION;
        Path targetPath = backupDirectory.resolve(fileName);
        Path tempPath = Files.createTempFile(backupDirectory, "snapshot-", ".tmp");

        CompoundTag backupTag = new CompoundTag();
        backupTag.putInt(BACKUP_FORMAT_VERSION_KEY, BACKUP_FORMAT_VERSION);
        backupTag.putString(TYPE_KEY, type.name());
        backupTag.putString(REASON_KEY, sanitizeReason(reason));
        backupTag.putLong(CREATED_AT_MILLIS_KEY, System.currentTimeMillis());
        backupTag.put(STORAGE_KEY, storageSnapshot == null ? new CompoundTag() : storageSnapshot.copy());

        try {
            NbtIo.writeCompressed(backupTag, tempPath);
            moveAtomically(tempPath, targetPath);
        } catch (IOException exception) {
            Files.deleteIfExists(tempPath);
            throw exception;
        }

        if (type.autoManaged()) {
            trimAutoManagedBackups(backupDirectory);
        }
        return targetPath;
    }

    private static void trimAutoManagedBackups(Path backupDirectory) throws IOException {
        List<Path> autoManagedBackups = new ArrayList<>();
        try (Stream<Path> stream = Files.list(backupDirectory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(FILE_EXTENSION))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .forEach(path -> {
                        DatabaseBackupInfo info = readBackupInfo(path);
                        if (info != null && info.type().autoManaged()) {
                            autoManagedBackups.add(path);
                        }
                    });
        }
        for (int index = AUTO_MANAGED_BACKUP_LIMIT; index < autoManagedBackups.size(); index++) {
            Files.deleteIfExists(autoManagedBackups.get(index));
        }
    }

    private static DatabaseBackupInfo readBackupInfo(Path path) {
        try {
            CompoundTag backupTag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            return parseBackupInfo(path.getFileName().toString(), backupTag);
        } catch (IOException exception) {
            LOGGER.warn("读取数据库备份元数据失败: {}", path, exception);
            return null;
        }
    }

    private static DatabaseBackupInfo parseBackupInfo(String fileName, CompoundTag backupTag) {
        DatabaseBackupType type = readBackupType(backupTag.getString(TYPE_KEY));
        String reason = backupTag.getString(REASON_KEY);
        long createdAtMillis = backupTag.getLong(CREATED_AT_MILLIS_KEY);
        return new DatabaseBackupInfo(fileName, type, reason, createdAtMillis);
    }

    private static DatabaseBackupType readBackupType(String serializedType) {
        try {
            return DatabaseBackupType.valueOf(serializedType);
        } catch (IllegalArgumentException exception) {
            return DatabaseBackupType.MANUAL;
        }
    }

    private static Path resolveBackupFile(MinecraftServer server, String fileName) throws IOException {
        if (fileName == null || fileName.isBlank() || fileName.contains("/") || fileName.contains("\\")) {
            throw new IOException("非法备份文件名: " + fileName);
        }
        Path backupFile = backupDirectory(server).resolve(fileName).normalize();
        if (!Files.isRegularFile(backupFile)) {
            throw new IOException("备份文件不存在: " + fileName);
        }
        return backupFile;
    }

    private static Path backupDirectory(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve(InfiniteInventory.MODID)
                .resolve("backups");
    }

    private static String sanitizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "snapshot";
        }
        return reason.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
