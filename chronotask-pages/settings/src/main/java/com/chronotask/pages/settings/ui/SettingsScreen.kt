package com.chronotask.pages.settings.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chronotask.components.common.QuickImportManager
import com.chronotask.components.common.appDataStore
import com.chronotask.components.ui.R
import com.chronotask.components.ui.compose.clickableNoIndicator
import com.chronotask.components.ui.theme.AppFont
import com.chronotask.components.ui.theme.AppLanguage
import com.chronotask.components.ui.theme.ChronoTheme
import com.chronotask.components.ui.theme.LocalChronoThemeIndex
import com.chronotask.components.ui.theme.LocaleManager
import com.chronotask.pages.settings.api.AboutAppArgument
import com.chronotask.pages.settings.api.AboutAuthorArgument
import com.chronotask.pages.settings.api.FontArgument
import com.chronotask.pages.settings.api.LanguageArgument
import com.chronotask.pages.settings.api.QuickImportArgument
import com.chronotask.pages.settings.api.SettingsArgument
import com.chronotask.pages.settings.api.ThemeArgument
import kotlinx.coroutines.launch
import androidx.core.net.toUri

/**
 * 设置页主页面
 *
 * 包含用户资料卡片、个人信息、外观、功能、系统等分组设置。
 * 各 Section 拆分为独立子函数，各自只订阅需要的 StateFlow，避免整页重组。
 *
 * @param argument 导航参数（预留扩展，当前使用默认值）
 */
