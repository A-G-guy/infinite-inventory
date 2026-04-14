package com.agguy.infiniteinventory.database;

import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 保留暂时无法解析的物品条目，避免依赖缺失时直接丢档。
 */
public final class UnresolvedStoredEntry {
    private static final String STACK_KEY = "stack";
    private static final String COUNT_KEY = "count";
    private static final String CATEGORY_KEY = "category";
    private static final String TAB_ID_KEY = "tab_id";
    private static final String FIRST_ADDED_KEY = "first_added";
    private static final String LAST_MODIFIED_KEY = "last_modified";

    private final CompoundTag stackTag;
    private final long amount;
    private final String tabId;
    private final long firstAdded;
    private final long lastModified;

    public UnresolvedStoredEntry(CompoundTag stackTag, long amount, String tabId, long lastModified) {
        this(stackTag, amount, tabId, lastModified, lastModified);
    }

    public UnresolvedStoredEntry(CompoundTag stackTag, long amount, String tabId, long lastModified, long firstAdded) {
        this.stackTag = stackTag == null ? new CompoundTag() : stackTag.copy();
        this.amount = Math.max(0L, amount);
        this.tabId = DatabaseTabs.normalizeConcreteTarget(tabId);
        this.firstAdded = Math.max(0L, firstAdded);
        this.lastModified = Math.max(this.firstAdded, Math.max(0L, lastModified));
    }

    public CompoundTag stackTag() {
        return this.stackTag.copy();
    }

    public long amount() {
        return this.amount;
    }

    public String tabId() {
        return this.tabId;
    }

    public long firstAdded() {
        return this.firstAdded;
    }

    public long lastModified() {
        return this.lastModified;
    }

    public UnresolvedStoredEntry withTabId(String newTabId, long sequence) {
        return new UnresolvedStoredEntry(
                this.stackTag,
                this.amount,
                newTabId,
                Math.max(this.lastModified, sequence),
                this.firstAdded
        );
    }

    public boolean isEmpty() {
        return this.amount <= 0L || this.stackTag.isEmpty();
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put(STACK_KEY, this.stackTag.copy());
        tag.putLong(COUNT_KEY, this.amount);
        tag.putString(TAB_ID_KEY, this.tabId);
        tag.putLong(FIRST_ADDED_KEY, this.firstAdded);
        tag.putLong(LAST_MODIFIED_KEY, this.lastModified);
        return tag;
    }

    public Optional<ResolvedStoredEntry> tryResolve(HolderLookup.Provider provider) {
        HolderLookup.Provider resolvedProvider = DatabaseHolderLookup.resolve(provider);
        if (resolvedProvider == null || this.isEmpty()) {
            return Optional.empty();
        }
        ItemStack stack = ItemStack.parseOptional(resolvedProvider, this.stackTag.copy());
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedStoredEntry(
                StoredStackKey.of(stack),
                new StoredStackEntry(this.tabId, this.amount, this.lastModified, this.firstAdded)
        ));
    }

    public static UnresolvedStoredEntry fromTag(CompoundTag tag) {
        if (tag == null) {
            return new UnresolvedStoredEntry(new CompoundTag(), 0L, DatabaseTabs.DEFAULT_TAB_ID, 0L);
        }
        return new UnresolvedStoredEntry(
                tag.getCompound(STACK_KEY),
                Math.max(0L, tag.getLong(COUNT_KEY)),
                readTabId(tag),
                Math.max(0L, tag.getLong(LAST_MODIFIED_KEY)),
                readFirstAdded(tag)
        );
    }

    private static String readTabId(CompoundTag tag) {
        if (tag.contains(TAB_ID_KEY)) {
            return DatabaseTabs.normalizeConcreteTarget(tag.getString(TAB_ID_KEY));
        }
        return DatabaseTabs.DEFAULT_TAB_ID;
    }

    private static long readFirstAdded(CompoundTag tag) {
        long lastModified = Math.max(0L, tag.getLong(LAST_MODIFIED_KEY));
        if (!tag.contains(FIRST_ADDED_KEY)) {
            return lastModified;
        }
        return Math.max(0L, tag.getLong(FIRST_ADDED_KEY));
    }

    public record ResolvedStoredEntry(StoredStackKey key, StoredStackEntry entry) {
    }
}
