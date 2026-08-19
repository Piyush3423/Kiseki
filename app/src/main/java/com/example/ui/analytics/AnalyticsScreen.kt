package com.example.ui.analytics

import com.example.ui.components.MonarchLogo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
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
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

import com.example.data.entity.DailyScore
import com.example.data.repository.ThemeMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.entity.ActivityTask
import com.example.data.entity.XpEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

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
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onNavigateToHistory: (() -> Unit)? = null
) {
    val isShadowMonarch = themeMode == ThemeMode.SHADOW_MONARCH
    val cardBorder = if (isShadowMonarch) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    }
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val allDailyScores by viewModel.allDailyScores.collectAsStateWithLifecycle()
    val momentumInfo by viewModel.momentumInfo.collectAsStateWithLifecycle()
    val whatChangedInsights by viewModel.whatChangedInsights.collectAsStateWithLifecycle()
    val allPersonalBests by viewModel.allPersonalBests.collectAsStateWithLifecycle()
    val levelInfo by viewModel.levelInfo.collectAsStateWithLifecycle()
    val xpThisWeek by viewModel.xpThisWeek.collectAsStateWithLifecycle()
    val xpThisMonth by viewModel.xpThisMonth.collectAsStateWithLifecycle()
    val allXpEvents by viewModel.allXpEvents.collectAsStateWithLifecycle()
    val allEndOfDayReviews by viewModel.allEndOfDayReviews.collectAsStateWithLifecycle()
    val focusAnalytics by viewModel.focusAnalyticsData.collectAsStateWithLifecycle()
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
                // Personal Level & XP Card
                PersonalXpCard(
                    levelInfo = levelInfo,
                    xpThisWeek = xpThisWeek,
                    xpThisMonth = xpThisMonth,
                    cardBorder = cardBorder,
                    isShadowMonarch = isShadowMonarch
                )

                // Focus Time Card
                FocusAnalyticsCard(
                    focusAnalytics = focusAnalytics,
                    cardBorder = cardBorder,
                    isShadowMonarch = isShadowMonarch
                )

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

                // Momentum Analytics Card
                MomentumAnalyticsCard(
                    momentumInfo = momentumInfo,
                    cardBorder = cardBorder,
                    isShadowMonarch = isShadowMonarch
                )

                // What Changed Insights Card
                WhatChangedAnalyticsCard(
                    insights = whatChangedInsights,
                    cardBorder = cardBorder,
                    isShadowMonarch = isShadowMonarch
                )

                // Daily Rank Analytics Card
                DailyRankAnalyticsCard(
                    allDailyScores = allDailyScores,
                    cardBorder = cardBorder,
                    isShadowMonarch = isShadowMonarch
                )

                // Activity Heatmap Card
                ActivityHeatmapCard(
                    tasks = tasks,
                    allDailyScores = allDailyScores,
                    allXpEvents = allXpEvents,
                    cardBorder = cardBorder,
                    isShadowMonarch = isShadowMonarch,
                    onNavigateToHistory = onNavigateToHistory
                )

                // Personal Bests Analytics Card
                PersonalBestsAnalyticsCard(
                    personalBests = allPersonalBests,
                    cardBorder = cardBorder,
                    isShadowMonarch = isShadowMonarch
                )

                // End-of-Day Obstacles & Friction Analytics Card
                EndOfDayObstaclesAnalyticsCard(
                    reviews = allEndOfDayReviews,
                    cardBorder = cardBorder,
                    isShadowMonarch = isShadowMonarch
                )

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

