package com.agguy.infiniteinventory.database;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class StoredItemDatabase implements INBTSerializable<CompoundTag> {
    public static final int CURRENT_SCHEMA_VERSION = 2;

    private static final String SCHEMA_VERSION_KEY = "schema_version";
    private static final String CLASSIFIER_VERSION_KEY = "classifier_version";
    private static final String ENTRIES_KEY = "entries";
    private static final String UNRESOLVED_ENTRIES_KEY = "unresolved_entries";
    private static final String STACK_KEY = "stack";
    private static final String COUNT_KEY = "count";
    private static final String CATEGORY_KEY = "category";
    private static final String LAST_MODIFIED_KEY = "last_modified";
    private static final String NEXT_SEQUENCE_KEY = "next_sequence";

    private final Map<StoredStackKey, StoredStackEntry> entries = new LinkedHashMap<>();
    private final java.util.List<UnresolvedStoredEntry> unresolvedEntries = new java.util.ArrayList<>();
    private long nextSequence = 1L;
    private long revision;
    private boolean needsResave;

    public Map<StoredStackKey, StoredStackEntry> entries() {
        return Collections.unmodifiableMap(this.entries);
    }

    public List<UnresolvedStoredEntry> unresolvedEntries() {
        return List.copyOf(this.unresolvedEntries);
    }

    public int unresolvedEntryCount() {
        return this.unresolvedEntries.size();
    }

    public boolean hasUnresolvedEntries() {
        return !this.unresolvedEntries.isEmpty();
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

    public long revision() {
        return this.revision;
    }

    public boolean needsResave() {
        return this.needsResave;
    }

    public void clear() {
        if (!this.hasStoredContent()) {
            return;
        }
        this.resetContent();
        this.markRuntimeStateDirty();
    }

    public void mergeFrom(StoredItemDatabase other) {
        if (other == null) {
            return;
        }
        boolean changed = false;
        long highestMergedSequence = 0L;
        for (Map.Entry<StoredStackKey, StoredStackEntry> mapEntry : other.entries().entrySet()) {
            this.mergeResolvedEntry(mapEntry.getKey(), mapEntry.getValue());
            highestMergedSequence = Math.max(highestMergedSequence, mapEntry.getValue().lastModified());
            changed = true;
        }
        for (UnresolvedStoredEntry unresolvedEntry : other.unresolvedEntries()) {
            if (!unresolvedEntry.isEmpty()) {
                this.unresolvedEntries.add(new UnresolvedStoredEntry(
                        unresolvedEntry.stackTag(),
                        unresolvedEntry.amount(),
                        unresolvedEntry.category(),
                        unresolvedEntry.lastModified()
                ));
                highestMergedSequence = Math.max(highestMergedSequence, unresolvedEntry.lastModified());
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        long nextAfterMergedEntries = highestMergedSequence == Long.MAX_VALUE ? Long.MAX_VALUE : highestMergedSequence + 1L;
        this.nextSequence = Math.max(this.nextSequence, Math.max(other.nextSequence, nextAfterMergedEntries));
        if (this.nextSequence <= 0L) {
            this.nextSequence = 1L;
        }
        this.markRuntimeStateDirty();
    }

    public void store(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        long sequence = this.nextSequence();
        StoredStackKey key = StoredStackKey.of(stack);
        DatabaseCategory normalizedCategory = DatabaseItemClassifier.INSTANCE.classify(stack);
        StoredStackEntry entry = this.entries.get(key);
        if (entry == null) {
            entry = new StoredStackEntry(normalizedCategory, 0L, sequence);
            this.entries.put(key, entry);
        } else if (entry.category() != normalizedCategory) {
            entry = new StoredStackEntry(normalizedCategory, entry.amount(), entry.lastModified());
            this.entries.put(key, entry);
        }
        entry.add(stack.getCount(), sequence);
        this.markRuntimeStateDirty();
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
        this.markRuntimeStateDirty();
        return extractedStack;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        root.putInt(SCHEMA_VERSION_KEY, CURRENT_SCHEMA_VERSION);
        root.putInt(CLASSIFIER_VERSION_KEY, DatabaseItemClassifier.CURRENT_VERSION);
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
        ListTag serializedUnresolvedEntries = new ListTag();
        for (UnresolvedStoredEntry unresolvedEntry : this.unresolvedEntries) {
            if (!unresolvedEntry.isEmpty()) {
                serializedUnresolvedEntries.add(unresolvedEntry.toTag());
            }
        }
        root.put(UNRESOLVED_ENTRIES_KEY, serializedUnresolvedEntries);
        root.putLong(NEXT_SEQUENCE_KEY, this.nextSequence);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.resetContent();
        if (tag == null || tag.isEmpty()) {
            this.markRuntimeStateDirty();
            return;
        }
        if (tag.contains(SCHEMA_VERSION_KEY)) {
            this.readCurrentFormat(provider, tag, Math.max(0, tag.getInt(SCHEMA_VERSION_KEY)));
        } else {
            this.readLegacyFormat(provider, tag);
        }
        this.markRuntimeStateDirty();
    }

    private void readCurrentFormat(HolderLookup.Provider provider, CompoundTag tag, int storedSchemaVersion) {
        long highestSequence = 0L;
        highestSequence = Math.max(highestSequence, this.readResolvedEntries(provider, tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND), true));
        for (Tag element : tag.getList(UNRESOLVED_ENTRIES_KEY, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag unresolvedEntryTag)) {
                continue;
            }
            UnresolvedStoredEntry unresolvedEntry = UnresolvedStoredEntry.fromTag(unresolvedEntryTag);
            if (unresolvedEntry.isEmpty()) {
                continue;
            }
            this.unresolvedEntries.add(unresolvedEntry);
            highestSequence = Math.max(highestSequence, unresolvedEntry.lastModified());
        }
        this.resolveUnresolvedEntries(provider);
        int storedClassifierVersion = storedSchemaVersion >= CURRENT_SCHEMA_VERSION ? Math.max(0, tag.getInt(CLASSIFIER_VERSION_KEY)) : 0;
        boolean recategorized = this.recategorizeResolvedEntriesIfNeeded(storedClassifierVersion);
        this.needsResave = storedSchemaVersion < CURRENT_SCHEMA_VERSION
                || storedClassifierVersion < DatabaseItemClassifier.CURRENT_VERSION
                || recategorized;
        this.finishNextSequence(tag.getLong(NEXT_SEQUENCE_KEY), highestSequence);
    }

    private void readLegacyFormat(HolderLookup.Provider provider, CompoundTag tag) {
        long highestSequence = this.readResolvedEntries(provider, tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND), true);
        this.resolveUnresolvedEntries(provider);
        this.recategorizeResolvedEntriesIfNeeded(0);
        this.finishNextSequence(tag.getLong(NEXT_SEQUENCE_KEY), highestSequence);
        this.needsResave = true;
    }

    private long readResolvedEntries(HolderLookup.Provider provider, ListTag entryList, boolean preserveInvalidEntries) {
        long highestSequence = 0L;
        for (Tag element : entryList) {
            if (!(element instanceof CompoundTag entryTag)) {
                continue;
            }
            CompoundTag stackTag = entryTag.getCompound(STACK_KEY);
            long amount = Math.max(0L, entryTag.getLong(COUNT_KEY));
            if (amount <= 0L) {
                continue;
            }
            long lastModified = Math.max(0L, entryTag.getLong(LAST_MODIFIED_KEY));
            if (provider == null) {
                if (preserveInvalidEntries && !stackTag.isEmpty()) {
                    this.unresolvedEntries.add(new UnresolvedStoredEntry(
                            stackTag,
                            amount,
                            readCategory(entryTag, null),
                            lastModified
                    ));
                    highestSequence = Math.max(highestSequence, lastModified);
                }
                continue;
            }
            ItemStack stack = ItemStack.parseOptional(provider, stackTag.copy());
            if (stack.isEmpty()) {
                if (preserveInvalidEntries && !stackTag.isEmpty()) {
                    this.unresolvedEntries.add(new UnresolvedStoredEntry(
                            stackTag,
                            amount,
                            readCategory(entryTag, null),
                            lastModified
                    ));
                    highestSequence = Math.max(highestSequence, lastModified);
                }
                continue;
            }
            this.mergeResolvedEntry(
                    StoredStackKey.of(stack),
                    new StoredStackEntry(readCategory(entryTag, stack), amount, lastModified)
            );
            highestSequence = Math.max(highestSequence, lastModified);
        }
        return highestSequence;
    }

    private void resolveUnresolvedEntries(HolderLookup.Provider provider) {
        if (provider == null || this.unresolvedEntries.isEmpty()) {
            return;
        }
        java.util.List<UnresolvedStoredEntry> stillUnresolvedEntries = new java.util.ArrayList<>(this.unresolvedEntries.size());
        for (UnresolvedStoredEntry unresolvedEntry : this.unresolvedEntries) {
            Optional<UnresolvedStoredEntry.ResolvedStoredEntry> resolvedEntry = unresolvedEntry.tryResolve(provider);
            if (resolvedEntry.isPresent()) {
                this.mergeResolvedEntry(resolvedEntry.get().key(), resolvedEntry.get().entry());
            } else {
                stillUnresolvedEntries.add(unresolvedEntry);
            }
        }
        this.unresolvedEntries.clear();
        this.unresolvedEntries.addAll(stillUnresolvedEntries);
    }

    private void mergeResolvedEntry(StoredStackKey key, StoredStackEntry incomingEntry) {
        if (key == null || incomingEntry == null || incomingEntry.isEmpty()) {
            return;
        }
        DatabaseCategory normalizedIncomingCategory = normalizeStoredCategory(incomingEntry.category());
        StoredStackEntry existingEntry = this.entries.get(key);
        if (existingEntry == null) {
            this.entries.put(key, new StoredStackEntry(normalizedIncomingCategory, incomingEntry.amount(), incomingEntry.lastModified()));
            return;
        }
        this.entries.put(key, new StoredStackEntry(
                mergeStoredCategory(existingEntry.category(), normalizedIncomingCategory),
                safeAdd(existingEntry.amount(), incomingEntry.amount()),
                Math.max(existingEntry.lastModified(), incomingEntry.lastModified())
        ));
    }

    private void finishNextSequence(long serializedNextSequence, long highestSequence) {
        this.nextSequence = Math.max(serializedNextSequence, highestSequence + 1L);
        if (this.nextSequence <= 0L) {
            this.nextSequence = 1L;
        }
    }

    private static DatabaseCategory readCategory(CompoundTag tag, ItemStack stack) {
        try {
            return DatabaseCategory.valueOf(tag.getString(CATEGORY_KEY));
        } catch (IllegalArgumentException exception) {
            return stack == null || stack.isEmpty() ? DatabaseCategory.OTHER : DatabaseItemClassifier.INSTANCE.classify(stack);
        }
    }

    private static DatabaseCategory mergeStoredCategory(DatabaseCategory existingCategory, DatabaseCategory incomingCategory) {
        DatabaseCategory normalizedExistingCategory = normalizeStoredCategory(existingCategory);
        DatabaseCategory normalizedIncomingCategory = normalizeStoredCategory(incomingCategory);
        if (normalizedExistingCategory == normalizedIncomingCategory) {
            return normalizedIncomingCategory;
        }
        if (normalizedExistingCategory == DatabaseCategory.OTHER) {
            return normalizedIncomingCategory;
        }
        if (normalizedIncomingCategory == DatabaseCategory.OTHER) {
            return normalizedExistingCategory;
        }
        return normalizedIncomingCategory;
    }

    private static DatabaseCategory normalizeStoredCategory(DatabaseCategory category) {
        if (category == null || category == DatabaseCategory.ALL) {
            return DatabaseCategory.OTHER;
        }
        return category;
    }

    private static long safeAdd(long left, long right) {
        if (right <= 0L) {
            return left;
        }
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private long nextSequence() {
        if (this.nextSequence == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return this.nextSequence++;
    }

    private boolean recategorizeResolvedEntriesIfNeeded(int storedClassifierVersion) {
        if (storedClassifierVersion >= DatabaseItemClassifier.CURRENT_VERSION || this.entries.isEmpty()) {
            return false;
        }
        boolean recategorized = false;
        Map<StoredStackKey, StoredStackEntry> recategorizedEntries = new LinkedHashMap<>(this.entries.size());
        for (Map.Entry<StoredStackKey, StoredStackEntry> mapEntry : this.entries.entrySet()) {
            StoredStackKey key = mapEntry.getKey();
            StoredStackEntry entry = mapEntry.getValue();
            DatabaseCategory recategorizedCategory = normalizeStoredCategory(DatabaseItemClassifier.INSTANCE.classify(key.displayStack()));
            if (entry.category() != recategorizedCategory) {
                recategorized = true;
            }
            recategorizedEntries.put(key, new StoredStackEntry(recategorizedCategory, entry.amount(), entry.lastModified()));
        }
        this.entries.clear();
        this.entries.putAll(recategorizedEntries);
        return recategorized;
    }

    private boolean hasStoredContent() {
        return !this.entries.isEmpty() || !this.unresolvedEntries.isEmpty() || this.nextSequence != 1L;
    }

    private void resetContent() {
        this.entries.clear();
        this.unresolvedEntries.clear();
        this.nextSequence = 1L;
        this.needsResave = false;
    }

    private void markRuntimeStateDirty() {
        if (this.revision < Long.MAX_VALUE) {
            this.revision++;
        }
    }
}
