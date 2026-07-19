package com.chronotask.components.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * QuickImportTask - 一键导入任务条目
 *
 * @param title 任务名称
 * @param targetMinutes 目标时长（分钟），null 表示无限制
 */
data class QuickImportTask(
    val title: String,
    val targetMinutes: Int? = null,
    val tagId: Long? = null
)

/**
 * QuickImportManager - 一键导入数据管理
 *
 * 核心职责：管理一键导入的任务列表（CRUD + DataStore 持久化）。
 * 数据格式：JSON 数组 [{"title":"任务名","targetMinutes":60}, ...]
 */
object QuickImportManager {

    /** 获取任务列表 Flow */
    val tasks: Flow<List<QuickImportTask>> = appDataStore.quickImportTasks.map { json ->
        parseTasks(json)
    }

    /** 获取一键导入是否启用 */
    val isEnabled: Flow<Boolean> = appDataStore.quickImportEnabled

    /** 设置启用状态 */
    suspend fun setEnabled(enabled: Boolean) {
        appDataStore.setQuickImportEnabled(enabled)
    }

    /** 保存任务列表 */
    suspend fun saveTasks(tasks: List<QuickImportTask>) {
        appDataStore.setQuickImportTasks(serializeTasks(tasks))
    }

    /** 添加任务（suspend 版本，内部直接读 DataStore 避免 runBlocking 死锁） */
    suspend fun addTask(task: QuickImportTask) {
        val json = appDataStore.quickImportTasks.first()
        val current = parseTasks(json)
        saveTasks(current + task)
    }

    /** 删除任务（suspend 版本） */
    suspend fun removeTask(index: Int) {
        val json = appDataStore.quickImportTasks.first()
        val current = parseTasks(json).toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            saveTasks(current)
        }
    }

    /** 更新任务（suspend 版本） */
    suspend fun updateTask(index: Int, task: QuickImportTask) {
        val json = appDataStore.quickImportTasks.first()
        val current = parseTasks(json).toMutableList()
        if (index in current.indices) {
            current[index] = task
            saveTasks(current)
        }
    }

    /** 解析 JSON 字符串为任务列表 */
    private fun parseTasks(json: String): List<QuickImportTask> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                QuickImportTask(
                    title = obj.optString("title", ""),
                    targetMinutes = if (obj.has("targetMinutes") && !obj.isNull("targetMinutes"))
                        obj.getInt("targetMinutes") else null,
                    tagId = if (obj.has("tagId") && !obj.isNull("tagId"))
                        obj.getLong("tagId") else null
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 判断指定标题的任务是否在一键导入列表中（suspend 版本）
     * 使用 first() 替代 runBlocking 避免主线程死锁
     */
    suspend fun containsTask(title: String): Boolean {
        val json = appDataStore.quickImportTasks.first()
        val current = parseTasks(json)
        return current.any { it.title == title }
    }

    fun serializeTasks(tasks: List<QuickImportTask>): String {
        val arr = JSONArray()
        tasks.forEach { task ->
            val obj = JSONObject()
            obj.put("title", task.title)
            if (task.targetMinutes != null) {
                obj.put("targetMinutes", task.targetMinutes)
            } else {
                obj.put("targetMinutes", JSONObject.NULL)
            }
            if (task.tagId != null) {
                obj.put("tagId", task.tagId)
            } else {
                obj.put("tagId", JSONObject.NULL)
            }
            arr.put(obj)
        }
        return arr.toString()
    }
}
