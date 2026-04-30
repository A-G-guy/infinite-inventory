package com.agguy.infiniteinventory.database.statistics;

/**
 * 小时趋势数据点，基于日志聚合。
 *
 * @param hourStartMillis 该小时开始时间戳
 * @param depositAmount   存入总量
 * @param extractAmount   取出总量
 */
public record HourlyTrend(
        long hourStartMillis,
        long depositAmount,
        long extractAmount
) {
}
