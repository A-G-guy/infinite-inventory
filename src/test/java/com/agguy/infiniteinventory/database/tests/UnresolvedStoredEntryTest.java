package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.UnresolvedStoredEntry;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnresolvedStoredEntryTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void shouldApplyCompactConstructorDefaults() {
        UnresolvedStoredEntry entry = new UnresolvedStoredEntry(null, -1L, null, -1L, -1L);

        assertEquals(0L, entry.amount());
        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, entry.tabId());
        assertEquals(0L, entry.firstAdded());
        assertEquals(0L, entry.lastModified());
    }

    @Test
    void stackTagShouldReturnCopy() {
        CompoundTag original = new CompoundTag();
        original.putString("id", "minecraft:stone");
        UnresolvedStoredEntry entry = new UnresolvedStoredEntry(original, 1L, "test", 100L);

        CompoundTag returned = entry.stackTag();
        assertNotSame(original, returned);
        assertEquals("minecraft:stone", returned.getString("id"));
    }

    @Test
    void isEmptyShouldReturnTrueWhenAmountIsZero() {
        UnresolvedStoredEntry entry = new UnresolvedStoredEntry(new CompoundTag(), 0L, "test", 100L);

        assertTrue(entry.isEmpty());
    }

    @Test
    void isEmptyShouldReturnTrueWhenStackTagIsEmpty() {
        UnresolvedStoredEntry entry = new UnresolvedStoredEntry(new CompoundTag(), 5L, "test", 100L);

        assertTrue(entry.isEmpty());
    }

    @Test
    void isEmptyShouldReturnFalseWhenAmountAndStackTagAreValid() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:stone");
        UnresolvedStoredEntry entry = new UnresolvedStoredEntry(tag, 5L, "test", 100L);

        assertFalse(entry.isEmpty());
    }

    @Test
    void withTabIdShouldPreserveFirstAddedAndUpdateLastModified() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:stone");
        UnresolvedStoredEntry entry = new UnresolvedStoredEntry(tag, 5L, "old_tab", 50L, 10L);

        UnresolvedStoredEntry updated = entry.withTabId("new_tab", 100L);

        assertEquals("new_tab", updated.tabId());
        assertEquals(10L, updated.firstAdded());
        assertEquals(100L, updated.lastModified());
    }

    @Test
    void withTabIdShouldNotDecreaseLastModified() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:stone");
        UnresolvedStoredEntry entry = new UnresolvedStoredEntry(tag, 5L, "old_tab", 100L, 10L);

        UnresolvedStoredEntry updated = entry.withTabId("new_tab", 50L);

        assertEquals(100L, updated.lastModified());
    }

    @Test
    void toTagShouldRoundTrip() {
        CompoundTag stackTag = new CompoundTag();
        stackTag.putString("id", "minecraft:diamond");
        stackTag.putInt("count", 1);
        UnresolvedStoredEntry entry = new UnresolvedStoredEntry(stackTag, 64L, "my_tab", 200L, 100L);

        UnresolvedStoredEntry restored = UnresolvedStoredEntry.fromTag(entry.toTag());

        assertEquals(entry.amount(), restored.amount());
        assertEquals(entry.tabId(), restored.tabId());
        assertEquals(entry.firstAdded(), restored.firstAdded());
        assertEquals(entry.lastModified(), restored.lastModified());
        assertEquals("minecraft:diamond", restored.stackTag().getString("id"));
    }

    @Test
    void fromTagShouldHandleNullTag() {
        UnresolvedStoredEntry entry = UnresolvedStoredEntry.fromTag(null);

        assertEquals(0L, entry.amount());
        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, entry.tabId());
    }

    @Test
    void fromTagShouldHandleEmptyCompoundTag() {
        UnresolvedStoredEntry entry = UnresolvedStoredEntry.fromTag(new CompoundTag());

        assertEquals(0L, entry.amount());
        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, entry.tabId());
    }

    @Test
    void fromTagShouldHandleMissingFirstAddedByFallingBackToLastModified() {
        CompoundTag tag = new CompoundTag();
        CompoundTag stackTag = new CompoundTag();
        stackTag.putString("id", "minecraft:apple");
        tag.put("stack", stackTag);
        tag.putLong("count", 10L);
        tag.putString("tab_id", "food");
        tag.putLong("last_modified", 500L);

        UnresolvedStoredEntry entry = UnresolvedStoredEntry.fromTag(tag);

        assertEquals(500L, entry.firstAdded());
    }

    @Test
    void tryResolveShouldReturnEmptyForEmptyEntry() {
        UnresolvedStoredEntry entry = new UnresolvedStoredEntry(new CompoundTag(), 0L, "test", 0L);

        assertTrue(entry.tryResolve(null).isEmpty());
    }
}
