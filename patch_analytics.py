import re

with open("app/src/main/java/com/example/ui/analytics/AnalyticsScreen.kt", "r") as f:
    content = f.read()

content = content.replace("val tasks by viewModel.allTasks.collectAsStateWithLifecycle()", 
"val tasks by viewModel.allTasks.collectAsStateWithLifecycle()\n    val allDailyScores by viewModel.allDailyScores.collectAsStateWithLifecycle()")

target = """                AnalyticsSummaryCards(
                    completedLast7Days = analyticsData.completedLast7Days,
                    completedLast30Days = analyticsData.completedLast30Days,
                    cardColors = cardColors,
                    cardBorder = cardBorder,
                    themeMode = themeMode
                )"""

replacement = """                AnalyticsSummaryCards(
                    completedLast7Days = analyticsData.completedLast7Days,
                    completedLast30Days = analyticsData.completedLast30Days,
                    cardColors = cardColors,
                    cardBorder = cardBorder,
                    themeMode = themeMode
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                DailyScoreAnalytics(allDailyScores, today)"""

content = content.replace(target, replacement)

target2 = """@Composable
private fun AnalyticsSummaryCards"""

replacement2 = """@Composable
private fun DailyScoreAnalytics(scores: List<com.example.data.entity.DailyScore>, today: LocalDate) {
    val last7DaysScores = scores.filter { LocalDate.parse(it.date).isAfter(today.minusDays(7)) }
    val last30DaysScores = scores.filter { LocalDate.parse(it.date).isAfter(today.minusDays(30)) }
    
    val avg7 = if (last7DaysScores.isNotEmpty()) last7DaysScores.map { it.score }.average().toInt() else 0
    val avg30 = if (last30DaysScores.isNotEmpty()) last30DaysScores.map { it.score }.average().toInt() else 0
    val highest = if (scores.isNotEmpty()) scores.maxOf { it.score } else 0
    
    val trend = if (avg7 >= avg30) "Upward" else "Downward"

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Daily Score Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScoreInsight("7-Day Avg", avg7.toString())
                ScoreInsight("30-Day Avg", avg30.toString())
                ScoreInsight("Highest", highest.toString())
                ScoreInsight("Trend", trend)
            }
        }
    }
}

@Composable
private fun ScoreInsight(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AnalyticsSummaryCards"""

content = content.replace(target2, replacement2)


with open("app/src/main/java/com/example/ui/analytics/AnalyticsScreen.kt", "w") as f:
    f.write(content)

