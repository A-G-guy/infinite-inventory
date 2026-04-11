package com.agguy.infiniteinventory.database;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record DatabaseSearchConfig(
        DatabaseSearchWeight displayNameWeight,
        DatabaseSearchWeight itemIdWeight,
        DatabaseSearchWeight pinyinWeight,
        DatabaseSearchWeight modNamespaceWeight,
        DatabaseSearchWeight countBoostWeight
) {
    private static final String DISPLAY_NAME_KEY = "display_name";
    private static final String ITEM_ID_KEY = "item_id";
    private static final String PINYIN_KEY = "pinyin";
    private static final String MOD_NAMESPACE_KEY = "mod_namespace";
    private static final String COUNT_BOOST_KEY = "count_boost";

    public DatabaseSearchConfig {
        displayNameWeight = normalizeWeight(displayNameWeight, DatabaseSearchField.DISPLAY_NAME);
        itemIdWeight = normalizeWeight(itemIdWeight, DatabaseSearchField.ITEM_ID);
        pinyinWeight = normalizeWeight(pinyinWeight, DatabaseSearchField.PINYIN);
        modNamespaceWeight = normalizeWeight(modNamespaceWeight, DatabaseSearchField.MOD_NAMESPACE);
        countBoostWeight = normalizeWeight(countBoostWeight, DatabaseSearchField.COUNT_BOOST);
        if (allTextFieldsDisabled(displayNameWeight, itemIdWeight, pinyinWeight, modNamespaceWeight)) {
            displayNameWeight = DatabaseSearchField.DISPLAY_NAME.defaultWeight();
        }
    }

    public static DatabaseSearchConfig defaultConfig() {
        return new DatabaseSearchConfig(
                DatabaseSearchField.DISPLAY_NAME.defaultWeight(),
                DatabaseSearchField.ITEM_ID.defaultWeight(),
                DatabaseSearchField.PINYIN.defaultWeight(),
                DatabaseSearchField.MOD_NAMESPACE.defaultWeight(),
                DatabaseSearchField.COUNT_BOOST.defaultWeight()
        );
    }

    public DatabaseSearchWeight weightFor(DatabaseSearchField field) {
        return switch (field) {
            case DISPLAY_NAME -> this.displayNameWeight;
            case ITEM_ID -> this.itemIdWeight;
            case PINYIN -> this.pinyinWeight;
            case MOD_NAMESPACE -> this.modNamespaceWeight;
            case COUNT_BOOST -> this.countBoostWeight;
        };
    }

    public DatabaseSearchConfig withWeight(DatabaseSearchField field, DatabaseSearchWeight newWeight) {
        DatabaseSearchWeight normalizedWeight = normalizeWeight(newWeight, field);
        return switch (field) {
            case DISPLAY_NAME -> new DatabaseSearchConfig(normalizedWeight, this.itemIdWeight, this.pinyinWeight, this.modNamespaceWeight, this.countBoostWeight);
            case ITEM_ID -> new DatabaseSearchConfig(this.displayNameWeight, normalizedWeight, this.pinyinWeight, this.modNamespaceWeight, this.countBoostWeight);
            case PINYIN -> new DatabaseSearchConfig(this.displayNameWeight, this.itemIdWeight, normalizedWeight, this.modNamespaceWeight, this.countBoostWeight);
            case MOD_NAMESPACE -> new DatabaseSearchConfig(this.displayNameWeight, this.itemIdWeight, this.pinyinWeight, normalizedWeight, this.countBoostWeight);
            case COUNT_BOOST -> new DatabaseSearchConfig(this.displayNameWeight, this.itemIdWeight, this.pinyinWeight, this.modNamespaceWeight, normalizedWeight);
        };
    }

    public boolean hasEnabledTextField() {
        return this.displayNameWeight != DatabaseSearchWeight.OFF
                || this.itemIdWeight != DatabaseSearchWeight.OFF
                || this.pinyinWeight != DatabaseSearchWeight.OFF
                || this.modNamespaceWeight != DatabaseSearchWeight.OFF;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(DISPLAY_NAME_KEY, this.displayNameWeight.name());
        tag.putString(ITEM_ID_KEY, this.itemIdWeight.name());
        tag.putString(PINYIN_KEY, this.pinyinWeight.name());
        tag.putString(MOD_NAMESPACE_KEY, this.modNamespaceWeight.name());
        tag.putString(COUNT_BOOST_KEY, this.countBoostWeight.name());
        return tag;
    }

    public static DatabaseSearchConfig fromTag(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return defaultConfig();
        }
        return new DatabaseSearchConfig(
                readWeight(tag.getString(DISPLAY_NAME_KEY), DatabaseSearchField.DISPLAY_NAME.defaultWeight()),
                readWeight(tag.getString(ITEM_ID_KEY), DatabaseSearchField.ITEM_ID.defaultWeight()),
                readWeight(tag.getString(PINYIN_KEY), DatabaseSearchField.PINYIN.defaultWeight()),
                readWeight(tag.getString(MOD_NAMESPACE_KEY), DatabaseSearchField.MOD_NAMESPACE.defaultWeight()),
                readWeight(tag.getString(COUNT_BOOST_KEY), DatabaseSearchField.COUNT_BOOST.defaultWeight())
        );
    }

    public static DatabaseSearchConfig read(FriendlyByteBuf buffer) {
        return new DatabaseSearchConfig(
                buffer.readEnum(DatabaseSearchWeight.class),
                buffer.readEnum(DatabaseSearchWeight.class),
                buffer.readEnum(DatabaseSearchWeight.class),
                buffer.readEnum(DatabaseSearchWeight.class),
                buffer.readEnum(DatabaseSearchWeight.class)
        );
    }

    public static void write(FriendlyByteBuf buffer, DatabaseSearchConfig config) {
        DatabaseSearchConfig normalizedConfig = config == null ? defaultConfig() : config;
        buffer.writeEnum(normalizedConfig.displayNameWeight());
        buffer.writeEnum(normalizedConfig.itemIdWeight());
        buffer.writeEnum(normalizedConfig.pinyinWeight());
        buffer.writeEnum(normalizedConfig.modNamespaceWeight());
        buffer.writeEnum(normalizedConfig.countBoostWeight());
    }

    private static DatabaseSearchWeight normalizeWeight(DatabaseSearchWeight weight, DatabaseSearchField field) {
        return weight == null ? field.defaultWeight() : weight;
    }

    private static boolean allTextFieldsDisabled(
            DatabaseSearchWeight displayNameWeight,
            DatabaseSearchWeight itemIdWeight,
            DatabaseSearchWeight pinyinWeight,
            DatabaseSearchWeight modNamespaceWeight
    ) {
        return displayNameWeight == DatabaseSearchWeight.OFF
                && itemIdWeight == DatabaseSearchWeight.OFF
                && pinyinWeight == DatabaseSearchWeight.OFF
                && modNamespaceWeight == DatabaseSearchWeight.OFF;
    }

    private static DatabaseSearchWeight readWeight(String serializedWeight, DatabaseSearchWeight fallbackValue) {
        try {
            return DatabaseSearchWeight.valueOf(serializedWeight);
        } catch (IllegalArgumentException exception) {
            return fallbackValue;
        }
    }
}
