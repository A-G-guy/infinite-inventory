package com.agguy.infiniteinventory.database;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record DatabaseTabQueryState(
        DatabaseSortOption sortOption,
        String searchText,
        DatabaseSearchConfig searchConfig,
        int pageIndex,
        int pageSize
) {
    private static final String SORT_OPTION_KEY = "sort_option";
    private static final String SEARCH_TEXT_KEY = "search_text";
    private static final String SEARCH_CONFIG_KEY = "search_config";
    private static final String PAGE_INDEX_KEY = "page_index";
    private static final String PAGE_SIZE_KEY = "page_size";

    public DatabaseTabQueryState {
        sortOption = sortOption == null ? DatabaseSortOption.RECENTLY_CHANGED : sortOption;
        searchText = normalizeSearchText(searchText);
        searchConfig = searchConfig == null ? DatabaseSearchConfig.defaultConfig() : searchConfig;
        pageIndex = Math.max(0, pageIndex);
        pageSize = normalizePageSize(pageSize);
    }

    public static DatabaseTabQueryState defaultState() {
        return new DatabaseTabQueryState(
                DatabaseSortOption.RECENTLY_CHANGED,
                "",
                DatabaseSearchConfig.defaultConfig(),
                0,
                DatabaseQuery.DEFAULT_PAGE_SIZE
        );
    }

    public DatabaseTabQueryState withSortOption(DatabaseSortOption newSortOption) {
        return new DatabaseTabQueryState(
                newSortOption,
                this.searchText,
                this.searchConfig,
                this.pageIndex,
                this.pageSize
        );
    }

    public DatabaseTabQueryState withSearchText(String newSearchText) {
        return new DatabaseTabQueryState(
                this.sortOption,
                newSearchText,
                this.searchConfig,
                0,
                this.pageSize
        );
    }

    public DatabaseTabQueryState withSearchConfig(DatabaseSearchConfig newSearchConfig) {
        return new DatabaseTabQueryState(
                this.sortOption,
                this.searchText,
                newSearchConfig,
                0,
                this.pageSize
        );
    }

    public DatabaseTabQueryState withPageIndex(int newPageIndex) {
        return new DatabaseTabQueryState(
                this.sortOption,
                this.searchText,
                this.searchConfig,
                Math.max(0, newPageIndex),
                this.pageSize
        );
    }

    public DatabaseTabQueryState withPageSize(int newPageSize) {
        return new DatabaseTabQueryState(
                this.sortOption,
                this.searchText,
                this.searchConfig,
                this.pageIndex,
                newPageSize
        );
    }

    public static DatabaseTabQueryState read(FriendlyByteBuf buffer) {
        return new DatabaseTabQueryState(
                buffer.readEnum(DatabaseSortOption.class),
                buffer.readUtf(DatabaseQuery.MAX_SEARCH_LENGTH),
                DatabaseSearchConfig.read(buffer),
                buffer.readVarInt(),
                buffer.readVarInt()
        );
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.sortOption);
        buffer.writeUtf(this.searchText, DatabaseQuery.MAX_SEARCH_LENGTH);
        DatabaseSearchConfig.write(buffer, this.searchConfig);
        buffer.writeVarInt(this.pageIndex);
        buffer.writeVarInt(this.pageSize);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(SORT_OPTION_KEY, this.sortOption.name());
        tag.putString(SEARCH_TEXT_KEY, this.searchText);
        tag.put(PAGE_INDEX_KEY, net.minecraft.nbt.IntTag.valueOf(this.pageIndex));
        tag.put(PAGE_SIZE_KEY, net.minecraft.nbt.IntTag.valueOf(this.pageSize));
        tag.put(SEARCH_CONFIG_KEY, this.searchConfig.toTag());
        return tag;
    }

    public static DatabaseTabQueryState fromTag(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return defaultState();
        }
        return new DatabaseTabQueryState(
                readEnum(tag.getString(SORT_OPTION_KEY), DatabaseSortOption.class, DatabaseSortOption.RECENTLY_CHANGED),
                tag.getString(SEARCH_TEXT_KEY),
                DatabaseSearchConfig.fromTag(tag.getCompound(SEARCH_CONFIG_KEY)),
                Math.max(0, tag.getInt(PAGE_INDEX_KEY)),
                normalizePageSize(tag.getInt(PAGE_SIZE_KEY))
        );
    }

    private static String normalizeSearchText(String searchText) {
        if (searchText == null) {
            return "";
        }
        String trimmed = searchText.trim();
        if (trimmed.length() <= DatabaseQuery.MAX_SEARCH_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, DatabaseQuery.MAX_SEARCH_LENGTH);
    }

    private static int normalizePageSize(int pageSize) {
        return Math.min(DatabaseQuery.MAX_PAGE_SIZE, Math.max(1, pageSize));
    }

    private static <E extends Enum<E>> E readEnum(String serializedValue, Class<E> enumType, E fallback) {
        if (serializedValue == null || serializedValue.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumType, serializedValue);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
