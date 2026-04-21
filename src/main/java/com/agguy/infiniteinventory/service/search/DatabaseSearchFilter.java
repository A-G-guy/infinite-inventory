package com.agguy.infiniteinventory.service.search;

public record DatabaseSearchFilter(
        DatabaseSearchFilterType type,
        String value
) {
    public DatabaseSearchFilter {
        type = type == null ? DatabaseSearchFilterType.MOD : type;
        value = value == null ? "" : value.trim();
    }

    public boolean isMeaningful() {
        return switch (this.type) {
            case MOD, CREATIVE_TAB -> !SearchTextNormalizer.compactNaturalText(this.value).isEmpty()
                    || !SearchTextNormalizer.compactIdentifierText(this.value).isEmpty();
            case TAG, ITEM_ID -> !SearchTextNormalizer.compactIdentifierText(this.value).isEmpty();
        };
    }

    public String canonicalToken() {
        String normalizedValue = switch (this.type) {
            case MOD, CREATIVE_TAB -> canonicalNaturalValue(this.value);
            case TAG, ITEM_ID -> canonicalIdentifierValue(this.value);
        };
        if (normalizedValue.isEmpty()) {
            return "";
        }
        if (normalizedValue.indexOf(' ') >= 0) {
            return this.type.prefix() + "\"" + normalizedValue + "\"";
        }
        return this.type.prefix() + normalizedValue;
    }

    private static String canonicalNaturalValue(String value) {
        return SearchTextNormalizer.normalizeQueryText(value);
    }

    private static String canonicalIdentifierValue(String value) {
        return SearchTextNormalizer.normalizeIdentifierText(value);
    }
}
