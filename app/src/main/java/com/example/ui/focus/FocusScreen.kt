package com.example.ui.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ActivityTask
import com.example.data.repository.ThemeMode
import com.example.domain.FocusAnalyticsEvaluator
import com.example.domain.XpEvaluator
import com.example.viewmodel.ActivityTaskViewModel
import com.example.viewmodel.FocusTimerState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    taskId: String,
    viewModel: ActivityTaskViewModel,
    onNavigateBack: () -> Unit,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    modifier: Modifier = Modifier
) {
    val task by viewModel.getTaskById(taskId).collectAsStateWithLifecycle()
    val timerState by viewModel.focusTimerState.collectAsStateWithLifecycle()
    val isShadowMonarch = themeMode == ThemeMode.SHADOW_MONARCH

    val backgroundColor = if (isShadowMonarch) Color(0xFF0D1017) else MaterialTheme.colorScheme.background
    val surfaceColor = if (isShadowMonarch) Color(0xFF161B26) else MaterialTheme.colorScheme.surface
    val accentColor = if (isShadowMonarch) Color(0xFF7967E8) else MaterialTheme.colorScheme.primary
    val onSurfaceColor = if (isShadowMonarch) Color(0xFFE6E8EE) else MaterialTheme.colorScheme.onBackground
    val mutedColor = if (isShadowMonarch) Color(0xFF8B949E) else MaterialTheme.colorScheme.onSurfaceVariant

    var showCustomDurationDialog by remember { mutableStateOf(false) }
    var customMinutesInput by remember { mutableStateOf("") }
    var showFinishSheet by remember { mutableStateOf(false) }
    var showExitWarningDialog by remember { mutableStateOf(false) }
    var markTaskCompleteOnFinish by remember { mutableStateOf(true) }

    val focusManager = LocalFocusManager.current

    val isTimerRunning = timerState.isRunning
    val isTimerPaused = timerState.isPaused

    // Initialize timer for this task if needed or if not matching
    LaunchedEffect(taskId) {
        if (timerState.taskId != taskId && !timerState.isRunning && !timerState.isPaused) {
            // Task has default estimated duration or 25 min default preset
            val defaultDuration = task?.estimatedDurationMinutes?.takeIf { it > 0 } ?: 25
            viewModel.resetFocusTimer(defaultDuration)
        }
    }

    // Custom Duration Dialog
    if (showCustomDurationDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDurationDialog = false },
            title = {
                Text(
                    text = "Custom Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Set your focus session length in minutes:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = mutedColor
                    )
                    OutlinedTextField(
                        value = customMinutesInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 3) {
                                customMinutesInput = input
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        placeholder = { Text("e.g. 50") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            cursorColor = accentColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minutes = customMinutesInput.toIntOrNull()
                        if (minutes != null && minutes > 0) {
                            viewModel.setTargetFocusDuration(minutes)
                        }
                        showCustomDurationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Set Duration")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDurationDialog = false }) {
                    Text("Cancel", color = onSurfaceColor)
                }
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Exit Warning Dialog
    if (showExitWarningDialog) {
        val actualFocusedMs = remember(showExitWarningDialog) { viewModel.getActualFocusedDurationMs() }
        AlertDialog(
            onDismissRequest = { showExitWarningDialog = false },
            title = {
                Text(
                    text = "Leave Focus Session?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Your active focus session of ${FocusAnalyticsEvaluator.formatDuration(actualFocusedMs)} will be saved before exiting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sessionStart = if (timerState.sessionStartTime > 0) timerState.sessionStartTime else System.currentTimeMillis() - actualFocusedMs
                        viewModel.saveFocusSession(
                            taskId = taskId,
                            startTime = sessionStart,
                            endTime = System.currentTimeMillis(),
                            durationMs = actualFocusedMs,
                            isTaskCompleted = false,
                            onSaved = {
                                showExitWarningDialog = false
                                onNavigateBack()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Save & Exit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.resetFocusTimer()
                        showExitWarningDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Finish Session Bottom Sheet
    if (showFinishSheet) {
        val currentTask = task
        val actualFocusedMs = remember(showFinishSheet) { viewModel.getActualFocusedDurationMs() }
        val isEligibleForXp = actualFocusedMs >= XpEvaluator.FOCUS_SESSION_MIN_MS_FOR_XP

        ModalBottomSheet(
            onDismissRequest = { showFinishSheet = false },
            containerColor = surfaceColor,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Focus Session Complete",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = onSurfaceColor
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isShadowMonarch) Color(0xFF1E2433) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Objective", style = MaterialTheme.typography.bodyMedium, color = mutedColor)
                            Text(
                                text = currentTask?.title ?: "Task",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = onSurfaceColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Actual Focused Time", style = MaterialTheme.typography.bodyMedium, color = mutedColor)
                            Text(
                                text = FocusAnalyticsEvaluator.formatDuration(actualFocusedMs),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }

                        if (isEligibleForXp) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "✨ Eligible for +5 Focus XP Bonus!",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = accentColor
                                )
                            }
                        }
                    }
                }

                if (currentTask != null && !currentTask.isCompleted) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { markTaskCompleteOnFinish = !markTaskCompleteOnFinish },
                        shape = RoundedCornerShape(12.dp),
                        color = if (markTaskCompleteOnFinish) accentColor.copy(alpha = 0.1f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (markTaskCompleteOnFinish) accentColor else mutedColor.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = markTaskCompleteOnFinish,
                                onCheckedChange = { markTaskCompleteOnFinish = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentColor,
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mark task as completed",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = onSurfaceColor
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showFinishSheet = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = onSurfaceColor)
                    ) {
                        Text("Keep Focusing")
                    }

                    Button(
                        onClick = {
                            val sessionStart = if (timerState.sessionStartTime > 0) timerState.sessionStartTime else System.currentTimeMillis() - actualFocusedMs
                            val sessionEnd = System.currentTimeMillis()

                            if (currentTask != null && markTaskCompleteOnFinish && !currentTask.isCompleted) {
                                // Use the exact existing task completion flow:
                                viewModel.completeTaskFromFocus(
                                    task = currentTask,
                                    startTime = sessionStart,
                                    endTime = sessionEnd,
                                    durationMs = actualFocusedMs,
                                    onComplete = {
                                        showFinishSheet = false
                                        onNavigateBack()
                                    }
                                )
                            } else {
                                viewModel.saveFocusSession(
                                    taskId = taskId,
                                    startTime = sessionStart,
                                    endTime = sessionEnd,
                                    durationMs = actualFocusedMs,
                                    isTaskCompleted = currentTask?.isCompleted == true,
                                    onSaved = {
                                        showFinishSheet = false
                                        onNavigateBack()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Save & Finish", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isTimerRunning || isTimerPaused || timerState.accumulatedFocusedMs > 0) {
                            showExitWarningDialog = true
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isShadowMonarch) Color(0xFF1E2433) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Focus Mode",
                        tint = onSurfaceColor
                    )
                }

                if (task?.category?.isNotBlank() == true) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = task?.category ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Objective Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CURRENT OBJECTIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = mutedColor
                )

                Text(
                    text = task?.title ?: "Machine Learning Assignment",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    color = onSurfaceColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Display Timer (Centered Ring)
            FocusTimerDisplay(
                timerState = timerState,
                viewModel = viewModel,
                isShadowMonarch = isShadowMonarch,
                accentColor = accentColor,
                onSurfaceColor = onSurfaceColor,
                mutedColor = mutedColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Duration Presets (Selectable when idle)
            AnimatedVisibility(
                visible = !isTimerRunning && !isTimerPaused,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = listOf(25, 45, 60)
                    presets.forEach { minutes ->
                        val isSelected = timerState.targetDurationMinutes == minutes
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setTargetFocusDuration(minutes) },
                            label = { Text("${minutes} min", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.White,
                                containerColor = if (isShadowMonarch) Color(0xFF161B26) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                labelColor = onSurfaceColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    val isCustom = timerState.targetDurationMinutes !in presets
                    FilterChip(
                        selected = isCustom,
                        onClick = {
                            customMinutesInput = if (isCustom) timerState.targetDurationMinutes.toString() else ""
                            showCustomDurationDialog = true
                        },
                        label = {
                            Text(
                                text = if (isCustom) "${timerState.targetDurationMinutes}m (Custom)" else "Custom",
                                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Color.White,
                            containerColor = if (isShadowMonarch) Color(0xFF161B26) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            labelColor = onSurfaceColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Control Buttons (Start, Pause, Finish)
            val view = androidx.compose.ui.platform.LocalView.current
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isTimerRunning && !isTimerPaused) {
                    // Start Button
                    Button(
                        onClick = {
                            com.example.util.KisekiHaptics.performFocusStart(view)
                            viewModel.startFocusTimer(taskId, timerState.targetDurationMinutes)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Start Focus", modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Focus",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                } else if (isTimerRunning) {
                    // Running state: Pause & Finish
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                com.example.util.KisekiHaptics.performFocusAction(view)
                                viewModel.pauseFocusTimer()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = onSurfaceColor
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, mutedColor.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Rounded.Pause, contentDescription = "Pause", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                com.example.util.KisekiHaptics.performFocusFinish(view)
                                viewModel.pauseFocusTimer()
                                showFinishSheet = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Icon(Icons.Rounded.Flag, contentDescription = "Finish", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Finish", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    // Paused state: Resume, Finish, Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = {
                                com.example.util.KisekiHaptics.performFocusAction(view)
                                viewModel.resetFocusTimer()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isShadowMonarch) Color(0xFF1E2433) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Reset Timer", tint = mutedColor)
                        }

                        Button(
                            onClick = {
                                com.example.util.KisekiHaptics.performFocusAction(view)
                                viewModel.resumeFocusTimer()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = {
                                com.example.util.KisekiHaptics.performFocusFinish(view)
                                showFinishSheet = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isShadowMonarch) Color(0xFF2B2544) else MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Rounded.Flag,
                                contentDescription = "Finish",
                                modifier = Modifier.size(20.dp),
                                tint = if (isShadowMonarch) Color(0xFFD6CEFF) else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Finish",
                                fontWeight = FontWeight.Bold,
                                color = if (isShadowMonarch) Color(0xFFD6CEFF) else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusTimerDisplay(
    timerState: FocusTimerState,
    viewModel: ActivityTaskViewModel,
    isShadowMonarch: Boolean,
    accentColor: Color,
    onSurfaceColor: Color,
    mutedColor: Color,
    modifier: Modifier = Modifier
) {
    var ticker by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    LaunchedEffect(timerState.isRunning) {
        while (timerState.isRunning) {
            delay(200L)
            ticker = System.currentTimeMillis()
        }
    }

    val actualFocusedMs = remember(ticker, timerState) {
        viewModel.getActualFocusedDurationMs()
    }

    val targetMs = timerState.targetDurationMinutes * 60 * 1000L
    val remainingMs = maxOf(0L, targetMs - actualFocusedMs)
    val isTimerRunning = timerState.isRunning
    val isTimerPaused = timerState.isPaused
    val hasStarted = actualFocusedMs > 0 || isTimerRunning || isTimerPaused

    val progress = if (targetMs > 0L) {
        (actualFocusedMs.toFloat() / targetMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 250),
        label = "focusProgress"
    )

    fun formatTimerDisplay(ms: Long): String {
        val totalSeconds = (ms + 500) / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, remainingMinutes, seconds)
        } else {
            String.format("%02d:%02d", remainingMinutes, seconds)
        }
    }

    Box(
        modifier = modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = if (isShadowMonarch) Color(0xFF202638) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            strokeWidth = 6.dp
        )

        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = accentColor,
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val displayTime = if (hasStarted) {
                formatTimerDisplay(remainingMs)
            } else {
                formatTimerDisplay(targetMs)
            }

            Text(
                text = displayTime,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = onSurfaceColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isTimerRunning) "Focusing" else if (isTimerPaused) "Paused" else "${timerState.targetDurationMinutes} min target",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isTimerRunning) accentColor else mutedColor
            )
        }
    }
}
