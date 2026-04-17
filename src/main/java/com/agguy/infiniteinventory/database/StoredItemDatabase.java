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
    public static final int CURRENT_SCHEMA_VERSION = 4;

    private static final String SCHEMA_VERSION_KEY = "schema_version";
    private static final String ENTRIES_KEY = "entries";
    private static final String UNRESOLVED_ENTRIES_KEY = "unresolved_entries";
    private static final String STACK_KEY = "stack";
    private static final String COUNT_KEY = "count";
    private static final String TAB_ID_KEY = "tab_id";
    private static final String FIRST_ADDED_KEY = "first_added";
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
                        unresolvedEntry.tabId(),
                        unresolvedEntry.lastModified(),
                        unresolvedEntry.firstAdded()
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
        this.store(stack, DatabaseTabs.DEFAULT_TAB_ID);
    }

    public void store(ItemStack stack, String tabId) {
        if (stack.isEmpty()) {
            return;
        }
        long sequence = this.nextSequence();
        String normalizedTabId = DatabaseTabs.normalizeConcreteTarget(tabId);
        StoredStackKey key = StoredStackKey.of(stack);
        StoredStackEntry entry = this.entries.get(key);
        if (entry == null) {
            entry = new StoredStackEntry(normalizedTabId, 0L, sequence, sequence);
            this.entries.put(key, entry);
        } else {
            entry.moveToTab(normalizedTabId, sequence);
        }
        entry.add(stack.getCount(), sequence);
        this.markRuntimeStateDirty();
    }

    public boolean transferTab(String sourceTabId, String targetTabId) {
        String normalizedSourceTabId = DatabaseTabs.normalizeConcreteTarget(sourceTabId), normalizedTargetTabId = DatabaseTabs.normalizeConcreteTarget(targetTabId);
        if (normalizedSourceTabId.equals(normalizedTargetTabId)) {
            return false;
        }
        long sequence = this.nextSequence();
        boolean changed = false;
        for (StoredStackEntry entry : this.entries.values()) {
            changed = entry.tabId().equals(normalizedSourceTabId) && entry.moveToTab(normalizedTargetTabId, sequence) || changed;
        }
        if (!this.unresolvedEntries.isEmpty()) {
            java.util.ArrayList<UnresolvedStoredEntry> updatedEntries = new java.util.ArrayList<>(this.unresolvedEntries.size());
            for (UnresolvedStoredEntry unresolvedEntry : this.unresolvedEntries) {
                if (unresolvedEntry.tabId().equals(normalizedSourceTabId)) {
                    updatedEntries.add(unresolvedEntry.withTabId(normalizedTargetTabId, sequence));
                    changed = true;
                } else {
                    updatedEntries.add(unresolvedEntry);
                }
            }
            this.unresolvedEntries.clear();
            this.unresolvedEntries.addAll(updatedEntries);
        }
        if (changed) {
            this.markRuntimeStateDirty();
        }
        return changed;
    }

    public boolean moveEntryToTab(StoredStackKey key, String sourceTabId, String targetTabId) {
        if (key == null) {
            return false;
        }
        StoredStackEntry entry = this.entries.get(key);
        if (entry == null || !entry.tabId().equals(DatabaseTabs.normalizeConcreteTarget(sourceTabId))) {
            return false;
        }
        boolean changed = entry.moveToTab(targetTabId, this.nextSequence());
        if (changed) {
            this.markRuntimeStateDirty();
        }
        return changed;
    }

    public boolean ensureTabAssignments(DatabaseTabDirectory tabDirectory) {
        if (tabDirectory == null) {
            return false;
        }
        String defaultTabId = tabDirectory.defaultConcreteTab().id();
        boolean changed = false;
        for (StoredStackEntry entry : this.entries.values()) {
            if (!tabDirectory.containsConcreteTab(entry.tabId())) {
                entry.moveToTab(defaultTabId, entry.lastModified());
                changed = true;
            }
        }
        if (!this.unresolvedEntries.isEmpty()) {
            java.util.ArrayList<UnresolvedStoredEntry> updatedUnresolvedEntries = new java.util.ArrayList<>(this.unresolvedEntries.size());
            for (UnresolvedStoredEntry unresolvedEntry : this.unresolvedEntries) {
                if (!tabDirectory.containsConcreteTab(unresolvedEntry.tabId())) {
                    updatedUnresolvedEntries.add(new UnresolvedStoredEntry(
                            unresolvedEntry.stackTag(),
                            unresolvedEntry.amount(),
                            defaultTabId,
                            unresolvedEntry.lastModified(),
                            unresolvedEntry.firstAdded()
                    ));
                    changed = true;
                } else {
                    updatedUnresolvedEntries.add(unresolvedEntry);
                }
            }
            if (changed) {
                this.unresolvedEntries.clear();
                this.unresolvedEntries.addAll(updatedUnresolvedEntries);
            }
        }
        if (changed) {
            this.markRuntimeStateDirty();
        }
        return changed;
    }

    public ItemStack extract(StoredStackKey key, int requestedAmount) {
        if (requestedAmount <= 0) {
            return ItemStack.EMPTY;
        }
        StoredStackEntry entry = this.entries.get(key);
        if (entry == null) {
            return ItemStack.EMPTY;
        }
        int maxExtractableAmount = Math.max(1, key.maxStackSize());
        int extractedAmount = (int) Math.min(entry.amount(), Math.min((long) requestedAmount, (long) maxExtractableAmount));
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
        ListTag serializedEntries = new ListTag();
        HolderLookup.Provider resolvedProvider = null;
        for (Map.Entry<StoredStackKey, StoredStackEntry> mapEntry : this.entries.entrySet()) {
            if (resolvedProvider == null) {
                resolvedProvider = DatabaseHolderLookup.require(provider, "stored item database serialization");
            }
            ItemStack stack = mapEntry.getKey().displayStack();
            Tag serializedStack = stack.saveOptional(resolvedProvider);
            if (!(serializedStack instanceof CompoundTag stackTag)) {
                continue;
            }
            StoredStackEntry entry = mapEntry.getValue();
            CompoundTag entryTag = new CompoundTag();
            entryTag.put(STACK_KEY, stackTag);
            entryTag.putLong(COUNT_KEY, entry.amount());
            entryTag.putString(TAB_ID_KEY, entry.tabId());
            entryTag.putLong(FIRST_ADDED_KEY, entry.firstAdded());
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
        HolderLookup.Provider resolvedProvider = DatabaseHolderLookup.resolve(provider);
        this.resetContent();
        if (tag == null || tag.isEmpty()) {
            this.markRuntimeStateDirty();
            return;
        }
        if (tag.contains(SCHEMA_VERSION_KEY)) {
            this.readCurrentFormat(resolvedProvider, tag, Math.max(0, tag.getInt(SCHEMA_VERSION_KEY)));
        } else {
            this.readLegacyFormat(resolvedProvider, tag);
        }
        this.markRuntimeStateDirty();
    }

    private void readCurrentFormat(HolderLookup.Provider provider, CompoundTag tag, int storedSchemaVersion) {
        long highestSequence = this.readResolvedEntries(provider, tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND), true, storedSchemaVersion);
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
        this.needsResave = storedSchemaVersion < CURRENT_SCHEMA_VERSION;
        this.finishNextSequence(tag.getLong(NEXT_SEQUENCE_KEY), highestSequence);
    }

    private void readLegacyFormat(HolderLookup.Provider provider, CompoundTag tag) {
        long highestSequence = this.readResolvedEntries(provider, tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND), true, 0);
        this.resolveUnresolvedEntries(provider);
        this.finishNextSequence(tag.getLong(NEXT_SEQUENCE_KEY), highestSequence);
        this.needsResave = true;
    }

    private long readResolvedEntries(HolderLookup.Provider provider, ListTag entryList, boolean preserveInvalidEntries, int storedSchemaVersion) {
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
            long firstAdded = readFirstAdded(entryTag, lastModified);
            String tabId = readTabId(entryTag, storedSchemaVersion);
            if (provider == null) {
                if (preserveInvalidEntries && !stackTag.isEmpty()) {
                    this.unresolvedEntries.add(new UnresolvedStoredEntry(
                            stackTag,
                            amount,
                            tabId,
                            lastModified,
                            firstAdded
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
                            tabId,
                            lastModified,
                            firstAdded
                    ));
                    highestSequence = Math.max(highestSequence, lastModified);
                }
                continue;
            }
            this.mergeResolvedEntry(
                    StoredStackKey.of(stack),
                    new StoredStackEntry(tabId, amount, lastModified, firstAdded)
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
        StoredStackEntry existingEntry = this.entries.get(key);
        if (existingEntry == null) {
            this.entries.put(key, new StoredStackEntry(
                    incomingEntry.tabId(),
                    incomingEntry.amount(),
                    incomingEntry.lastModified(),
                    incomingEntry.firstAdded()
            ));
            return;
        }
        String mergedTabId = incomingEntry.lastModified() >= existingEntry.lastModified()
                ? incomingEntry.tabId()
                : existingEntry.tabId();
        this.entries.put(key, new StoredStackEntry(
                mergedTabId,
                safeAdd(existingEntry.amount(), incomingEntry.amount()),
                Math.max(existingEntry.lastModified(), incomingEntry.lastModified()),
                mergeFirstAdded(existingEntry.firstAdded(), incomingEntry.firstAdded())
        ));
    }

    private void finishNextSequence(long serializedNextSequence, long highestSequence) {
        this.nextSequence = Math.max(serializedNextSequence, highestSequence + 1L);
        if (this.nextSequence <= 0L) {
            this.nextSequence = 1L;
        }
    }

    private static String readTabId(CompoundTag tag, int storedSchemaVersion) {
        if (storedSchemaVersion >= CURRENT_SCHEMA_VERSION && tag.contains(TAB_ID_KEY)) {
            return DatabaseTabs.normalizeConcreteTarget(tag.getString(TAB_ID_KEY));
        }
        if (tag.contains(TAB_ID_KEY)) {
            return DatabaseTabs.normalizeConcreteTarget(tag.getString(TAB_ID_KEY));
        }
        return DatabaseTabs.DEFAULT_TAB_ID;
    }

    private static long readFirstAdded(CompoundTag tag, long lastModified) {
        if (!tag.contains(FIRST_ADDED_KEY)) {
            return Math.max(0L, lastModified);
        }
        return Math.max(0L, tag.getLong(FIRST_ADDED_KEY));
    }

    private static long mergeFirstAdded(long existingFirstAdded, long incomingFirstAdded) {
        long normalizedExisting = Math.max(0L, existingFirstAdded);
        long normalizedIncoming = Math.max(0L, incomingFirstAdded);
        if (normalizedExisting == 0L) {
            return normalizedIncoming;
        }
        if (normalizedIncoming == 0L) {
            return normalizedExisting;
        }
        return Math.min(normalizedExisting, normalizedIncoming);
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

    long nextSequence() {
        if (this.nextSequence == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return this.nextSequence++;
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

    void markRuntimeStateDirty() {
        if (this.revision < Long.MAX_VALUE) {
            this.revision++;
        }
    }
}
