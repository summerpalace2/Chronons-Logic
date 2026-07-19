package com.chronotask.pages.notes.data

import com.chronotask.components.database.entity.NoteHistoryEntity

/**
 * NotesSection - 按日期分组的笔记
 *
 * 用于 UI 层展示，将笔记列表按日期分组并附带中文标签。
 *
 * @param dateLabel 分组标题（如 "今天"、"昨天"、"7月15日"）
 * @param items     该分组下的笔记列表（按 sessionStartTime 降序排列）
 */
data class NotesSection(
    val dateLabel: String,
    val items: List<NoteHistoryEntity>
)

/**
 * NoteEditorMode - 笔记编辑器模式
 *
 * 控制 NoteEditorOverlay 的行为：
 * - CREATE：新建模式，输入框为空，自动聚焦标题
 * - READ：只读模式，隐藏保存按钮
 * - EDIT：编辑模式，填充已有数据
 */
enum class NoteEditorMode {
    CREATE, READ, EDIT
}