package com.agguy.infiniteinventory.database;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record DatabaseScopedTabRef(DatabaseScope scope, String tabId) {
    private static final String SCOPE_KEY = "scope";
    private static final String TAB_ID_KEY = "tab_id";

    public DatabaseScopedTabRef {
        scope = DatabaseScope.normalize(scope);
        if (DatabaseTabs.isAllTabId(tabId)) {
            tabId = DatabaseTabs.ALL_TAB_ID;
        } else if (DatabaseTabs.isFavoritesTabId(tabId)) {
            tabId = DatabaseTabs.FAVORITES_TAB_ID;
        } else {
            tabId = DatabaseTabs.normalizeConcreteTarget(tabId);
        }
    }

    public static DatabaseScopedTabRef defaultTab() {
        return allTab(DatabaseScope.defaultScope());
    }

    public static DatabaseScopedTabRef allTab(DatabaseScope scope) {
        return new DatabaseScopedTabRef(scope, DatabaseTabs.ALL_TAB_ID);
    }

    public static DatabaseScopedTabRef favoritesTab(DatabaseScope scope) {
        return new DatabaseScopedTabRef(scope, DatabaseTabs.FAVORITES_TAB_ID);
    }

    public static DatabaseScopedTabRef concreteTab(DatabaseScope scope, String tabId) {
        return new DatabaseScopedTabRef(scope, tabId);
    }

    public boolean isAllTab() {
        return DatabaseTabs.isAllTabId(this.tabId);
    }

    public boolean isFavoritesTab() {
        return DatabaseTabs.isFavoritesTabId(this.tabId);
    }

    public boolean isSystemTab() {
        return DatabaseTabs.isSystemTabId(this.tabId);
    }

    public boolean isConcreteTab() {
        return !this.isSystemTab();
    }

    public DatabaseScopedTabRef withScope(DatabaseScope nextScope) {
        return new DatabaseScopedTabRef(nextScope, this.tabId);
    }

    public DatabaseScopedTabRef withTabId(String nextTabId) {
        return new DatabaseScopedTabRef(this.scope, nextTabId);
    }

    public static DatabaseScopedTabRef read(FriendlyByteBuf buffer) {
        return new DatabaseScopedTabRef(
                buffer.readEnum(DatabaseScope.class),
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH)
        );
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.scope);
        buffer.writeUtf(this.tabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(SCOPE_KEY, this.scope.name());
        tag.putString(TAB_ID_KEY, this.tabId);
        return tag;
    }

    public static DatabaseScopedTabRef fromTag(CompoundTag tag, DatabaseScope fallbackScope) {
        if (tag == null || tag.isEmpty()) {
            return allTab(fallbackScope);
        }
        return new DatabaseScopedTabRef(
                DatabaseScope.read(tag.getString(SCOPE_KEY), fallbackScope),
                tag.getString(TAB_ID_KEY)
        );
    }
}
