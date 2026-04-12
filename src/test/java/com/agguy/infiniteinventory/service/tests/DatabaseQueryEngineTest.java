package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.service.DatabaseQueryEngine;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseQueryEngineTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    private final DatabaseQueryEngine queryEngine = DatabaseQueryEngine.INSTANCE;

    @Test
    void noSearchQueriesShouldReuseSortedCacheAcrossPages() throws ReflectiveOperationException {
        StoredItemDatabase database = this.seededBrowseDatabase();
        DatabaseQuery firstPageQuery = this.query(DatabaseCategory.ALL, DatabaseSortOption.NAME_ASC, "", 0, 1);

        DatabasePage firstPage = this.queryEngine.buildPage(database, firstPageQuery);
        DatabasePage secondPage = this.queryEngine.buildPage(database, firstPageQuery.withPageIndex(1));
        Object runtimeIndex = this.runtimeIndexFor(database);

        assertEquals(3, firstPage.totalEntries());
        assertEquals(3, secondPage.totalEntries());
        assertNotEquals(firstPage.entries().getFirst().key().registryName(), secondPage.entries().getFirst().key().registryName());
        assertEquals(1, this.noSearchCacheSize(runtimeIndex, DatabaseCategory.ALL));
        assertEquals(0, this.searchCacheSize(runtimeIndex));
    }

    @Test
    void searchQueriesShouldCacheByFingerprintAndRebuildAfterRevisionChanges() throws ReflectiveOperationException {
        StoredItemDatabase database = this.seededSearchDatabase();
        DatabaseQuery searchQuery = this.query(DatabaseCategory.ALL, DatabaseSortOption.COUNT_DESC, "diamond", 0, 1);

        this.queryEngine.buildPage(database, searchQuery);
        Object firstRuntimeIndex = this.runtimeIndexFor(database);
        assertEquals(1, this.searchCacheSize(firstRuntimeIndex));

        this.queryEngine.buildPage(database, searchQuery.withPageIndex(1));
        assertSame(firstRuntimeIndex, this.runtimeIndexFor(database));
        assertEquals(1, this.searchCacheSize(firstRuntimeIndex));

        database.store(new ItemStack(Items.DIAMOND_BLOCK, 2));
        this.queryEngine.buildPage(database, searchQuery);
        Object rebuiltRuntimeIndex = this.runtimeIndexFor(database);

        assertNotSame(firstRuntimeIndex, rebuiltRuntimeIndex);
        assertEquals(1, this.searchCacheSize(rebuiltRuntimeIndex));
    }

    @Test
    void categoryQueriesShouldOnlyReturnBucketedEntries() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.STONE, 8));
        database.store(new ItemStack(Items.DIRT, 3));
        database.store(new ItemStack(Items.BREAD, 2));

        DatabasePage page = this.queryEngine.buildPage(
                database,
                this.query(DatabaseCategory.BLOCKS, DatabaseSortOption.RECENTLY_CHANGED, "", 0, 10)
        );

        assertEquals(2, page.totalEntries());
        assertEquals(11L, page.totalItems());
        assertTrue(page.entries().stream().allMatch(entry -> entry.view().category() == DatabaseCategory.BLOCKS));
    }

    @Test
    void pageWindowCalculationShouldAvoidIntegerOverflow() throws ReflectiveOperationException {
        Method fromIndexMethod = DatabaseQueryEngine.class.getDeclaredMethod("resolvePageFromIndex", int.class, int.class, int.class);
        Method toIndexMethod = DatabaseQueryEngine.class.getDeclaredMethod("resolvePageToIndex", int.class, int.class, int.class);
        fromIndexMethod.setAccessible(true);
        toIndexMethod.setAccessible(true);

        int totalEntries = Integer.MAX_VALUE;
        int pageSize = 640;
        int pageIndex = totalEntries / pageSize;
        int fromIndex = (int) fromIndexMethod.invoke(null, pageIndex, pageSize, totalEntries);
        int toIndex = (int) toIndexMethod.invoke(null, fromIndex, pageSize, totalEntries);

        assertEquals(2_147_483_520, fromIndex);
        assertEquals(Integer.MAX_VALUE, toIndex);
    }

    private DatabaseQuery query(DatabaseCategory category, DatabaseSortOption sortOption, String searchText, int pageIndex, int pageSize) {
        return new DatabaseQuery(DatabaseScope.PERSONAL, category, sortOption, searchText, pageIndex, pageSize);
    }

    private StoredItemDatabase seededBrowseDatabase() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.APPLE, 3));
        database.store(new ItemStack(Items.STONE, 8));
        database.store(new ItemStack(Items.DIRT, 2));
        return database;
    }

    private StoredItemDatabase seededSearchDatabase() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.DIAMOND, 16));
        database.store(new ItemStack(Items.DIAMOND_SWORD, 1));
        database.store(new ItemStack(Items.DIAMOND_PICKAXE, 1));
        return database;
    }

    private Object runtimeIndexFor(StoredItemDatabase database) throws ReflectiveOperationException {
        Field runtimeIndexesField = DatabaseQueryEngine.class.getDeclaredField("runtimeIndexes");
        runtimeIndexesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<StoredItemDatabase, Object> runtimeIndexes = (Map<StoredItemDatabase, Object>) runtimeIndexesField.get(this.queryEngine);
        return runtimeIndexes.get(database);
    }

    private int noSearchCacheSize(Object runtimeIndex, DatabaseCategory category) throws ReflectiveOperationException {
        Field noSearchSortedCacheField = runtimeIndex.getClass().getDeclaredField("noSearchSortedCache");
        noSearchSortedCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        EnumMap<DatabaseCategory, EnumMap<DatabaseSortOption, ?>> noSearchSortedCache =
                (EnumMap<DatabaseCategory, EnumMap<DatabaseSortOption, ?>>) noSearchSortedCacheField.get(runtimeIndex);
        return noSearchSortedCache.get(category).size();
    }

    private int searchCacheSize(Object runtimeIndex) throws ReflectiveOperationException {
        Field searchCacheField = runtimeIndex.getClass().getDeclaredField("searchCache");
        searchCacheField.setAccessible(true);
        Map<?, ?> searchCache = (Map<?, ?>) searchCacheField.get(runtimeIndex);
        return searchCache.size();
    }
}
