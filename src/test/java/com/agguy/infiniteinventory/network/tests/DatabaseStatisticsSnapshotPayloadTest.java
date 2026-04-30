package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.statistics.CategoryBreakdown;
import com.agguy.infiniteinventory.database.statistics.DailyTrend;
import com.agguy.infiniteinventory.database.statistics.DatabaseStatisticsSnapshot;
import com.agguy.infiniteinventory.database.statistics.HourlyTrend;
import com.agguy.infiniteinventory.database.statistics.NamespaceBreakdown;
import com.agguy.infiniteinventory.database.statistics.TabBreakdown;
import com.agguy.infiniteinventory.network.DatabaseStatisticsSnapshotPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * DatabaseStatisticsSnapshotPayload 序列化对称性测试。
 */
class DatabaseStatisticsSnapshotPayloadTest {

    @Test
    void emptySnapshotShouldRoundTripThroughStreamCodec() {
        DatabaseStatisticsSnapshot snapshot = new DatabaseStatisticsSnapshot(
                DatabaseScope.PERSONAL, 0, 0,
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
        DatabaseStatisticsSnapshotPayload payload = new DatabaseStatisticsSnapshotPayload(snapshot);

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DatabaseStatisticsSnapshotPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseStatisticsSnapshotPayload restored = DatabaseStatisticsSnapshotPayload.STREAM_CODEC.decode(buffer);

        DatabaseStatisticsSnapshot rs = restored.snapshot();
        assertEquals(DatabaseScope.PERSONAL, rs.scope());
        assertEquals(0, rs.totalEntries());
        assertEquals(0, rs.totalItems());
        assertNotNull(rs.categoryBreakdowns());
        assertNotNull(rs.namespaceBreakdowns());
        assertNotNull(rs.tabBreakdowns());
        assertNotNull(rs.dailyTrends());
        assertNotNull(rs.hourlyTrends());
    }

    @Test
    void fullSnapshotShouldRoundTripThroughStreamCodec() {
        List<CategoryBreakdown> categories = List.of(
                new CategoryBreakdown(DatabaseCategory.BLOCKS, 5, 50, 25.0),
                new CategoryBreakdown(DatabaseCategory.TOOLS_WEAPONS, 3, 30, 15.0)
        );
        List<NamespaceBreakdown> namespaces = List.of(
                new NamespaceBreakdown("minecraft", 10, 100, 50.0),
                new NamespaceBreakdown("modb", 5, 50, 25.0)
        );
        List<TabBreakdown> tabs = List.of(
                new TabBreakdown("default", "Default", 15, 150, 75.0)
        );
        List<DailyTrend> daily = List.of(
                new DailyTrend(1_700_000_000_000L, 5, 50, 2, 20, 1, 0)
        );
        List<HourlyTrend> hourly = List.of(
                new HourlyTrend(1_700_000_000_000L, 10, 5)
        );

        DatabaseStatisticsSnapshot snapshot = new DatabaseStatisticsSnapshot(
                DatabaseScope.PUBLIC, 20, 200,
                categories, namespaces, tabs, daily, hourly
        );
        DatabaseStatisticsSnapshotPayload payload = new DatabaseStatisticsSnapshotPayload(snapshot);

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DatabaseStatisticsSnapshotPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseStatisticsSnapshotPayload restored = DatabaseStatisticsSnapshotPayload.STREAM_CODEC.decode(buffer);

        DatabaseStatisticsSnapshot rs = restored.snapshot();
        assertEquals(DatabaseScope.PUBLIC, rs.scope());
        assertEquals(20, rs.totalEntries());
        assertEquals(200, rs.totalItems());

        assertEquals(2, rs.categoryBreakdowns().size());
        CategoryBreakdown cb = rs.categoryBreakdowns().get(0);
        assertEquals(DatabaseCategory.BLOCKS, cb.category());
        assertEquals(5, cb.entryCount());
        assertEquals(50, cb.itemCount());
        assertEquals(25.0, cb.percentage(), 0.001);

        assertEquals(2, rs.namespaceBreakdowns().size());
        NamespaceBreakdown nb = rs.namespaceBreakdowns().get(1);
        assertEquals("modb", nb.namespace());
        assertEquals(5, nb.entryCount());
        assertEquals(50, nb.itemCount());
        assertEquals(25.0, nb.percentage(), 0.001);

        assertEquals(1, rs.tabBreakdowns().size());
        TabBreakdown tb = rs.tabBreakdowns().get(0);
        assertEquals("default", tb.tabId());
        assertEquals("Default", tb.tabDisplayName());
        assertEquals(15, tb.entryCount());
        assertEquals(150, tb.itemCount());
        assertEquals(75.0, tb.percentage(), 0.001);

        assertEquals(1, rs.dailyTrends().size());
        DailyTrend dt = rs.dailyTrends().get(0);
        assertEquals(1_700_000_000_000L, dt.dayStartMillis());
        assertEquals(5, dt.depositCount());
        assertEquals(50, dt.depositAmount());
        assertEquals(2, dt.extractCount());
        assertEquals(20, dt.extractAmount());
        assertEquals(1, dt.transferCount());
        assertEquals(0, dt.deleteCount());

        assertEquals(1, rs.hourlyTrends().size());
        HourlyTrend ht = rs.hourlyTrends().get(0);
        assertEquals(1_700_000_000_000L, ht.hourStartMillis());
        assertEquals(10, ht.depositAmount());
        assertEquals(5, ht.extractAmount());
    }

    @Test
    void typeShouldReturnCorrectType() {
        DatabaseStatisticsSnapshot snapshot = new DatabaseStatisticsSnapshot(
                DatabaseScope.PERSONAL, 0, 0,
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
        assertEquals(DatabaseStatisticsSnapshotPayload.TYPE,
                new DatabaseStatisticsSnapshotPayload(snapshot).type());
    }
}
