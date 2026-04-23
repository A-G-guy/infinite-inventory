package com.agguy.infiniteinventory.compat.jei;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/**
 * 客户端单例缓存，存储数据库物品数量，供 JEI 面板叠加渲染使用。
 *
 * <p>采用 LRU 淘汰策略防止极端情况下（大型整合包含数万种物品）内存无限增长。
 * 上限设为 4096，对绝大多数使用场景足够且内存占用可控。</p>
 */
public final class JeiAmountCache {
    public static final JeiAmountCache INSTANCE = new JeiAmountCache();
    private static final int MAX_CACHE_SIZE = 4096;

    private final Map<ItemStack, Long> personalAmounts = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ItemStack, Long> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private final Map<ItemStack, Long> publicAmounts = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ItemStack, Long> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private JeiAmountCache() {
    }

    public synchronized void update(Object2LongMap<ItemStack> personal, Object2LongMap<ItemStack> publicItems) {
        this.personalAmounts.clear();
        this.personalAmounts.putAll(personal);
        this.publicAmounts.clear();
        this.publicAmounts.putAll(publicItems);
    }

    public synchronized long getPersonalAmount(ItemStack stack) {
        return findAmount(this.personalAmounts, stack);
    }

    public synchronized long getPublicAmount(ItemStack stack) {
        return findAmount(this.publicAmounts, stack);
    }

    public static boolean isAvailable() {
        return com.agguy.infiniteinventory.compat.jei.JeiCompat.isAvailable();
    }

    private static long findAmount(Map<ItemStack, Long> map, ItemStack target) {
        for (Map.Entry<ItemStack, Long> entry : map.entrySet()) {
            if (ItemStack.isSameItemSameComponents(entry.getKey(), target)) {
                return entry.getValue();
            }
        }
        return 0L;
    }
}
