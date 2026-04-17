package com.agguy.infiniteinventory.database;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record DatabaseAutoStoreTarget(DatabaseScope scope, String tabId) {
    private static final String SCOPE_KEY = "scope";
    private static final String TAB_ID_KEY = "tab_id";
    private static final DatabaseAutoStoreTarget DEFAULT = new DatabaseAutoStoreTarget(
            DatabaseScope.PERSONAL,
            DatabaseTabs.DEFAULT_TAB_ID
    );

    public DatabaseAutoStoreTarget {
        scope = DatabaseScope.normalize(scope);
        tabId = DatabaseTabs.normalizeConcreteTarget(tabId);
    }

    public static DatabaseAutoStoreTarget defaultTarget() {
        return DEFAULT;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(SCOPE_KEY, this.scope.name());
        tag.putString(TAB_ID_KEY, this.tabId);
        return tag;
    }

    public static DatabaseAutoStoreTarget fromTag(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return defaultTarget();
        }
        DatabaseScope scope = readScope(tag.getString(SCOPE_KEY));
        return new DatabaseAutoStoreTarget(scope, tag.getString(TAB_ID_KEY));
    }

    public static DatabaseAutoStoreTarget read(FriendlyByteBuf buffer) {
        if (buffer == null) {
            return defaultTarget();
        }
        return new DatabaseAutoStoreTarget(
                buffer.readEnum(DatabaseScope.class),
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH)
        );
    }

    public static void write(FriendlyByteBuf buffer, DatabaseAutoStoreTarget target) {
        DatabaseAutoStoreTarget normalizedTarget = target == null ? defaultTarget() : target;
        buffer.writeEnum(normalizedTarget.scope);
        buffer.writeUtf(normalizedTarget.tabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
    }

    private static DatabaseScope readScope(String serializedScope) {
        try {
            return DatabaseScope.valueOf(serializedScope);
        } catch (IllegalArgumentException exception) {
            return DatabaseScope.PERSONAL;
        }
    }
}
