package com.agguy.infiniteinventory.database.statistics;

/**
 * 按模组命名空间的统计项。
 *
 * @param namespace   模组命名空间（如 "minecraft", "ironchest"）
 * @param entryCount  该命名空间下不同物品种类数
 * @param itemCount   该命名空间下总物品数量
 * @param percentage  占总量的百分比 (0.0 ~ 100.0)
 */
public record NamespaceBreakdown(
        String namespace,
        long entryCount,
        long itemCount,
        double percentage
) {
}
