package com.agguy.infiniteinventory.database;

public final class StoredStackEntry {
    private final DatabaseCategory category;
    private long amount;
    private final long firstAdded;
    private long lastModified;

    public StoredStackEntry(DatabaseCategory category, long amount, long lastModified) {
        this(category, amount, lastModified, lastModified);
    }

    public StoredStackEntry(DatabaseCategory category, long amount, long lastModified, long firstAdded) {
        this.category = category;
        this.amount = Math.max(0L, amount);
        this.firstAdded = Math.max(0L, firstAdded);
        this.lastModified = Math.max(this.firstAdded, Math.max(0L, lastModified));
    }

    public DatabaseCategory category() {
        return this.category;
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
