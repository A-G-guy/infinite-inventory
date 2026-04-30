package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.statistics.CategoryBreakdown;
import com.agguy.infiniteinventory.database.statistics.DailyTrend;
import com.agguy.infiniteinventory.database.statistics.DatabaseStatisticsSnapshot;
import com.agguy.infiniteinventory.database.statistics.HourlyTrend;
import com.agguy.infiniteinventory.database.statistics.NamespaceBreakdown;
import com.agguy.infiniteinventory.database.statistics.TabBreakdown;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端向客户端返回统计快照 (S2C)。
 * 复用日志快照的模式。
 */
public record DatabaseStatisticsSnapshotPayload(DatabaseStatisticsSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<DatabaseStatisticsSnapshotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "database_statistics_snapshot")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseStatisticsSnapshotPayload> STREAM_CODEC =
            StreamCodec.of(DatabaseStatisticsSnapshotPayload::write, DatabaseStatisticsSnapshotPayload::read);

    @Override
    public Type<DatabaseStatisticsSnapshotPayload> type() {
        return TYPE;
    }

    private static DatabaseStatisticsSnapshotPayload read(RegistryFriendlyByteBuf buffer) {
        DatabaseScope scope = DatabaseScope.read(buffer.readUtf(), DatabaseScope.PERSONAL);
        long totalEntries = buffer.readVarLong();
        long totalItems = buffer.readVarLong();

        List<CategoryBreakdown> categoryBreakdowns = readCategoryBreakdowns(buffer);
        List<NamespaceBreakdown> namespaceBreakdowns = readNamespaceBreakdowns(buffer);
        List<TabBreakdown> tabBreakdowns = readTabBreakdowns(buffer);
        List<DailyTrend> dailyTrends = readDailyTrends(buffer);
        List<HourlyTrend> hourlyTrends = readHourlyTrends(buffer);

        DatabaseStatisticsSnapshot snapshot = new DatabaseStatisticsSnapshot(
                scope, totalEntries, totalItems,
                categoryBreakdowns, namespaceBreakdowns, tabBreakdowns,
                dailyTrends, hourlyTrends
        );
        return new DatabaseStatisticsSnapshotPayload(snapshot);
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseStatisticsSnapshotPayload payload) {
        DatabaseStatisticsSnapshot s = payload.snapshot();
        buffer.writeUtf(s.scope().name());
        buffer.writeVarLong(s.totalEntries());
        buffer.writeVarLong(s.totalItems());
        writeCategoryBreakdowns(buffer, s.categoryBreakdowns());
        writeNamespaceBreakdowns(buffer, s.namespaceBreakdowns());
        writeTabBreakdowns(buffer, s.tabBreakdowns());
        writeDailyTrends(buffer, s.dailyTrends());
        writeHourlyTrends(buffer, s.hourlyTrends());
    }

    private static List<CategoryBreakdown> readCategoryBreakdowns(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        NetworkConstants.checkListSize(count, NetworkConstants.MAX_STATISTICS_CATEGORY_COUNT, "categoryBreakdowns");
        List<CategoryBreakdown> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String categoryName = buffer.readUtf(64);
            long entryCount = buffer.readVarLong();
            long itemCount = buffer.readVarLong();
            double percentage = buffer.readDouble();
            result.add(new CategoryBreakdown(
                    com.agguy.infiniteinventory.database.DatabaseCategory.valueOf(categoryName),
                    entryCount, itemCount, percentage
            ));
        }
        return List.copyOf(result);
    }

    private static void writeCategoryBreakdowns(RegistryFriendlyByteBuf buffer, List<CategoryBreakdown> list) {
        buffer.writeVarInt(list.size());
        for (CategoryBreakdown b : list) {
            buffer.writeUtf(b.category().name(), 64);
            buffer.writeVarLong(b.entryCount());
            buffer.writeVarLong(b.itemCount());
            buffer.writeDouble(b.percentage());
        }
    }

    private static List<NamespaceBreakdown> readNamespaceBreakdowns(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        NetworkConstants.checkListSize(count, NetworkConstants.MAX_STATISTICS_NAMESPACE_COUNT, "namespaceBreakdowns");
        List<NamespaceBreakdown> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String namespace = buffer.readUtf(128);
            long entryCount = buffer.readVarLong();
            long itemCount = buffer.readVarLong();
            double percentage = buffer.readDouble();
            result.add(new NamespaceBreakdown(namespace, entryCount, itemCount, percentage));
        }
        return List.copyOf(result);
    }

    private static void writeNamespaceBreakdowns(RegistryFriendlyByteBuf buffer, List<NamespaceBreakdown> list) {
        buffer.writeVarInt(list.size());
        for (NamespaceBreakdown b : list) {
            buffer.writeUtf(b.namespace(), 128);
            buffer.writeVarLong(b.entryCount());
            buffer.writeVarLong(b.itemCount());
            buffer.writeDouble(b.percentage());
        }
    }

    private static List<TabBreakdown> readTabBreakdowns(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        NetworkConstants.checkListSize(count, NetworkConstants.MAX_STATISTICS_TAB_COUNT, "tabBreakdowns");
        List<TabBreakdown> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String tabId = buffer.readUtf(128);
            String tabDisplayName = buffer.readUtf(256);
            long entryCount = buffer.readVarLong();
            long itemCount = buffer.readVarLong();
            double percentage = buffer.readDouble();
            result.add(new TabBreakdown(tabId, tabDisplayName, entryCount, itemCount, percentage));
        }
        return List.copyOf(result);
    }

    private static void writeTabBreakdowns(RegistryFriendlyByteBuf buffer, List<TabBreakdown> list) {
        buffer.writeVarInt(list.size());
        for (TabBreakdown b : list) {
            buffer.writeUtf(b.tabId(), 128);
            buffer.writeUtf(b.tabDisplayName(), 256);
            buffer.writeVarLong(b.entryCount());
            buffer.writeVarLong(b.itemCount());
            buffer.writeDouble(b.percentage());
        }
    }

    private static List<DailyTrend> readDailyTrends(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        NetworkConstants.checkListSize(count, NetworkConstants.MAX_STATISTICS_DAILY_TREND_COUNT, "dailyTrends");
        List<DailyTrend> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long dayStartMillis = buffer.readVarLong();
            long depositCount = buffer.readVarLong();
            long depositAmount = buffer.readVarLong();
            long extractCount = buffer.readVarLong();
            long extractAmount = buffer.readVarLong();
            long transferCount = buffer.readVarLong();
            long deleteCount = buffer.readVarLong();
            result.add(new DailyTrend(dayStartMillis, depositCount, depositAmount, extractCount, extractAmount, transferCount, deleteCount));
        }
        return List.copyOf(result);
    }

    private static void writeDailyTrends(RegistryFriendlyByteBuf buffer, List<DailyTrend> list) {
        buffer.writeVarInt(list.size());
        for (DailyTrend t : list) {
            buffer.writeVarLong(t.dayStartMillis());
            buffer.writeVarLong(t.depositCount());
            buffer.writeVarLong(t.depositAmount());
            buffer.writeVarLong(t.extractCount());
            buffer.writeVarLong(t.extractAmount());
            buffer.writeVarLong(t.transferCount());
            buffer.writeVarLong(t.deleteCount());
        }
    }

    private static List<HourlyTrend> readHourlyTrends(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        NetworkConstants.checkListSize(count, NetworkConstants.MAX_STATISTICS_HOURLY_TREND_COUNT, "hourlyTrends");
        List<HourlyTrend> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long hourStartMillis = buffer.readVarLong();
            long depositAmount = buffer.readVarLong();
            long extractAmount = buffer.readVarLong();
            result.add(new HourlyTrend(hourStartMillis, depositAmount, extractAmount));
        }
        return List.copyOf(result);
    }

    private static void writeHourlyTrends(RegistryFriendlyByteBuf buffer, List<HourlyTrend> list) {
        buffer.writeVarInt(list.size());
        for (HourlyTrend t : list) {
            buffer.writeVarLong(t.hourStartMillis());
            buffer.writeVarLong(t.depositAmount());
            buffer.writeVarLong(t.extractAmount());
        }
    }
}
