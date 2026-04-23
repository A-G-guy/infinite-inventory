package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseBackupInfo;
import com.agguy.infiniteinventory.database.DatabaseBackupManager;
import com.agguy.infiniteinventory.database.DatabaseBackupType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DatabaseBackupManager} 纯逻辑部分的单元测试。
 * <p>
 * 涉及文件 IO 与 MinecraftServer 的方法不在此覆盖。
 */
class DatabaseBackupManagerTest {

    @Test
    void sanitizeReasonShouldReturnSnapshotWhenNull() throws Exception {
        String result = invokePrivateStatic("sanitizeReason", new Class<?>[]{String.class}, new Object[]{null});

        assertEquals("snapshot", result);
    }

    @Test
    void sanitizeReasonShouldReturnSnapshotWhenBlank() throws Exception {
        String result = invokePrivateStatic("sanitizeReason", new Class<?>[]{String.class}, new Object[]{"   "});

        assertEquals("snapshot", result);
    }

    @Test
    void sanitizeReasonShouldTrimInput() throws Exception {
        String result = invokePrivateStatic("sanitizeReason", new Class<?>[]{String.class}, new Object[]{"  pre-update  "});

        assertEquals("pre-update", result);
    }

    @Test
    void sanitizeReasonShouldLowercaseInput() throws Exception {
        String result = invokePrivateStatic("sanitizeReason", new Class<?>[]{String.class}, new Object[]{"PRE-UPDATE"});

        assertEquals("pre-update", result);
    }

    @Test
    void sanitizeReasonShouldReplaceInvalidCharactersWithUnderscore() throws Exception {
        String result = invokePrivateStatic("sanitizeReason", new Class<?>[]{String.class}, new Object[]{"pre update!@#"});

        // 正则 [^a-z0-9._-]+ 会将连续非法字符合并为一个下划线
        assertEquals("pre_update_", result);
    }

    @Test
    void sanitizeReasonShouldPreserveValidCharacters() throws Exception {
        String result = invokePrivateStatic("sanitizeReason", new Class<?>[]{String.class}, new Object[]{"v1.2.3-alpha_beta"});

        assertEquals("v1.2.3-alpha_beta", result);
    }

    @Test
    void readBackupTypeShouldReturnManualForInvalidName() throws Exception {
        DatabaseBackupType result = invokePrivateStatic("readBackupType", new Class<?>[]{String.class}, new Object[]{"UNKNOWN"});

        assertEquals(DatabaseBackupType.MANUAL, result);
    }

    @Test
    void readBackupTypeShouldReturnManualForEmptyString() throws Exception {
        DatabaseBackupType result = invokePrivateStatic("readBackupType", new Class<?>[]{String.class}, new Object[]{""});

        assertEquals(DatabaseBackupType.MANUAL, result);
    }

    @Test
    void readBackupTypeShouldDeserializeValidNames() throws Exception {
        assertEquals(DatabaseBackupType.MANUAL, invokePrivateStatic("readBackupType", new Class<?>[]{String.class}, new Object[]{"MANUAL"}));
        assertEquals(DatabaseBackupType.ROLLING, invokePrivateStatic("readBackupType", new Class<?>[]{String.class}, new Object[]{"ROLLING"}));
        assertEquals(DatabaseBackupType.MIGRATION, invokePrivateStatic("readBackupType", new Class<?>[]{String.class}, new Object[]{"MIGRATION"}));
        assertEquals(DatabaseBackupType.PRE_RESTORE, invokePrivateStatic("readBackupType", new Class<?>[]{String.class}, new Object[]{"PRE_RESTORE"}));
    }

    @Test
    void parseBackupInfoShouldExtractFieldsFromTag() throws Exception {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("type", "MANUAL");
        tag.putString("reason", "test-reason");
        tag.putLong("created_at_millis", 1_704_153_600_000L);

        DatabaseBackupInfo info = invokePrivateStatic("parseBackupInfo", new Class<?>[]{String.class, net.minecraft.nbt.CompoundTag.class}, new Object[]{"test.nbt.gz", tag});

        assertNotNull(info);
        assertEquals("test.nbt.gz", info.fileName());
        assertEquals(DatabaseBackupType.MANUAL, info.type());
        assertEquals("test-reason", info.reason());
        assertEquals(1_704_153_600_000L, info.createdAtMillis());
    }

    @Test
    void parseBackupInfoShouldFallbackToManualForMissingType() throws Exception {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("reason", "test");
        tag.putLong("created_at_millis", 1L);

        DatabaseBackupInfo info = invokePrivateStatic("parseBackupInfo", new Class<?>[]{String.class, net.minecraft.nbt.CompoundTag.class}, new Object[]{"file.nbt.gz", tag});

        assertEquals(DatabaseBackupType.MANUAL, info.type());
    }

    @Test
    void rollingBackupIntervalShouldBeFifteenMinutes() {
        assertEquals(15L * 60L * 1000L, DatabaseBackupManager.ROLLING_BACKUP_INTERVAL_MILLIS);
    }

    @Test
    void autoManagedTypesShouldIncludeMigrationRollingAndPreRestore() {
        assertTrue(DatabaseBackupType.MIGRATION.autoManaged());
        assertTrue(DatabaseBackupType.ROLLING.autoManaged());
        assertTrue(DatabaseBackupType.PRE_RESTORE.autoManaged());
        assertFalse(DatabaseBackupType.MANUAL.autoManaged());
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokePrivateStatic(String methodName, Class<?>[] paramTypes, Object[] args)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = DatabaseBackupManager.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return (T) method.invoke(null, args);
    }
}
