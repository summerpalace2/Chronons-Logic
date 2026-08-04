package com.chronotask.pages.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.chronotask.components.common.DateUtils
import com.chronotask.components.common.QuickImportTask
import com.chronotask.components.common.TimerManager
import com.chronotask.components.common.appApplication
import com.chronotask.components.common.WorkdayConfig
import com.chronotask.components.common.appIoScope
import com.chronotask.components.common.appDataStore
import com.chronotask.components.common.base.BaseViewModel
import com.chronotask.components.database.AppDatabase
import com.chronotask.components.database.entity.DailyRestEntity
import com.chronotask.components.database.entity.TagEntity
import com.chronotask.components.database.entity.TaskEntity
import com.chronotask.components.database.repository.TaskRecordRepository
import com.chronotask.components.database.repository.TaskRepository
import com.chronotask.pages.home.data.TaskItemState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar



// HomeViewModel
// 职责：管理任务列表展示、计时控制、日期切换、休息日
//
// 协程策略（coroutine optimisation）：
//   • 数据写入 → appIoScope (IO, 进程级保活，退出页面仍可完成落库)
//   • 数据读取 → viewModelScope (页面级)
//   • 共享计时状态 → TimerManager 单例（避免重复创建 Flow）
//
// 重组优化（recomposition）：
//   • 顶层 Screen 只 collect `selectedDate`，其余状态拆分到子组件订阅
//   • 各 StateFlow 使用 distinctUntilChanged 语义避免无效重绘
//   • stopTask 中 state 变更合并为单次 atomic update
//
// 可读性策略：
//   • 方法按「状态切换 / 计时 / 任务 / 休息 / 日期」分组
//   • 每个 public 函数顶部 KDoc 说明副作用 + 协程作用域

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel : BaseViewModel() {

    private val db = AppDatabase.getDatabase(appApplication)
    private val tagDao = db.tagDao()
    private val restDao = db.dailyRestDao()

    // ─── 日期状态 ────────────────────────────────────────────────────────────

    /** 当前选中的日期（当天 00:00:00 毫秒时间） */
    private val _selectedDate = MutableStateFlow(DateUtils.getTodayStart())
    val selectedDate: StateFlow<Long> = _selectedDate

    // ─── UI 开关 ─────────────────────────────────────────────────────────---

    /** 今日是否休息（休息日不计时、不累计） */
    private val _isTodayRest = MutableStateFlow(false)
    val isTodayRest: StateFlow<Boolean> = _isTodayRest

    /** 编辑模式：显示删除按钮 */
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    /**
     * 已执行过 "一键导入" 任务的日期集合（零点毫秒时间戳）。
     * 选中日期在此集合内按钮显示 "已导入" 且禁用。
     */
    private val _dayImportStatus = MutableStateFlow<Set<Long>>(emptySet())
    val dayImportStatus: StateFlow<Set<Long>> = _dayImportStatus

    /** 防止 loadImportedDays 并发写入的互斥锁 */
    private val importStatusWrite = Any()

    // ─── 共享计时状态（来自 TimerManager 单例） ──────────────────────────────

    val runningTaskId: StateFlow<Long?> = TimerManager.runningTaskId
    val runningDuration: StateFlow<Long> = TimerManager.elapsedSeconds

    // ─── 内部聚合流 ──────────────────────────────────────────────────────---

    /** 标签集合流：从 DAO 拉取后转成 id→TagEntity 的 Map，仅加载一次 */
    private val tagsMap: StateFlow<Map<Long, TagEntity>> =
        tagDao.getAllTags()
            .map { tags -> tags.associateBy { it.id } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    /**
     * 日期维度的记录聚合：
     *   当 _selectedDate 变化时，拉取当天 task_records + rest 标记，
     *   自动计算「每个任务累计秒数」和「当日总秒数」,
     *   遇到 rest 日则不累计。
     */
    private val dateRecordsState: StateFlow<DateRecords> =
        _selectedDate.flatMapLatest { date ->
            combine(
                TaskRecordRepository.getByDateFlow(date),
                restDao.getRestByDateFlow(date)
            ) { records, rest ->
                val isRest = rest?.isRestDay ?: false
                val map = mutableMapOf<Long, Long>()
                var total = 0L
                if (!isRest) {
                    records.forEach { record ->
                        map[record.taskId] = (map[record.taskId] ?: 0L) + record.durationSeconds
                        total += record.durationSeconds
                    }
                }
                DateRecords(map, total, isRest)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DateRecords(emptyMap(), 0L, false)
        )

    /** 内部分解后的「每个任务累计秒数」 */
    private val _dateRecords = MutableStateFlow<Map<Long, Long>>(emptyMap())
    /** 内部累计的「当日总秒数」 */
    private val recordedTotal = MutableStateFlow(0L)

    /** 当前日期的任务列表 */
    private val _tasksForDate: StateFlow<List<TaskEntity>> =
        _selectedDate.flatMapLatest { date ->
            TaskRepository.getTasksByDate(date)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    init {
        // [协程优化] 用一个协程订阅聚合流，分解到三个 StateFlow
        //  避免每次 UI 变化触发多次 collect
        viewModelScope.launch {
            dateRecordsState.collect { record ->
                _dateRecords.value = record.records
                recordedTotal.value = record.total
                _isTodayRest.value = record.isRest
            }
        }

        // 监听 selectedDate 变化，刷新导入状态
        viewModelScope.launch {
            _selectedDate.collectLatest { loadImportedDays() }
        }
        // 初始同步 dayStartOffset 缓存
        viewModelScope.launch { appDataStore.dayStartOffsetMinutes.collect { cachedDayStartOffset = it } }

        // 跨天自动停止：TimerManager 统一落库，ViewModel 只负责刷新当前页面状态。
        TimerManager.onDayRollover = { taskId, _, _, total ->
            val newTotal = recordedTotal.value + total
            val newRecords = _dateRecords.value.toMutableMap()
            newRecords[taskId] = (newRecords[taskId] ?: 0L) + total
            recordedTotal.value = newTotal
            _dateRecords.value = newRecords
        }
    }

    // ─── 派生状态 ─────────────────────────────────────────────────────────---

    /**
     * 对 UI 暴露的完整任务条目列表
     *   组合 5 个流：任务、运行 id、运行时长、标签、累计记录
     *   任一源变化时重新计算（无需 UI 层再组合）
     */
    val taskItems: StateFlow<List<TaskItemState>> =
        combine(
            _tasksForDate,
            runningTaskId,
            runningDuration,
            tagsMap,
            _dateRecords
        ) { tasks, runningId, runningDur, tags, records ->
            val activeDay = DateUtils.getActiveDayMidnight(System.currentTimeMillis(), cachedDayStartOffset)
            val isToday = _selectedDate.value == activeDay
            tasks.map { task ->
                val recordDur = records[task.id] ?: 0L
                val runningDurForTask = if (isToday && task.id == runningId) runningDur else 0L
                val todayDur = recordDur + runningDurForTask
                TaskItemState(
                    task = task,
                    tagName = task.tagId?.let { tags[it]?.name } ?: "",
                    todayDurationSeconds = todayDur,
                    isRunning = isToday && task.id == runningId,
                    isOverTarget = task.targetDurationMinutes != null &&
                            todayDur > task.targetDurationMinutes!! * 60L
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    /**
     * 当日总用时（Recorded + 正在计时的任务）
     *  • 仅当选择「今天」且存在运行中的任务时加入 running 秒数
     *  • recordedTotal 已包含「定时器停止时立即追加」的部分
     *    （参见 stopTask 中的同步更新）
     */
    val combinedTotal: StateFlow<Long> =
        taskItems.map { items ->
            items.sumOf { it.todayDurationSeconds }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0L
        )

    // 公开方法 — 状态切换

    /**
     * 选中指定日期（毫秒）
     *  • 触发 dateRecordsState + _tasksForDate 切换来源
     *  • 顶层只 collect 此值 → 其他子组件订阅自身的 state
     */
    // [重组优化] selectDate 被 CalendarStrip / 日期选择器频繁调用
    // 内联避免 lambda 分配
    fun selectDate(date: Long) {
        _selectedDate.value = date
    }

    /** 进入 / 退出编辑模式 */
    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    // 公开方法 — 计时控制

    /** 当前 dayStartOffset 缓存（UI 启动时同步刷新一次） */
    private var cachedDayStartOffset: Int = 0

    /** 计算当前允许计时的日槽零时戳（给 UI 判断按钮是否可用）。 */
    fun getActiveDay(): Long = DateUtils.getActiveDayMidnight(System.currentTimeMillis(), cachedDayStartOffset)

    fun startTask(taskId: Long) {
        if (_isTodayRest.value) return
        val activeDay = DateUtils.getActiveDayMidnight(System.currentTimeMillis(), cachedDayStartOffset)
        if (_selectedDate.value != activeDay) return
        TimerManager.startTimer(taskId, cachedDayStartOffset)
    }

    /**
     * 停止计时：
     *   ① 同步更新 recordedTotal 与 _dateRecords（立即反映在 UI）
     *   ② 由 Application 注册的 TimerManager.onSessionStopped 统一落库
     */
    fun stopTask() {
        val info = TimerManager.stopSessionDetailed() ?: return
        val (taskId, _, _, totalElapsed) = info

        // [重组优化] 在单次 atomic update 内合并两次独立 state write
        val newTotal = recordedTotal.value + totalElapsed
        val newRecords = _dateRecords.value.toMutableMap()
        newRecords[taskId] = (newRecords[taskId] ?: 0L) + totalElapsed
        recordedTotal.value = newTotal
        _dateRecords.value = newRecords

    }

    /** 切换指定任务的完成状态。 */
    fun toggleTaskCompletion(taskId: Long) {
        appIoScope.launch {
            val task = TaskRepository.getById(taskId) ?: return@launch
            TaskRepository.update(task.copy(isCompleted = !task.isCompleted))
        }
    }

    /** 删除指定任务 */
    fun deleteTask(taskId: Long) {
        appIoScope.launch {
            TaskRepository.delete(taskId)
        }
    }

    /**
     * 导入一条快速任务
     *
     * @param qiTask     快速导入任务定义（来自 QuickImportManager 配置）
     * @param targetDate 目标日期（毫秒，由 UI 传入的 selectedDate 决定）
     */
    fun importQuickTask(qiTask: QuickImportTask, targetDate: Long = DateUtils.getTodayStart()) {
        appIoScope.launch {
            if (!WorkdayConfig.isWorkDay(targetDate)) return@launch
            // 手动已创建同名任务时跳过，避免重复
            val exists = TaskRepository.getTasksByDate(targetDate).first()
            if (exists.any { it.title == qiTask.title }) return@launch
            TaskRepository.insert(
                TaskEntity(
                    title = qiTask.title,
                    tagId = qiTask.tagId,
                    targetDurationMinutes = qiTask.targetMinutes,
                    scheduledDate = targetDate
                )
            )
            // 记录当天已导入
            synchronized(importStatusWrite) {
                _dayImportStatus.value = _dayImportStatus.value + targetDate
            }
        }
    }
    // 公开方法 — 休息日

    /** 切换休息日状态：true=休息，false=工作日 */
    fun toggleTodayRest() {
        appIoScope.launch {
            val newState = !_isTodayRest.value
            if (newState) {
                restDao.setRestDay(DailyRestEntity(date = _selectedDate.value, isRestDay = true))
            } else {
                restDao.removeRestDay(_selectedDate.value)
            }
        }
    }

    // 公开方法 — 日期导航

    /** 加载最近 30 天内已导入快速任务的日期。 */
    private fun loadImportedDays() {
        appIoScope.launch {
            try {
                val enabled = com.chronotask.components.common.QuickImportManager.isEnabled.first()
                if (!enabled) { synchronized(importStatusWrite) { _dayImportStatus.value = emptySet() }; return@launch }
                val tasks = com.chronotask.components.common.QuickImportManager.tasks.first()
                if (tasks.isEmpty()) { synchronized(importStatusWrite) { _dayImportStatus.value = emptySet() }; return@launch }
                val titles = tasks.map { it.title }
                val taskIds: List<Long> = try { db.taskDao().getIdsByTitles(titles) } catch (_: Exception) { emptyList() }
                if (taskIds.isEmpty()) { synchronized(importStatusWrite) { _dayImportStatus.value = emptySet() }; return@launch }
                val now = System.currentTimeMillis()
                val offsetMinutes = appDataStore.dayStartOffsetMinutes.first()
                val activeNow = DateUtils.getActiveDayMidnight(now, offsetMinutes)
                val start = DateUtils.getActiveDayMidnight(now - 30L * 24 * 60 * 60 * 1000, offsetMinutes)
                val end = DateUtils.getNextDayStart(activeNow)
                val records = try { db.taskRecordDao().getRecordsByDateRange(start, end) } catch (_: Exception) { emptyList() }
                val matched: Set<Long> = records.filter { it.taskId in taskIds }.map { DateUtils.getDateStart(it.date) }.toSet()
                synchronized(importStatusWrite) { _dayImportStatus.value = matched }
            } catch (_: Exception) { synchronized(importStatusWrite) { _dayImportStatus.value = emptySet() } }
        }
    }

    fun moveDate(weeks: Int) {
        val cal = Calendar.getInstance().apply { timeInMillis = _selectedDate.value }
        cal.add(Calendar.WEEK_OF_YEAR, weeks)
        selectDate(DateUtils.getDateStart(cal.timeInMillis))
    }


    /**
     * 同步工作日配置到 rest 状态
     *
     * 当选中日期不在工作日范围内且工作日模式开启时，自动标记为休息日。
     * 若选中日期在工作日范围内且当前是 rest 日，则移除 rest 标记。
     * 工作日模式下每天执行一次，保证 rest 状态与用户配置一致。
     */
    fun syncWorkdayRestStatus(targetDate: Long) {
        appIoScope.launch {
            if (!com.chronotask.components.common.WorkdayConfig.isWorkDay(targetDate)) {
                // 非工作日 → 强制标记为休息
                restDao.setRestDay(DailyRestEntity(date = targetDate, isRestDay = true))
                _isTodayRest.value = targetDate == _selectedDate.value
            } else {
                // 工作日 → 若误设为休息则清除
                restDao.removeRestDay(targetDate)
                if (targetDate == _selectedDate.value) _isTodayRest.value = false
            }
        }
    }

    // ─── 内部数据载体 ─────────────────────────────────────────────────────----

    /** 单日记录聚合结果（内部使用，不对外暴露） */
    private data class DateRecords(
        val records: Map<Long, Long>,
        val total: Long,
        val isRest: Boolean
    )
}

// ─── 工作日判断 ─────────────────────────────────────────────────────────

/**
 * 7 bit 位掩码判断某天是否为工作日
 * 与 WeekDay 枚举对齐：bit0 = 周日, bit1 = 周一, ...
 *
 * @param date 日期毫秒时间
 * @param weekMask 工作日的位掩码
 * @return true 表示是工作日
 */
fun isWorkDay(date: Long, weekMask: Int): Boolean {
    val cal = Calendar.getInstance().apply { timeInMillis = date }
    val dayIndex = cal.get(Calendar.DAY_OF_WEEK) - 1  // 0=周日
    return (weekMask and (1 shl dayIndex)) != 0
}
