package com.agguy.infiniteinventory.database;

public final class StoredStackEntry {
    private String tabId;
    private long amount;
    private final long firstAdded;
    private long lastModified;

    public StoredStackEntry(String tabId, long amount, long lastModified) {
        this(tabId, amount, lastModified, lastModified);
    }

    public StoredStackEntry(String tabId, long amount, long lastModified, long firstAdded) {
        this.tabId = DatabaseTabs.normalizeConcreteTarget(tabId);
        this.amount = Math.max(0L, amount);
        this.firstAdded = Math.max(0L, firstAdded);
        this.lastModified = Math.max(this.firstAdded, Math.max(0L, lastModified));
    }

    public String tabId() {
        return this.tabId;
    }

    public long amount() {
        return this.amount;
    }

    public long firstAdded() {
        return this.firstAdded;
    }

    public long lastModified() {
        return this.lastModified;
    }

    public void add(long delta, long sequence) {
        this.amount = safeAdd(this.amount, delta);
        this.lastModified = Math.max(this.lastModified, sequence);
    }

    public long remove(long delta, long sequence) {
        long removed = Math.min(Math.max(0L, delta), this.amount);
        this.amount -= removed;
        this.lastModified = Math.max(this.lastModified, sequence);
        return removed;
    }

    public boolean moveToTab(String targetTabId, long sequence) {
        String normalizedTargetTabId = DatabaseTabs.normalizeConcreteTarget(targetTabId);
        if (this.tabId.equals(normalizedTargetTabId)) {
            return false;
        }
        this.tabId = normalizedTargetTabId;
        this.lastModified = Math.max(this.lastModified, sequence);
        return true;
    }

    public boolean isEmpty() {
        return this.amount <= 0L;
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
}
