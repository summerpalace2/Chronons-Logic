package com.chronotask.pages.taskdetail.viewmodel

import androidx.lifecycle.viewModelScope
import com.chronotask.components.common.base.BaseViewModel
import com.chronotask.components.common.TimerManager
import com.chronotask.components.common.QuickImportManager
import com.chronotask.components.common.QuickImportTask
import com.chronotask.components.common.DateUtils
import com.chronotask.components.common.appIoScope
import com.chronotask.components.common.appDataStore
import com.chronotask.components.database.AppDatabase
import com.chronotask.components.database.entity.NoteHistoryEntity
import com.chronotask.components.database.entity.TaskEntity
import com.chronotask.components.database.repository.NoteHistoryRepository
import com.chronotask.components.database.repository.TaskRecordRepository
import com.chronotask.components.database.repository.TaskRepository
import com.chronotask.components.ui.R
import com.chronotask.components.ui.theme.LocaleManager
import com.chronotask.components.common.appApplication
import com.chronotask.pages.taskdetail.data.HistorySection
import com.chronotask.pages.taskdetail.data.TaskComparisonData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

/**
 * TaskDetailViewModel - 任务详情页状态管理
 *
 * 核心职责：管理任务计时、笔记编辑、历史记录、统计数据。
 *
 * 协程策略：
 * - 读操作（Flow 转换）使用 viewModelScope（与 ViewModel 生命周期绑定）
 * - 写操作（数据库增删改）使用 appIoScope（进程级，退出页面后仍可完成保存）
 */
