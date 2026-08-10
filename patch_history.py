import re

with open("app/src/main/java/com/example/ui/history/HistoryScreen.kt", "r") as f:
    content = f.read()

content = content.replace("val tasks by viewModel.allTasks.collectAsStateWithLifecycle()", 
"val tasks by viewModel.allTasks.collectAsStateWithLifecycle()\n    val allDailyScores by viewModel.allDailyScores.collectAsStateWithLifecycle()")

target = """                Text(
                    text = if (date == today) "TODAY" else date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )"""

replacement = """                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (date == today) "TODAY" else date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    val scoreForDate = allDailyScores.find { it.date == date.toString() }
                    if (scoreForDate != null) {
                        Text(
                            text = "Score: ${scoreForDate.score}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/history/HistoryScreen.kt", "w") as f:
    f.write(content)

