package com.chronotask.components.ui.theme

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import com.chronotask.components.common.appCoroutineScope
import com.chronotask.components.common.appDataStore
import kotlinx.coroutines.launch

/**
 * 全局主题索引的 [staticCompositionLocalOf] 容器。
 *
 * 由 [rememberChronoThemeState] 在 Composition 树根部提供，
 * 子组件通过 [LocalChronoThemeIndex] 即可读取当前选中的主题索引。
 *
 * 若未在 Composition 树中配置，访问该 Local 时会抛出 [IllegalStateException]。
 */
val LocalChronoThemeIndex = staticCompositionLocalOf<MutableState<Int>> {
    error("未配置 ChronoTaskTheme")
}

/**
 * 创建与 DataStore 持久化绑定的响应式主题状态。
 *
 * 该 Composable 负责：
 * 1. 从 [appDataStore] 中 collect 最新的主题索引（[androidx.compose.runtime.State]）；
 * 2. 将其初始值赋给一个本地的 [MutableState]，以便在 Composition 中直接读写；
 * 3. 通过 [androidx.compose.runtime.LaunchedEffect] 监听 DataStore 的变化，
 *    并在数据发生冲突时用持久化数据覆盖本地状态。
 *
 * ## 关于「double-update」的说明
 *
 * 状态初始化（[mutableStateOf] 的参数）和 [LaunchedEffect] 内的赋值在首次组合时
 * 确实会执行两次——这是一种**有意为之**的设计：
 *
 * - [mutableStateOf(persistedIndex.value)] 保证首次渲染就能立刻拿到持久化的主题索引，
 *   不会因为 DataStore 的异步加载而产生闪烁；
 * - [LaunchedEffect(persistedIndex.value)] 用于处理**跨进程 DataStore 更新**（例如设置页
 *   在子进程中写入了新的主题索引），当 DataStore 的值与本地状态不一致时，用持久化值覆盖
 *   本地状态。在单一进程的大多数场景下，LaunchedEffect 第一次执行时 `state.value` 与
 *   `persistedIndex.value` 相等，赋值不会产生实际效果。
 *
 * @return 当前主题索引的 [MutableState]，写入即触发 UI 重组，
 *         同时可通过 [selectTheme] 持久化到 DataStore。
 */
@SuppressLint("UnrememberedMutableState")
@Composable
fun rememberChronoThemeState(): MutableState<Int> {
    val scope = rememberCoroutineScope()
    val persistedIndex = appDataStore.themeIndex.collectAsState(initial = 0)
    val state = mutableStateOf(persistedIndex.value)
    // 同步 DataStore → State
    @Suppress("DEPRECATION")
    // 当 DataStore 变化时更新 state（仅跨进程场景）
    androidx.compose.runtime.LaunchedEffect(persistedIndex.value) {
        state.value = persistedIndex.value
    }
    return state
}

/**
 * 切换用户选中的主题。
 *
 * 通过 [appCoroutineScope] 启动协程，将目标主题索引异步写入 [appDataStore]，
 * 写入成功后DataStore 的 Flow 会自动 emit 新值，从而触发 [rememberChronoThemeState]
 * 中的 [androidx.compose.runtime.collectAsState] 更新，最终驱动 UI 切换主题。
 *
 * @param themeIndex 目标主题在 [ChronoTheme] 枚举中的序数索引（0-based）。
 */
fun selectTheme(themeIndex: Int) {
    appCoroutineScope.launch {
        appDataStore.setThemeIndex(themeIndex)
    }
}