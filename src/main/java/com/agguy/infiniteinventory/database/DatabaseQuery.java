package com.agguy.infiniteinventory.database;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record DatabaseQuery(
        DatabaseScope scope,
        DatabaseCategory category,
        DatabaseSortOption sortOption,
        String searchText,
        DatabaseSearchConfig searchConfig,
        int pageIndex,
        int pageSize
) {
    private static final String SCOPE_KEY = "scope";
    private static final String CATEGORY_KEY = "category";
    private static final String SORT_OPTION_KEY = "sort_option";
    private static final String SEARCH_TEXT_KEY = "search_text";
    private static final String SEARCH_CONFIG_KEY = "search_config";
    private static final String PAGE_INDEX_KEY = "page_index";
    private static final String PAGE_SIZE_KEY = "page_size";

    public static final int MAX_SEARCH_LENGTH = 64;
    public static final int DEFAULT_PAGE_SIZE = 54;
    public static final int MAX_PAGE_SIZE = 640;

    public DatabaseQuery {
        scope = DatabaseScope.normalize(scope);
        category = category == null ? DatabaseCategory.ALL : category;
        sortOption = sortOption == null ? DatabaseSortOption.RECENTLY_CHANGED : sortOption;
        searchText = normalize(searchText);
        searchConfig = searchConfig == null ? DatabaseSearchConfig.defaultConfig() : searchConfig;
        pageIndex = Math.max(0, pageIndex);
        pageSize = normalizePageSize(pageSize);
    }

    public DatabaseQuery(
            DatabaseScope scope,
            DatabaseCategory category,
            DatabaseSortOption sortOption,
            String searchText,
            int pageIndex,
            int pageSize
    ) {
        this(scope, category, sortOption, searchText, DatabaseSearchConfig.defaultConfig(), pageIndex, pageSize);
    }

    public static DatabaseQuery defaultQuery() {
        return defaultQuery(DatabaseScope.defaultScope());
    }

    public static DatabaseQuery defaultQuery(DatabaseScope scope) {
        return new DatabaseQuery(scope, DatabaseCategory.ALL, DatabaseSortOption.RECENTLY_CHANGED, "", DatabaseSearchConfig.defaultConfig(), 0, DEFAULT_PAGE_SIZE);
    }

    public static DatabaseQuery normalizeForScope(DatabaseScope scope, DatabaseQuery query) {
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        if (query == null) {
            return defaultQuery(normalizedScope);
        }
        return query.scope() == normalizedScope ? query : query.withScope(normalizedScope);
    }

    public DatabaseQuery withCategory(DatabaseCategory newCategory) {
        return new DatabaseQuery(this.scope, newCategory, this.sortOption, this.searchText, this.searchConfig, 0, this.pageSize);
    }

    public DatabaseQuery withSortOption(DatabaseSortOption newSortOption) {
        return new DatabaseQuery(this.scope, this.category, newSortOption, this.searchText, this.searchConfig, this.pageIndex, this.pageSize);
    }

    public DatabaseQuery withSearchText(String newSearchText) {
        return new DatabaseQuery(this.scope, this.category, this.sortOption, newSearchText, this.searchConfig, 0, this.pageSize);
    }

    public DatabaseQuery withSearchConfig(DatabaseSearchConfig newSearchConfig) {
        return new DatabaseQuery(this.scope, this.category, this.sortOption, this.searchText, newSearchConfig, 0, this.pageSize);
    }

    public DatabaseQuery withPageIndex(int newPageIndex) {
        return new DatabaseQuery(this.scope, this.category, this.sortOption, this.searchText, this.searchConfig, newPageIndex, this.pageSize);
    }

    public DatabaseQuery withPageSize(int newPageSize) {
        return new DatabaseQuery(this.scope, this.category, this.sortOption, this.searchText, this.searchConfig, this.pageIndex, newPageSize);
    }

    public DatabaseQuery withScope(DatabaseScope newScope) {
        return new DatabaseQuery(newScope, this.category, this.sortOption, this.searchText, this.searchConfig, this.pageIndex, this.pageSize);
    }

    public static DatabaseQuery read(FriendlyByteBuf buffer) {
        return new DatabaseQuery(
                buffer.readEnum(DatabaseScope.class),
                buffer.readEnum(DatabaseCategory.class),
                buffer.readEnum(DatabaseSortOption.class),
                buffer.readUtf(MAX_SEARCH_LENGTH),
                DatabaseSearchConfig.read(buffer),
                buffer.readVarInt(),
                buffer.readVarInt()
        );
    }

    public static void write(FriendlyByteBuf buffer, DatabaseQuery query) {
        buffer.writeEnum(query.scope());
        buffer.writeEnum(query.category());
        buffer.writeEnum(query.sortOption());
        buffer.writeUtf(query.searchText(), MAX_SEARCH_LENGTH);
        DatabaseSearchConfig.write(buffer, query.searchConfig());
        buffer.writeVarInt(query.pageIndex());
        buffer.writeVarInt(query.pageSize());
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(SCOPE_KEY, this.scope.name());
        tag.putString(CATEGORY_KEY, this.category.name());
        tag.putString(SORT_OPTION_KEY, this.sortOption.name());
        tag.putString(SEARCH_TEXT_KEY, this.searchText);
        tag.put(SEARCH_CONFIG_KEY, this.searchConfig.toTag());
        tag.putInt(PAGE_INDEX_KEY, this.pageIndex);
        tag.putInt(PAGE_SIZE_KEY, this.pageSize);
        return tag;
    }

    public static DatabaseQuery fromTag(CompoundTag tag, DatabaseScope fallbackScope) {
        if (tag == null || tag.isEmpty()) {
            return defaultQuery(fallbackScope);
        }
        DatabaseScope scope = readEnum(tag.getString(SCOPE_KEY), DatabaseScope.class, DatabaseScope.normalize(fallbackScope));
        DatabaseCategory category = readEnum(tag.getString(CATEGORY_KEY), DatabaseCategory.class, DatabaseCategory.ALL);
        DatabaseSortOption sortOption = readEnum(tag.getString(SORT_OPTION_KEY), DatabaseSortOption.class, DatabaseSortOption.RECENTLY_CHANGED);
        return new DatabaseQuery(
                scope,
                category,
                sortOption,
                tag.getString(SEARCH_TEXT_KEY),
                DatabaseSearchConfig.fromTag(tag.getCompound(SEARCH_CONFIG_KEY)),
                tag.getInt(PAGE_INDEX_KEY),
                tag.getInt(PAGE_SIZE_KEY)
        );
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= MAX_SEARCH_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_SEARCH_LENGTH);
    }

    private static int normalizePageSize(int pageSize) {
        return Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
    }

    private static <T extends Enum<T>> T readEnum(String serializedName, Class<T> enumType, T fallbackValue) {
        try {
            return Enum.valueOf(enumType, serializedName);
        } catch (IllegalArgumentException exception) {
            return fallbackValue;
        }
    }
}
