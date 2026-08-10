import re

with open("app/src/main/java/com/example/ui/home/HomeScreen.kt", "r") as f:
    content = f.read()

target = """            Spacer(modifier = Modifier.height(12.dp))

            // Main Content"""

replacement = """            Spacer(modifier = Modifier.height(12.dp))

            val scoreForSelectedDate = allDailyScores.find { it.date == selectedDate.toString() }
            if (scoreForSelectedDate != null && (totalSelectedDateTasks > 0 || scoreForSelectedDate.score > 0)) {
                com.example.ui.components.DailyScoreCard(score = scoreForSelectedDate, selectedDate = selectedDate, today = today)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Main Content"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/home/HomeScreen.kt", "w") as f:
    f.write(content)

