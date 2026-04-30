package com.agguy.infiniteinventory.database.statistics;

/**
 * 日趋势数据点，基于日志聚合。
 *
 * @param dayStartMillis  当天0点的时间戳
 * @param depositCount    存入次数
 * @param depositAmount   存入总量
 * @param extractCount    取出次数
 * @param extractAmount   取出总量
 * @param transferCount   转移次数
 * @param deleteCount     删除次数
 */
public record DailyTrend(
        long dayStartMillis,
        long depositCount,
        long depositAmount,
        long extractCount,
        long extractAmount,
        long transferCount,
        long deleteCount
) {
}
