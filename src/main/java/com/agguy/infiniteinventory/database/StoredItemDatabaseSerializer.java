package com.agguy.infiniteinventory.database;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * 负责 {@link StoredItemDatabase} 的 NBT 序列化与反序列化逻辑。
 *
 * <p>设计意图：将序列化/反序列化这一独立职责从数据容器中剥离，降低原类复杂度，
 * 同时保留对 schema 版本升级与未解析条目恢复的完整支持。</p>
 *
 * <p>本类为包级可见，仅由 StoredItemDatabase 委托调用。</p>
 */
final class StoredItemDatabaseSerializer {

    private static final String SCHEMA_VERSION_KEY = "schema_version";
    private static final String ENTRIES_KEY = "entries";
    private static final String UNRESOLVED_ENTRIES_KEY = "unresolved_entries";
    private static final String STACK_KEY = "stack";
    private static final String COUNT_KEY = "count";
    private static final String TAB_ID_KEY = "tab_id";
    private static final String FIRST_ADDED_KEY = "first_added";
    private static final String LAST_MODIFIED_KEY = "last_modified";
    private static final String NEXT_SEQUENCE_KEY = "next_sequence";
    private static final String LOG_ENTRIES_KEY = "log_entries";
    private static final String NOTES_KEY = "notes";
    private static final String NOTE_KEY = "note_key";
    private static final String NOTE_TEXT_KEY = "note_text";
    private static final String STARRED_ENTRIES_KEY = "starred_entries";

    private StoredItemDatabaseSerializer() {
    }

