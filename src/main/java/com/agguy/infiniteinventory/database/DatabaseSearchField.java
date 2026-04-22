package com.agguy.infiniteinventory.database;

public enum DatabaseSearchField {
    DISPLAY_NAME("screen.infiniteinventory.search_field.display_name", true, DatabaseSearchWeight.HIGH),
    ITEM_ID("screen.infiniteinventory.search_field.item_id", true, DatabaseSearchWeight.MEDIUM),
    PINYIN("screen.infiniteinventory.search_field.pinyin", true, DatabaseSearchWeight.HIGH),
    MOD_NAMESPACE("screen.infiniteinventory.search_field.mod_namespace", true, DatabaseSearchWeight.LOW),
    NOTE("screen.infiniteinventory.search_field.note", true, DatabaseSearchWeight.MEDIUM),
    COUNT_BOOST("screen.infiniteinventory.search_field.count_boost", false, DatabaseSearchWeight.LOW);

    private final String translationKey;
    private final boolean textField;
    private final DatabaseSearchWeight defaultWeight;

    DatabaseSearchField(String translationKey, boolean textField, DatabaseSearchWeight defaultWeight) {
        this.translationKey = translationKey;
        this.textField = textField;
        this.defaultWeight = defaultWeight;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public boolean isTextField() {
        return this.textField;
    }

    public DatabaseSearchWeight defaultWeight() {
        return this.defaultWeight;
    }
}
