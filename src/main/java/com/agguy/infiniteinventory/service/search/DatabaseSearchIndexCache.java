package com.agguy.infiniteinventory.service.search;

import com.agguy.infiniteinventory.database.StoredStackKey;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

final class DatabaseSearchIndexCache {
    private final Map<StoredStackKey, DatabaseSearchIndex> cache = new WeakHashMap<>();

    synchronized DatabaseSearchIndex resolve(StoredStackKey key, Function<StoredStackKey, DatabaseSearchIndex> builder) {
        return this.cache.computeIfAbsent(key, builder);
    }
}
