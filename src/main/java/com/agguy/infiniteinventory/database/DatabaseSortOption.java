package com.agguy.infiniteinventory.database;

public enum DatabaseSortOption {
    RECENTLY_CHANGED("screen.infiniteinventory.sort.recent"),
    NAME_ASC("screen.infiniteinventory.sort.name_asc"),
    NAME_DESC("screen.infiniteinventory.sort.name_desc"),
    COUNT_DESC("screen.infiniteinventory.sort.count_desc"),
    COUNT_ASC("screen.infiniteinventory.sort.count_asc");

    private final String translationKey;

    DatabaseSortOption(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }
}
