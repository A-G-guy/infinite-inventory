package com.agguy.infiniteinventory.database;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record DatabaseScopedTabRef(DatabaseScope scope, String tabId) {
    private static final String SCOPE_KEY = "scope";
    private static final String TAB_ID_KEY = "tab_id";

    public DatabaseScopedTabRef {
        scope = DatabaseScope.normalize(scope);
        tabId = DatabaseTabs.isAllTabId(tabId)
                ? DatabaseTabs.ALL_TAB_ID
                : DatabaseTabs.normalizeConcreteTarget(tabId);
    }

    public static DatabaseScopedTabRef defaultTab() {
        return allTab(DatabaseScope.defaultScope());
    }

    public static DatabaseScopedTabRef allTab(DatabaseScope scope) {
        return new DatabaseScopedTabRef(scope, DatabaseTabs.ALL_TAB_ID);
    }

    public static DatabaseScopedTabRef concreteTab(DatabaseScope scope, String tabId) {
        return new DatabaseScopedTabRef(scope, tabId);
    }

    public boolean isAllTab() {
        return DatabaseTabs.isAllTabId(this.tabId);
    }

    public boolean isConcreteTab() {
        return !this.isAllTab();
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
