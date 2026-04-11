package com.agguy.infiniteinventory.database;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class StoredItemDatabase implements INBTSerializable<CompoundTag> {
    private static final String ENTRIES_KEY = "entries";
    private static final String STACK_KEY = "stack";
    private static final String COUNT_KEY = "count";
    private static final String CATEGORY_KEY = "category";
    private static final String LAST_MODIFIED_KEY = "last_modified";
    private static final String NEXT_SEQUENCE_KEY = "next_sequence";

    private final Map<StoredStackKey, StoredStackEntry> entries = new LinkedHashMap<>();
    private long nextSequence = 1L;

    public Map<StoredStackKey, StoredStackEntry> entries() {
        return Collections.unmodifiableMap(this.entries);
    }

    public long getAmount(StoredStackKey key) {
        StoredStackEntry entry = this.entries.get(key);
        return entry == null ? 0L : entry.amount();
    }

    public int entryCount() {
        return this.entries.size();
    }

    public long totalItemCount() {
        long total = 0L;
        for (StoredStackEntry entry : this.entries.values()) {
            if (Long.MAX_VALUE - total < entry.amount()) {
                return Long.MAX_VALUE;
            }
            total += entry.amount();
        }
        return total;
    }

    public void store(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        long sequence = this.nextSequence();
        StoredStackKey key = StoredStackKey.of(stack);
        StoredStackEntry entry = this.entries.get(key);
        if (entry == null) {
            entry = new StoredStackEntry(DatabaseCategory.classify(stack), 0L, sequence);
            this.entries.put(key, entry);
        }
        entry.add(stack.getCount(), sequence);
    }

    public ItemStack extract(StoredStackKey key, int requestedAmount) {
        if (requestedAmount <= 0) {
            return ItemStack.EMPTY;
        }
        StoredStackEntry entry = this.entries.get(key);
        if (entry == null) {
            return ItemStack.EMPTY;
        }
        int extractedAmount = (int) Math.min(entry.amount(), Math.min((long) requestedAmount, Integer.MAX_VALUE));
        if (extractedAmount <= 0) {
            return ItemStack.EMPTY;
        }
        long sequence = this.nextSequence();
        entry.remove(extractedAmount, sequence);
        ItemStack extractedStack = key.toStack(extractedAmount);
        if (entry.isEmpty()) {
            this.entries.remove(key);
        }
        return extractedStack;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        ListTag serializedEntries = new ListTag();
        for (Map.Entry<StoredStackKey, StoredStackEntry> mapEntry : this.entries.entrySet()) {
            ItemStack stack = mapEntry.getKey().displayStack();
            Tag serializedStack = stack.saveOptional(provider);
            if (!(serializedStack instanceof CompoundTag stackTag)) {
                continue;
            }
            StoredStackEntry entry = mapEntry.getValue();
            CompoundTag entryTag = new CompoundTag();
            entryTag.put(STACK_KEY, stackTag);
            entryTag.putLong(COUNT_KEY, entry.amount());
            entryTag.putString(CATEGORY_KEY, entry.category().name());
            entryTag.putLong(LAST_MODIFIED_KEY, entry.lastModified());
            serializedEntries.add(entryTag);
        }
        root.put(ENTRIES_KEY, serializedEntries);
        root.putLong(NEXT_SEQUENCE_KEY, this.nextSequence);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.entries.clear();
        long highestSequence = 0L;
        for (Tag element : tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag entryTag)) {
                continue;
            }
            ItemStack stack = ItemStack.parseOptional(provider, entryTag.getCompound(STACK_KEY));
            if (stack.isEmpty()) {
                continue;
            }
            long amount = Math.max(0L, entryTag.getLong(COUNT_KEY));
            if (amount <= 0L) {
                continue;
            }
            DatabaseCategory category = readCategory(entryTag, stack);
            long lastModified = Math.max(0L, entryTag.getLong(LAST_MODIFIED_KEY));
            this.entries.put(StoredStackKey.of(stack), new StoredStackEntry(category, amount, lastModified));
            highestSequence = Math.max(highestSequence, lastModified);
        }
        this.nextSequence = Math.max(tag.getLong(NEXT_SEQUENCE_KEY), highestSequence + 1L);
        if (this.nextSequence <= 0L) {
            this.nextSequence = 1L;
        }
    }

    private static DatabaseCategory readCategory(CompoundTag tag, ItemStack stack) {
        try {
            return DatabaseCategory.valueOf(tag.getString(CATEGORY_KEY));
        } catch (IllegalArgumentException exception) {
            return DatabaseCategory.classify(stack);
        }
    }

    private long nextSequence() {
        if (this.nextSequence == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return this.nextSequence++;
    }
}
