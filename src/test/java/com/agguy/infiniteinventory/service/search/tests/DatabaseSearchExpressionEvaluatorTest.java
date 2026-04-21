package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseTabQueryState;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.database.tests.DatabaseTestReflectionHelper;
import com.agguy.infiniteinventory.service.search.DatabaseCreativeTabSearchResolver;
import com.agguy.infiniteinventory.service.search.DatabaseItemSearchMetadata;
import com.agguy.infiniteinventory.service.search.DatabaseParsedSearchQuery;
import com.agguy.infiniteinventory.service.search.DatabaseSearchEnvironment;
import com.agguy.infiniteinventory.service.search.DatabaseSearchExpressionEvaluator;
import com.agguy.infiniteinventory.service.search.DatabaseSearchIndex;
import com.agguy.infiniteinventory.service.search.DatabaseSearchQueryParser;
import com.agguy.infiniteinventory.service.search.DatabaseSearchQueryParserContext;
import com.agguy.infiniteinventory.service.search.DatabaseSearchRanking;
import com.agguy.infiniteinventory.service.search.SearchTextNormalizer;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSearchExpressionEvaluatorTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    private final DatabaseSearchExpressionEvaluator evaluator = DatabaseSearchExpressionEvaluator.INSTANCE;

    @Test
    void evaluatorShouldMatchModTagItemIdAndCreativeTabFilters() throws ReflectiveOperationException {
        StoredStackKey key = DatabaseTestReflectionHelper.fakeKey("infiniteinventory:database_access_item", new ItemStack(Items.STONE));
        DatabaseSearchIndex searchIndex = this.index("database access item", "infiniteinventory:database_access_item", "infiniteinventory");
        DatabaseItemSearchMetadata searchMetadata = new DatabaseItemSearchMetadata(
                "Infinite Inventory",
                "infinite inventory",
                "infiniteinventory",
                List.of("minecraft:logs"),
                List.of("minecraftlogs")
        );
        DatabaseSearchEnvironment searchEnvironment = new DatabaseSearchEnvironment(
                FeatureFlags.DEFAULT_FLAGS,
                true,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
        );
        this.seedCreativeTabSearchCache(searchEnvironment, key, "minecraft:building_blocks", "building blocks");

        assertTrue(this.evaluate("@infinite inventory", searchIndex, searchMetadata, key, searchEnvironment).matched());
        assertTrue(this.evaluate("#minecraft:logs", searchIndex, searchMetadata, key, searchEnvironment).matched());
        assertTrue(this.evaluate("&infiniteinventory:database_access_item", searchIndex, searchMetadata, key, searchEnvironment).matched());
        assertTrue(this.evaluate("%building blocks", searchIndex, searchMetadata, key, searchEnvironment).matched());
        assertFalse(this.evaluate("%combat", searchIndex, searchMetadata, key, searchEnvironment).matched());
    }

    private DatabaseSearchRanking evaluate(
            String searchText,
            DatabaseSearchIndex searchIndex,
            DatabaseItemSearchMetadata searchMetadata,
            StoredStackKey key,
            DatabaseSearchEnvironment searchEnvironment
    ) {
        DatabaseParsedSearchQuery parsedQuery = DatabaseSearchQueryParser.INSTANCE.parse(
                searchText,
                new DatabaseSearchQueryParserContext(
                        Set.of("infinite inventory"),
                        Set.of("building blocks")
                )
        );
        return this.evaluator.evaluate(
                new DatabaseTabQueryState(
                        DatabaseSortOption.RECENTLY_CHANGED,
                        searchText,
                        DatabaseSearchConfig.defaultConfig(),
                        0,
                        DatabaseQuery.DEFAULT_PAGE_SIZE
                ),
                parsedQuery,
                searchIndex,
                searchMetadata,
                key,
                1L,
                searchEnvironment
        );
    }

    private DatabaseSearchIndex index(String displayName, String registryName, String namespace) {
        String registryPath = registryName.substring(registryName.indexOf(':') + 1);
        return new DatabaseSearchIndex(
                displayName,
                SearchTextNormalizer.normalizeNaturalText(displayName),
                SearchTextNormalizer.compactNaturalText(displayName),
                List.of("database", "access", "item"),
                List.of(SearchTextNormalizer.normalizeNaturalText(displayName)),
                List.of(SearchTextNormalizer.compactNaturalText(displayName)),
                SearchTextNormalizer.normalizeIdentifierText(registryName),
                SearchTextNormalizer.compactIdentifierText(registryName),
                SearchTextNormalizer.normalizeIdentifierText(registryPath),
                SearchTextNormalizer.compactIdentifierText(registryPath),
                List.of("database", "access", "item"),
                SearchTextNormalizer.normalizeIdentifierText(namespace),
                "",
                "",
                List.of()
        );
    }

    @SuppressWarnings("unchecked")
    private void seedCreativeTabSearchCache(
            DatabaseSearchEnvironment searchEnvironment,
            StoredStackKey key,
            String tabRegistryName,
            String tabDisplayName
    ) throws ReflectiveOperationException {
        DatabaseCreativeTabSearchResolver resolver = DatabaseCreativeTabSearchResolver.INSTANCE;
        Field availableTabsCacheField = DatabaseCreativeTabSearchResolver.class.getDeclaredField("availableTabsCache");
        availableTabsCacheField.setAccessible(true);
        Map<Object, Object> availableTabsCache = (Map<Object, Object>) availableTabsCacheField.get(resolver);
        Field itemTabCacheField = DatabaseCreativeTabSearchResolver.class.getDeclaredField("itemTabCache");
        itemTabCacheField.setAccessible(true);
        Map<Object, Object> itemTabCache = (Map<Object, Object>) itemTabCacheField.get(resolver);

        Class<?> entryClass = Class.forName("com.agguy.infiniteinventory.service.search.DatabaseCreativeTabSearchEntry");
        Constructor<?> constructor = entryClass.getDeclaredConstructor(
                Class.forName("net.minecraft.world.item.CreativeModeTab"),
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class
        );
        constructor.setAccessible(true);
        String normalizedRegistryName = tabRegistryName.toLowerCase(Locale.ROOT);
        String compactRegistryName = normalizedRegistryName.replace(":", "");
        String registryPath = normalizedRegistryName.substring(normalizedRegistryName.indexOf(':') + 1);
        String compactRegistryPath = registryPath.replace("_", "");
        String normalizedDisplayName = tabDisplayName.toLowerCase(Locale.ROOT);
        String compactDisplayName = normalizedDisplayName.replace(" ", "");
        Object entry = constructor.newInstance(
                CreativeModeTabs.searchTab(),
                normalizedRegistryName,
                compactRegistryName,
                registryPath,
                compactRegistryPath,
                normalizedDisplayName,
                compactDisplayName
        );

        availableTabsCache.put(searchEnvironment.signature(), List.of(entry));
        Map<StoredStackKey, List<?>> cachedTabsByKey = new HashMap<>();
        cachedTabsByKey.put(key, List.of(entry));
        itemTabCache.put(searchEnvironment.signature(), cachedTabsByKey);
    }
}
