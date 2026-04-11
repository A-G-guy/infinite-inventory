package com.agguy.infiniteinventory.database;

import net.minecraft.network.FriendlyByteBuf;

public record DatabaseQuery(DatabaseCategory category, DatabaseSortOption sortOption, String searchText, int pageIndex) {
    public static final int MAX_SEARCH_LENGTH = 64;

    public DatabaseQuery {
        category = category == null ? DatabaseCategory.ALL : category;
        sortOption = sortOption == null ? DatabaseSortOption.RECENTLY_CHANGED : sortOption;
        searchText = normalize(searchText);
        pageIndex = Math.max(0, pageIndex);
    }

    public static DatabaseQuery defaultQuery() {
        return new DatabaseQuery(DatabaseCategory.ALL, DatabaseSortOption.RECENTLY_CHANGED, "", 0);
    }

    public DatabaseQuery withCategory(DatabaseCategory newCategory) {
        return new DatabaseQuery(newCategory, this.sortOption, this.searchText, 0);
    }

    public DatabaseQuery withSortOption(DatabaseSortOption newSortOption) {
        return new DatabaseQuery(this.category, newSortOption, this.searchText, this.pageIndex);
    }

    public DatabaseQuery withSearchText(String newSearchText) {
        return new DatabaseQuery(this.category, this.sortOption, newSearchText, 0);
    }

    public DatabaseQuery withPageIndex(int newPageIndex) {
        return new DatabaseQuery(this.category, this.sortOption, this.searchText, newPageIndex);
    }

    public static DatabaseQuery read(FriendlyByteBuf buffer) {
        return new DatabaseQuery(
                buffer.readEnum(DatabaseCategory.class),
                buffer.readEnum(DatabaseSortOption.class),
                buffer.readUtf(MAX_SEARCH_LENGTH),
                buffer.readVarInt()
        );
    }

    public static void write(FriendlyByteBuf buffer, DatabaseQuery query) {
        buffer.writeEnum(query.category());
        buffer.writeEnum(query.sortOption());
        buffer.writeUtf(query.searchText(), MAX_SEARCH_LENGTH);
        buffer.writeVarInt(query.pageIndex());
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
}
