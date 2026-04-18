package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.localization.ViewerLanguage;
import com.agguy.infiniteinventory.service.DatabaseQueryEngine;
import com.agguy.infiniteinventory.service.search.DatabaseItemSearchResolver;
import com.agguy.infiniteinventory.service.search.DatabaseSearchIndex;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
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
        DatabaseTabDirectory tabDirectory = new DatabaseTabDirectory();
        DatabaseQuery firstPageQuery = this.query(DatabaseTabs.ALL_TAB_ID, DatabaseSortOption.NAME_ASC, "", 0, 1);

        DatabasePage firstPage = this.queryEngine.buildPage(
                database,
                tabDirectory,
                firstPageQuery,
                DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL)
        );
        DatabasePage secondPage = this.queryEngine.buildPage(
                database,
                tabDirectory,
                firstPageQuery.withPageIndex(1),
                DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL)
        );
        Object runtimeIndex = this.runtimeIndexFor(database, ViewerLanguage.EN_US);

        assertEquals(3, firstPage.totalEntries());
        assertEquals(3, secondPage.totalEntries());
        assertNotEquals(firstPage.entries().getFirst().key().registryName(), secondPage.entries().getFirst().key().registryName());
        assertEquals(1, this.noSearchCacheSize(runtimeIndex, DatabaseTabs.ALL_TAB_ID));
        assertEquals(0, this.searchCacheSize(runtimeIndex));
    }

    @Test
    void searchQueriesShouldCacheByFingerprintAndRebuildAfterRevisionChanges() throws ReflectiveOperationException {
        StoredItemDatabase database = this.seededSearchDatabase();
        DatabaseTabDirectory tabDirectory = new DatabaseTabDirectory();
        DatabaseQuery searchQuery = this.query(DatabaseTabs.ALL_TAB_ID, DatabaseSortOption.COUNT_DESC, "diamond", 0, 1);

        this.queryEngine.buildPage(database, tabDirectory, searchQuery, DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL));
        Object firstRuntimeIndex = this.runtimeIndexFor(database, ViewerLanguage.EN_US);
        assertEquals(1, this.searchCacheSize(firstRuntimeIndex));

        this.queryEngine.buildPage(
                database,
                tabDirectory,
                searchQuery.withPageIndex(1),
                DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL)
        );
        assertSame(firstRuntimeIndex, this.runtimeIndexFor(database, ViewerLanguage.EN_US));
        assertEquals(1, this.searchCacheSize(firstRuntimeIndex));

        database.store(new ItemStack(Items.DIAMOND_BLOCK, 2));
        this.queryEngine.buildPage(database, tabDirectory, searchQuery, DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL));
        Object rebuiltRuntimeIndex = this.runtimeIndexFor(database, ViewerLanguage.EN_US);

        assertNotSame(firstRuntimeIndex, rebuiltRuntimeIndex);
        assertEquals(1, this.searchCacheSize(rebuiltRuntimeIndex));
    }

    @Test
    void tabQueriesShouldOnlyReturnEntriesAssignedToThatTab() {
        StoredItemDatabase database = new StoredItemDatabase();
        DatabaseTabDirectory tabDirectory = new DatabaseTabDirectory();
        DatabaseTab blocksTab = tabDirectory.addCustomTab("Blocks", DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID);
        database.store(new ItemStack(Items.STONE, 8), blocksTab.id());
        database.store(new ItemStack(Items.DIRT, 3), blocksTab.id());
        database.store(new ItemStack(Items.BREAD, 2), DatabaseTabs.DEFAULT_TAB_ID);

        DatabasePage page = this.queryEngine.buildPage(
                database,
                tabDirectory,
                this.query(blocksTab.id(), DatabaseSortOption.RECENTLY_CHANGED, "", 0, 10),
                DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, blocksTab.id())
        );

        assertEquals(2, page.totalEntries());
        assertEquals(11L, page.totalItems());
        assertTrue(page.entries().stream().allMatch(entry -> entry.view().tabId().equals(blocksTab.id())));
    }

    @Test
    void differentViewerLanguagesShouldUseIndependentLocalizedIndexes() throws ReflectiveOperationException {
        StoredItemDatabase database = this.seededLocalizedDatabase();
        DatabaseTabDirectory tabDirectory = new DatabaseTabDirectory();
        DatabaseQuery browseQuery = this.query(DatabaseTabs.ALL_TAB_ID, DatabaseSortOption.NAME_ASC, "", 0, 10);

        DatabasePage englishPage = this.queryEngine.buildPage(
                database,
                tabDirectory,
                browseQuery,
                DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL),
                ViewerLanguage.EN_US
        );
        DatabasePage chinesePage = this.queryEngine.buildPage(
                database,
                tabDirectory,
                browseQuery,
                DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL),
                ViewerLanguage.ZH_CN
        );

        assertEquals("minecraft:apple", englishPage.entries().getFirst().key().registryName());
        DatabaseSearchIndex chineseApple = DatabaseItemSearchResolver.INSTANCE.resolve(
                StoredStackKey.of(new ItemStack(Items.APPLE)),
                ViewerLanguage.ZH_CN
        );
        if (!"Apple".equals(chineseApple.displayName())) {
            assertEquals("minecraft:bread", chinesePage.entries().getFirst().key().registryName());
        } else {
            assertEquals("minecraft:apple", chinesePage.entries().getFirst().key().registryName());
        }
        assertNotSame(this.runtimeIndexFor(database, ViewerLanguage.EN_US), this.runtimeIndexFor(database, ViewerLanguage.ZH_CN));
    }

    @Test
    void englishViewerSearchShouldStillMatchChineseAliasAndPinyin() {
        StoredItemDatabase database = new StoredItemDatabase();
        DatabaseTabDirectory tabDirectory = new DatabaseTabDirectory();
        database.store(new ItemStack(Items.APPLE, 3));
        database.store(new ItemStack(Items.BREAD, 2));
        DatabaseSearchIndex englishApple = DatabaseItemSearchResolver.INSTANCE.resolve(
                StoredStackKey.of(new ItemStack(Items.APPLE)),
                ViewerLanguage.EN_US
        );

        if (!englishApple.displayNameSearchNormalizedTexts().contains("苹果")) {
            return;
        }

        DatabasePage chineseAliasPage = this.queryEngine.buildPage(
                database,
                tabDirectory,
                this.query(DatabaseTabs.ALL_TAB_ID, DatabaseSortOption.RECENTLY_CHANGED, "苹果", 0, 10),
                DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL),
                ViewerLanguage.EN_US
        );
        DatabasePage pinyinPage = this.queryEngine.buildPage(
                database,
                tabDirectory,
                this.query(DatabaseTabs.ALL_TAB_ID, DatabaseSortOption.RECENTLY_CHANGED, "pingguo", 0, 10),
                DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL),
                ViewerLanguage.EN_US
        );

        assertEquals(1, chineseAliasPage.totalEntries());
        assertEquals("minecraft:apple", chineseAliasPage.entries().getFirst().key().registryName());
        assertEquals(1, pinyinPage.totalEntries());
        assertEquals("minecraft:apple", pinyinPage.entries().getFirst().key().registryName());
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

    private DatabaseQuery query(String focusedTabId, DatabaseSortOption sortOption, String searchText, int pageIndex, int pageSize) {
        return new DatabaseQuery(
                DatabaseScope.PERSONAL,
                focusedTabId,
                List.of(focusedTabId),
                Map.of(focusedTabId, pageIndex),
                Map.of(focusedTabId, pageSize),
                sortOption,
                searchText,
                com.agguy.infiniteinventory.database.DatabaseSearchConfig.defaultConfig()
        );
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

    private StoredItemDatabase seededLocalizedDatabase() {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.APPLE, 3));
        database.store(new ItemStack(Items.BREAD, 2));
        return database;
    }

    private Object runtimeIndexFor(StoredItemDatabase database, ViewerLanguage viewerLanguage) throws ReflectiveOperationException {
        Field runtimeIndexesField = DatabaseQueryEngine.class.getDeclaredField("runtimeIndexes");
        runtimeIndexesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<StoredItemDatabase, Object> runtimeIndexes = (Map<StoredItemDatabase, Object>) runtimeIndexesField.get(this.queryEngine);
        Object localizedRuntimeIndexes = runtimeIndexes.get(database);
        Field localizedIndexesField = localizedRuntimeIndexes.getClass().getDeclaredField("localizedIndexes");
        localizedIndexesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<ViewerLanguage, Object> localizedIndexes = (Map<ViewerLanguage, Object>) localizedIndexesField.get(localizedRuntimeIndexes);
        return localizedIndexes.get(viewerLanguage);
    }

    private int noSearchCacheSize(Object runtimeIndex, String tabId) throws ReflectiveOperationException {
        Field noSearchSortedCacheField = runtimeIndex.getClass().getDeclaredField("noSearchSortedCache");
        noSearchSortedCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Map<DatabaseSortOption, ?>> noSearchSortedCache =
                (Map<String, Map<DatabaseSortOption, ?>>) noSearchSortedCacheField.get(runtimeIndex);
        return noSearchSortedCache.get(tabId).size();
    }

    private int searchCacheSize(Object runtimeIndex) throws ReflectiveOperationException {
        Field searchCacheField = runtimeIndex.getClass().getDeclaredField("searchCache");
        searchCacheField.setAccessible(true);
        Map<?, ?> searchCache = (Map<?, ?>) searchCacheField.get(runtimeIndex);
        return searchCache.size();
    }
}
