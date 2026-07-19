package com.chronotask.pages.settings.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronotask.components.ui.R
import com.chronotask.components.ui.compose.clickableNoIndicator
import com.chronotask.components.ui.theme.AppLanguage
import com.chronotask.components.ui.theme.LocaleManager
import com.chronotask.pages.settings.api.LanguageArgument

/**
 * LanguageSettingsScreen - 语言设置页
 *
 * 显示四种语言选项，选中后即时更新 LocaleManager.currentLocale → Compose 重组 → stringResource() 按新 locale 读取。
 * 需要 Activity recreate 以完整刷新所有 resources 确保切换完整。
 */
@Composable
fun LanguageSettingsScreen() {
    val currentLocale by LocaleManager.currentLocale.collectAsState()
    var selectedLanguage by remember(currentLocale) {
        mutableIntStateOf(AppLanguage.entries.indexOfFirst { it.locale == currentLocale }.coerceAtLeast(0))
    }
    val scope = rememberCoroutineScope()

    val activity = LocalContext.current as? android.app.Activity
    val languages = AppLanguage.entries.map { it.displayName }

    SubPageContainer(title = stringResource(R.string.language_settings), onBack = { LanguageArgument.popBackStack() }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsGroup {
                languages.forEachIndexed { index, name ->
                    val isSelected = selectedLanguage == index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableNoIndicator {
                                selectedLanguage = index
                                val lang = AppLanguage.entries[index]
                                // 更新 locale：触发 Compose 重组 + 持久化到 SharedPreferences
                                LocaleManager.updateLocale(lang.locale)
                                activity?.recreate()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                    if (index < languages.size - 1) {
                        SettingsDivider()
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