@Composable
fun PersonalXpCard(
    levelInfo: com.example.domain.LevelInfo,
    xpThisWeek: Int,
    xpThisMonth: Int,
    cardBorder: BorderStroke,
    isShadowMonarch: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Level & Progress",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Personal XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "LV. ${levelInfo.level}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Level Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Next Level Progress",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${levelInfo.currentLevelXp} / ${levelInfo.requiredXpForNextLevel} XP",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { levelInfo.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Grid of XP Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                XpStatItem(
                    label = "Total XP",
                    value = "${levelInfo.totalXp}",
                    modifier = Modifier.weight(1f)
                )
                XpStatItem(
                    label = "This Week",
                    value = "+$xpThisWeek",
                    modifier = Modifier.weight(1f)
                )
                XpStatItem(
                    label = "This Month",
                    value = "+$xpThisMonth",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun XpStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailyRankAnalyticsCard(
    allDailyScores: List<DailyScore>,
    cardBorder: BorderStroke,
    isShadowMonarch: Boolean,
    modifier: Modifier = Modifier
) {
    val rankCounts = remember(allDailyScores) {
        val counts = mutableMapOf("S" to 0, "A" to 0, "B" to 0, "C" to 0, "D" to 0, "E" to 0)
        allDailyScores.forEach { scoreObj ->
            val rank = com.example.domain.dailyScoreToRank(scoreObj.score)
            counts[rank] = (counts[rank] ?: 0) + 1
        }
        counts
    }

    val sRankDays = rankCounts["S"] ?: 0
    val aRankDays = rankCounts["A"] ?: 0

    val maxCount = remember(rankCounts) {
        (rankCounts.values.maxOrNull() ?: 0).coerceAtLeast(1)
    }

    val mostCommonRank = remember(rankCounts, allDailyScores) {
        if (allDailyScores.isNotEmpty()) {
            val maxVal = rankCounts.values.maxOrNull() ?: 0
            if (maxVal > 0) {
                rankCounts.entries.firstOrNull { it.value == maxVal }?.key ?: "-"
            } else "-"
        } else "-"
    }

    Card(
        modifier = modifier
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
                Column {
                    Text(
                        text = "Daily Rank Distribution",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ranks earned from daily scores",
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
                    Text(
                        text = "${allDailyScores.size} Days Tracked",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RankStatTile(
                    label = "S Rank Days",
                    value = "$sRankDays",
                    modifier = Modifier.weight(1f)
                )
                RankStatTile(
                    label = "A Rank Days",
                    value = "$aRankDays",
                    modifier = Modifier.weight(1f)
                )
                RankStatTile(
                    label = "Most Common Rank",
                    value = mostCommonRank,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val ranksOrder = listOf("S", "A", "B", "C", "D", "E")
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ranksOrder.forEach { rankKey ->
                    val count = rankCounts[rankKey] ?: 0
                    val fraction = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(durationMillis = 500),
                        label = "RankBarAnimation_$rankKey"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rankKey,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = if (rankKey == "S") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(28.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            if (animatedFraction > 0f) {
                                val barColor = when (rankKey) {
                                    "S" -> MaterialTheme.colorScheme.primary
                                    "A" -> MaterialTheme.colorScheme.secondary
                                    "B" -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = animatedFraction)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(barColor)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PersonalBestsAnalyticsCard(
    personalBests: List<com.example.data.entity.PersonalBest>,
    cardBorder: BorderStroke,
    isShadowMonarch: Boolean,
    modifier: Modifier = Modifier
) {
    val pbMap = remember(personalBests) {
        personalBests.associateBy { it.recordKey }
    }

    val highestScore = pbMap[com.example.domain.PersonalBestEvaluator.KEY_HIGHEST_SCORE]?.value ?: 0
    val mostTasks = pbMap[com.example.domain.PersonalBestEvaluator.KEY_MOST_TASKS]?.value ?: 0
    val bestXp = pbMap[com.example.domain.PersonalBestEvaluator.KEY_MOST_XP]?.value ?: 0
    val longestStreak = pbMap[com.example.domain.PersonalBestEvaluator.KEY_LONGEST_STREAK]?.value ?: 0
    val mostHp = pbMap[com.example.domain.PersonalBestEvaluator.KEY_MOST_HIGH_PRIORITY]?.value ?: 0

    Card(
        modifier = modifier
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "Personal Bests",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Personal Bests",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "All-time productivity records",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PersonalBestTile(
                    title = "Highest Daily Score",
                    value = if (highestScore > 0) "$highestScore" else "--",
                    modifier = Modifier.weight(1f)
                )
                PersonalBestTile(
                    title = "Most Tasks",
                    value = if (mostTasks > 0) "$mostTasks" else "--",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PersonalBestTile(
                    title = "Best XP Day",
                    value = if (bestXp > 0) "$bestXp XP" else "--",
                    modifier = Modifier.weight(1f)
                )
                PersonalBestTile(
                    title = "Longest Streak",
                    value = if (longestStreak > 0) "$longestStreak days" else "--",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            PersonalBestTile(
                title = "Most High Priority Completed",
                value = if (mostHp > 0) "$mostHp" else "--",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PersonalBestTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { androidx.compose.animation.core.Animatable(1f) }

    LaunchedEffect(value) {
        if (value != "--" && value.isNotBlank()) {
            scaleAnim.animateTo(0.97f, tween(100, easing = FastOutSlowInEasing))
            scaleAnim.animateTo(1.03f, tween(120, easing = FastOutSlowInEasing))
            scaleAnim.animateTo(1.0f, tween(110, easing = FastOutSlowInEasing))
        }
    }

    Surface(
        modifier = modifier.graphicsLayer {
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
        },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    (fadeIn(tween(180, easing = FastOutSlowInEasing)) + slideInVertically(tween(180, easing = FastOutSlowInEasing)) { -it / 4 }) togetherWith
                            (fadeOut(tween(160, easing = FastOutSlowInEasing)) + slideOutVertically(tween(160, easing = FastOutSlowInEasing)) { it / 4 })
                },
                label = "PersonalBestValueTransition"
            ) { targetValue ->
                Text(
                    text = targetValue,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun MomentumAnalyticsCard(
    momentumInfo: com.example.domain.MomentumResult,
    cardBorder: BorderStroke,
    isShadowMonarch: Boolean = false,
    modifier: Modifier = Modifier
) {
    val trendArrow = when (momentumInfo.trend) {
        com.example.domain.MomentumTrend.IMPROVING -> "↑"
        com.example.domain.MomentumTrend.STABLE -> "→"
        com.example.domain.MomentumTrend.DECLINING -> "↓"
    }

    val trendText = when (momentumInfo.trend) {
        com.example.domain.MomentumTrend.IMPROVING -> "improving"
        com.example.domain.MomentumTrend.STABLE -> "stable"
        com.example.domain.MomentumTrend.DECLINING -> "declining"
    }

    val trendColor = when (momentumInfo.trend) {
        com.example.domain.MomentumTrend.IMPROVING -> Color(0xFF34A853)
        com.example.domain.MomentumTrend.STABLE -> if (isShadowMonarch) Color(0xFFA9B0C0) else MaterialTheme.colorScheme.onSurfaceVariant
        com.example.domain.MomentumTrend.DECLINING -> Color(0xFFEA4335)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Momentum",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "14-day consistency weighted model",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (momentumInfo.hasEnoughHistory) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = trendColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, trendColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$trendArrow $trendText",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = trendColor
                            )
                        }
                    }
                }
            }

            if (!momentumInfo.hasEnoughHistory) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Need more history to compute momentum",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isShadowMonarch) Color(0xFF161B26) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Current Momentum",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${momentumInfo.currentMomentum}%",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isShadowMonarch) Color(0xFF161B26) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Last week",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${momentumInfo.lastWeekMomentum}%",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isShadowMonarch) Color(0xFF161B26) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val changeText = if (momentumInfo.change >= 0) "+${momentumInfo.change}%" else "${momentumInfo.change}%"
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = changeText,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = trendColor
                                )
                                Text(
                                    text = " $trendArrow",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = trendColor
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "14-Day Trend",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FourteenDayMomentumGraph(
                    history = momentumInfo.dailyMomentumHistory,
                    isShadowMonarch = isShadowMonarch
                )
            }
        }
    }
}

@Composable
fun FourteenDayMomentumGraph(
    history: List<Pair<String, Int>>,
    isShadowMonarch: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    val primaryColor = if (isShadowMonarch) Color(0xFF7967E8) else MaterialTheme.colorScheme.primary
    val gridColor = if (isShadowMonarch) Color(0xFF283044) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(vertical = 8.dp)
        ) {
            val width = size.width
            val height = size.height
            val numPoints = history.size
            if (numPoints < 2) return@Canvas

            val stepX = width / (numPoints - 1)

            listOf(0f, 0.5f, 1f).forEach { fraction ->
                val y = height * (1f - fraction)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val points = history.mapIndexed { index, pair ->
                val x = index * stepX
                val normVal = (pair.second / 100f).coerceIn(0f, 1f)
                val y = height * (1f - normVal)
                Offset(x, y)
            }

            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val pPrev = points[i - 1]
                    val pCurr = points[i]
                    val controlX1 = pPrev.x + (pCurr.x - pPrev.x) / 2f
                    val controlY1 = pPrev.y
                    val controlX2 = pPrev.x + (pCurr.x - pPrev.x) / 2f
                    val controlY2 = pCurr.y
                    cubicTo(controlX1, controlY1, controlX2, controlY2, pCurr.x, pCurr.y)
                }
            }

            val fillPath = Path().apply {
                addPath(path)
                lineTo(points.last().x, height)
                lineTo(points.first().x, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        primaryColor.copy(alpha = 0.02f)
                    )
                )
            )

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 2.5.dp.toPx())
            )

            points.forEachIndexed { index, pt ->
                val isToday = index == points.size - 1
                drawCircle(
                    color = primaryColor,
                    radius = if (isToday) 4.dp.toPx() else 2.5.dp.toPx(),
                    center = pt
                )
                if (isToday) {
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val firstDate = history.firstOrNull()?.first ?: ""
            val midDate = history.getOrNull(history.size / 2)?.first ?: ""
            val lastDate = history.lastOrNull()?.first ?: ""

            Text(
                text = formatShortDate(firstDate),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = labelColor
            )
            Text(
                text = formatShortDate(midDate),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = labelColor
            )
            Text(
                text = "Today (${formatShortDate(lastDate)})",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = labelColor
            )
        }
    }
}

private fun formatShortDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    return try {
        val parsed = LocalDate.parse(dateStr)
        val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        parsed.format(formatter)
    } catch (e: Exception) {
        dateStr
    }
}

enum class HeatmapRange(val label: String, val days: Int) {
    THREE_MONTHS("3 Months", 90),
    SIX_MONTHS("6 Months", 180),
    ONE_YEAR("1 Year", 365)
}

data class HeatmapDayData(
    val date: LocalDate,
    val score: Int,
    val rank: String,
    val completedTasks: Int,
    val totalTasks: Int,
    val xpEarned: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHeatmapCard(
    tasks: List<ActivityTask>,
    allDailyScores: List<DailyScore>,
    allXpEvents: List<XpEvent>,
    cardBorder: BorderStroke,
    isShadowMonarch: Boolean = false,
    onNavigateToHistory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(HeatmapRange.THREE_MONTHS) }
    val today = remember { LocalDate.now() }
    val zoneId = remember { ZoneId.systemDefault() }

    var selectedDayData by remember { mutableStateOf<HeatmapDayData?>(null) }

    val dayDataMap = remember(tasks, allDailyScores, allXpEvents, today, zoneId, selectedRange) {
        val startDate = today.minusDays((selectedRange.days - 1).toLong())
        val scoresMap = allDailyScores.associateBy { it.date }
        val computedScoresMap = com.example.domain.DailyScoreCalculator.calculateAllScores(tasks).associateBy { it.date }

        val xpByDate = mutableMapOf<String, Int>()
        allXpEvents.forEach { event ->
            val dateStr = Instant.ofEpochMilli(event.timestamp).atZone(zoneId).toLocalDate().toString()
            xpByDate[dateStr] = (xpByDate[dateStr] ?: 0) + event.amount
        }

        val completedByDate = mutableMapOf<String, Int>()
        tasks.forEach { task ->
            if (task.isCompleted && task.completedAt != null) {
                val cDateStr = Instant.ofEpochMilli(task.completedAt).atZone(zoneId).toLocalDate().toString()
                completedByDate[cDateStr] = (completedByDate[cDateStr] ?: 0) + 1
            }
        }

        val scheduledByDate = mutableMapOf<String, Int>()
        tasks.forEach { task ->
            val sDateStr = if (task.dueDate != null) {
                Instant.ofEpochMilli(task.dueDate).atZone(zoneId).toLocalDate().toString()
            } else {
                Instant.ofEpochMilli(task.createdAt).atZone(zoneId).toLocalDate().toString()
            }
            scheduledByDate[sDateStr] = (scheduledByDate[sDateStr] ?: 0) + 1
        }

        val map = mutableMapOf<LocalDate, HeatmapDayData>()
        var curr = startDate
        while (!curr.isAfter(today)) {
            val dateStr = curr.toString()
            val score = scoresMap[dateStr]?.score ?: computedScoresMap[dateStr]?.score ?: 0
            val rank = com.example.domain.dailyScoreToRank(score)
            val comp = completedByDate[dateStr] ?: 0
            val sched = scheduledByDate[dateStr] ?: 0
            val total = maxOf(sched, comp)
            val xp = xpByDate[dateStr] ?: 0

            map[curr] = HeatmapDayData(
                date = curr,
                score = score,
                rank = rank,
                completedTasks = comp,
                totalTasks = total,
                xpEarned = xp
            )
            curr = curr.plusDays(1)
        }
        map
    }

    androidx.compose.runtime.LaunchedEffect(dayDataMap, today) {
        if (selectedDayData == null || !dayDataMap.containsKey(selectedDayData?.date)) {
            selectedDayData = dayDataMap[today] ?: dayDataMap.values.lastOrNull()
        }
    }

    val weekColumns = remember(selectedRange, today) {
        val startDate = today.minusDays((selectedRange.days - 1).toLong())
        var current = startDate
        while (current.dayOfWeek != java.time.DayOfWeek.MONDAY) {
            current = current.minusDays(1)
        }

        val weeks = mutableListOf<List<LocalDate?>>()
        while (!current.isAfter(today)) {
            val week = (0..6).map { dayOffset ->
                val date = current.plusDays(dayOffset.toLong())
                if (date.isBefore(startDate) || date.isAfter(today)) null else date
            }
            weeks.add(week)
            current = current.plusDays(7)
        }
        weeks
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Activity Heatmap",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Daily score & completion heatmap",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HeatmapRange.values().forEach { range ->
                        val selected = selectedRange == range
                        FilterChip(
                            selected = selected,
                            onClick = { selectedRange = range },
                            label = {
                                Text(
                                    text = when(range) {
                                        HeatmapRange.THREE_MONTHS -> "3M"
                                        HeatmapRange.SIX_MONTHS -> "6M"
                                        HeatmapRange.ONE_YEAR -> "1Y"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }

            val scrollState = rememberScrollState()
            androidx.compose.runtime.LaunchedEffect(selectedRange) {
                scrollState.scrollTo(scrollState.maxValue)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.padding(top = 22.dp, end = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier.size(width = 12.dp, height = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index == 0 || index == 2 || index == 4) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    weekColumns.forEachIndexed { weekIndex, week ->
                        val firstValidDate = week.firstOrNull { it != null }
                        val prevWeekFirstValidDate = if (weekIndex > 0) weekColumns[weekIndex - 1].firstOrNull { it != null } else null

                        val isNewMonth = firstValidDate != null && (prevWeekFirstValidDate == null || firstValidDate.month != prevWeekFirstValidDate.month)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(18.dp)
                                    .width(12.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                if (isNewMonth && firstValidDate != null) {
                                    val monthStr = firstValidDate.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
                                    Text(
                                        text = monthStr,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            week.forEach { date ->
                                if (date == null) {
                                    Box(modifier = Modifier.size(12.dp))
                                } else {
                                    val dayData = dayDataMap[date] ?: HeatmapDayData(date, 0, "E", 0, 0, 0)
                                    val isSelected = selectedDayData?.date == date
                                    val cellColor = getHeatmapCellColor(dayData.score, isShadowMonarch)

                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(cellColor)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(
                                                        1.5.dp,
                                                        if (isShadowMonarch) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        RoundedCornerShape(3.dp)
                                                    )
                                                } else Modifier
                                            )
                                            .clickable {
                                                selectedDayData = dayData
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                listOf(0, 20, 50, 70, 88, 100).forEach { sampleScore ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getHeatmapCellColor(sampleScore, isShadowMonarch))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "More",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            selectedDayData?.let { dayData ->
                HeatmapDayDetailCard(
                    dayData = dayData,
                    isShadowMonarch = isShadowMonarch,
                    onNavigateToHistory = onNavigateToHistory
                )
            }
        }
    }
}

@Composable
fun HeatmapDayDetailCard(
    dayData: HeatmapDayData,
    isShadowMonarch: Boolean,
    onNavigateToHistory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault()) }
    val formattedDate = remember(dayData.date) { dayData.date.format(dateFormatter) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isShadowMonarch) Color(0xFF161B26) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (dayData.score > 0) {
                    val rankColor = when (dayData.rank) {
                        "S" -> MaterialTheme.colorScheme.primary
                        "A" -> MaterialTheme.colorScheme.secondary
                        "B" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = rankColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, rankColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Rank ${dayData.rank}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = rankColor
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailStatTile(
                    label = "Daily Score",
                    value = "${dayData.score}",
                    modifier = Modifier.weight(1f)
                )
                DetailStatTile(
                    label = "Rank",
                    value = dayData.rank,
                    modifier = Modifier.weight(1f)
                )
                DetailStatTile(
                    label = "Tasks",
                    value = "${dayData.completedTasks}/${dayData.totalTasks}",
                    modifier = Modifier.weight(1f)
                )
                DetailStatTile(
                    label = "XP",
                    value = "+${dayData.xpEarned}",
                    modifier = Modifier.weight(1f)
                )
            }

            if (onNavigateToHistory != null) {
                Button(
                    onClick = onNavigateToHistory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "View Day",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "View Day",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun getHeatmapCellColor(score: Int, isShadowMonarch: Boolean): Color {
    val basePrimary = if (isShadowMonarch) Color(0xFF7967E8) else MaterialTheme.colorScheme.primary
    val emptyColor = if (isShadowMonarch) {
        Color(0xFF1E2433)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    return when {
        score <= 0 -> emptyColor
        score in 1..39 -> basePrimary.copy(alpha = 0.20f)
        score in 40..59 -> basePrimary.copy(alpha = 0.40f)
        score in 60..79 -> basePrimary.copy(alpha = 0.65f)
        score in 80..94 -> basePrimary.copy(alpha = 0.85f)
        else -> basePrimary
    }
}

@Composable
fun WhatChangedAnalyticsCard(
    insights: List<com.example.domain.InsightItem>,
    cardBorder: BorderStroke,
    isShadowMonarch: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "WHAT CHANGED",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Behavioral shifts & productivity insights",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (insights.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isShadowMonarch) Color(0xFF161B26) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Not enough task history yet to detect insights. Keep completing daily tasks!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    insights.forEach { insight ->
                        InsightItemRow(
                            insight = insight,
                            isShadowMonarch = isShadowMonarch
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightItemRow(
    insight: com.example.domain.InsightItem,
    isShadowMonarch: Boolean,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(insight) {
        isVisible = true
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "InsightRowAlpha"
    )

    val offsetYAnim by animateFloatAsState(
        targetValue = if (isVisible) 0f else 6f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "InsightRowOffsetY"
    )

    val (iconText, iconColor, badgeBg) = when (insight.type) {
        com.example.domain.InsightType.MAJOR_IMPROVEMENT -> Triple(
            "↑",
            Color(0xFF34A853),
            Color(0xFF34A853).copy(alpha = 0.15f)
        )
        com.example.domain.InsightType.MAJOR_DECLINE -> Triple(
            "↓",
            Color(0xFFEA4335),
            Color(0xFFEA4335).copy(alpha = 0.15f)
        )
        com.example.domain.InsightType.REPEATED_PATTERN -> Triple(
            "↻",
            if (isShadowMonarch) Color(0xFF9D8CFF) else MaterialTheme.colorScheme.primary,
            if (isShadowMonarch) Color(0xFF9D8CFF).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
        com.example.domain.InsightType.ACTIONABLE_OBSERVATION -> Triple(
            "💡",
            if (isShadowMonarch) Color(0xFFFFB74D) else Color(0xFFF57C00),
            if (isShadowMonarch) Color(0xFFFFB74D).copy(alpha = 0.15f) else Color(0xFFFFF3E0)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = alphaAnim
                translationY = offsetYAnim.dp.toPx()
            },
        shape = RoundedCornerShape(16.dp),
        color = if (isShadowMonarch) Color(0xFF161B26) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeBg,
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = iconText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = iconColor
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = insight.period,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = iconColor
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${insight.dataCount} data points",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusAnalyticsCard(
    focusAnalytics: com.example.domain.FocusAnalyticsData,
    cardBorder: BorderStroke,
    isShadowMonarch: Boolean
) {
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
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isShadowMonarch) Color(0xFF2B2544) else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = if (isShadowMonarch) Color(0xFF7967E8) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Focus Time",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (focusAnalytics.totalSessionsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isShadowMonarch) Color(0xFF202638) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${focusAnalytics.totalSessionsCount} sessions",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 3-Column Stats: Focus today, Focus this week, Average session
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Focus today
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Focus today",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = focusAnalytics.focusTodayFormatted,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isShadowMonarch) Color(0xFF7967E8) else MaterialTheme.colorScheme.primary
                    )
                }

                // Focus this week
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Focus this week",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = focusAnalytics.focusWeekFormatted,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Average session
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Average session",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = focusAnalytics.averageSessionFormatted,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

