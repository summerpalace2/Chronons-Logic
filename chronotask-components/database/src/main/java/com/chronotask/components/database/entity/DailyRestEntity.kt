package com.chronotask.components.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 每日休息状态实体
 *
 * 作用：标记某一天是否为休息日。用于"每日休息"功能，
 * 让用户可以在特定日期暂停计时统计，不影响连续记录。
 *
 * 注意：使用 date 作为主键（不是自增ID），确保每天只有一条记录，
 * 同一天重复插入时会覆盖（与 DAO 的 REPLACE 策略配合）。
 */
@Entity(tableName = "daily_rest")
data class DailyRestEntity(
    /** 当天零点的时间戳，主键（每天唯一一条记录） */
    @PrimaryKey
    val date: Long,

    /** 是否设为休息日：true=休息日，false=正常工作日 */
    val isRestDay: Boolean = false
)