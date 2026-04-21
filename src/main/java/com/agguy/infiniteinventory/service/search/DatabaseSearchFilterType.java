package com.agguy.infiniteinventory.service.search;

public enum DatabaseSearchFilterType {
    MOD('@'),
    TAG('#'),
    ITEM_ID('&'),
    CREATIVE_TAB('%');

    private final char prefix;

    DatabaseSearchFilterType(char prefix) {
        this.prefix = prefix;
    }

    public char prefix() {
        return this.prefix;
    }

    public static DatabaseSearchFilterType fromPrefix(char prefix) {
        for (DatabaseSearchFilterType type : values()) {
            if (type.prefix == prefix) {
                return type;
            }
        }
        return null;
    }
}
