package com.agguy.infiniteinventory.database;

import java.util.List;

public enum DatabaseSortOption {
    RECENTLY_CHANGED("screen.infiniteinventory.sort.recent"),
    RECENTLY_ADDED("screen.infiniteinventory.sort.recently_added"),
    NAME_ASC("screen.infiniteinventory.sort.name_asc"),
    NAME_DESC("screen.infiniteinventory.sort.name_desc"),
    COUNT_DESC("screen.infiniteinventory.sort.count_desc"),
    COUNT_ASC("screen.infiniteinventory.sort.count_asc"),
    MOD_NAMESPACE_ASC("screen.infiniteinventory.sort.mod_namespace_asc"),
    MOD_NAMESPACE_DESC("screen.infiniteinventory.sort.mod_namespace_desc"),
    ITEM_ID_ASC("screen.infiniteinventory.sort.item_id_asc"),
    ITEM_ID_DESC("screen.infiniteinventory.sort.item_id_desc");

    private static final List<DatabaseSortOption> ORDERED_VALUES = List.of(values());

    private final String translationKey;

    DatabaseSortOption(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public static List<DatabaseSortOption> orderedValues() {
        return ORDERED_VALUES;
    }
}
