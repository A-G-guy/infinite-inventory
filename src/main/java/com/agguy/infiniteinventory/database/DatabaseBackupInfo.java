package com.agguy.infiniteinventory.database;

public record DatabaseBackupInfo(
        String fileName,
        DatabaseBackupType type,
        String reason,
        long createdAtMillis
) {
}
