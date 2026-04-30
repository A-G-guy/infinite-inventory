package com.agguy.infiniteinventory.database.statistics;

import com.agguy.infiniteinventory.database.DatabaseScope;
import java.util.List;

/**
 * 数据库统计快照，包含某作用域下所有聚合统计信息。
 * 设计为不可变记录，便于缓存和网络传输。
 *
 * @param scope                 作用域
 * @param totalEntries          不同物品种类数
 * @param totalItems            总物品数量
 * @param categoryBreakdowns    各分类统计
 * @param namespaceBreakdowns   各模组命名空间统计
 * @param tabBreakdowns         各标签页统计
 * @param dailyTrends           近7天出入库趋势
 * @param hourlyTrends          近24小时出入库趋势
 */
public record DatabaseStatisticsSnapshot(
        DatabaseScope scope,
        long totalEntries,
        long totalItems,
        List<CategoryBreakdown> categoryBreakdowns,
        List<NamespaceBreakdown> namespaceBreakdowns,
        List<TabBreakdown> tabBreakdowns,
        List<DailyTrend> dailyTrends,
        List<HourlyTrend> hourlyTrends
) {
    public DatabaseStatisticsSnapshot {
        categoryBreakdowns = categoryBreakdowns == null ? List.of() : List.copyOf(categoryBreakdowns);
        namespaceBreakdowns = namespaceBreakdowns == null ? List.of() : List.copyOf(namespaceBreakdowns);
        tabBreakdowns = tabBreakdowns == null ? List.of() : List.copyOf(tabBreakdowns);
        dailyTrends = dailyTrends == null ? List.of() : List.copyOf(dailyTrends);
        hourlyTrends = hourlyTrends == null ? List.of() : List.copyOf(hourlyTrends);
    }
}
