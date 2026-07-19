
package com.chronotask.components.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 首次使用引导弹窗
 * @param onConfirm 点击我已知晓的回调
 */
@Composable
fun FirstUseGuideDialog(onConfirm: () -> Unit) {
    Dialog(
        onDismissRequest = onConfirm,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                // 标题
                Text(
                    text = "欢迎使用「Chrono Logic」",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 痛点描述
                Text(
                    text = "你是否也有这样的困惑？",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "明明忙了一整天，却想不起来自己到底做完了几件事；\n打算学习2小时，刷一下手机、回复临时消息、处理突发小事... 一转眼时间就没了，真正专注的时间少之又少。\n\n信息时代，注意力是我们最宝贵的资源，而大多数人都不知道自己的时间真正花在了哪里。这款App，就是帮你找到这个答案。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 核心理念
                Text(
                    text = "核心使用理念",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "开始计时，就是专注的开始\n启动计时时，请暂时放下手机，放下杂事，只做手头这一件事。哪怕中途喝水上厕所，也请暂停计时——我们记录的，是你真正在投入做事的有效时间，不是坐在桌前的“假忙碌”。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 核心功能
                Text(
                    text = "核心功能",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                val features = listOf(
                    "1. 精准有效计时：只统计你真正专注的时间，帮你剔除无效消耗",
                    "2. 多维度数据统计：支持按日/周/月维度查看学习数据，清晰看到自己的成长轨迹",
                    "3. 专属任务笔记：每个创建的任务都可以配套独立笔记，支持Markdown语法渲染，永久保存",
                    "4. 一键导入模板：自定义每日固定任务模板，无需每天重复创建，一键自动同步到新的一天",
                    "5. 横向对比分析：自动统计当前任务在本周/本月的平均完成时间，帮你直观看到自己的进步",
                    "6. 工作日模式：自定义工作日和休息日，适配你的生活节奏，统计更精准",
                    "7. 自定义任务刷新时间：支持修改每日任务的刷新节点，不再是死板的0点刷新"
                )
                features.forEach { feature ->
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = "我已知晓，开始使用",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
