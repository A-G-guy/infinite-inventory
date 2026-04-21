package com.agguy.infiniteinventory.service.search;

import com.agguy.infiniteinventory.database.StoredStackKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.fml.ModList;

public final class DatabaseCreativeTabSearchResolver {
    public static final DatabaseCreativeTabSearchResolver INSTANCE = new DatabaseCreativeTabSearchResolver();

    private final Map<DatabaseSearchEnvironmentSignature, List<DatabaseCreativeTabSearchEntry>> availableTabsCache = new LinkedHashMap<>();
    private final Map<DatabaseSearchEnvironmentSignature, Map<StoredStackKey, List<DatabaseCreativeTabSearchEntry>>> itemTabCache = new LinkedHashMap<>();

    private DatabaseCreativeTabSearchResolver() {
    }

    public synchronized Set<String> creativeTabDisplayNameCandidates(DatabaseSearchEnvironment searchEnvironment) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        for (DatabaseCreativeTabSearchEntry tabEntry : this.availableTabs(searchEnvironment)) {
            if (!tabEntry.displayNameNormalized().isEmpty()) {
                candidates.add(tabEntry.displayNameNormalized());
            }
        }
        return Set.copyOf(candidates);
    }

    public synchronized List<DatabaseCreativeTabSearchEntry> tabsForItem(StoredStackKey key, DatabaseSearchEnvironment searchEnvironment) {
        DatabaseSearchEnvironmentSignature signature = normalizeEnvironment(searchEnvironment).signature();
        Map<StoredStackKey, List<DatabaseCreativeTabSearchEntry>> cacheForEnvironment =
                this.itemTabCache.computeIfAbsent(signature, ignored -> new WeakHashMap<>());
        List<DatabaseCreativeTabSearchEntry> cachedTabs = cacheForEnvironment.get(key);
        if (cachedTabs != null) {
            return cachedTabs;
        }

        java.util.ArrayList<DatabaseCreativeTabSearchEntry> matchedTabs = new java.util.ArrayList<>();
        for (DatabaseCreativeTabSearchEntry tabEntry : this.availableTabs(searchEnvironment)) {
            if (tabEntry.tab().contains(key.displayStack())) {
                matchedTabs.add(tabEntry);
            }
        }
        List<DatabaseCreativeTabSearchEntry> resolvedTabs = List.copyOf(matchedTabs);
        cacheForEnvironment.put(key, resolvedTabs);
        return resolvedTabs;
    }

    private List<DatabaseCreativeTabSearchEntry> availableTabs(DatabaseSearchEnvironment searchEnvironment) {
        DatabaseSearchEnvironment normalizedEnvironment = normalizeEnvironment(searchEnvironment);
        return this.availableTabsCache.computeIfAbsent(normalizedEnvironment.signature(), ignored -> this.loadTabs(normalizedEnvironment));
    }

    private List<DatabaseCreativeTabSearchEntry> loadTabs(DatabaseSearchEnvironment searchEnvironment) {
        if (ModList.get() != null) {
            CreativeModeTabs.tryRebuildTabContents(
                    searchEnvironment.enabledFeatures(),
                    searchEnvironment.hasPermissions(),
                    searchEnvironment.registryAccess()
            );
        }
        java.util.ArrayList<DatabaseCreativeTabSearchEntry> tabs = new ArrayList<>();
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            if (tab.getType() != CreativeModeTab.Type.CATEGORY || !tab.shouldDisplay()) {
                continue;
            }
            ResourceLocation tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            tabs.add(DatabaseCreativeTabSearchEntry.of(tab, tabId));
        }
        return List.copyOf(tabs);
    }

    private static DatabaseSearchEnvironment normalizeEnvironment(DatabaseSearchEnvironment searchEnvironment) {
        return searchEnvironment == null ? DatabaseSearchEnvironment.defaultEnvironment() : searchEnvironment;
    }
}
