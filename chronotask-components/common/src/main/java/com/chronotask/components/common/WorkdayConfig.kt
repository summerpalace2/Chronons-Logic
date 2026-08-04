package com.chronotask.components.common

import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * 工作日配置工具类
 * 提供工作日判断相关功能
 */
object WorkdayConfig {
    /**
     * 判断指定日期是否为工作日
     * @param date 日期毫秒时间戳
     * @return true为工作日，false为休息日
     */
    suspend fun isWorkDay(date: Long): Boolean {
        // 获取工作日模式是否开启
        val enabled = appDataStore.workdayEnabled.first()
        if (!enabled) return true // 工作日模式未开启，默认都是工作日
        // 获取工作日掩码
        val mask = appDataStore.workdayWeekMask.first()
        val cal = Calendar.getInstance().apply { timeInMillis = date }
        val dayIndex = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=周日
        return (mask and (1 shl dayIndex)) != 0
    }
}
