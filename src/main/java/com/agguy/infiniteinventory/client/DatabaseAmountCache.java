package com.agguy.infiniteinventory.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/**
 * 客户端单例缓存，存储数据库物品数量与所属分类名称，供悬浮提示显示使用。
 *
 * <p>采用 LRU 淘汰策略防止极端情况下内存无限增长。上限设为 4096，
 * 对绝大多数使用场景足够且内存占用可控。不依赖 JEI，为模组独立功能。</p>
 */
public final class DatabaseAmountCache {
    public static final DatabaseAmountCache INSTANCE = new DatabaseAmountCache();
    private static final int MAX_CACHE_SIZE = 4096;

    private final Map<ItemStack, Entry> personalAmounts = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ItemStack, Entry> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private final Map<ItemStack, Entry> publicAmounts = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ItemStack, Entry> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private DatabaseAmountCache() {
    }

    public synchronized void update(List<Entry> personal, List<Entry> publicItems) {
        this.personalAmounts.clear();
        for (Entry entry : personal) {
            this.personalAmounts.put(entry.stack().copyWithCount(1), entry);
        }
        this.publicAmounts.clear();
        for (Entry entry : publicItems) {
            this.publicAmounts.put(entry.stack().copyWithCount(1), entry);
        }
    }

    public synchronized Entry getPersonal(ItemStack stack) {
        return findEntry(this.personalAmounts, stack);
    }

    public synchronized Entry getPublic(ItemStack stack) {
        return findEntry(this.publicAmounts, stack);
    }

    public synchronized boolean isAvailable() {
        return !this.personalAmounts.isEmpty() || !this.publicAmounts.isEmpty();
    }

    private static Entry findEntry(Map<ItemStack, Entry> map, ItemStack target) {
        for (Map.Entry<ItemStack, Entry> entry : map.entrySet()) {
            if (ItemStack.isSameItemSameComponents(entry.getKey(), target)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public record Entry(ItemStack stack, String tabName, long amount) {
    }
}
