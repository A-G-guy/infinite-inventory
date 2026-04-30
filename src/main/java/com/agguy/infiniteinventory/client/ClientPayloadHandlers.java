package com.agguy.infiniteinventory.client;

import com.agguy.infiniteinventory.network.DatabaseAmountDeltaSyncPayload;
import com.agguy.infiniteinventory.network.DatabaseAmountSyncPayload;
import com.agguy.infiniteinventory.network.DatabaseDepositConflictPayload;
import com.agguy.infiniteinventory.network.DatabaseLogSnapshotPayload;
import com.agguy.infiniteinventory.network.DatabaseSnapshotPayload;
import com.agguy.infiniteinventory.network.DatabaseStatisticsSnapshotPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

/**
 * 客户端网络 Payload 处理器。
 *
 * <p>所有 {@code playToClient} 的 handler 必须放在此类中，不得直接写在
 * {@link com.agguy.infiniteinventory.network.ModNetwork} 里。原因是 {@code ModNetwork}
 * 属于 common（双端加载）代码，若直接引用 {@link Minecraft}，专用服务器在类初始化阶段就会
 * 因 {@code ClassNotFoundException: net.minecraft.client.Minecraft} 而崩溃。
 * </p>
 */
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    public static void handleSnapshot(DatabaseSnapshotPayload payload) {
        if (Minecraft.getInstance().player != null) {
            PersonalDatabaseClient.applySnapshot(payload.viewState());
        }
    }

    public static void handleAmountSync(DatabaseAmountSyncPayload payload) {
        DatabaseAmountCache.INSTANCE.update(
                toCacheEntries(payload.personalEntries()),
                toCacheEntries(payload.publicEntries())
        );
    }

    public static void handleAmountDeltaSync(DatabaseAmountDeltaSyncPayload payload) {
        DatabaseAmountCache.INSTANCE.applyDelta(
                toCacheDeltas(payload.personalDeltas()),
                toCacheDeltas(payload.publicDeltas())
        );
    }

    public static void handleLogSnapshot(DatabaseLogSnapshotPayload payload) {
        PersonalDatabaseClient.applyLogSnapshot(payload);
    }

    public static void handleDepositConflict(DatabaseDepositConflictPayload payload) {
        PersonalDatabaseClient.applyDepositConflict(payload);
    }

    public static void handleStatisticsSnapshot(DatabaseStatisticsSnapshotPayload payload) {
        PersonalDatabaseClient.applyStatisticsSnapshot(payload);
    }

    private static List<DatabaseAmountCache.Entry> toCacheEntries(List<DatabaseAmountSyncPayload.AmountEntry> entries) {
        return entries.stream()
                .map(e -> new DatabaseAmountCache.Entry(e.stack(), e.tabName(), e.amount()))
                .toList();
    }

    private static List<DatabaseAmountCache.Delta> toCacheDeltas(List<DatabaseAmountDeltaSyncPayload.DeltaEntry> deltas) {
        List<DatabaseAmountCache.Delta> result = new ArrayList<>(deltas.size());
        for (DatabaseAmountDeltaSyncPayload.DeltaEntry d : deltas) {
            result.add(new DatabaseAmountCache.Delta(d.stack(), d.tabName(), d.amount(), d.removed()));
        }
        return result;
    }
}
