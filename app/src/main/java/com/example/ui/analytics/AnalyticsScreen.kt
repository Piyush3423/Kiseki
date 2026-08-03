package com.example.ui.analytics

import com.example.ui.components.MonarchLogo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.ActivityTaskViewModel

import com.example.data.repository.ThemeMode
import androidx.compose.foundation.BorderStroke

private data class DailyCompletion(
    val date: LocalDate,
    val dayLabel: String,
    val count: Int,
    val isToday: Boolean
)

private data class AnalyticsData(
    val scheduledTodayCount: Int,
    val completedTodayCount: Int,
    val pendingTodayCount: Int,
    val completionPercentage: Int,
    val last7DaysCompletedCount: Int,
    val currentStreak: Int,
    val totalTasksCount: Int,
    val dailyCompletions: List<DailyCompletion>
)

private fun getRankForStreak(streak: Int): String {
    return when {
        streak >= 50 -> "Rank Monarch"
        streak >= 30 -> "Rank S"
        streak >= 14 -> "Rank A"
        streak >= 7 -> "Rank B"
        streak >= 4 -> "Rank C"
        streak >= 2 -> "Rank D"
        else -> "Rank E"
    }
}

@Composable
fun AnalyticsScreen(
    viewModel: ActivityTaskViewModel,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val isShadowMonarch = themeMode == ThemeMode.SHADOW_MONARCH
    val cardBorder = if (isShadowMonarch) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    }
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val zoneId = remember { ZoneId.systemDefault() }

    val analyticsData = remember(tasks, today, zoneId) {
        val tasksScheduledToday = tasks.filter { task ->
            if (task.dueDate != null) {
                val taskDate = Instant.ofEpochMilli(task.dueDate).atZone(zoneId).toLocalDate()
                taskDate == today
            } else {
                true
            }
        }

        val tasksCompletedToday = tasks.filter { task ->
            task.isCompleted && task.completedAt != null &&
                    Instant.ofEpochMilli(task.completedAt).atZone(zoneId).toLocalDate() == today
        }

        val schedCount = tasksScheduledToday.size
        val compTodayCount = tasksCompletedToday.size
        val pendingCount = tasksScheduledToday.count { !it.isCompleted }

        val percentage = if (schedCount > 0) {
            ((compTodayCount.toFloat() / schedCount.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        } else {
            if (compTodayCount > 0) 100 else 0
        }

        val sevenDaysAgo = today.minusDays(6)
        val completedLast7Days = tasks.count { task ->
            task.isCompleted && task.completedAt != null && run {
                val date = Instant.ofEpochMilli(task.completedAt).atZone(zoneId).toLocalDate()
                !date.isBefore(sevenDaysAgo) && !date.isAfter(today)
            }
        }

        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        val dailyCompletions = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val count = tasks.count { task ->
                task.isCompleted && task.completedAt != null &&
                        Instant.ofEpochMilli(task.completedAt).atZone(zoneId).toLocalDate() == date
            }
            DailyCompletion(
                date = date,
                dayLabel = if (date == today) "Today" else date.format(dayFormatter),
                count = count,
                isToday = date == today
            )
        }

        val completedDatesSet = tasks
            .filter { it.isCompleted && it.completedAt != null }
            .map { Instant.ofEpochMilli(it.completedAt!!).atZone(zoneId).toLocalDate() }
            .toSet()

        var streak = 0
        var checkDate = when {
            today in completedDatesSet -> today
            today.minusDays(1) in completedDatesSet -> today.minusDays(1)
            else -> null
        }

        while (checkDate != null && checkDate in completedDatesSet) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        AnalyticsData(
            scheduledTodayCount = schedCount,
            completedTodayCount = compTodayCount,
            pendingTodayCount = pendingCount,
            completionPercentage = percentage,
            last7DaysCompletedCount = completedLast7Days,
            currentStreak = streak,
            totalTasksCount = tasks.size,
            dailyCompletions = dailyCompletions
        )
    }

    val animatedProgress by animateFloatAsState(
        targetValue = analyticsData.completionPercentage / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "ProgressAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Track your productivity and progress",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Analytics,
                        contentDescription = "Analytics",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (analyticsData.totalTasksCount == 0) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "No activity data yet",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create and complete tasks to see your analytics and streaks here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Today's Progress Hero Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(cardBorder, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isShadowMonarch) "Level Progress" else "Today's Completion",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "${analyticsData.completionPercentage}%",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "${analyticsData.completedTodayCount} of ${analyticsData.scheduledTodayCount} scheduled tasks completed today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 7-Day Completion Bar Chart Card
                SevenDayCompletionChart(dailyCompletions = analyticsData.dailyCompletions)

                // Grid Stats Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTile(
                        title = "Tasks Scheduled Today",
                        value = "${analyticsData.scheduledTodayCount}",
                        icon = Icons.Rounded.CalendarMonth,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    MetricTile(
                        title = "Tasks Pending Today",
                        value = "${analyticsData.pendingTodayCount}",
                        icon = Icons.Rounded.PendingActions,
                        iconTint = Color(0xFFF29900),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTile(
                        title = "Completed Today",
                        value = "${analyticsData.completedTodayCount}",
                        icon = Icons.Rounded.CheckCircle,
                        iconTint = Color(0xFF34A853),
                        modifier = Modifier.weight(1f)
                    )

                    MetricTile(
                        title = "Last 7 Days",
                        value = "${analyticsData.last7DaysCompletedCount}",
                        icon = Icons.Rounded.DateRange,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Streak Banner Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(cardBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val streakColor = if (isShadowMonarch) MaterialTheme.colorScheme.primary else Color(0xFFFF6D00)
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(streakColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Whatshot,
                                contentDescription = "Streak",
                                tint = streakColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Current Streak",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isShadowMonarch) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        MonarchLogo(
                                            size = 14.dp,
                                            showAura = false,
                                            isSelected = true
                                        )
                                        Text(
                                            text = getRankForStreak(analyticsData.currentStreak),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (analyticsData.currentStreak == 1) "1 Day Active" else "${analyticsData.currentStreak} Days Consecutive",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${analyticsData.currentStreak}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = streakColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SevenDayCompletionChart(
    dailyCompletions: List<DailyCompletion>,
    modifier: Modifier = Modifier
) {
    val maxCount = remember(dailyCompletions) {
        (dailyCompletions.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "7-Day Activity",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Completed tasks by day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val total7Days = dailyCompletions.sumOf { it.count }
                    Text(
                        text = "$total7Days Done",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bars Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyCompletions.forEach { daily ->
                    val fraction = (daily.count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(durationMillis = 500),
                        label = "BarHeightAnimation_${daily.date}"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Count above bar
                        Text(
                            text = "${daily.count}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (daily.isToday) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (daily.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bar Track
                        Box(
                            modifier = Modifier
                                .height(110.dp)
                                .width(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (daily.count > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(animatedFraction)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (daily.isToday) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.tertiary
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Day Label
                        Text(
                            text = daily.dayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (daily.isToday) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = if (daily.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

