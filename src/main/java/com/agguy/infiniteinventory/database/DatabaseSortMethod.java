package com.agguy.infiniteinventory.database;

import java.util.List;

public enum DatabaseSortMethod {
    RECENTLY_CHANGED(
            "screen.infiniteinventory.sort.field.recently_changed",
            "screen.infiniteinventory.sort.button.recently_changed"
    ),
    RECENTLY_ADDED(
            "screen.infiniteinventory.sort.field.recently_added",
            "screen.infiniteinventory.sort.button.recently_added"
    ),
    NAME("screen.infiniteinventory.sort.field.name", "screen.infiniteinventory.sort.button.name"),
    COUNT("screen.infiniteinventory.sort.field.count", "screen.infiniteinventory.sort.button.count"),
    MOD_NAMESPACE("screen.infiniteinventory.sort.field.mod_namespace", "screen.infiniteinventory.sort.button.mod_namespace"),
    ITEM_ID("screen.infiniteinventory.sort.field.item_id", "screen.infiniteinventory.sort.button.item_id");

    private static final List<DatabaseSortMethod> ORDERED_VALUES = List.of(values());

    private final String translationKey;
    private final String buttonTranslationKey;

    DatabaseSortMethod(String translationKey, String buttonTranslationKey) {
        this.translationKey = translationKey;
        this.buttonTranslationKey = buttonTranslationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public String buttonTranslationKey() {
        return this.buttonTranslationKey;
    }

    public static List<DatabaseSortMethod> orderedValues() {
        return ORDERED_VALUES;
    }
}
