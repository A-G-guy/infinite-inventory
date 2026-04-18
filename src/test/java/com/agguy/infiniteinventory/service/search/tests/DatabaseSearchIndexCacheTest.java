package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.localization.ViewerLanguage;
import com.agguy.infiniteinventory.service.search.DatabaseSearchIndex;
import com.agguy.infiniteinventory.service.search.PinyinIndexData;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class DatabaseSearchIndexCacheTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void resolveShouldReuseEquivalentKeysAndUseWeakBackingMap() throws ReflectiveOperationException, ClassNotFoundException {
        Class<?> cacheClass = Class.forName("com.agguy.infiniteinventory.service.search.DatabaseSearchIndexCache");
        Constructor<?> constructor = cacheClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object cache = constructor.newInstance();

        Field cacheField = cacheClass.getDeclaredField("cache");
        cacheField.setAccessible(true);
        assertEquals(WeakHashMap.class, cacheField.get(cache).getClass());

        Method resolveMethod = cacheClass.getDeclaredMethod("resolve", StoredStackKey.class, ViewerLanguage.class, Function.class);
        resolveMethod.setAccessible(true);

        AtomicInteger builderCalls = new AtomicInteger();
        Function<StoredStackKey, DatabaseSearchIndex> builder = key -> {
            builderCalls.incrementAndGet();
            return DatabaseSearchIndex.of(key, "stone", List.of(), PinyinIndexData.empty());
        };

        StoredStackKey firstKey = StoredStackKey.of(new ItemStack(Items.STONE));
        StoredStackKey equivalentKey = StoredStackKey.of(new ItemStack(Items.STONE));
        Object firstIndex = resolveMethod.invoke(cache, firstKey, ViewerLanguage.EN_US, builder);
        Object secondIndex = resolveMethod.invoke(cache, equivalentKey, ViewerLanguage.EN_US, builder);
        Object thirdIndex = resolveMethod.invoke(cache, equivalentKey, ViewerLanguage.ZH_CN, builder);

        assertSame(firstIndex, secondIndex);
        assertNotSame(firstIndex, thirdIndex);
        assertEquals(2, builderCalls.get());
    }
}
