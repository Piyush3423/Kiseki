import re

with open('app/src/main/java/com/example/ui/home/HomeScreen.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r'fun TaskItemCard\(\s*task: ActivityTask,\s*onToggleComplete: \(ActivityTask\) -> Unit,\s*onDelete: \(ActivityTask\) -> Unit,\s*onEdit: \(ActivityTask\) -> Unit,\s*modifier: Modifier = Modifier\s*\)\s*\{\s*val alpha = if \(task.isCompleted\) 0.6f else 1f\s*Box\(\s*modifier = modifier\s*\.fillMaxWidth\(\)\s*\.clip\(RoundedCornerShape\(16.dp\)\)\s*\.background\(MaterialTheme.colorScheme.surface\)\s*\.border\(1.dp, MaterialTheme.colorScheme.outline.copy\(alpha = 0.5f\), RoundedCornerShape\(16.dp\)\)\s*\.padding\(16.dp\)\s*\.alpha\(alpha\)\s*\) \{', re.DOTALL)

replacement = """fun TaskItemCard(
    task: ActivityTask,
    onToggleComplete: (ActivityTask) -> Unit,
    onDelete: (ActivityTask) -> Unit,
    onEdit: (ActivityTask) -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val alpha = if (task.isCompleted) 0.6f else 1f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .alpha(alpha)
    ) {"""

if pattern.search(content):
    content = pattern.sub(replacement, content, count=1)
    with open('app/src/main/java/com/example/ui/home/HomeScreen.kt', 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Failed to match")
