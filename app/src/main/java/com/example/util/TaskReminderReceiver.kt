package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.database.KisekiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            // Reschedule active reminders after reboot
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = KisekiDatabase.getDatabase(context)
                    val tasks = db.activityTaskDao().getAllTasksOneShot()
                    val now = System.currentTimeMillis()
                    tasks.filter { !it.isCompleted && it.isReminderEnabled && it.reminderTime != null && it.reminderTime > now }
                        .forEach { task ->
                            ReminderScheduler.scheduleOrCancelReminder(context, task)
                        }
                    ReminderScheduler.scheduleEndOfDayReview(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (intent.action == "com.example.ACTION_END_OF_DAY_REVIEW" || intent.getBooleanExtra("IS_END_OF_DAY_REVIEW", false)) {
            ReminderScheduler.showEndOfDayReviewNotification(context)
            return
        }

        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        val reminderText = intent.getStringExtra("REMINDER_TEXT") ?: "It's time for your task!"

        ReminderScheduler.showNotification(context, taskId, taskTitle, reminderText)
    }
}
