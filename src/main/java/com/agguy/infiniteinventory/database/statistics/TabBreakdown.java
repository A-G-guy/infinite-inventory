package com.agguy.infiniteinventory.database.statistics;

/**
 * 按标签页的统计项。
 *
 * @param tabId          标签页 ID
 * @param tabDisplayName 标签页显示名称
 * @param entryCount     该标签页下不同物品种类数
 * @param itemCount      该标签页下总物品数量
 * @param percentage     占总量的百分比 (0.0 ~ 100.0)
 */
public record TabBreakdown(
        String tabId,
        String tabDisplayName,
        long entryCount,
        long itemCount,
        double percentage
) {
}
