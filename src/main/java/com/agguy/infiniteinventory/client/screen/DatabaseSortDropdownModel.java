package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseSortDirection;
import com.agguy.infiniteinventory.database.DatabaseSortMethod;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import java.util.List;

final class DatabaseSortDropdownModel {
    private DatabaseSortDropdownModel() {
    }

    static List<DatabaseSortMethod> methodOptions() {
        return DatabaseSortMethod.orderedValues();
    }

    static DatabaseSortOption resolveDirection(DatabaseSortOption currentSort, DatabaseSortDirection direction) {
        DatabaseSortOption resolvedSort = currentSort == null ? DatabaseSortOption.RECENTLY_CHANGED : currentSort;
        return DatabaseSortOption.of(resolvedSort.method(), direction);
    }

    static DatabaseSortOption resolveMethod(DatabaseSortOption currentSort, DatabaseSortMethod method) {
        DatabaseSortOption resolvedSort = currentSort == null ? DatabaseSortOption.RECENTLY_CHANGED : currentSort;
        return DatabaseSortOption.of(method, resolvedSort.direction());
    }

    static SortAction actionForDirection(DatabaseSortOption currentSort, DatabaseSortDirection direction) {
        return new SortAction(resolveDirection(currentSort, direction), false);
    }

    static SortAction actionForMethod(DatabaseSortOption currentSort, DatabaseSortMethod method) {
        return new SortAction(resolveMethod(currentSort, method), true);
    }

    record SortAction(DatabaseSortOption nextSort, boolean closeMenu) {
        SortAction {
            nextSort = nextSort == null ? DatabaseSortOption.RECENTLY_CHANGED : nextSort;
        }
    }
}
