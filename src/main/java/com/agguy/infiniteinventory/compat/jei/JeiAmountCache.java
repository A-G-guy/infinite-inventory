package com.agguy.infiniteinventory.compat.jei;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.world.item.ItemStack;

/**
 * 客户端单例缓存，存储数据库物品数量，供 JEI 面板叠加渲染使用。
 */
public final class JeiAmountCache {
    public static final JeiAmountCache INSTANCE = new JeiAmountCache();

    private final Object2LongMap<ItemStack> personalAmounts = new Object2LongOpenHashMap<>();
    private final Object2LongMap<ItemStack> publicAmounts = new Object2LongOpenHashMap<>();

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

    private static long findAmount(Object2LongMap<ItemStack> map, ItemStack target) {
        for (Object2LongMap.Entry<ItemStack> entry : map.object2LongEntrySet()) {
            if (ItemStack.isSameItemSameComponents(entry.getKey(), target)) {
                return entry.getLongValue();
            }
        }
        return 0L;
    }
}
