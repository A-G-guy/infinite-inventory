package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseViewPreferencesAttachmentTest {
    @Test
    void shouldPersistQueriesPerScopeAndLastScope() {
        DatabaseViewPreferencesAttachment preferences = new DatabaseViewPreferencesAttachment();
        DatabaseQuery personalQuery = new DatabaseQuery(DatabaseScope.PERSONAL, DatabaseCategory.MATERIALS, DatabaseSortOption.NAME_ASC, "iron", 2, 81);
        DatabaseQuery publicQuery = new DatabaseQuery(DatabaseScope.PUBLIC, DatabaseCategory.BLOCKS, DatabaseSortOption.COUNT_DESC, "stone", 1, 96);
        preferences.setQuery(DatabaseScope.PERSONAL, personalQuery);
        preferences.setQuery(DatabaseScope.PUBLIC, publicQuery);
        preferences.setLastScope(DatabaseScope.PUBLIC);

        DatabaseViewPreferencesAttachment restored = new DatabaseViewPreferencesAttachment();
        restored.deserializeNBT(null, preferences.serializeNBT(null));

        assertEquals(DatabaseScope.PUBLIC, restored.lastScope());
        assertEquals(personalQuery, restored.queryFor(DatabaseScope.PERSONAL));
        assertEquals(publicQuery, restored.queryFor(DatabaseScope.PUBLIC));
    }
}
