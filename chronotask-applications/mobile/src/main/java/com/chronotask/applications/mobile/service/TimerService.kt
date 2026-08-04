package com.chronotask.applications.mobile.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.chronotask.applications.mobile.MainActivity
import com.chronotask.applications.mobile.R
import com.chronotask.components.common.TimerManager
import com.chronotask.components.common.appIoScope
import com.chronotask.components.database.repository.TaskRepository
import kotlinx.coroutines.launch

/**
 * TimerService.kt
 *
 * 核心职责：为用户主动启动的计时会话提供 Android Foreground Service 和常驻通知。
 * 主要导出：服务生命周期、服务启动/同步/停止入口及通知动作协议。
 *
 * 计时数据不在 Service 内重复维护，所有时间计算委托给 TimerManager；Service 只负责：
 * - 尽快调用 startForeground，满足 Android 前台服务启动约束。
 * - 用 NotificationCompat 的系统 Chronometer 展示实时计时。
 * - 将通知操作转发给 TimerManager，避免多个计时状态源。
 */
class TimerService : Service() {

    /** 当前通知展示的任务标题，异步从数据库加载，初始使用兜底文案。 */
    private var currentTaskTitle: String? = null

    /** 当前标题对应的任务 ID，切换任务时用于清除旧标题。 */
    private var titleTaskId: Long? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * 处理来自应用入口、通知按钮和系统重启的服务命令。
     *
     * @param intent 命令 Intent；系统恢复服务时可能为 null。
     * @param flags Android 服务启动标志。
     * @param startId 本次启动请求的序号。
     * @return 活动会话保持 START_STICKY，无活动会话不请求重启。
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                TimerManager.pauseTimer()
                refreshNotification()
            }

            ACTION_RESUME -> {
                TimerManager.resumeTimer()
                refreshNotification()
            }

            ACTION_STOP -> {
                TimerManager.stopSessionDetailed()
                stopForegroundAndSelf()
            }

            ACTION_START, ACTION_SYNC, null -> {
                showForegroundNotification()
            }
        }

        return if (TimerManager.getCurrentTaskId() != null) {
            START_STICKY
        } else {
            START_NOT_STICKY
        }
    }

    /** 前台服务不提供绑定接口，所有操作通过明确的 Intent action 进入。 */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 立即进入前台并展示通知，然后异步补充任务标题。
     * Android 要求 startForegroundService 后尽快调用 startForeground，因此不等待数据库查询。
     */
    private fun showForegroundNotification() {
        val taskId = TimerManager.getCurrentTaskId()
        if (taskId == null) {
            stopForegroundAndSelf()
            return
        }

        if (titleTaskId != taskId) {
            titleTaskId = taskId
            currentTaskTitle = null
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        loadTaskTitle(taskId)
    }

    /** 使用当前 TimerManager 状态重建通知，处理暂停/恢复后的按钮与计时起点变化。 */
    private fun refreshNotification() {
        if (TimerManager.getCurrentTaskId() == null) {
            stopForegroundAndSelf()
            return
        }
        notifyTimerNotification()
    }

    /**
     * 从任务表加载通知标题。
     *
     * @param taskId 当前会话的任务 ID；查询完成后会再次校验会话，避免旧查询覆盖新通知。
     */
    private fun loadTaskTitle(taskId: Long) {
        appIoScope.launch {
            val title = runCatching { TaskRepository.getById(taskId)?.title }.getOrNull()
                ?: getString(R.string.notification_timer_fallback_task)
            if (!TimerManager.isRunning(taskId)) return@launch
            currentTaskTitle = title
            notifyTimerNotification()
        }
    }

    /**
     * 更新计时通知。
     * Android 13 及以上需要用户授予通知权限；权限未授予时跳过更新，避免服务因安全异常崩溃。
     */
    private fun notifyTimerNotification() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
    }

    /** 创建低打扰通知渠道，计时通知不产生声音和震动。 */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_timer_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_timer_channel_description)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** 构建常驻通知，使用通知栏原生 Chronometer 避免每秒刷新整条通知。 */
    private fun buildNotification(): Notification {
        val elapsedSeconds = TimerManager.elapsedSeconds.value
        val paused = TimerManager.isPaused.value
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(currentTaskTitle ?: getString(R.string.notification_timer_fallback_task))
            .setContentText(
                getString(
                    if (paused) R.string.notification_timer_paused
                    else R.string.notification_timer_running
                )
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis() - elapsedSeconds * 1000L)
            .setUsesChronometer(!paused)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(createOpenTaskPendingIntent())

        val timerAction = if (paused) {
            NotificationCompat.Action(
                R.drawable.ic_stat_timer,
                getString(R.string.notification_timer_resume),
                createServicePendingIntent(ACTION_RESUME, REQUEST_RESUME)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_stat_timer,
                getString(R.string.notification_timer_pause),
                createServicePendingIntent(ACTION_PAUSE, REQUEST_PAUSE)
            )
        }
        return builder
            .addAction(timerAction)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_stat_timer,
                    getString(R.string.notification_timer_stop),
                    createServicePendingIntent(ACTION_STOP, REQUEST_STOP)
                )
            )
            .build()
    }

    /** 创建点击通知后打开任务详情页的 PendingIntent。 */
    private fun createOpenTaskPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, TimerManager.getCurrentTaskId() ?: -1L)
        }
        return PendingIntent.getActivity(
            this,
            REQUEST_OPEN_TASK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 创建通知动作使用的 Service PendingIntent。 */
    private fun createServicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, TimerService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 移除常驻通知并停止当前服务实例。 */
    private fun stopForegroundAndSelf() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val ACTION_START = "com.chronotask.timer.action.START"
        const val ACTION_SYNC = "com.chronotask.timer.action.SYNC"
        const val ACTION_PAUSE = "com.chronotask.timer.action.PAUSE"
        const val ACTION_RESUME = "com.chronotask.timer.action.RESUME"
        const val ACTION_STOP = "com.chronotask.timer.action.STOP"
        const val EXTRA_TASK_ID = "com.chronotask.timer.extra.TASK_ID"

        private const val CHANNEL_ID = "active_timer"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_OPEN_TASK = 1002
        private const val REQUEST_PAUSE = 1003
        private const val REQUEST_RESUME = 1004
        private const val REQUEST_STOP = 1005

        /** 从用户操作启动计时前台服务。 */
        fun start(context: android.content.Context, taskId: Long) {
            val intent = Intent(context, TimerService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_TASK_ID, taskId)
            ContextCompat.startForegroundService(context, intent)
        }

        /** 在应用回到前台或进程恢复后同步前台服务状态。 */
        fun sync(context: android.content.Context) {
            if (TimerManager.getCurrentTaskId() == null) {
                context.stopService(Intent(context, TimerService::class.java))
                return
            }
            ContextCompat.startForegroundService(
                context,
                Intent(context, TimerService::class.java).setAction(ACTION_SYNC)
            )
        }

        /** 停止通知服务；会话数据必须先由 TimerManager 处理。 */
        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, TimerService::class.java))
        }
    }
}
