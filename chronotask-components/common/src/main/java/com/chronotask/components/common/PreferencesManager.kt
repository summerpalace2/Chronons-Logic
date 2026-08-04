package com.chronotask.components.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

//全局 DataStore 实例，自动从 Application Context 创建
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chrono_preferences")

// 全局偏好管理，通过 [appDataStore] 单例读写

object appDataStore {
    private fun dataStore(): DataStore<Preferences> =
        appContext.dataStore

    // ─── 主题 ───
    private val THEME_INDEX = intPreferencesKey("theme_index")
    val themeIndex: Flow<Int> = dataStore().data.map { it[THEME_INDEX] ?: 0 }.distinctUntilChanged()
    suspend fun setThemeIndex(index: Int) {
        dataStore().edit { it[THEME_INDEX] = index }
    }



    // ─── 用户名 ───
    private val USER_NAME = stringPreferencesKey("user_name")
    val userName: Flow<String> =
        dataStore().data.map { it[USER_NAME] ?: "Alex Chen" }.distinctUntilChanged()

    suspend fun setUserName(name: String) {
        dataStore().edit { it[USER_NAME] = name }
    }



    // ─── 头像 URI ───
    private val AVATAR_URI = stringPreferencesKey("avatar_uri")
    val avatarUri: Flow<String?> = dataStore().data.map { it[AVATAR_URI] }.distinctUntilChanged()
    suspend fun setAvatarUri(uri: String?) {
        dataStore().edit { if (uri != null) it[AVATAR_URI] = uri else it.remove(AVATAR_URI) }
    }



    // ─── 横向对比 ───
    private val HORIZONTAL_COMPARISON = booleanPreferencesKey("horizontal_comparison")
    val horizontalComparison: Flow<Boolean> =
        dataStore().data.map { it[HORIZONTAL_COMPARISON] ?: true }.distinctUntilChanged()

    suspend fun setHorizontalComparison(enabled: Boolean) {
        dataStore().edit { it[HORIZONTAL_COMPARISON] = enabled }
    }



    // ─── 字体 ───
    private val FONT_INDEX = intPreferencesKey("font_index")
    val fontIndex: Flow<Int> =
        dataStore().data.map { it[FONT_INDEX] ?: 2 }.distinctUntilChanged()  // 默认 Noto Sans SC

    suspend fun setFontIndex(index: Int) {
        dataStore().edit { it[FONT_INDEX] = index }
    }



    // ─── 字号（sp） ───
    private val FONT_SIZE = intPreferencesKey("font_size")
    val fontSize: Flow<Int> =
        dataStore().data.map { it[FONT_SIZE] ?: 16 }.distinctUntilChanged()  // 默认 16sp

    suspend fun setFontSize(size: Int) {
        dataStore().edit { it[FONT_SIZE] = size }
    }



    // ─── 语言 ───
    private val LANGUAGE_INDEX = intPreferencesKey("language_index")
    val languageIndex: Flow<Int> =
        dataStore().data.map { it[LANGUAGE_INDEX] ?: 0 }.distinctUntilChanged()  // 默认简体中文

    suspend fun setLanguageIndex(index: Int) {
        dataStore().edit { it[LANGUAGE_INDEX] = index }
    }



    // ─── 一键导入 ───
    private val QUICK_IMPORT_ENABLED = booleanPreferencesKey("quick_import_enabled")
    val quickImportEnabled: Flow<Boolean> =
        dataStore().data.map { it[QUICK_IMPORT_ENABLED] ?: false }.distinctUntilChanged()

    suspend fun setQuickImportEnabled(enabled: Boolean) {
        dataStore().edit { it[QUICK_IMPORT_ENABLED] = enabled }
    }

    private val QUICK_IMPORT_TASKS = stringPreferencesKey("quick_import_tasks")
    val quickImportTasks: Flow<String> =
        dataStore().data.map { it[QUICK_IMPORT_TASKS] ?: "[]" }.distinctUntilChanged()

    suspend fun setQuickImportTasks(tasksJson: String) {
        dataStore().edit { it[QUICK_IMPORT_TASKS] = tasksJson }
    }



    // ─── 最后导入日期 ───
    private val LAST_IMPORT_DATE = stringPreferencesKey("last_import_date")
    val lastImportDate: Flow<String> =
        dataStore().data.map { it[LAST_IMPORT_DATE] ?: "" }.distinctUntilChanged()

    suspend fun setLastImportDate(date: String) {
        dataStore().edit { it[LAST_IMPORT_DATE] = date }
    }



    // ─── 工作日模式 ───
    private val WORKDAY_ENABLED = booleanPreferencesKey("workday_enabled")
    val workdayEnabled: Flow<Boolean> =
        dataStore().data.map { it[WORKDAY_ENABLED] ?: false }.distinctUntilChanged()

    suspend fun setWorkdayEnabled(enabled: Boolean) {
        dataStore().edit { it[WORKDAY_ENABLED] = enabled }
    }

    // 7 bit 位掩码：bit0 = 周日, bit1 = 周一, ... bit6 = 周六, 1 = 启用
    private val WORKDAY_WEEK_MASK = intPreferencesKey("workday_week_mask")
    val workdayWeekMask: Flow<Int> =
        dataStore().data.map { it[WORKDAY_WEEK_MASK] ?: 0x7E }.distinctUntilChanged()  // 默认周一~五

    suspend fun setWorkdayWeekMask(mask: Int) {
        dataStore().edit { it[WORKDAY_WEEK_MASK] = mask }
    }



    // ─── 一天的起始偏移（总分钟数，支持精确到分钟）───
    private val DAY_START_OFFSET_MINUTES = intPreferencesKey("day_start_offset_minutes")
    val dayStartOffsetMinutes: Flow<Int> =
        dataStore().data.map { it[DAY_START_OFFSET_MINUTES] ?: 0 }.distinctUntilChanged()

    suspend fun setDayStartOffsetMinutes(minutes: Int) {
        dataStore().edit { it[DAY_START_OFFSET_MINUTES] = minutes.coerceIn(0, 1439) }
    }

    // ─── 首次使用标记 ───
    private val IS_FIRST_USE = booleanPreferencesKey("is_first_use")
    val isFirstUse: Flow<Boolean> = dataStore().data.map { it[IS_FIRST_USE] ?: true }.distinctUntilChanged()
    suspend fun markFirstUseComplete() {
        dataStore().edit { it[IS_FIRST_USE] = false }
    }

}







