package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseBackupInfo;
import com.agguy.infiniteinventory.database.DatabaseBackupType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseBackupInfoTest {

    @Test
    void shouldPreserveAllFields() {
        DatabaseBackupInfo info = new DatabaseBackupInfo(
                "backup_2024.nbt",
                DatabaseBackupType.MANUAL,
                "Pre-update backup",
                1700000000000L
        );

        assertEquals("backup_2024.nbt", info.fileName());
        assertEquals(DatabaseBackupType.MANUAL, info.type());
        assertEquals("Pre-update backup", info.reason());
        assertEquals(1700000000000L, info.createdAtMillis());
    }

    @Test
    void shouldAllowNullFields() {
        DatabaseBackupInfo info = new DatabaseBackupInfo(null, null, null, 0);

        assertEquals(null, info.fileName());
        assertEquals(null, info.type());
        assertEquals(null, info.reason());
    }
}
