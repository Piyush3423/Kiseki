package com.example.ui.taskdetails

import com.example.data.entity.ActivityTask
import com.example.data.model.Priority
import com.example.data.model.RepeatType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailsUiStateTest {

    @Test
    fun `task details ui state distinguishes loading success and not found`() {
        val sampleTask = ActivityTask(
            id = "task-123",
            title = "Write Unit Tests",
            description = "Ensure task details never flicker",
            category = "Engineering",
            groupId = null,
            priority = Priority.High,
            estimatedDurationMinutes = 45,
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000,
            isReminderEnabled = true,
            reminderTime = System.currentTimeMillis() + 86400000,
            repeatType = RepeatType.None,
            parentTaskId = null,
            customDays = null
        )

        val loadingState: TaskDetailsUiState = TaskDetailsUiState.Loading
        val successState: TaskDetailsUiState = TaskDetailsUiState.Success(sampleTask)
        val notFoundState: TaskDetailsUiState = TaskDetailsUiState.NotFound
        val errorState: TaskDetailsUiState = TaskDetailsUiState.Error("Database error")

        assertTrue(loadingState is TaskDetailsUiState.Loading)
        assertTrue(successState is TaskDetailsUiState.Success)
        assertEquals(sampleTask.id, (successState as TaskDetailsUiState.Success).task.id)
        assertTrue(notFoundState is TaskDetailsUiState.NotFound)
        assertTrue(errorState is TaskDetailsUiState.Error)
        assertEquals("Database error", (errorState as TaskDetailsUiState.Error).message)
    }

    @Test
    fun `flow transformation maps valid task to Success and null to NotFound`() = runTest {
        val sourceFlow = MutableStateFlow<ActivityTask?>(null)

        val sampleTask = ActivityTask(
            id = "task-456",
            title = "Deep Focus Session",
            category = "Work",
            priority = Priority.Medium
        )

        val recordedStates = mutableListOf<TaskDetailsUiState>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            sourceFlow.map { task ->
                if (task != null) {
                    TaskDetailsUiState.Success(task)
                } else {
                    TaskDetailsUiState.NotFound
                }
            }.collect {
                recordedStates.add(it)
            }
        }

        // When source flow is null, maps to NotFound
        assertEquals(listOf(TaskDetailsUiState.NotFound), recordedStates)

        // Once Room emits a valid task, maps to Success
        sourceFlow.value = sampleTask
        assertEquals(listOf(TaskDetailsUiState.NotFound, TaskDetailsUiState.Success(sampleTask)), recordedStates)

        // If task is updated in Room, stays in Success with updated data
        val updatedTask = sampleTask.copy(isCompleted = true)
        sourceFlow.value = updatedTask
        assertEquals(
            listOf(
                TaskDetailsUiState.NotFound,
                TaskDetailsUiState.Success(sampleTask),
                TaskDetailsUiState.Success(updatedTask)
            ),
            recordedStates
        )

        // If task is deleted in Room, maps to NotFound
        sourceFlow.value = null
        assertEquals(
            listOf(
                TaskDetailsUiState.NotFound,
                TaskDetailsUiState.Success(sampleTask),
                TaskDetailsUiState.Success(updatedTask),
                TaskDetailsUiState.NotFound
            ),
            recordedStates
        )

        collectJob.cancel()
    }
}
