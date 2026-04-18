package com.agguy.infiniteinventory.service.search;

import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.localization.ViewerLanguage;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

final class DatabaseSearchIndexCache {
    private final Map<StoredStackKey, EnumMap<ViewerLanguage, DatabaseSearchIndex>> cache = new WeakHashMap<>();

    synchronized DatabaseSearchIndex resolve(
            StoredStackKey key,
            ViewerLanguage viewerLanguage,
            Function<StoredStackKey, DatabaseSearchIndex> builder
    ) {
        EnumMap<ViewerLanguage, DatabaseSearchIndex> localizedIndexes =
                this.cache.computeIfAbsent(key, ignored -> new EnumMap<>(ViewerLanguage.class));
        ViewerLanguage normalizedLanguage = viewerLanguage == null ? ViewerLanguage.defaultLanguage() : viewerLanguage;
        return localizedIndexes.computeIfAbsent(normalizedLanguage, ignored -> builder.apply(key));
    }
}
