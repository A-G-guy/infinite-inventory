package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseItemClassifier;
import com.agguy.infiniteinventory.database.DatabaseLogAction;
import com.agguy.infiniteinventory.database.DatabaseLogEntry;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.database.statistics.CategoryBreakdown;
import com.agguy.infiniteinventory.database.statistics.DailyTrend;
import com.agguy.infiniteinventory.database.statistics.DatabaseStatisticsSnapshot;
import com.agguy.infiniteinventory.database.statistics.HourlyTrend;
import com.agguy.infiniteinventory.database.statistics.NamespaceBreakdown;
import com.agguy.infiniteinventory.database.statistics.TabBreakdown;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * 统计聚合计算引擎。
 *
 * <p>单次遍历 entries Map 同时产出分类、命名空间、标签页三组 breakdown 和日/小时趋势。
 */
final class PersonalDatabaseServiceStatisticsHelper {
    private static final int MAX_NAMESPACE_BREAKDOWNS = 20;
    private static final int MAX_TAB_BREAKDOWNS = 30;
    private static final int DAILY_TREND_DAYS = 7;
    private static final int HOURLY_TREND_HOURS = 24;

    private PersonalDatabaseServiceStatisticsHelper() {
    }

    static DatabaseStatisticsSnapshot buildStatisticsSnapshot(
            PersonalDatabaseService service,
            ServerPlayer player,
            DatabaseScope scope
    ) {
        StoredItemDatabase database = service.resolveDatabaseForView(player, scope);
        DatabaseTabDirectory tabDirectory = service.resolveTabsForView(player, scope);

        long totalEntries = database.entryCount();
        long totalItems = 0L;

        Map<DatabaseCategory, long[]> categoryCounts = new EnumMap<>(DatabaseCategory.class);
        Map<String, long[]> namespaceCounts = new LinkedHashMap<>();
        Map<String, long[]> tabCounts = new LinkedHashMap<>();

        for (Map.Entry<StoredStackKey, StoredStackEntry> entry : database.entries().entrySet()) {
            StoredStackKey key = entry.getKey();
            StoredStackEntry value = entry.getValue();
            long amount = value.amount();
            totalItems = safeAdd(totalItems, amount);

            DatabaseCategory category = DatabaseItemClassifier.INSTANCE.classify(key.displayStack());
            categoryCounts.computeIfAbsent(category, k -> new long[2]);
            categoryCounts.get(category)[0]++;
            categoryCounts.get(category)[1] = safeAdd(categoryCounts.get(category)[1], amount);

            String ns = key.registryNamespace();
            namespaceCounts.computeIfAbsent(ns, k -> new long[2]);
            namespaceCounts.get(ns)[0]++;
            namespaceCounts.get(ns)[1] = safeAdd(namespaceCounts.get(ns)[1], amount);

            String tabId = value.tabId();
            tabCounts.computeIfAbsent(tabId, k -> new long[2]);
            tabCounts.get(tabId)[0]++;
            tabCounts.get(tabId)[1] = safeAdd(tabCounts.get(tabId)[1], amount);
        }

        List<CategoryBreakdown> categoryBreakdowns = buildCategoryBreakdowns(categoryCounts, totalItems);
        List<NamespaceBreakdown> namespaceBreakdowns = buildNamespaceBreakdowns(namespaceCounts, totalItems);
        List<TabBreakdown> tabBreakdowns = buildTabBreakdowns(tabCounts, totalItems, tabDirectory);
        List<DailyTrend> dailyTrends = buildDailyTrends(database.logEntries());
        List<HourlyTrend> hourlyTrends = buildHourlyTrends(database.logEntries());

        return new DatabaseStatisticsSnapshot(
                scope,
                totalEntries,
                totalItems,
                categoryBreakdowns,
                namespaceBreakdowns,
                tabBreakdowns,
                dailyTrends,
                hourlyTrends
        );
    }

    private static List<CategoryBreakdown> buildCategoryBreakdowns(
            Map<DatabaseCategory, long[]> categoryCounts,
            long totalItems
    ) {
        List<CategoryBreakdown> result = new ArrayList<>(categoryCounts.size());
        for (Map.Entry<DatabaseCategory, long[]> entry : categoryCounts.entrySet()) {
            long[] counts = entry.getValue();
            double percentage = totalItems > 0 ? (counts[1] * 100.0 / totalItems) : 0.0;
            result.add(new CategoryBreakdown(entry.getKey(), counts[0], counts[1], percentage));
        }
        result.sort(Comparator.comparingDouble(CategoryBreakdown::percentage).reversed());
        return List.copyOf(result);
    }

    private static List<NamespaceBreakdown> buildNamespaceBreakdowns(
            Map<String, long[]> namespaceCounts,
            long totalItems
    ) {
        List<NamespaceBreakdown> result = new ArrayList<>(namespaceCounts.size());
        for (Map.Entry<String, long[]> entry : namespaceCounts.entrySet()) {
            long[] counts = entry.getValue();
            double percentage = totalItems > 0 ? (counts[1] * 100.0 / totalItems) : 0.0;
            result.add(new NamespaceBreakdown(entry.getKey(), counts[0], counts[1], percentage));
        }
        result.sort(Comparator.comparingDouble(NamespaceBreakdown::percentage).reversed());
        if (result.size() > MAX_NAMESPACE_BREAKDOWNS) {
            result = result.subList(0, MAX_NAMESPACE_BREAKDOWNS);
        }
        return List.copyOf(result);
    }

