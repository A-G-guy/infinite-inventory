package com.agguy.infiniteinventory.database;

public enum DatabaseSortDirection {
    ASC("screen.infiniteinventory.sort.direction.asc"),
    DESC("screen.infiniteinventory.sort.direction.desc");

    private final String translationKey;

    DatabaseSortDirection(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }
}
