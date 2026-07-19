package com.chronotask.pages.notes.viewmodel

import androidx.lifecycle.viewModelScope
import com.chronotask.components.common.DateUtils
import com.chronotask.components.common.appApplication
import com.chronotask.components.ui.theme.LocaleManager
import com.chronotask.components.common.appIoScope
import com.chronotask.components.common.base.BaseViewModel
import com.chronotask.components.database.entity.NoteHistoryEntity
import com.chronotask.components.database.repository.NoteHistoryRepository
import com.chronotask.components.ui.R
import com.chronotask.pages.notes.data.NotesSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * NotesViewModel - 笔记模块状态管理
 *
 * 核心职责：管理笔记列表展示、搜索筛选、增删改。
 *
 * 协程策略：
 * - 读操作（Flow 转换）使用 viewModelScope（与 ViewModel 生命周期绑定）
 * - 写操作（数据库增删改）使用 appIoScope（进程级，退出页面后仍可完成保存）
 */
class NotesViewModel : BaseViewModel() {

    // ── 搜索筛选状态 ──────────────────────────────────────────

    /** 标题搜索关键词 */
    private val _searchTitle = MutableStateFlow("")
    val searchTitle: StateFlow<String> = _searchTitle

    /** 日期筛选（毫秒时间戳，null 表示不筛选） */
    /** locale 变化信号流 - 使 grouped 在语言切换时重新计算 */
    private val localeFlow = LocaleManager.currentLocale

        private val _searchDate = MutableStateFlow<Long?>(null)
    val searchDate: StateFlow<Long?> = _searchDate

    /** 是否处于筛选状态（标题非空 或 日期非空） */
    private val _hasActiveFilter = MutableStateFlow(false)
    val hasActiveFilter: StateFlow<Boolean> = _hasActiveFilter

    // ── 数据流 ────────────────────────────────────────────────

    /**
     * 全部笔记流（从数据库实时获取）
     * 使用 stateIn + Lazily 避免无订阅时浪费资源
     */
    private val allNotes = NoteHistoryRepository.getAllNotesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * 分组后的笔记列表（带日期分组标题）
     *
     * 筛选逻辑：
     * - 无筛选条件 → 展示全部笔记
     * - 仅标题搜索 → 按标题模糊匹配
     * - 仅日期筛选 → 按日期精确匹配
     * - 两者都有 → 取交集
     *
     * 分组规则：今天 / 昨天 / 具体日期
     * 排序：按分组内最新 sessionStartTime 降序
     */
    val groupedNotes: StateFlow<List<NotesSection>> = combine(
        allNotes,
        _searchTitle,
        _searchDate,
        localeFlow
    ) { notes, titleQuery, dateFilter, _ ->
        // 第一步：应用筛选条件
        var filtered = notes
        if (titleQuery.isNotBlank()) {
            filtered = filtered.filter { it.title.contains(titleQuery, ignoreCase = true) }
        }
        if (dateFilter != null) {
            filtered = filtered.filter { it.date == dateFilter }
        }

        // 第二步：按日期分组（locale 变化时重新执行）
        val dateFormat = SimpleDateFormat(appApplication.getString(R.string.date_format_short), Locale.getDefault())
        val todayStart = DateUtils.getTodayStart()
        val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L
        filtered.groupBy { entity ->
            when {
                entity.date >= todayStart -> appApplication.getString(R.string.today)
                entity.date >= yesterdayStart -> appApplication.getString(R.string.yesterday)
                else -> dateFormat.format(Date(entity.date))
            }
        }.toList()
            .sortedByDescending { it.second.first().sessionStartTime }
            .map { (label, items) -> NotesSection(label, items) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── 搜索筛选操作 ──────────────────────────────────────────

    /**
     * 更新标题搜索关键词
     * @param query 用户输入的搜索词
     */
    fun updateSearchTitle(query: String) {
        _searchTitle.value = query
        updateFilterState()
    }

    /**
     * 更新日期筛选
     * @param date 选中的日期毫秒时间戳，null 表示清除
     */
    fun updateSearchDate(date: Long?) {
        _searchDate.value = date
        updateFilterState()
    }

    /**
     * 提交搜索（点击搜索按钮触发）
     * 重新计算筛选状态
     */
    fun commitSearch() {
        updateFilterState()
    }

    /**
     * 清除所有筛选条件（标题 + 日期）
     */
    fun clearAllFilters() {
        _searchTitle.value = ""
        _searchDate.value = null
        _hasActiveFilter.value = false
    }

    /**
     * 根据当前标题和日期值更新筛选状态标志
     */
    private fun updateFilterState() {
        _hasActiveFilter.value = _searchTitle.value.isNotBlank() || _searchDate.value != null
    }

    // ── 数据库写操作（使用 appIoScope 保活） ─────────────────

    /**
     * 删除笔记
     * @param id 笔记记录 ID
     */
    fun deleteNote(id: Long) {
        appIoScope.launch {
            NoteHistoryRepository.deleteNote(id)
        }
    }

    /**
     * 插入新笔记
     * @param text  笔记正文
     * @param title 笔记标题
     */
    fun insertNote(text: String, title: String) {
        appIoScope.launch {
            NoteHistoryRepository.insertNote(
                NoteHistoryEntity(
                    taskId = 0L,
                    date = DateUtils.getTodayStart(),
                    sessionStartTime = System.currentTimeMillis(),
                    durationSeconds = 0,
                    note = text,
                    title = title,
                    sourceTaskTitle = ""
                )
            )
        }
    }

    /**
     * 更新笔记内容
     * @param id   笔记记录 ID
     * @param text 新的笔记文本
     */
    fun updateNote(id: Long, text: String) {
        appIoScope.launch {
            NoteHistoryRepository.updateNote(id, text)
        }
    }

    /**
     * 更新笔记标题
     * @param id    笔记记录 ID
     * @param title 新的标题
     */
    fun updateNoteTitle(id: Long, title: String) {
        appIoScope.launch {
            NoteHistoryRepository.updateTitle(id, title)
        }
    }
}