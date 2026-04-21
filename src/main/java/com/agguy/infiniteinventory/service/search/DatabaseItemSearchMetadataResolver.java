package com.agguy.infiniteinventory.service.search;

import com.agguy.infiniteinventory.database.StoredStackKey;
import java.util.Map;
import java.util.WeakHashMap;

public final class DatabaseItemSearchMetadataResolver {
    public static final DatabaseItemSearchMetadataResolver INSTANCE = new DatabaseItemSearchMetadataResolver();

    private final Map<StoredStackKey, DatabaseItemSearchMetadata> cache = new WeakHashMap<>();

    private DatabaseItemSearchMetadataResolver() {
    }

    public synchronized DatabaseItemSearchMetadata resolve(StoredStackKey key) {
        return this.cache.computeIfAbsent(key, DatabaseItemSearchMetadata::of);
    }
}
