package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.data.entity.ActivityTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val CHANNEL_ID = "task_reminders_channel"
    const val CHANNEL_NAME = "Task Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for task reminders and due dates"
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleOrCancelReminder(context: Context, task: ActivityTask) {
        createNotificationChannel(context)

        val prefsRepo = com.example.data.repository.UserPreferencesRepository(context)
        val isGlobalEnabled = prefsRepo.isRemindersEnabledSync()

        val isScheduled = isGlobalEnabled &&
                !task.isCompleted &&
                task.isReminderEnabled &&
                task.reminderTime != null &&
                task.reminderTime > System.currentTimeMillis()

        if (isScheduled) {
            scheduleReminder(context, task)
        } else {
            cancelReminder(context, task.id)
        }
    }

    private fun scheduleReminder(context: Context, task: ActivityTask) {
        val reminderTime = task.reminderTime ?: return
        val delayMillis = (reminderTime - System.currentTimeMillis()).coerceAtLeast(0)
        val reminderText = formatReminderText(task.dueDate, reminderTime)

        // 1. WorkManager
        val data = workDataOf(
            "TASK_ID" to task.id,
            "TASK_TITLE" to task.title,
            "REMINDER_TEXT" to reminderText
        )

        val workRequest = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_${task.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        // 2. AlarmManager fallback for exact timing
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
            putExtra("REMINDER_TEXT", reminderText)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, taskId: String) {
        // 1. Cancel WorkManager
        try {
            WorkManager.getInstance(context).cancelUniqueWork("reminder_$taskId")
        } catch (e: Exception) {
            // Ignore if WorkManager not initialized
        }

        // 2. Cancel AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        // 3. Dismiss existing notification if visible
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(taskId.hashCode())
    }

    fun showNotification(context: Context, taskId: String, taskTitle: String, reminderText: String) {
        val prefsRepo = com.example.data.repository.UserPreferencesRepository(context)
        if (!prefsRepo.isRemindersEnabledSync()) {
            return
        }

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("NAVIGATE_TASK_ID", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(taskTitle)
            .setContentText(reminderText)
            .setSubText("Task Reminder")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(taskId.hashCode(), notification)
    }

    fun scheduleEndOfDayReview(context: Context, timeStr: String? = null) {
        createNotificationChannel(context)
        val prefsRepo = com.example.data.repository.UserPreferencesRepository(context)
        val isEnabled = prefsRepo.isEndOfDayReviewEnabledSync()
        val isRemindersEnabled = prefsRepo.isRemindersEnabledSync()
        if (!isEnabled || !isRemindersEnabled) {
            cancelEndOfDayReview(context)
            return
        }

        val time = timeStr ?: prefsRepo.getEndOfDayReviewTimeSync()
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 21
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = "com.example.ACTION_END_OF_DAY_REVIEW"
            putExtra("IS_END_OF_DAY_REVIEW", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            888999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelEndOfDayReview(context: Context) {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = "com.example.ACTION_END_OF_DAY_REVIEW"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            888999,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(888999)
    }

    fun showEndOfDayReviewNotification(context: Context) {
        val prefsRepo = com.example.data.repository.UserPreferencesRepository(context)
        if (!prefsRepo.isEndOfDayReviewEnabledSync() || !prefsRepo.isRemindersEnabledSync()) {
            return
        }

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_END_OF_DAY_REVIEW", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            888999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Day Complete • End-of-Day Review")
            .setContentText("Take 15 seconds to review today's achievements and obstacles.")
            .setSubText("End-of-Day Review")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(888999, notification)

        // Reschedule for next day
        scheduleEndOfDayReview(context)
    }

    private fun formatReminderText(dueDate: Long?, reminderTime: Long): String {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val dateOnlyFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        return if (dueDate != null) {
            "Due: ${dateOnlyFormat.format(Date(dueDate))} • Reminder: ${timeFormat.format(Date(reminderTime))}"
        } else {
            "Reminder: ${dateFormat.format(Date(reminderTime))}"
        }
    }
}

class TaskReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val taskId = inputData.getString("TASK_ID") ?: return Result.failure()
        val taskTitle = inputData.getString("TASK_TITLE") ?: "Task Reminder"
        val reminderText = inputData.getString("REMINDER_TEXT") ?: "It's time for your task!"

        ReminderScheduler.showNotification(context, taskId, taskTitle, reminderText)
        return Result.success()
    }
}
