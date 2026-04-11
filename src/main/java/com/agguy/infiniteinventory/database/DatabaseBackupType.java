package com.agguy.infiniteinventory.database;

public enum DatabaseBackupType {
    MIGRATION(true),
    ROLLING(true),
    PRE_RESTORE(true),
    MANUAL(false);

    private final boolean autoManaged;

    DatabaseBackupType(boolean autoManaged) {
        this.autoManaged = autoManaged;
    }

    public boolean autoManaged() {
        return this.autoManaged;
    }
}
