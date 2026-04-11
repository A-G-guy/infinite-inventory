package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoredItemDatabaseTest {
    @Test
    void shouldPreserveLegacyInvalidEntriesWhenProviderUnavailable() {
        CompoundTag legacyRoot = new CompoundTag();
        ListTag entries = new ListTag();
        entries.add(this.entryTag(this.invalidStackTag("missing:ghost_item"), 9L, DatabaseCategory.OTHER, 11L));
        legacyRoot.put("entries", entries);
        legacyRoot.putLong("next_sequence", 20L);

        StoredItemDatabase restored = new StoredItemDatabase();
        restored.deserializeNBT(null, legacyRoot);

        assertEquals(0, restored.entryCount());
        assertEquals(1, restored.unresolvedEntryCount());

        StoredItemDatabase roundTripped = new StoredItemDatabase();
        roundTripped.deserializeNBT(null, restored.serializeNBT(null));

        assertEquals(0, roundTripped.entryCount());
        assertEquals(1, roundTripped.unresolvedEntryCount());
    }

    @Test
    void shouldPreserveCurrentFormatUnresolvedEntriesWithoutDroppingThem() {
        CompoundTag currentRoot = new CompoundTag();
        currentRoot.putInt("schema_version", StoredItemDatabase.CURRENT_SCHEMA_VERSION);
        currentRoot.put("entries", new ListTag());

        ListTag unresolvedEntries = new ListTag();
        unresolvedEntries.add(this.entryTag(this.invalidStackTag("missing:ghost_apple"), 5L, DatabaseCategory.CONSUMABLES, 7L));
        currentRoot.put("unresolved_entries", unresolvedEntries);
        currentRoot.putLong("next_sequence", 10L);

        StoredItemDatabase restored = new StoredItemDatabase();
        restored.deserializeNBT(null, currentRoot);

        assertEquals(0, restored.entryCount());
        assertEquals(1, restored.unresolvedEntryCount());
        assertEquals(10L, restored.serializeNBT(null).getLong("next_sequence"));
    }

    private CompoundTag entryTag(CompoundTag stackTag, long count, DatabaseCategory category, long lastModified) {
        CompoundTag entryTag = new CompoundTag();
        entryTag.put("stack", stackTag);
        entryTag.putLong("count", count);
        entryTag.putString("category", category.name());
        entryTag.putLong("last_modified", lastModified);
        return entryTag;
    }

    private CompoundTag invalidStackTag(String itemId) {
        CompoundTag stackTag = new CompoundTag();
        stackTag.putString("id", itemId);
        stackTag.putInt("count", 1);
        return stackTag;
    }
}
