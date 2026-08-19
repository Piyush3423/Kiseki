package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.data.database.KisekiDatabase

class TodayWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = KisekiDatabase.getDatabase(context)
        val allTasks = try {
            database.activityTaskDao().getAllTasksOneShot()
        } catch (e: Exception) {
            emptyList()
        }
        val stats = TodayWidgetDataHelper.computeTodayStats(allTasks)

        val homeIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_ACTION", "TODAY")
        }

        val taskIntent = if (!stats.nextTaskId.isNullOrBlank()) {
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NAVIGATE_TASK_ID", stats.nextTaskId)
            }
        } else {
            homeIntent
        }

        provideContent {
            GlanceTheme {
                TodayWidgetContent(
                    stats = stats,
                    onOpenHome = actionStartActivity(homeIntent),
                    onOpenTask = actionStartActivity(taskIntent)
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun TodayWidgetContent(
    stats: TodayWidgetStats,
    onOpenHome: androidx.glance.action.Action,
    onOpenTask: androidx.glance.action.Action
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(18.dp)
            .padding(14.dp)
            .clickable(onOpenHome)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) {
            // Header Row: TODAY & Percentage
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${stats.percentage}%",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(3.dp))

            // Completed count
            Text(
                text = "${stats.completedCount} / ${stats.totalCount} completed",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Progress bar
            val progressFraction = if (stats.totalCount > 0) {
                stats.completedCount.toFloat() / stats.totalCount.toFloat()
            } else {
                0f
            }

            LinearProgressIndicator(
                progress = progressFraction.coerceIn(0f, 1f),
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.surfaceVariant
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Next Task Container
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(12.dp)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .clickable(onOpenTask)
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Next:",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = stats.nextTaskTitle,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
