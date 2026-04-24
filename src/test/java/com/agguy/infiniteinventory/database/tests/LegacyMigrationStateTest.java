package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.LegacyMigrationState;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyMigrationStateTest {

    @Test
    void shouldApplyCompactConstructorDefaults() {
        LegacyMigrationState state = new LegacyMigrationState(null, -1L, -5);

        assertEquals(LegacyMigrationState.Status.SKIPPED_EXISTING_STORAGE, state.status());
        assertEquals(0L, state.recordedAtMillis());
        assertEquals(0, state.legacyEntryCount());
    }

    @Test
    void toTagShouldRoundTripAllStatuses() {
        for (LegacyMigrationState.Status status : LegacyMigrationState.Status.values()) {
            LegacyMigrationState state = new LegacyMigrationState(status, 1000L, 42);
            LegacyMigrationState restored = LegacyMigrationState.fromTag(state.toTag());

            assertEquals(state.status(), restored.status());
            assertEquals(state.recordedAtMillis(), restored.recordedAtMillis());
            assertEquals(state.legacyEntryCount(), restored.legacyEntryCount());
        }
    }

    @Test
    void fromTagShouldHandleNullTag() {
        LegacyMigrationState state = LegacyMigrationState.fromTag(null);

        assertEquals(LegacyMigrationState.Status.SKIPPED_EXISTING_STORAGE, state.status());
        assertEquals(0L, state.recordedAtMillis());
        assertEquals(0, state.legacyEntryCount());
    }

    @Test
    void fromTagShouldHandleEmptyTag() {
        LegacyMigrationState state = LegacyMigrationState.fromTag(new CompoundTag());

        assertEquals(LegacyMigrationState.Status.SKIPPED_EXISTING_STORAGE, state.status());
        assertEquals(0L, state.recordedAtMillis());
        assertEquals(0, state.legacyEntryCount());
    }

    @Test
    void fromTagShouldHandleUnknownStatusGracefully() {
        CompoundTag tag = new CompoundTag();
        tag.putString("status", "NONEXISTENT_STATUS");
        tag.putLong("recorded_at_millis", 500L);
        tag.putInt("legacy_entry_count", 10);

        LegacyMigrationState state = LegacyMigrationState.fromTag(tag);

        assertEquals(LegacyMigrationState.Status.SKIPPED_EXISTING_STORAGE, state.status());
        assertEquals(500L, state.recordedAtMillis());
        assertEquals(10, state.legacyEntryCount());
    }
}
