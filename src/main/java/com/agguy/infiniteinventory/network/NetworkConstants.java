package com.agguy.infiniteinventory.network;

/**
 * 网络 Payload 反序列化的安全边界常量。
 *
 * <p>防止恶意或损坏的 Payload 通过超大列表导致服务端/客户端 OOM。</p>
 */
public final class NetworkConstants {
    private NetworkConstants() {
    }

    /** 选择条目列表的最大长度（{@code DatabaseSelectionPayload}）。 */
    public static final int MAX_SELECTION_ENTRY_COUNT = 10000;
    /** 物品堆栈列表的最大长度（{@code DatabaseNotePayload}、{@code DatabaseStarPayload}）。 */
    public static final int MAX_STACK_LIST_COUNT = 1000;
    /** 面板视图的最大数量（{@code DatabaseViewState}）。 */
    public static final int MAX_PANEL_COUNT = 100;
    /** 标签页列表的最大数量（{@code DatabaseViewState.readTabs}）。 */
    public static final int MAX_TAB_COUNT = 1000;
    /** 日志条目列表的最大数量（{@code DatabaseLogSnapshotPayload}）。 */
    public static final int MAX_LOG_ENTRY_COUNT = 1000;
    /** 数量同步条目列表的最大数量（{@code DatabaseAmountSyncPayload}）。 */
    public static final int MAX_AMOUNT_SYNC_ENTRY_COUNT = 100000;
    /** 增量数量同步条目列表的最大数量（{@code DatabaseAmountDeltaSyncPayload}）。 */
    public static final int MAX_AMOUNT_DELTA_SYNC_ENTRY_COUNT = 100000;
    /** Query 中可见标签页的最大数量。 */
    public static final int MAX_QUERY_VISIBLE_TAB_COUNT = 16;
    /** Query 中标签状态的最大数量。 */
    public static final int MAX_QUERY_TAB_STATE_COUNT = 100;
    /** Query 中隐藏顶部标签页的最大数量。 */
    public static final int MAX_QUERY_HIDDEN_TOP_TAB_COUNT = 100;
    /** 统计快照中分类 breakdown 的最大数量。 */
    public static final int MAX_STATISTICS_CATEGORY_COUNT = 16;
    /** 统计快照中命名空间 breakdown 的最大数量。 */
    public static final int MAX_STATISTICS_NAMESPACE_COUNT = 32;
    /** 统计快照中标签页 breakdown 的最大数量。 */
    public static final int MAX_STATISTICS_TAB_COUNT = 48;
    /** 统计快照中日趋势的最大数量。 */
    public static final int MAX_STATISTICS_DAILY_TREND_COUNT = 16;
    /** 统计快照中小时趋势的最大数量。 */
    public static final int MAX_STATISTICS_HOURLY_TREND_COUNT = 32;

    /**
     * 校验列表长度是否在安全范围内。
     *
     * @param count     从缓冲区读取的列表长度
     * @param maxCount  允许的最大长度
     * @param fieldName 字段名称，用于异常信息
     * @throws IllegalStateException 若长度为负或超过上限
     */
    public static void checkListSize(int count, int maxCount, String fieldName) {
        if (count < 0 || count > maxCount) {
            throw new IllegalStateException("Payload list size for " + fieldName + " out of bounds: " + count + " (max " + maxCount + ")");
        }
    }
}
