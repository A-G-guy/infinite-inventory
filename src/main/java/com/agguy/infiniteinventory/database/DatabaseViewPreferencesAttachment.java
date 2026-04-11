package com.agguy.infiniteinventory.database;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class DatabaseViewPreferencesAttachment implements INBTSerializable<CompoundTag> {
    private static final String LAST_SCOPE_KEY = "last_scope";
    private static final String PERSONAL_QUERY_KEY = "personal_query";
    private static final String PUBLIC_QUERY_KEY = "public_query";

    private DatabaseScope lastScope = DatabaseScope.defaultScope();
    private DatabaseQuery personalQuery = DatabaseQuery.defaultQuery(DatabaseScope.PERSONAL);
    private DatabaseQuery publicQuery = DatabaseQuery.defaultQuery(DatabaseScope.PUBLIC);

    public DatabaseScope lastScope() {
        return this.lastScope;
    }

    public DatabaseQuery queryFor(DatabaseScope scope) {
        return DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC ? this.publicQuery : this.personalQuery;
    }

    public void setLastScope(DatabaseScope scope) {
        this.lastScope = DatabaseScope.normalize(scope);
    }

    public void updateQuery(DatabaseQuery query) {
        DatabaseQuery normalizedQuery = query == null
                ? DatabaseQuery.defaultQuery(DatabaseScope.defaultScope())
                : query;
        this.setQuery(normalizedQuery.scope(), normalizedQuery);
        this.lastScope = normalizedQuery.scope();
    }

    public void setQuery(DatabaseScope scope, DatabaseQuery query) {
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        DatabaseQuery normalizedQuery = DatabaseQuery.normalizeForScope(normalizedScope, query);
        if (normalizedScope == DatabaseScope.PUBLIC) {
            this.publicQuery = normalizedQuery;
        } else {
            this.personalQuery = normalizedQuery;
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString(LAST_SCOPE_KEY, this.lastScope.name());
        tag.put(PERSONAL_QUERY_KEY, this.personalQuery.toTag());
        tag.put(PUBLIC_QUERY_KEY, this.publicQuery.toTag());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.lastScope = readScope(tag.getString(LAST_SCOPE_KEY));
        this.personalQuery = DatabaseQuery.fromTag(tag.getCompound(PERSONAL_QUERY_KEY), DatabaseScope.PERSONAL);
        this.publicQuery = DatabaseQuery.fromTag(tag.getCompound(PUBLIC_QUERY_KEY), DatabaseScope.PUBLIC);
    }

    private static DatabaseScope readScope(String serializedScope) {
        try {
            return DatabaseScope.valueOf(serializedScope);
        } catch (IllegalArgumentException exception) {
            return DatabaseScope.defaultScope();
        }
    }
}
