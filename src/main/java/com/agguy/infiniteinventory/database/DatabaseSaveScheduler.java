package com.agguy.infiniteinventory.database;

import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 将高频数据变更的磁盘 I/O 合并到 3 秒窗口内执行，兼顾数据安全与性能。 */
final class DatabaseSaveScheduler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long FORCE_SAVE_INTERVAL_MILLIS = 3000L;

    private boolean pending = false;
    private long lastSavedAtMillis = 0L;

    void request() {
        this.pending = true;
    }

    boolean trySave(MinecraftServer server) {
        if (!this.pending) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastSavedAtMillis < FORCE_SAVE_INTERVAL_MILLIS) {
            return false;
        }
        this.pending = false;
        this.lastSavedAtMillis = now;
        try {
            server.overworld().getDataStorage().save();
            return true;
        } catch (Exception e) {
            LOGGER.warn("后台保存数据库数据时发生异常", e);
            this.pending = true;
            return false;
        }
    }
}
