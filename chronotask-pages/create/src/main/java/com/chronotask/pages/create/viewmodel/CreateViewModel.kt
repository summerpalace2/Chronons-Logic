package com.chronotask.pages.create.viewmodel

import androidx.lifecycle.viewModelScope
import com.chronotask.components.common.base.BaseViewModel
import com.chronotask.components.common.appApplication
import com.chronotask.components.common.appIoScope
import com.chronotask.components.common.DateUtils
import com.chronotask.components.common.QuickImportManager
import com.chronotask.components.common.QuickImportTask
import com.chronotask.components.database.AppDatabase
import com.chronotask.components.database.entity.TagEntity
import com.chronotask.components.database.entity.TaskEntity
import com.chronotask.pages.create.api.CreateMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 创建/编辑任务 ViewModel
 *
 * 职责：
 * - 管理表单状态（标题、标签选择、时间设定）
 * - 加载已有任务数据（编辑模式）
 * - 执行保存/更新/删除操作
 *
 * 协程优化：
 * - 数据写入使用 appIoScope（保活 IO 作用域），退出页面后协程仍可完成数据保存
 * - 数据读取（loadTask）使用 viewModelScope，与页面生命周期绑定
 */
class CreateViewModel : BaseViewModel() {

    private val db = AppDatabase.getDatabase(appApplication)
    private val taskDao = db.taskDao()
    private val tagDao = db.tagDao()

    private var mode: CreateMode = CreateMode.Normal

    // ── 表单状态 ──

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = _selectedTagId

    private val _hours = MutableStateFlow(0)
    val hours: StateFlow<Int> = _hours

    private val _minutes = MutableStateFlow(0)
    val minutes: StateFlow<Int> = _minutes

    private val _isUnlimited = MutableStateFlow(true)
    val isUnlimited: StateFlow<Boolean> = _isUnlimited

    // 编辑模式：记录正在编辑的任务 ID
    private var editingTaskId: Long? = null

    // 标签列表（响应式，5 秒内无订阅者自动释放）
    val tags: StateFlow<List<TagEntity>> = tagDao.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── 状态更新方法 ──

    /**
     * 设置创建模式
     * @param m 创建模式（Normal 标准 / QuickImport 快速导入）
     */
    fun setMode(m: CreateMode) {
        mode = m
    }

    /**
     * 更新任务标题
     * @param text 新的标题文本
     */
    fun updateTitle(text: String) { _title.value = text }

    /**
     * 选择标签（点击已选标签则取消选择）
     * @param tagId 标签 ID，null 表示取消选择
     */
    fun selectTag(tagId: Long?) { _selectedTagId.value = tagId }

    /**
     * 设置目标小时数
     * 同时关闭"无限时"模式（用户设定具体时长即视为有限目标）
     * @param h 小时值，自动约束在 [0, 23]
     */
    fun setHours(h: Int) {
        _hours.value = h.coerceIn(0, 23)
        _isUnlimited.value = false
    }

    /**
     * 设置目标分钟数
     * 同时关闭"无限时"模式
     * @param m 分钟值，自动约束在 [0, 59]
     */
    fun setMinutes(m: Int) {
        _minutes.value = m.coerceIn(0, 59)
        _isUnlimited.value = false
    }

    /**
     * 切换"无限时"模式
     */
    fun toggleUnlimited() { _isUnlimited.value = !_isUnlimited.value }

    // ── 数据加载 ──

    /**
     * 加载已有任务数据（编辑模式）
     * 使用 viewModelScope，与页面生命周期绑定，页面退出自动取消
     * @param taskId 要加载的任务 ID
     */
    fun loadTask(taskId: Long) {
        editingTaskId = taskId
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            _title.value = task.title
            _selectedTagId.value = task.tagId
            val targetMin = task.targetDurationMinutes
            if (targetMin != null) {
                _isUnlimited.value = false
                _hours.value = targetMin / 60
                _minutes.value = targetMin % 60
            } else {
                _isUnlimited.value = true
                _hours.value = 0
                _minutes.value = 0
            }
        }
    }

    // ── 保存操作 ──

    /**
     * 保存任务（新建或更新）
     *
     * 流程：
     * 1. 校验标题非空
     * 2. 计算目标分钟数（无限时则为 null）
     * 3. 根据模式分发到不同保存逻辑
     *
     * @return true 表示保存成功，false 表示校验失败（标题为空）
     */
    fun save(): Boolean {
        val titleText = _title.value.trim()
        if (titleText.isEmpty()) return false

        val targetMinutes = resolveTargetMinutes()

        when (mode) {
            CreateMode.QuickImport -> saveQuickImport(titleText, targetMinutes)
            CreateMode.Normal -> saveNormalTask(titleText, targetMinutes)
        }
        return true
    }

    /**
     * 计算目标分钟数
     * 规则：无限时且未设置具体时长 → null（表示无目标）
     *       否则 → 小时×60 + 分钟
     */
    private fun resolveTargetMinutes(): Int? {
        val totalMin = _hours.value * 60 + _minutes.value
        return if (_isUnlimited.value && totalMin == 0) null else totalMin
    }

    /**
     * 快速导入模式：写入 DataStore
     * 使用 appIoScope 保活，即使页面退出也能完成写入
     */
    private fun saveQuickImport(title: String, targetMinutes: Int?) {
        appIoScope.launch {
            QuickImportManager.addTask(
                QuickImportTask(
                    title = title,
                    targetMinutes = targetMinutes,
                    tagId = _selectedTagId.value
                )
            )
        }
    }

    /**
     * 标准模式：写入 Room 数据库（更新 or 插入）
     * 使用 appIoScope 保活，即使页面退出也能完成写入
     */
    private fun saveNormalTask(title: String, targetMinutes: Int?) {
        appIoScope.launch {
            val existingId = editingTaskId
            if (existingId != null) {
                taskDao.updateTask(
                    TaskEntity(
                        id = existingId,
                        title = title,
                        tagId = _selectedTagId.value,
                        targetDurationMinutes = targetMinutes
                    )
                )
            } else {
                taskDao.insertTask(
                    TaskEntity(
                        title = title,
                        tagId = _selectedTagId.value,
                        targetDurationMinutes = targetMinutes,
                        scheduledDate = DateUtils.getTodayStart()
                    )
                )
            }
        }
    }

    // ── 标签管理 ──

    /**
     * 新增标签
     * 使用 appIoScope 保活写入，避免页面退出时丢失
     * @param name 标签名称（空值直接返回，不操作）
     */
    fun addTag(name: String) {
        if (name.isBlank()) return
        appIoScope.launch {
            val newId = tagDao.insertTag(TagEntity(name = name.trim()))
            // 自动选中新创建的标签，确保当前任务关联到此标签
            _selectedTagId.value = newId
        }
    }

    /**
     * 删除标签
     * 使用 appIoScope 保活写入
     * @param tagId 要删除的标签 ID
     */
    fun deleteTag(tagId: Long) {
        appIoScope.launch {
            tagDao.deleteTag(tagId)
        }
    }
}