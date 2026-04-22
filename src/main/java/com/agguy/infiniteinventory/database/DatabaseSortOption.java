package com.agguy.infiniteinventory.database;

import java.util.List;

public enum DatabaseSortOption {
    RECENTLY_CHANGED("screen.infiniteinventory.sort.recent", DatabaseSortMethod.RECENTLY_CHANGED, DatabaseSortDirection.DESC),
    RECENTLY_ADDED("screen.infiniteinventory.sort.recently_added", DatabaseSortMethod.RECENTLY_ADDED, DatabaseSortDirection.DESC),
    NAME_ASC("screen.infiniteinventory.sort.name_asc", DatabaseSortMethod.NAME, DatabaseSortDirection.ASC),
    NAME_DESC("screen.infiniteinventory.sort.name_desc", DatabaseSortMethod.NAME, DatabaseSortDirection.DESC),
    COUNT_DESC("screen.infiniteinventory.sort.count_desc", DatabaseSortMethod.COUNT, DatabaseSortDirection.DESC),
    COUNT_ASC("screen.infiniteinventory.sort.count_asc", DatabaseSortMethod.COUNT, DatabaseSortDirection.ASC),
    MOD_NAMESPACE_ASC("screen.infiniteinventory.sort.mod_namespace_asc", DatabaseSortMethod.MOD_NAMESPACE, DatabaseSortDirection.ASC),
    MOD_NAMESPACE_DESC("screen.infiniteinventory.sort.mod_namespace_desc", DatabaseSortMethod.MOD_NAMESPACE, DatabaseSortDirection.DESC),
    ITEM_ID_ASC("screen.infiniteinventory.sort.item_id_asc", DatabaseSortMethod.ITEM_ID, DatabaseSortDirection.ASC),
    ITEM_ID_DESC("screen.infiniteinventory.sort.item_id_desc", DatabaseSortMethod.ITEM_ID, DatabaseSortDirection.DESC),
    STARRED_DESC("screen.infiniteinventory.sort.starred_desc", DatabaseSortMethod.STARRED, DatabaseSortDirection.DESC),
    STARRED_ASC("screen.infiniteinventory.sort.starred_asc", DatabaseSortMethod.STARRED, DatabaseSortDirection.ASC),
    RECENTLY_CHANGED_ASC(
            "screen.infiniteinventory.sort.recent_asc",
            DatabaseSortMethod.RECENTLY_CHANGED,
            DatabaseSortDirection.ASC
    ),
    RECENTLY_ADDED_ASC(
            "screen.infiniteinventory.sort.recently_added_asc",
            DatabaseSortMethod.RECENTLY_ADDED,
            DatabaseSortDirection.ASC
    );

    private static final List<DatabaseSortOption> ORDERED_VALUES = List.of(values());

    private final String translationKey;
    private final DatabaseSortMethod method;
    private final DatabaseSortDirection direction;

    DatabaseSortOption(String translationKey, DatabaseSortMethod method, DatabaseSortDirection direction) {
        this.translationKey = translationKey;
        this.method = method;
        this.direction = direction;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public DatabaseSortMethod method() {
        return this.method;
    }

    public DatabaseSortDirection direction() {
        return this.direction;
    }

    public static List<DatabaseSortOption> orderedValues() {
        return ORDERED_VALUES;
    }

    public static DatabaseSortOption of(DatabaseSortMethod method, DatabaseSortDirection direction) {
        DatabaseSortMethod resolvedMethod = method == null ? DatabaseSortMethod.RECENTLY_CHANGED : method;
        DatabaseSortDirection resolvedDirection = direction == null ? DatabaseSortDirection.DESC : direction;
        return switch (resolvedMethod) {
            case RECENTLY_CHANGED -> resolvedDirection == DatabaseSortDirection.ASC ? RECENTLY_CHANGED_ASC : RECENTLY_CHANGED;
            case RECENTLY_ADDED -> resolvedDirection == DatabaseSortDirection.ASC ? RECENTLY_ADDED_ASC : RECENTLY_ADDED;
            case NAME -> resolvedDirection == DatabaseSortDirection.ASC ? NAME_ASC : NAME_DESC;
            case COUNT -> resolvedDirection == DatabaseSortDirection.ASC ? COUNT_ASC : COUNT_DESC;
            case MOD_NAMESPACE -> resolvedDirection == DatabaseSortDirection.ASC ? MOD_NAMESPACE_ASC : MOD_NAMESPACE_DESC;
            case ITEM_ID -> resolvedDirection == DatabaseSortDirection.ASC ? ITEM_ID_ASC : ITEM_ID_DESC;
            case STARRED -> resolvedDirection == DatabaseSortDirection.ASC ? STARRED_ASC : STARRED_DESC;
        };
    }
}
