package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.DailyScore
import com.example.domain.dailyScoreToRank
import java.time.LocalDate

@Composable
fun DailyScoreCard(
    score: DailyScore,
    selectedDate: LocalDate,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val rank = remember(score.score) { dailyScoreToRank(score.score) }

    val animatedScoreProgress by animateFloatAsState(
        targetValue = (score.score / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "DailyScoreProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val dateLabel = if (selectedDate == today) "TODAY" else selectedDate.toString()
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Daily Score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DAILY RANK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AnimatedContent(
                            targetState = rank,
                            transitionSpec = {
                                (fadeIn(tween(220, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220, easing = FastOutSlowInEasing))) togetherWith
                                        (fadeOut(tween(160, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 0.96f, animationSpec = tween(160, easing = FastOutSlowInEasing)))
                            },
                            label = "DailyRankTransition"
                        ) { targetRank ->
                            Text(
                                text = targetRank,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                ),
                                color = if (targetRank == "S") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { animatedScoreProgress },
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp,
                            color = if (score.score >= 80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        )
                        AnimatedContent(
                            targetState = score.score,
                            transitionSpec = {
                                (fadeIn(tween(200, easing = FastOutSlowInEasing)) + slideInVertically(tween(200, easing = FastOutSlowInEasing)) { -it / 4 }) togetherWith
                                        (fadeOut(tween(180, easing = FastOutSlowInEasing)) + slideOutVertically(tween(180, easing = FastOutSlowInEasing)) { it / 4 })
                            },
                            label = "ScoreTextTransition"
                        ) { targetScore ->
                            Text(
                                text = "$targetScore",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScoreMetric("Tasks", score.completionScore)
                ScoreMetric("Priority", score.priorityPerformance)
                ScoreMetric("Timing", score.onTimeScore)
                ScoreMetric("Consistency", score.consistencyScore)
            }
        }
    }
}

@Composable
private fun ScoreMetric(label: String, value: Float) {
    val pct = (value * 100).toInt()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = pct,
            transitionSpec = {
                (fadeIn(tween(200, easing = FastOutSlowInEasing)) + slideInVertically(tween(200, easing = FastOutSlowInEasing)) { -it / 4 }) togetherWith
                        (fadeOut(tween(180, easing = FastOutSlowInEasing)) + slideOutVertically(tween(180, easing = FastOutSlowInEasing)) { it / 4 })
            },
            label = "ScoreMetricText"
        ) { targetPct ->
            Text(
                text = "$targetPct%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
