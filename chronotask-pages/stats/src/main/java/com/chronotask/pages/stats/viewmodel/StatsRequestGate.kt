package com.chronotask.pages.stats.viewmodel

/**
 * 统计读取的最后写入保护。
 *
 * StatsViewModel 在主线程上创建请求，因此不需要额外同步；协程恢复后只允许最新
 * 请求发布状态，避免慢查询在被取消后仍覆盖用户后来选择的周期。
 */
internal class StatsRequestGate {
    private var latestRequestId = 0L

    fun beginRequest(): Long = ++latestRequestId

    fun isLatest(requestId: Long): Boolean = requestId == latestRequestId
}
