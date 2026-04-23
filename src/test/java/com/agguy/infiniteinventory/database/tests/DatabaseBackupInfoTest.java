package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseBackupInfo;
import com.agguy.infiniteinventory.database.DatabaseBackupType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DatabaseBackupInfo} record 的字段访问与比较行为测试。
 */
class DatabaseBackupInfoTest {

    @Test
    void shouldStoreAllFields() {
        DatabaseBackupInfo info = new DatabaseBackupInfo(
                "20240101-120000-000_manual_test.nbt.gz",
                DatabaseBackupType.MANUAL,
                "pre-update",
                1_704_153_600_000L
        );

        assertEquals("20240101-120000-000_manual_test.nbt.gz", info.fileName());
        assertEquals(DatabaseBackupType.MANUAL, info.type());
        assertEquals("pre-update", info.reason());
        assertEquals(1_704_153_600_000L, info.createdAtMillis());
    }

    @Test
    void shouldSupportMigrationType() {
        DatabaseBackupInfo info = new DatabaseBackupInfo(
                "20240101-120000-000_migration_schema-v2.nbt.gz",
                DatabaseBackupType.MIGRATION,
                "schema-v2",
                1_704_153_600_000L
        );

        assertEquals(DatabaseBackupType.MIGRATION, info.type());
        assertTrue(info.type().autoManaged());
    }

    @Test
    void shouldSupportRollingType() {
        DatabaseBackupInfo info = new DatabaseBackupInfo(
                "20240101-120000-000_rolling_dirty-window.nbt.gz",
                DatabaseBackupType.ROLLING,
                "dirty-window",
                1_704_153_600_000L
        );

        assertEquals(DatabaseBackupType.ROLLING, info.type());
        assertTrue(info.type().autoManaged());
    }

    @Test
    void shouldSupportPreRestoreType() {
        DatabaseBackupInfo info = new DatabaseBackupInfo(
                "20240101-120000-000_pre_restore_before-import.nbt.gz",
                DatabaseBackupType.PRE_RESTORE,
                "before-import",
                1_704_153_600_000L
        );

        assertEquals(DatabaseBackupType.PRE_RESTORE, info.type());
        assertTrue(info.type().autoManaged());
    }

    @Test
    void manualTypeShouldNotBeAutoManaged() {
        DatabaseBackupInfo info = new DatabaseBackupInfo(
                "20240101-120000-000_manual_test.nbt.gz",
                DatabaseBackupType.MANUAL,
                "test",
                1_704_153_600_000L
        );

        assertEquals(DatabaseBackupType.MANUAL, info.type());
        assertEquals(false, info.type().autoManaged());
    }

    @Test
    void shouldSupportZeroTimestamp() {
        DatabaseBackupInfo info = new DatabaseBackupInfo(
                "19700101-000000-000_manual_epoch.nbt.gz",
                DatabaseBackupType.MANUAL,
                "epoch",
                0L
        );

        assertEquals(0L, info.createdAtMillis());
    }

    @Test
    void shouldSupportNegativeTimestamp() {
        DatabaseBackupInfo info = new DatabaseBackupInfo(
                "19691231-235959-999_manual_before-epoch.nbt.gz",
                DatabaseBackupType.MANUAL,
                "before-epoch",
                -1L
        );

        assertEquals(-1L, info.createdAtMillis());
    }

    @Test
    void shouldSupportEmptyReason() {
        DatabaseBackupInfo info = new DatabaseBackupInfo(
                "20240101-120000-000_manual_.nbt.gz",
                DatabaseBackupType.MANUAL,
                "",
                1_704_153_600_000L
        );

        assertEquals("", info.reason());
    }

    @Test
    void shouldSupportLongReason() {
        String longReason = "a".repeat(200);
        DatabaseBackupInfo info = new DatabaseBackupInfo(
                "20240101-120000-000_manual_long.nbt.gz",
                DatabaseBackupType.MANUAL,
                longReason,
                1_704_153_600_000L
        );

        assertEquals(longReason, info.reason());
    }

    @Test
    void equalsShouldBeTrueForSameValues() {
        DatabaseBackupInfo a = new DatabaseBackupInfo("file.nbt.gz", DatabaseBackupType.MANUAL, "reason", 1L);
        DatabaseBackupInfo b = new DatabaseBackupInfo("file.nbt.gz", DatabaseBackupType.MANUAL, "reason", 1L);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equalsShouldBeFalseForDifferentFileName() {
        DatabaseBackupInfo a = new DatabaseBackupInfo("a.nbt.gz", DatabaseBackupType.MANUAL, "reason", 1L);
        DatabaseBackupInfo b = new DatabaseBackupInfo("b.nbt.gz", DatabaseBackupType.MANUAL, "reason", 1L);

        assertNotEquals(a, b);
    }

    @Test
    void equalsShouldBeFalseForDifferentType() {
        DatabaseBackupInfo a = new DatabaseBackupInfo("file.nbt.gz", DatabaseBackupType.MANUAL, "reason", 1L);
        DatabaseBackupInfo b = new DatabaseBackupInfo("file.nbt.gz", DatabaseBackupType.ROLLING, "reason", 1L);

        assertNotEquals(a, b);
    }

    @Test
    void equalsShouldBeFalseForDifferentReason() {
        DatabaseBackupInfo a = new DatabaseBackupInfo("file.nbt.gz", DatabaseBackupType.MANUAL, "a", 1L);
        DatabaseBackupInfo b = new DatabaseBackupInfo("file.nbt.gz", DatabaseBackupType.MANUAL, "b", 1L);

        assertNotEquals(a, b);
    }

    @Test
    void equalsShouldBeFalseForDifferentTimestamp() {
        DatabaseBackupInfo a = new DatabaseBackupInfo("file.nbt.gz", DatabaseBackupType.MANUAL, "reason", 1L);
        DatabaseBackupInfo b = new DatabaseBackupInfo("file.nbt.gz", DatabaseBackupType.MANUAL, "reason", 2L);

        assertNotEquals(a, b);
    }

    @Test
    void toStringShouldContainFileName() {
        DatabaseBackupInfo info = new DatabaseBackupInfo("test.nbt.gz", DatabaseBackupType.MANUAL, "r", 1L);
        String str = info.toString();

        assertTrue(str.contains("test.nbt.gz"));
    }
}