class TaskDetailViewModel(
    private val taskId: Long,
    private val recordDate: Long = DateUtils.getTodayStart()
) : BaseViewModel() {

    private val db = AppDatabase.getDatabase(appApplication)
    private val tagDao = db.tagDao()

    // ── 任务基础信息 ──────────────────────────────────────────

    private val _task = MutableStateFlow<TaskEntity?>(null)
    val task: StateFlow<TaskEntity?> = _task

    private val _tagName = MutableStateFlow("")
    val tagName: StateFlow<String> = _tagName

    // ── 计时状态 ──────────────────────────────────────────────

    val isRunning: StateFlow<Boolean> = TimerManager.runningTaskId
        .map { it == taskId }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val elapsedSeconds: StateFlow<Long> = TimerManager.elapsedSeconds

    private val _todayRecordSeconds = MutableStateFlow(0L)
    val todayRecordSeconds: StateFlow<Long> = _todayRecordSeconds

    // ── 笔记状态 ──────────────────────────────────────────────

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note

    private val _noteTitle = MutableStateFlow("")
    val noteTitle: StateFlow<String> = _noteTitle

    // ── 对比数据 ──────────────────────────────────────────────

    private val _comparisonData = MutableStateFlow(TaskComparisonData())
    val comparisonData: StateFlow<TaskComparisonData> = _comparisonData

    /**
     * 缓存一键导入任务列表（避免每次 loadComparisonData 都读 DataStore）
     * 在 init 中通过 first() 预加载，后续直接用这个缓存判断
     */
    private var cachedQuickImportTasks: List<QuickImportTask> = emptyList()

    // ── 笔记历史 ──────────────────────────────────────────────

    /**
     * 计时笔记历史记录（按时间倒序）
     */
    val noteHistory: StateFlow<List<NoteHistoryEntity>> = NoteHistoryRepository.getByTask(taskId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * 当前任务的最新笔记（取最新一条，若无则为 null）
     */
    val currentNote: StateFlow<NoteHistoryEntity?> = noteHistory
        .map { list -> list.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

        /**
     * locale 变化信号流
     * 每次 locale 切换时发射新值，使依赖 locale 的 Flow 重新计算
     */
    private val localeFlow = LocaleManager.currentLocale

    /**
     * 按日期分组的笔记历史（带分组标题）
     */
    val groupedNoteHistory: StateFlow<List<HistorySection>> = combine(
        noteHistory,
        localeFlow
    ) { historyList, _ ->
        val dateFormat = java.text.SimpleDateFormat(appApplication.getString(R.string.date_format_short), Locale.getDefault())
        val todayStart = DateUtils.getTodayStart()
        val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L
        historyList.groupBy { entity ->
            when {
                entity.date == todayStart -> appApplication.getString(R.string.today)
                entity.date == yesterdayStart -> appApplication.getString(R.string.yesterday)
                else -> dateFormat.format(java.util.Date(entity.date))
            }
        }.toList()
            .sortedByDescending { it.second.first().sessionStartTime }
            .map { (label, items) -> HistorySection(label, items) }
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── 内部状态 ──────────────────────────────────────────────

    private var currentRecordId: Long = 0L
    val isToday = recordDate == DateUtils.getTodayStart()


    private var noteSaveJob: kotlinx.coroutines.Job? = null
    private var lastSavedNote: String = ""

    companion object {
        private const val NOTE_SAVE_DELAY_MS = 800L
    }

    init {
        // 预缓存一键导入列表（同步等待 DataStore 首次 emit，避免后续 loadComparisonData 异步竞争）
        cachedQuickImportTasks = kotlinx.coroutines.runBlocking {
            QuickImportManager.tasks.first()
        }
        loadTaskInfo()
        loadTodayRecord()
        loadComparisonData()
    }

    // ── 数据加载 ──────────────────────────────────────────

    /**
     * 加载任务基础信息（标题 + 标签名）
     */
    private fun loadTaskInfo() {
        appIoScope.launch {
            val taskEntity = TaskRepository.getById(taskId)
            _task.value = taskEntity
            if (taskEntity != null) {
                val allTags = tagDao.getAllTagsSync()
                _tagName.value = allTags.firstOrNull { it.id == taskEntity.tagId }?.name ?: ""
            }
        }
    }

    /**
     * 加载今日计时记录
     */
    fun loadTodayRecord() {
        appIoScope.launch {
            val record = TaskRecordRepository.getByTaskAndDate(taskId, recordDate)
            _todayRecordSeconds.value = record?.durationSeconds ?: 0L
            _note.value = record?.note ?: ""
            currentRecordId = record?.id ?: 0L
        }
    }

    /**
     * 加载横向对比数据（本周/本月日均）
     */
    fun loadComparisonData() {
        appIoScope.launch {
            val cal = java.util.Calendar.getInstance()
            val todayMillis = System.currentTimeMillis()
            cal.timeInMillis = todayMillis
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val todayStart = cal.timeInMillis

            // 横向对比：与当前任务标题相同（一键导入类同标题）的所有任务做聚合均值
            val taskTitle = _task.value?.title
            val idsToAggregate: List<Long> = if (taskTitle.isNullOrBlank()) {
                listOf(taskId)
            } else {
                // 用 title 查找所有匹配任务 ID（含自身）
                val matched: List<Long> = try {
                    com.chronotask.components.database.AppDatabase
                        .getDatabase(com.chronotask.components.common.appApplication)
                        .taskDao()
                        .getIdsByTitles(listOf(taskTitle))
                } catch (_: Exception) {
                    emptyList()
                }
                matched.ifEmpty { listOf(taskId) }
            }

            val weekAvg = TaskRecordRepository.getWeekAverageByIds(idsToAggregate, todayStart)
            val monthAvg = TaskRecordRepository.getMonthAverageByIds(idsToAggregate, todayStart)

            _comparisonData.value = TaskComparisonData(
                weekAvgSeconds = weekAvg,
                monthAvgSeconds = monthAvg
            )
        }
    }

    fun startTimer() {
        viewModelScope.launch {
            val offset = appDataStore.dayStartOffsetMinutes.first()
            val activeDay = DateUtils.getActiveDayMidnight(System.currentTimeMillis(), offset)
            if (recordDate != activeDay) return@launch
            TimerManager.startTimer(taskId, offset)
        }
    }

    fun stopTimer() {
        val info = TimerManager.stopSessionDetailed() ?: return
        val totalElapsed = info.totalSeconds
        val startDay = info.sessionStartDay
        // [修复] 保留已有计时 + 本次会话秒数（previous + 当前 session）
        val previousSeconds = _todayRecordSeconds.value
        val newTotal = previousSeconds + totalElapsed
        // 先同步更新 UI，防止停止瞬间回弹
        _todayRecordSeconds.value = newTotal
        appIoScope.launch {
            val endMs = startDay + totalElapsed * 1000L
            TaskRecordRepository.saveTimerResultByDays(info.taskId, startDay, endMs)
            TimerManager.resetTimer()
        }
        loadComparisonData()
    }

    // ── 笔记操作（使用 appIoScope 保活） ──────────────────

    /**
     * 更新笔记内容（带 800ms 防抖保存）
     */
    fun updateNote(text: String) {
        _note.value = text
        noteSaveJob?.cancel()
        noteSaveJob = appIoScope.launch {
            kotlinx.coroutines.delay(NOTE_SAVE_DELAY_MS)
            TaskRecordRepository.updateNote(taskId, recordDate, text)
            lastSavedNote = text
        }
    }

    /**
     * 更新笔记标题
     */
    fun updateNoteTitle(id: Long, title: String) {
        appIoScope.launch {
            NoteHistoryRepository.updateTitle(id, title)
        }
    }

    /**
     * 保存任务笔记
     * 更新最新笔记的标题和内容；若无笔记则新建一条。
     */
    fun saveTaskNote(title: String, content: String) {
        appIoScope.launch {
            val existing = noteHistory.value.firstOrNull()
            if (existing != null) {
                NoteHistoryRepository.updateNoteAndTitle(existing.id, content, title)
            } else {
                NoteHistoryRepository.insertNote(
                    NoteHistoryEntity(
                        taskId = taskId,
                        date = recordDate,
                        sessionStartTime = System.currentTimeMillis(),
                        durationSeconds = 0,
                        note = content,
                        title = title,
                        sourceTaskTitle = _task.value?.title ?: ""
                    )
                )
            }
        }
    }

    /**
     * 替换当前计时（编辑模式：直接覆盖今日计时时长）
     * @param hours   小时
     * @param minutes 分钟
     */
    fun setElapsedTime(hours: Int, minutes: Int) {
        val totalSeconds = (hours * 3600 + minutes * 60).toLong()
        appIoScope.launch {
            val existingRecord = TaskRecordRepository.getByTaskAndDate(taskId, recordDate)
            if (existingRecord != null) {
                val updated = existingRecord.copy(durationSeconds = totalSeconds)
                TaskRecordRepository.update(updated)
            } else {
                TaskRecordRepository.insert(
                    com.chronotask.components.database.entity.TaskRecordEntity(
                        taskId = taskId,
                        date = recordDate,
                        durationSeconds = totalSeconds,
                        note = ""
                    )
                )
            }
            _todayRecordSeconds.value = totalSeconds
            loadComparisonData()
        }
    }

    /**
     * [保留] 手动累加计时（原 addManualTime 语义，暂未被 UI 调用）
     */
    @Suppress("unused")
    fun addManualTime(hours: Int, minutes: Int) {
        setElapsedTime(hours, minutes)  // 当前统一走替换语义
    }

    fun deleteNoteHistory(noteId: Long) {
        appIoScope.launch {
            NoteHistoryRepository.deleteNote(noteId)
        }
    }

    fun updateNoteHistory(noteId: Long, text: String) {
        appIoScope.launch {
            NoteHistoryRepository.updateNote(noteId, text)
        }
    }

    // ── 目标时长 ──────────────────────────────────────────

    fun updateTargetDuration(hours: Int, minutes: Int) {
        val totalMinutes = hours * 60 + minutes
        appIoScope.launch {
            val taskEntity = _task.value ?: return@launch
            val updatedTask = taskEntity.copy(
                targetDurationMinutes = if (totalMinutes > 0) totalMinutes else null
            )
            TaskRepository.update(updatedTask)
            _task.value = updatedTask
        }
    }

    // ── 生命周期 ──────────────────────────────────────────






    override fun onCleared() {
        super.onCleared()
        noteSaveJob?.cancel()
        val currentNote = _note.value
        if (currentNote != lastSavedNote) {
            appIoScope.launch {
                TaskRecordRepository.updateNote(taskId, recordDate, currentNote)
            }
        }
    }
}
