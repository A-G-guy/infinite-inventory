package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseBackupType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseBackupTypeTest {

    @Test
    void shouldHaveFourTypes() {
        assertEquals(4, DatabaseBackupType.values().length);
    }

    @Test
    void migrationShouldBeAutoManaged() {
        assertTrue(DatabaseBackupType.MIGRATION.autoManaged());
    }

    @Test
    void rollingShouldBeAutoManaged() {
        assertTrue(DatabaseBackupType.ROLLING.autoManaged());
    }

    @Test
    void preRestoreShouldBeAutoManaged() {
        assertTrue(DatabaseBackupType.PRE_RESTORE.autoManaged());
    }

    @Test
    void manualShouldNotBeAutoManaged() {
        assertFalse(DatabaseBackupType.MANUAL.autoManaged());
    }
}
