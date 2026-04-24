package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabaseCategoryTest {

    @Test
    void shouldHaveSevenCategories() {
        assertEquals(7, DatabaseCategory.values().length);
    }

    @Test
    void eachCategoryShouldHaveTranslationKey() {
        for (DatabaseCategory category : DatabaseCategory.values()) {
            assertNotNull(category.translationKey());
        }
    }

    @Test
    void allShouldBeFirst() {
        assertEquals(DatabaseCategory.ALL, DatabaseCategory.values()[0]);
    }
}