    private static List<TabBreakdown> buildTabBreakdowns(
            Map<String, long[]> tabCounts,
            long totalItems,
            DatabaseTabDirectory tabDirectory
    ) {
        List<TabBreakdown> result = new ArrayList<>(tabCounts.size());
        for (Map.Entry<String, long[]> entry : tabCounts.entrySet()) {
            String tabId = entry.getKey();
            long[] counts = entry.getValue();
            double percentage = totalItems > 0 ? (counts[1] * 100.0 / totalItems) : 0.0;
            String displayName = resolveTabDisplayName(tabDirectory, tabId);
            result.add(new TabBreakdown(tabId, displayName, counts[0], counts[1], percentage));
        }
        result.sort(Comparator.comparingDouble(TabBreakdown::percentage).reversed());
        if (result.size() > MAX_TAB_BREAKDOWNS) {
            result = result.subList(0, MAX_TAB_BREAKDOWNS);
        }
        return List.copyOf(result);
    }

    private static String resolveTabDisplayName(DatabaseTabDirectory tabDirectory, String tabId) {
        if (tabId == null || tabId.isEmpty()) {
            return tabId;
        }
        if (DatabaseTabs.DEFAULT_TAB_ID.equals(tabId)) {
            return DatabaseTabs.DEFAULT_TAB_TRANSLATION_KEY;
        }
        if (DatabaseTabs.ALL_TAB_ID.equals(tabId)) {
            return DatabaseTabs.ALL_TAB_TRANSLATION_KEY;
        }
        for (DatabaseTab tab : tabDirectory.orderedTabs()) {
            if (tab.id().equals(tabId)) {
                String name = tab.displayName();
                return name.isBlank() ? tabId : name;
            }
        }
        return tabId;
    }

    private static List<DailyTrend> buildDailyTrends(List<DatabaseLogEntry> logEntries) {
        if (logEntries.isEmpty()) {
            return List.of();
        }
        ZoneId zone = ZoneId.systemDefault();
        Map<Long, long[]> dayMap = new LinkedHashMap<>();

        for (DatabaseLogEntry entry : logEntries) {
            ZonedDateTime dateTime = ZonedDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(entry.timestampMillis()),
                    zone
            );
            ZonedDateTime dayStart = dateTime.toLocalDate().atStartOfDay(zone);
            long dayMillis = dayStart.toInstant().toEpochMilli();

            long[] counts = dayMap.computeIfAbsent(dayMillis, k -> new long[6]);
            switch (entry.action()) {
                case DEPOSIT -> {
                    counts[0]++;
                    counts[1] = safeAdd(counts[1], entry.amount());
                }
                case EXTRACT -> {
                    counts[2]++;
                    counts[3] = safeAdd(counts[3], entry.amount());
                }
                case TRANSFER -> counts[4]++;
                case DELETE -> counts[5]++;
                default -> {
                    // no-op for unknown actions
                }
            }
        }

        List<Map.Entry<Long, long[]>> sortedDays = new ArrayList<>(dayMap.entrySet());
        sortedDays.sort(Map.Entry.comparingByKey());

        if (sortedDays.size() > DAILY_TREND_DAYS) {
            sortedDays = sortedDays.subList(sortedDays.size() - DAILY_TREND_DAYS, sortedDays.size());
        }

        List<DailyTrend> result = new ArrayList<>(sortedDays.size());
        for (Map.Entry<Long, long[]> dayEntry : sortedDays) {
            long[] c = dayEntry.getValue();
            result.add(new DailyTrend(dayEntry.getKey(), c[0], c[1], c[2], c[3], c[4], c[5]));
        }
        return List.copyOf(result);
    }

    private static List<HourlyTrend> buildHourlyTrends(List<DatabaseLogEntry> logEntries) {
        if (logEntries.isEmpty()) {
            return List.of();
        }
        ZoneId zone = ZoneId.systemDefault();
        Map<Long, long[]> hourMap = new LinkedHashMap<>();

        for (DatabaseLogEntry entry : logEntries) {
            ZonedDateTime dateTime = ZonedDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(entry.timestampMillis()),
                    zone
            );
            ZonedDateTime hourStart = dateTime.withMinute(0).withSecond(0).withNano(0);
            long hourMillis = hourStart.toInstant().toEpochMilli();

            long[] counts = hourMap.computeIfAbsent(hourMillis, k -> new long[2]);
            switch (entry.action()) {
                case DEPOSIT -> counts[0] = safeAdd(counts[0], entry.amount());
                case EXTRACT -> counts[1] = safeAdd(counts[1], entry.amount());
                default -> {
                    // no-op for non-deposit/extract actions
                }
            }
        }

        List<Map.Entry<Long, long[]>> sortedHours = new ArrayList<>(hourMap.entrySet());
        sortedHours.sort(Map.Entry.comparingByKey());

        if (sortedHours.size() > HOURLY_TREND_HOURS) {
            sortedHours = sortedHours.subList(sortedHours.size() - HOURLY_TREND_HOURS, sortedHours.size());
        }

        List<HourlyTrend> result = new ArrayList<>(sortedHours.size());
        for (Map.Entry<Long, long[]> hourEntry : sortedHours) {
            long[] c = hourEntry.getValue();
            result.add(new HourlyTrend(hourEntry.getKey(), c[0], c[1]));
        }
        return List.copyOf(result);
    }

    private static long safeAdd(long left, long right) {
        if (right <= 0L) {
            return left;
        }
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
