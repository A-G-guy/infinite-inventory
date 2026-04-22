package com.agguy.infiniteinventory.database;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class DatabaseViewPreferencesAttachment implements INBTSerializable<CompoundTag> {
    private static final String QUERY_KEY = "query";
    private static final String LEGACY_LAST_SCOPE_KEY = "last_scope";
    private static final String LEGACY_PERSONAL_QUERY_KEY = "personal_query";
    private static final String LEGACY_PUBLIC_QUERY_KEY = "public_query";
    private static final String ENHANCEMENT_CONFIG_KEY = "enhancement_config";
    private static final String AUTO_STORE_TARGET_KEY = "auto_store_target";
    private static final String LEGACY_AUTO_STORE_TARGET_TAB_ID_KEY = "auto_store_target_tab_id";

    private DatabaseQuery query = DatabaseQuery.defaultQuery();
    private DatabaseEnhancementConfig enhancementConfig = DatabaseEnhancementConfig.defaultConfig();
    private DatabaseAutoStoreTarget autoStoreTarget = DatabaseAutoStoreTarget.defaultTarget();

    public DatabaseQuery query() {
        return this.query;
    }

    public DatabaseEnhancementConfig enhancementConfig() {
        return this.enhancementConfig;
    }

    public DatabaseAutoStoreTarget autoStoreTarget() {
        return this.autoStoreTarget;
    }

    public DatabaseScope lastScope() {
        return this.query.scope();
    }

    public DatabaseQuery queryFor(DatabaseScope scope) {
        return DatabaseQuery.normalizeForScope(scope, this.query);
    }

    public void setQuery(DatabaseQuery query) {
        this.query = query == null ? DatabaseQuery.defaultQuery() : query;
    }

    public void setQuery(DatabaseScope scope, DatabaseQuery query) {
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        DatabaseQuery normalizedQuery = DatabaseQuery.normalizeForScope(normalizedScope, query);
        if (this.query == null) {
            this.query = normalizedQuery;
            return;
        }
        java.util.LinkedHashMap<DatabaseScopedTabRef, DatabaseTabQueryState> mergedTabStates = new java.util.LinkedHashMap<>();
        for (java.util.Map.Entry<DatabaseScopedTabRef, DatabaseTabQueryState> entry : this.query.tabStates().entrySet()) {
            if (entry.getKey().scope() != normalizedScope) {
                mergedTabStates.put(entry.getKey(), entry.getValue());
            }
        }
        mergedTabStates.putAll(normalizedQuery.tabStates());
        DatabaseQuery preferredQuery = this.query.scope() == normalizedScope ? normalizedQuery : this.query;
        this.query = new DatabaseQuery(preferredQuery.focusedTab(), preferredQuery.visibleTabs(), mergedTabStates);
    }

    public void setLastScope(DatabaseScope scope) {
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        if (this.query == null) {
            this.query = DatabaseQuery.defaultQuery(normalizedScope);
            return;
        }
        DatabaseQuery preferredQuery = this.query.queryForScope(normalizedScope);
        this.query = new DatabaseQuery(preferredQuery.focusedTab(), preferredQuery.visibleTabs(), this.query.tabStates());
    }

    public void setEnhancementConfig(DatabaseEnhancementConfig config) {
        this.enhancementConfig = config == null ? DatabaseEnhancementConfig.defaultConfig() : config;
    }

    public void setAutoStoreTarget(DatabaseAutoStoreTarget target) {
        this.autoStoreTarget = target == null ? DatabaseAutoStoreTarget.defaultTarget() : target;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.put(QUERY_KEY, this.query.toTag());
        tag.put(ENHANCEMENT_CONFIG_KEY, this.enhancementConfig.toTag());
        tag.put(AUTO_STORE_TARGET_KEY, this.autoStoreTarget.toTag());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.query = readQuery(tag);
        this.enhancementConfig = DatabaseEnhancementConfig.fromTag(tag.getCompound(ENHANCEMENT_CONFIG_KEY));
        this.autoStoreTarget = readAutoStoreTarget(tag);
    }

    private static DatabaseQuery readQuery(CompoundTag tag) {
        if (tag.contains(QUERY_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return DatabaseQuery.fromTag(tag.getCompound(QUERY_KEY), DatabaseScope.defaultScope());
        }
        DatabaseScope lastScope = DatabaseScope.read(tag.getString(LEGACY_LAST_SCOPE_KEY), DatabaseScope.defaultScope());
        DatabaseQuery legacyPersonalQuery = DatabaseQuery.fromTag(tag.getCompound(LEGACY_PERSONAL_QUERY_KEY), DatabaseScope.PERSONAL);
        DatabaseQuery legacyPublicQuery = DatabaseQuery.fromTag(tag.getCompound(LEGACY_PUBLIC_QUERY_KEY), DatabaseScope.PUBLIC);
        return mergeLegacyQueries(lastScope, legacyPersonalQuery, legacyPublicQuery);
    }

    private static DatabaseQuery mergeLegacyQueries(
            DatabaseScope lastScope,
            DatabaseQuery personalQuery,
            DatabaseQuery publicQuery
    ) {
        DatabaseQuery normalizedPersonalQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PERSONAL, personalQuery);
        DatabaseQuery normalizedPublicQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PUBLIC, publicQuery);
        DatabaseQuery preferredQuery = lastScope == DatabaseScope.PUBLIC ? normalizedPublicQuery : normalizedPersonalQuery;

        java.util.LinkedHashMap<DatabaseScopedTabRef, DatabaseTabQueryState> mergedTabStates = new java.util.LinkedHashMap<>();
        mergedTabStates.putAll(normalizedPersonalQuery.tabStates());
        mergedTabStates.putAll(normalizedPublicQuery.tabStates());

        java.util.LinkedHashSet<DatabaseScopedTabRef> visibleTabs = new java.util.LinkedHashSet<>();
        if (lastScope == DatabaseScope.PUBLIC) {
            visibleTabs.addAll(normalizedPublicQuery.visibleTabs());
        } else {
            visibleTabs.addAll(normalizedPersonalQuery.visibleTabs());
        }
        if (visibleTabs.isEmpty()) {
            visibleTabs.add(preferredQuery.focusedTab());
        }
        return new DatabaseQuery(preferredQuery.focusedTab(), java.util.List.copyOf(visibleTabs), mergedTabStates);
    }

    private static DatabaseAutoStoreTarget readAutoStoreTarget(CompoundTag tag) {
        if (tag.contains(AUTO_STORE_TARGET_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return DatabaseAutoStoreTarget.fromTag(tag.getCompound(AUTO_STORE_TARGET_KEY));
        }
        return new DatabaseAutoStoreTarget(
                DatabaseScope.PERSONAL,
                tag.getString(LEGACY_AUTO_STORE_TARGET_TAB_ID_KEY)
        );
    }
}
