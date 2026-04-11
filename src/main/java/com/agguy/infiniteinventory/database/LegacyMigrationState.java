package com.agguy.infiniteinventory.database;

import net.minecraft.nbt.CompoundTag;

/**
 * 记录旧个人附件迁移状态，避免重复导入或重复告警。
 */
public record LegacyMigrationState(Status status, long recordedAtMillis, int legacyEntryCount) {
    private static final String STATUS_KEY = "status";
    private static final String RECORDED_AT_KEY = "recorded_at_millis";
    private static final String LEGACY_ENTRY_COUNT_KEY = "legacy_entry_count";

    public LegacyMigrationState {
        status = status == null ? Status.SKIPPED_EXISTING_STORAGE : status;
        recordedAtMillis = Math.max(0L, recordedAtMillis);
        legacyEntryCount = Math.max(0, legacyEntryCount);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(STATUS_KEY, this.status.name());
        tag.putLong(RECORDED_AT_KEY, this.recordedAtMillis);
        tag.putInt(LEGACY_ENTRY_COUNT_KEY, this.legacyEntryCount);
        return tag;
    }

    public static LegacyMigrationState fromTag(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return new LegacyMigrationState(Status.SKIPPED_EXISTING_STORAGE, 0L, 0);
        }
        return new LegacyMigrationState(
                readStatus(tag.getString(STATUS_KEY)),
                tag.getLong(RECORDED_AT_KEY),
                tag.getInt(LEGACY_ENTRY_COUNT_KEY)
        );
    }

    private static Status readStatus(String serializedStatus) {
        try {
            return Status.valueOf(serializedStatus);
        } catch (IllegalArgumentException exception) {
            return Status.SKIPPED_EXISTING_STORAGE;
        }
    }

    public enum Status {
        PENDING_CLEANUP,
        MIGRATED,
        SKIPPED_EXISTING_STORAGE
    }
}
