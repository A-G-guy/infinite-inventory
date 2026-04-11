package com.agguy.infiniteinventory.database;

public final class DatabasePagination {
    private DatabasePagination() {
    }

    public static int resolveTotalPages(int totalEntries, int pageSize) {
        int safeEntries = Math.max(0, totalEntries);
        int safePageSize = Math.max(1, pageSize);
        if (safeEntries == 0) {
            return 1;
        }
        int occupiedPages = (safeEntries + safePageSize - 1) / safePageSize;
        if (safeEntries % safePageSize == 0) {
            return occupiedPages + 1;
        }
        return occupiedPages;
    }
}
