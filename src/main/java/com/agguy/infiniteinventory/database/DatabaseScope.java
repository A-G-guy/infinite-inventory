package com.agguy.infiniteinventory.database;

public enum DatabaseScope {
    PERSONAL(
            "screen.infiniteinventory.scope.personal",
            "screen.infiniteinventory.database.section.personal",
            "screen.infiniteinventory.empty.personal"
    ),
    PUBLIC(
            "screen.infiniteinventory.scope.public",
            "screen.infiniteinventory.database.section.public",
            "screen.infiniteinventory.empty.public"
    );

    private final String translationKey;
    private final String sectionTranslationKey;
    private final String emptyTranslationKey;

    DatabaseScope(String translationKey, String sectionTranslationKey, String emptyTranslationKey) {
        this.translationKey = translationKey;
        this.sectionTranslationKey = sectionTranslationKey;
        this.emptyTranslationKey = emptyTranslationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public String sectionTranslationKey() {
        return this.sectionTranslationKey;
    }

    public String emptyTranslationKey() {
        return this.emptyTranslationKey;
    }

    public static DatabaseScope defaultScope() {
        return PERSONAL;
    }

    public static DatabaseScope normalize(DatabaseScope scope) {
        return scope == null ? defaultScope() : scope;
    }
}