@Composable
fun SettingsScreen(@Suppress("UNUSED_PARAMETER") argument: SettingsArgument = SettingsArgument) {
    val scope = rememberCoroutineScope()

    // ── 用户信息：从 DataStore Flow 读取（响应式） ──
    val savedUserName by appDataStore.userName.collectAsState(initial = "Alex Chen")
    val savedAvatarUri by appDataStore.avatarUri.collectAsState(initial = null)

    // ── 头像 URI → Bitmap 转换（仅在 URI 变化时执行） ──
    val context = LocalContext.current
    val avatarBitmap = remember(savedAvatarUri) {
        savedAvatarUri?.let { uriStr ->
            try {
                if (uriStr.startsWith("file:")) {
                    // 内部存储文件，直接读取
                    BitmapFactory.decodeFile(java.net.URI(uriStr).path)
                } else {
                    val uri = uriStr.toUri()
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    // ── 头像选择器（相册） ──
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch {
                try {
                    // 复制到内部存储，保证 URI 持久可用
                    val inputStream = context.contentResolver.openInputStream(selectedUri)
                    val file = java.io.File(context.filesDir, "avatar.jpg")
                    inputStream?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    appDataStore.setAvatarUri(file.toURI().toString())
                } catch (_: Exception) {
                    // 复制失败时回退到原始 URI
                    appDataStore.setAvatarUri(selectedUri.toString())
                }
            }
        }
    }

    // ── 编辑名称弹窗状态 ──
    var showNameDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // 1. 用户资料卡片
        UserProfileCard(
            userName = savedUserName,
            avatarBitmap = avatarBitmap
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. 个人信息
        PersonalInfoSection(
            onEditName = { showNameDialog = true },
            onChangeAvatar = { galleryLauncher.launch("image/*") }
        )

        // 3. 外观
        AppearanceSection()

        // 4. 功能
        FunctionSection(scope = scope)

        // 5. 系统
        SystemSection()

        Spacer(modifier = Modifier.height(32.dp))
    }

    // ── 编辑名称弹窗 ──
    if (showNameDialog) {
        EditNameDialog(
            initialName = savedUserName,
            onConfirm = { name ->
                scope.launch { appDataStore.setUserName(name) }
                showNameDialog = false
            },
            onDismiss = { showNameDialog = false }
        )
    }
}


/**
 * PersonalInfoSection - 个人信息设置区
 *
 * 包含编辑用户名和更换头像两个入口。
 */
@Composable
private fun PersonalInfoSection(
    onEditName: () -> Unit,
    onChangeAvatar: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.personal_info))
    SettingsGroup {
        SettingsRow(
            icon = Icons.Default.Person,
            title = stringResource(R.string.edit_name),
            onClick = onEditName
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        SettingsRow(
            icon = Icons.Default.Face,
            title = stringResource(R.string.change_avatar),
            onClick = onChangeAvatar
        )
    }
}

/**
 * AppearanceSection - 外观设置区
 *
 * 显示当前主题方案和字体，点击跳转到对应设置页。
 */
@Composable
private fun AppearanceSection() {
    val themeState = LocalChronoThemeIndex.current
    val themeIndex by themeState
    val currentTheme = ChronoTheme.entries.getOrElse(themeIndex) { ChronoTheme.default }

    val savedFontIndex by appDataStore.fontIndex.collectAsState(initial = 2)
    val currentFont = AppFont.entries.getOrElse(savedFontIndex) { AppFont.default }

    SettingsSection(title = stringResource(R.string.appearance))
    SettingsGroup {
        SettingsRow(
            icon = Icons.Default.Palette,
            title = stringResource(R.string.theme),
            subtitle = stringResource(currentTheme.displayNameResId),
            onClick = { ThemeArgument.navigate() }
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        SettingsRow(
            icon = Icons.Default.TextFields,
            title = stringResource(R.string.font),
            subtitle = stringResource(currentFont.displayNameResId),
            onClick = { FontArgument.navigate() }
        )
    }
}

/**
 * FunctionSection - 功能设置区
 */
@Composable
private fun FunctionSection(scope: kotlinx.coroutines.CoroutineScope) {
    val quickImportEnabled by appDataStore.quickImportEnabled.collectAsState(initial = false)
    val horizontalComparison by appDataStore.horizontalComparison.collectAsState(initial = false)
    val dayStartOffsetMinutes by appDataStore.dayStartOffsetMinutes.collectAsState(initial = 0)
    val workdayEnabled by appDataStore.workdayEnabled.collectAsState(initial = false)
    val workdayWeekMask by appDataStore.workdayWeekMask.collectAsState(initial = 0x3E)

    var showQuickImportInfo by remember { mutableStateOf(false) }
    var showHorizontalCompInfo by remember { mutableStateOf(false) }
    var showWorkdayInfo by remember { mutableStateOf(false) }
    var showWorkdayPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRefreshTimeInfo by remember { mutableStateOf(false) }

    SettingsSection(title = stringResource(R.string.functions))
    SettingsGroup {
        // ── 一键导入行：[i] 标题 + Switch ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickableNoIndicator { /* do nothing; switch handles toggle */ }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoIconSmall(onClick = { showQuickImportInfo = true })
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.quick_import),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = quickImportEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        QuickImportManager.setEnabled(enabled)
                        if (!enabled && horizontalComparison) {
                            appDataStore.setHorizontalComparison(false)
                        }
                    }
                },
                colors = SwitchDefaults.colors()
            )
        }
        // 子行：编辑导入项
        SettingsRow(
            icon = null,
            title = stringResource(R.string.edit_quick_import),
            onClick = { QuickImportArgument.navigate() })

        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // ── 横向对比行：[i] 标题 + Switch ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickableNoIndicator { }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoIconSmall(onClick = { showHorizontalCompInfo = true })
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.horizontal_comparison_toggle),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = horizontalComparison,
                enabled = quickImportEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { appDataStore.setHorizontalComparison(enabled) }
                }
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // ── 工作日行：[i] + 图标 + 文字 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickableNoIndicator { showWorkdayPicker = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoIconSmall(onClick = { showWorkdayInfo = true })
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.workday_settings),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(if (workdayEnabled) R.string.workday_mode_toggle else R.string.workday_default),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // ── 刷新时间行 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickableNoIndicator { showTimePicker = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoIconSmall(onClick = { showRefreshTimeInfo = true })
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.NightsStay,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.refresh_time),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "%d:%02d".format(dayStartOffsetMinutes / 60, dayStartOffsetMinutes % 60),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    // ── Info 弹窗 ──
    if (showQuickImportInfo) {
        FeatureInfoDialog(
            title = stringResource(R.string.quick_import_title),
            message = stringResource(R.string.quick_import_info),
            onDismiss = { showQuickImportInfo = false })
    }
    if (showHorizontalCompInfo) {
        FeatureInfoDialog(
            title = stringResource(R.string.horizontal_comparison_title),
            message = stringResource(R.string.horizontal_comparison_info),
            onDismiss = { showHorizontalCompInfo = false })
    }
    if (showWorkdayInfo) {
        FeatureInfoDialog(
            title = stringResource(R.string.workday_settings_title),
            message = stringResource(R.string.workday_settings_info),
            onDismiss = { showWorkdayInfo = false })
    }

    // ── 工作日选择弹窗 ──
    if (showWorkdayPicker) {
        WorkdayPickerDialog(
            currentMask = workdayWeekMask,
            onConfirm = { newMask ->
                scope.launch { appDataStore.setWorkdayWeekMask(newMask) }
                showWorkdayPicker = false
            },
            onDismiss = { showWorkdayPicker = false }
        )
    }

    // ── 刷新时间滚轮弹窗 ──
    if (showTimePicker) {
        com.chronotask.components.ui.picker.VerticalTimePickerDialog(
            initialHours = dayStartOffsetMinutes / 60,
            initialMinutes = dayStartOffsetMinutes % 60,
            onConfirm = { hours, minutes ->
                scope.launch { appDataStore.setDayStartOffsetMinutes(hours * 60 + minutes) }
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    // ── 刷新时间说明弹窗 ──
    if (showRefreshTimeInfo) {
        FeatureInfoDialog(
            title = stringResource(R.string.refresh_time_title),
            message = stringResource(R.string.refresh_time_info),
            onDismiss = { showRefreshTimeInfo = false }
        )
    }
}


/**
 * SystemSection - 系统设置区
 */
@Composable
private fun SystemSection() {
    val currentLocale by LocaleManager.currentLocale.collectAsState()
    val currentLang = AppLanguage.entries.firstOrNull { it.locale == currentLocale }
        ?: AppLanguage.ENGLISH

    SettingsSection(title = stringResource(R.string.system))
    SettingsGroup {
        SettingsRow(
            icon = Icons.Default.Language,
            title = stringResource(R.string.language),
            subtitle = currentLang.displayName,
            onClick = { LanguageArgument.navigate() })
        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        SettingsRow(
            icon = Icons.Default.Info,
            title = stringResource(R.string.about_app),
            onClick = { AboutAppArgument.navigate() })
        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        SettingsRow(
            icon = Icons.Default.AccountCircle,
            title = stringResource(R.string.about_author),
            onClick = { AboutAuthorArgument.navigate() })
    }
}


/**
 * SettingsRow - 简单的设置行
 */
@Composable
private fun SettingsRow(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoIndicator { onClick() }
            .padding(horizontal = 16.dp, vertical = if (trailing != null) 10.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