    /**
     * 将数据库完整状态序列化为 NBT 复合标签。
     *
     * @param database 要序列化的数据库实例
     * @param provider 用于物品堆栈序列化的注册表查找提供者
     * @return 包含完整数据库状态的 NBT 标签
     */
    static CompoundTag serialize(StoredItemDatabase database, HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        root.putInt(SCHEMA_VERSION_KEY, StoredItemDatabase.CURRENT_SCHEMA_VERSION);
        ListTag serializedEntries = new ListTag();
        HolderLookup.Provider resolvedProvider = null;

        for (Map.Entry<StoredStackKey, StoredStackEntry> mapEntry : database.entries().entrySet()) {
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
        for (UnresolvedStoredEntry unresolvedEntry : database.unresolvedEntries()) {
            if (!unresolvedEntry.isEmpty()) {
                serializedUnresolvedEntries.add(unresolvedEntry.toTag());
            }
        }
        root.put(UNRESOLVED_ENTRIES_KEY, serializedUnresolvedEntries);

        if (!database.logEntries().isEmpty() || !database.notes().isEmpty() || !database.starredEntries().isEmpty()) {
            if (resolvedProvider == null) {
                resolvedProvider = DatabaseHolderLookup.require(provider, "stored item database serialization");
            }
            if (!database.logEntries().isEmpty()) {
                root.put(LOG_ENTRIES_KEY, DatabaseLogEntry.writeList(resolvedProvider, database.logEntries()));
            }
            if (!database.notes().isEmpty()) {
                ListTag serializedNotes = new ListTag();
                for (Map.Entry<StoredStackKey, String> noteEntry : database.notes().entrySet()) {
                    Tag serializedStack = noteEntry.getKey().displayStack().saveOptional(resolvedProvider);
                    if (!(serializedStack instanceof CompoundTag stackTag)) {
                        continue;
                    }
                    CompoundTag noteTag = new CompoundTag();
                    noteTag.put(NOTE_KEY, stackTag);
                    noteTag.putString(NOTE_TEXT_KEY, noteEntry.getValue());
                    serializedNotes.add(noteTag);
                }
                root.put(NOTES_KEY, serializedNotes);
            }
            if (!database.starredEntries().isEmpty()) {
                ListTag serializedStarred = new ListTag();
                for (StoredStackKey starredKey : database.starredEntries()) {
                    Tag serializedStack = starredKey.displayStack().saveOptional(resolvedProvider);
                    if (!(serializedStack instanceof CompoundTag stackTag)) {
                        continue;
                    }
                    CompoundTag starredTag = new CompoundTag();
                    starredTag.put(NOTE_KEY, stackTag);
                    serializedStarred.add(starredTag);
                }
                root.put(STARRED_ENTRIES_KEY, serializedStarred);
            }
        }

        root.putLong(NEXT_SEQUENCE_KEY, database.nextSequence());
        return root;
    }

    /**
     * 从 NBT 复合标签反序列化数据库状态。
     *
     * @param database 要填充数据的目标数据库实例
     * @param provider 用于物品堆栈反序列化的注册表查找提供者
     * @param tag      包含数据库状态的 NBT 标签
     */
    static void deserialize(StoredItemDatabase database, HolderLookup.Provider provider, CompoundTag tag) {
        HolderLookup.Provider resolvedProvider = DatabaseHolderLookup.resolve(provider);
        database.resetContent();
        if (tag == null || tag.isEmpty()) {
            database.markRuntimeStateDirty();
            return;
        }
        if (tag.contains(SCHEMA_VERSION_KEY)) {
            readCurrentFormat(database, resolvedProvider, tag, Math.max(0, tag.getInt(SCHEMA_VERSION_KEY)));
        } else {
            readLegacyFormat(database, resolvedProvider, tag);
        }
        database.markRuntimeStateDirty();
    }

    private static void readCurrentFormat(StoredItemDatabase database, HolderLookup.Provider provider, CompoundTag tag, int storedSchemaVersion) {
        long highestSequence = readResolvedEntries(database, provider, tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND), true, storedSchemaVersion);
        for (Tag element : tag.getList(UNRESOLVED_ENTRIES_KEY, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag unresolvedEntryTag)) {
                continue;
            }
            UnresolvedStoredEntry unresolvedEntry = UnresolvedStoredEntry.fromTag(unresolvedEntryTag);
            if (unresolvedEntry.isEmpty()) {
                continue;
            }
            database.unresolvedEntriesInternal().add(unresolvedEntry);
            highestSequence = Math.max(highestSequence, unresolvedEntry.lastModified());
        }
        resolveUnresolvedEntries(database, provider);
        database.logEntriesInternal().clear();
        database.logEntriesInternal().addAll(DatabaseLogEntry.readList(provider, tag.getList(LOG_ENTRIES_KEY, Tag.TAG_COMPOUND)));
        for (Tag element : tag.getList(NOTES_KEY, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag noteTag)) {
                continue;
            }
            CompoundTag stackTag = noteTag.getCompound(NOTE_KEY);
            String noteText = noteTag.getString(NOTE_TEXT_KEY);
            if (stackTag.isEmpty() || noteText.isEmpty() || provider == null) {
                continue;
            }
            ItemStack stack = ItemStack.parseOptional(provider, stackTag.copy());
            if (!stack.isEmpty()) {
                database.notesInternal().put(StoredStackKey.of(stack), noteText);
            }
        }
        for (Tag element : tag.getList(STARRED_ENTRIES_KEY, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag starredTag)) {
                continue;
            }
            CompoundTag stackTag = starredTag.getCompound(NOTE_KEY);
            if (stackTag.isEmpty() || provider == null) {
                continue;
            }
            ItemStack stack = ItemStack.parseOptional(provider, stackTag.copy());
            if (!stack.isEmpty()) {
                database.starredEntriesInternal().add(StoredStackKey.of(stack));
            }
        }
        database.setNeedsResave(storedSchemaVersion < StoredItemDatabase.CURRENT_SCHEMA_VERSION);
        finishNextSequence(database, tag.getLong(NEXT_SEQUENCE_KEY), highestSequence);
    }

    private static void readLegacyFormat(StoredItemDatabase database, HolderLookup.Provider provider, CompoundTag tag) {
        long highestSequence = readResolvedEntries(database, provider, tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND), true, 0);
        resolveUnresolvedEntries(database, provider);
        finishNextSequence(database, tag.getLong(NEXT_SEQUENCE_KEY), highestSequence);
        database.setNeedsResave(true);
    }

    private static long readResolvedEntries(StoredItemDatabase database, HolderLookup.Provider provider, ListTag entryList, boolean preserveInvalidEntries, int storedSchemaVersion) {
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
            long firstAdded = StoredItemDatabaseHelper.readFirstAdded(entryTag, lastModified);
            String tabId = StoredItemDatabaseHelper.readTabId(entryTag, storedSchemaVersion, StoredItemDatabase.CURRENT_SCHEMA_VERSION);
            if (provider == null) {
                if (preserveInvalidEntries && !stackTag.isEmpty()) {
                    database.unresolvedEntriesInternal().add(new UnresolvedStoredEntry(
                            stackTag, amount, tabId, lastModified, firstAdded
                    ));
                    highestSequence = Math.max(highestSequence, lastModified);
                }
                continue;
            }
            ItemStack stack = ItemStack.parseOptional(provider, stackTag.copy());
            if (stack.isEmpty()) {
                if (preserveInvalidEntries && !stackTag.isEmpty()) {
                    database.unresolvedEntriesInternal().add(new UnresolvedStoredEntry(stackTag, amount, tabId, lastModified, firstAdded));
                    highestSequence = Math.max(highestSequence, lastModified);
                }
                continue;
            }
            database.mergeResolvedEntryInternal(
                    StoredStackKey.of(stack),
                    new StoredStackEntry(tabId, amount, lastModified, firstAdded)
            );
            highestSequence = Math.max(highestSequence, lastModified);
        }
        return highestSequence;
    }

    private static void resolveUnresolvedEntries(StoredItemDatabase database, HolderLookup.Provider provider) {
        if (provider == null || database.unresolvedEntriesInternal().isEmpty()) {
            return;
        }
        java.util.List<UnresolvedStoredEntry> stillUnresolvedEntries = new java.util.ArrayList<>(database.unresolvedEntriesInternal().size());
        for (UnresolvedStoredEntry unresolvedEntry : database.unresolvedEntriesInternal()) {
            Optional<UnresolvedStoredEntry.ResolvedStoredEntry> resolvedEntry = unresolvedEntry.tryResolve(provider);
            if (resolvedEntry.isPresent()) {
                database.mergeResolvedEntryInternal(resolvedEntry.get().key(), resolvedEntry.get().entry());
            } else {
                stillUnresolvedEntries.add(unresolvedEntry);
            }
        }
        database.unresolvedEntriesInternal().clear();
        database.unresolvedEntriesInternal().addAll(stillUnresolvedEntries);
    }

    private static void finishNextSequence(StoredItemDatabase database, long serializedNextSequence, long highestSequence) {
        long nextSequence = Math.max(serializedNextSequence, highestSequence + 1L);
        if (nextSequence <= 0L) {
            nextSequence = 1L;
        }
        database.setNextSequence(nextSequence);
    }
}
