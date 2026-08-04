package com.chronotask.pages.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronotask.components.ui.R
import com.chronotask.components.ui.compose.clickableNoIndicator


/**
 * SettingsComponents.kt
 *
 * 核心职责：设置页的全部可复用 UI 组件。
 * 主要导出：
 * - SubPageContainer    — 子页面骨架（顶栏 + 内容区）
 * - SettingsSection     — 分组标题
 * - SettingsGroup       — 卡片容器
 * - SettingsDivider     — 行内分隔线
 * - ThemeOption         — 主题选择行（带选中圆点）
 * - ContactItem         — 联系方式行（emoji + 标签 + 值）
 * - UserProfileCard     — 用户资料卡片（头像 + 编辑按钮 + 用户名）
 * - EditNameDialog      — 编辑用户名弹窗
 */


/**
 * 子页面骨架
 *
 * 顶部返回栏 + 标题 + 自定义内容的垂直布局。
 *
 * @param title 页面标题
 * @param onBack 返回按钮回调
 * @param content 页面主体内容
 */
@Composable
fun SubPageContainer(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        content()
    }
}

/**
 * 分组标题
 *
 * 设置页中各功能分组的标签文字。
 *
 * @param title 分组名称（如 "外观"、"系统"）
 */
@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

/**
 * 设置分组容器
 *
 * 最低表面色背景的圆角卡片，包裹一组设置行。
 *
 * @param content 分组内的设置行
 */
@Composable
fun SettingsGroup(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        content()
    }
}

/** 设置分组内的行间分隔线。 */
@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * 主题选择行
 *
 * 带选中状态圆点的设置行，用于主题/字体等选择场景。
 *
 * @param icon 可选左侧图标
 * @param title 选项名称
 * @param selected 是否选中（显示实心圆点）
 * @param onClick 点击回调
 */
@Composable
fun ThemeOption(
    icon: ImageVector? = null,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoIndicator(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(
                    2.dp,
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 联系方式行
 *
 * emoji 图标 + 标签 + 实际值，用于"关于作者"页面。
 *
 * @param icon emoji 图标文字
 * @param label 联系方式类型（如 "微信"、"邮箱"）
 * @param value 实际联系值
 */
@Composable
fun ContactItem(
    icon: String,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 用户资料卡片
 *
 * 圆形头像 + 右下角浮动编辑按钮 + 用户名，居中排列。
 * 头像为空时显示默认 Person 图标，点击编辑按钮可更换头像。
 *
 * @param userName 当前用户名称
 * @param avatarBitmap 当前头像位图（null 时显示默认 Icon）
 */
@Composable
internal fun UserProfileCard(
    userName: String,
    avatarBitmap: android.graphics.Bitmap?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                // 头像圆形区域
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.avatar),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = userName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * 编辑用户名弹窗
 *
 * 全屏 AlertDialog 包裹 OutlinedTextField，内置 tempName 状态。
 * 确认时若名称为空则回退为默认名称 "Alex Chen"。
 *
 * @param initialName 初始用户名（弹窗打开时填入输入框）
 * @param onConfirm 确认回调（传入最终编辑结果，已处理空值回退）
 * @param onDismiss 取消/点击外部区域回调
 */
@Composable
internal fun EditNameDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempName by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_name)) },
        text = {
            OutlinedTextField(
                value = tempName,
                onValueChange = { tempName = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.enter_name_hint)) }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val name = tempName.ifBlank { "Alex Chen" }
                onConfirm(name)
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * FeatureInfoDialog - 通用功能说明浮窗（标题 + 说明文本）\n * 标题由 title 参数提供，正文由 message 参数提供
 */
@Composable
fun FeatureInfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_ok), color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}



/**
 * InfoIconSmall - 设置页顶角小信息按钮
 * 14dp 圆形图标，居中显示 "i"，点击弹出功能说明弹窗。
 */
@Composable
fun InfoIconSmall(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickableNoIndicator(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "i",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 7.sp,  // ← 图标缩小的话，字号也一起调小，比如 6.sp
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(0.dp)
        )
    }
}
/**
 * WorkdayPickerDialog - 工作日选择弹窗
 *
 * 包含周一到周日的选择Tab，支持多选，默认选中周一到周五。
 * 掩码规则：bit0=周日，bit1=周一，...，bit6=周六，1表示选中为工作日
 *
 * @param currentMask 当前选中的工作日掩码
 * @param onConfirm 确认回调，返回新的工作日掩码
 * @param onDismiss 关闭弹窗回调
 */
@Composable
fun WorkdayPickerDialog(
    currentMask: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var tempMask by remember { mutableIntStateOf(currentMask) }

    // 星期列表：索引0=周日对应bit0，...，索引6=周六对应bit6
    val weekDays = listOf(
        R.string.weekday_mon to 1,
        R.string.weekday_tue to 2,
        R.string.weekday_wed to 3,
        R.string.weekday_thu to 4,
        R.string.weekday_fri to 5,
        R.string.weekday_sat to 6,
        R.string.weekday_sun to 0
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.workday_picker_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.workday_picker_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                // 网格布局的星期选择按钮
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(80.dp)
                ) {
                    items(weekDays) { (nameResId, bitIndex) ->
                        val isSelected = (tempMask and (1 shl bitIndex)) != 0
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                tempMask = if (isSelected) {
                                    tempMask and (1 shl bitIndex).inv()
                                } else {
                                    tempMask or (1 shl bitIndex)
                                }
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(nameResId),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tempMask) }) {
                Text(
                    text = stringResource(R.string.confirm),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

