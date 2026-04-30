package com.agguy.infiniteinventory.database.statistics;

import com.agguy.infiniteinventory.database.DatabaseCategory;

/**
 * 按功能分类的统计项。
 *
 * @param category    功能分类
 * @param entryCount  该分类下不同物品种类数
 * @param itemCount   该分类下总物品数量
 * @param percentage  占总量的百分比 (0.0 ~ 100.0)
 */
public record CategoryBreakdown(
        DatabaseCategory category,
        long entryCount,
        long itemCount,
        double percentage
) {
}
