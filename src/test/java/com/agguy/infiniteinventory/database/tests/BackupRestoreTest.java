package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseStorageSavedData;
import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据库备份与恢复相关测试。
 *
 * <p>验证导出/恢复对称性、损坏快照的降级处理，以及恢复操作不会破坏已有数据。</p>
 */
class BackupRestoreTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void exportAndRestoreShouldBeSymmetric() {
        DatabaseStorageSavedData original = DatabaseStorageSavedData.fromTag(null, null);
        original.publicDatabase().store(new ItemStack(Items.STONE, 64));
        original.publicDatabase().store(new ItemStack(Items.DIRT, 32));

        UUID playerId = UUID.randomUUID();
        original.personalDatabase(playerId).store(new ItemStack(Items.DIAMOND, 16));
        original.personalTabs(playerId).addCustomTab("tools", "minecraft:iron_pickaxe");

        CompoundTag exported = original.exportStorageTag(null);
        assertNotNull(exported);
        assertTrue(exported.contains("schema_version"));

        DatabaseStorageSavedData restored = DatabaseStorageSavedData.fromTag(exported, null);
        assertEquals(2, restored.publicDatabase().entryCount());
        assertEquals(64L, restored.publicDatabase().getAmount(StoredStackKey.of(new ItemStack(Items.STONE))));
        assertEquals(32L, restored.publicDatabase().getAmount(StoredStackKey.of(new ItemStack(Items.DIRT))));
        assertEquals(1, restored.personalDatabase(playerId).entryCount());
        assertEquals(16L, restored.personalDatabase(playerId).getAmount(StoredStackKey.of(new ItemStack(Items.DIAMOND))));
        assertEquals(4, restored.personalTabs(playerId).orderedTabs().size());
    }

    @Test
    void restoreFromSnapshotWithValidTagShouldReplaceData() {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.fromTag(null, null);
        storage.publicDatabase().store(new ItemStack(Items.STONE, 10));

        CompoundTag snapshot = new CompoundTag();
        snapshot.putInt("schema_version", DatabaseStorageSavedData.CURRENT_SCHEMA_VERSION);
        snapshot.put("public_database", new StoredItemDatabase().serializeNBT(null));
        snapshot.put("public_tabs", new DatabaseTabDirectory().toTag());
        snapshot.put("personal_databases", new net.minecraft.nbt.ListTag());
        snapshot.put("personal_tab_directories", new net.minecraft.nbt.ListTag());
        snapshot.put("migration_states", new net.minecraft.nbt.ListTag());

        storage.restoreFromSnapshot(snapshot, null);

        assertEquals(0, storage.publicDatabase().entryCount());
    }

    @Test
    void restoreFromSnapshotWithNullTagShouldClearData() {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.fromTag(null, null);
        storage.publicDatabase().store(new ItemStack(Items.STONE, 10));

        storage.restoreFromSnapshot(null, null);

        assertEquals(0, storage.publicDatabase().entryCount());
    }

    @Test
    void loadFromStorageTagShouldGracefullyHandleMissingSchemaVersion() throws Exception {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.fromTag(null, null);
        storage.publicDatabase().store(new ItemStack(Items.STONE, 10));

        CompoundTag legacyTag = new CompoundTag();
        legacyTag.put("database", new CompoundTag());
        legacyTag.put("migration_states", new net.minecraft.nbt.ListTag());

        Method method = DatabaseStorageSavedData.class.getDeclaredMethod("loadFromStorageTag", CompoundTag.class, net.minecraft.core.HolderLookup.Provider.class);
        method.setAccessible(true);
        method.invoke(storage, legacyTag, (net.minecraft.core.HolderLookup.Provider) null);

        // 旧格式加载后数据会被处理，但不应抛出异常
        assertNotNull(storage.publicDatabase());
    }

    @Test
    void exportShouldIncludeSchemaVersion() {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.fromTag(null, null);
        CompoundTag exported = storage.exportStorageTag(null);
        assertTrue(exported.contains("schema_version"));
        assertEquals(DatabaseStorageSavedData.CURRENT_SCHEMA_VERSION, exported.getInt("schema_version"));
    }

    @Test
    void restoreFromSnapshotShouldClearPendingMigrationBackup() {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.fromTag(null, null);

        CompoundTag snapshot = new CompoundTag();
        snapshot.putInt("schema_version", DatabaseStorageSavedData.CURRENT_SCHEMA_VERSION);
        snapshot.put("public_database", new StoredItemDatabase().serializeNBT(null));
        snapshot.put("public_tabs", new DatabaseTabDirectory().toTag());
        snapshot.put("personal_databases", new net.minecraft.nbt.ListTag());
        snapshot.put("personal_tab_directories", new net.minecraft.nbt.ListTag());
        snapshot.put("migration_states", new net.minecraft.nbt.ListTag());

        storage.restoreFromSnapshot(snapshot, null);

        // 恢复后 pendingMigrationBackup 应为 null
        try {
            java.lang.reflect.Field field = DatabaseStorageSavedData.class.getDeclaredField("pendingMigrationBackup");
            field.setAccessible(true);
            assertEquals(null, field.get(storage));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void personalDatabaseShouldSurviveRoundTrip() {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.fromTag(null, null);
        UUID playerId = UUID.randomUUID();
        storage.personalDatabase(playerId).store(new ItemStack(Items.GOLD_INGOT, 99));
        storage.personalDatabase(playerId).setNote(
                StoredStackKey.of(new ItemStack(Items.GOLD_INGOT)),
                "precious"
        );

        CompoundTag exported = storage.exportStorageTag(null);
        DatabaseStorageSavedData restored = DatabaseStorageSavedData.fromTag(exported, null);

        assertTrue(restored.hasPersonalDatabase(playerId));
        assertEquals(99L, restored.personalDatabase(playerId).getAmount(StoredStackKey.of(new ItemStack(Items.GOLD_INGOT))));
        assertEquals("precious", restored.personalDatabase(playerId).noteFor(StoredStackKey.of(new ItemStack(Items.GOLD_INGOT))));
    }
}
