package com.example.domain

import com.example.data.entity.ActivityTask
import com.example.data.model.Priority
import org.junit.Assert.*
import org.junit.Test

class TaskFrictionEvaluatorTest {

    @Test
    fun testSingleFailureDoesNotTriggerHighFriction() {
        val task = ActivityTask(
            title = "Test Task",
            rescheduleCount = 1,
            frictionScore = 1.0f
        )

        assertFalse(TaskFrictionEvaluator.isHighFrictionTask(task))
    }

    @Test
    fun testHighFrictionDetection_reschedules() {
        val task = ActivityTask(
            title = "Postponed Task",
            rescheduleCount = 3,
            frictionScore = 3.0f
        )

        assertTrue(TaskFrictionEvaluator.isHighFrictionTask(task))
    }

    @Test
    fun testHighFrictionDetection_misses() {
        val task = ActivityTask(
            title = "Missed Task",
            missCount = 3,
            frictionScore = 4.5f
        )

        assertTrue(TaskFrictionEvaluator.isHighFrictionTask(task))
    }

    @Test
    fun testHighFrictionDetection_scoreThreshold() {
        val task = ActivityTask(
            title = "High Friction Task",
            frictionScore = 4.0f
        )

        assertTrue(TaskFrictionEvaluator.isHighFrictionTask(task))
    }

    @Test
    fun testCompletedTaskNotHighFriction() {
        val task = ActivityTask(
            title = "Done Task",
            isCompleted = true,
            rescheduleCount = 5,
            frictionScore = 10.0f
        )

        assertFalse(TaskFrictionEvaluator.isHighFrictionTask(task))
    }

    @Test
    fun testSuppressedTaskNotHighFriction() {
        val now = System.currentTimeMillis()
        val task = ActivityTask(
            title = "Suppressed Task",
            frictionScore = 5.0f,
            rescheduleCount = 4,
            frictionSuppressedUntil = now + (7 * 24 * 3600 * 1000L)
        )

        assertFalse(TaskFrictionEvaluator.isHighFrictionTask(task, currentTime = now))
    }

    @Test
    fun testRecordReschedule() {
        val initialTask = ActivityTask(title = "Task", rescheduleCount = 1, frictionScore = 1.0f)
        val rescheduled = TaskFrictionEvaluator.recordReschedule(initialTask, newDueDate = 2000000L)

        assertEquals(2, rescheduled.rescheduleCount)
        assertEquals(2.0f, rescheduled.frictionScore, 0.01f)
        assertNotNull(rescheduled.frictionSuppressedUntil)
    }

    @Test
    fun testRecordMiss() {
        val initialTask = ActivityTask(title = "Task", missCount = 0, frictionScore = 0f)
        val missed = TaskFrictionEvaluator.recordMiss(initialTask)

        assertEquals(1, missed.missCount)
        assertEquals(1.5f, missed.frictionScore, 0.01f)
    }

    @Test
    fun testRecordCompletion_onTime() {
        val initialTask = ActivityTask(
            title = "Task",
            dueDate = 2000L,
            frictionScore = 2.5f
        )
        val completed = TaskFrictionEvaluator.recordCompletion(initialTask, completedAt = 1500L)

        assertTrue(completed.isCompleted)
        assertEquals(1.5f, completed.frictionScore, 0.01f)
    }

    @Test
    fun testRecordCompletion_late() {
        val initialTask = ActivityTask(
            title = "Task",
            dueDate = 1000L,
            frictionScore = 2.0f
        )
        val completed = TaskFrictionEvaluator.recordCompletion(initialTask, completedAt = 2000L)

        assertTrue(completed.isCompleted)
        assertEquals(1, completed.lateCompletionCount)
        assertEquals(2.5f, completed.frictionScore, 0.01f)
    }

    @Test
    fun testLowerPriority() {
        val highTask = ActivityTask(title = "High Task", priority = Priority.High)
        val loweredMed = TaskFrictionEvaluator.lowerPriority(highTask)
        assertEquals(Priority.Medium, loweredMed.priority)

        val loweredLow = TaskFrictionEvaluator.lowerPriority(loweredMed)
        assertEquals(Priority.Low, loweredLow.priority)

        val lowest = TaskFrictionEvaluator.lowerPriority(loweredLow)
        assertEquals(Priority.Low, lowest.priority)
    }

    @Test
    fun testCreateSubtasksForParent() {
        val parent = ActivityTask(id = "parent-1", title = "Big Task", category = "Work", priority = Priority.High)
        val subtaskTitles = listOf("Part 1", "Part 2", "Part 3")

        val subtasks = TaskFrictionEvaluator.createSubtasksForParent(parent, subtaskTitles)

        assertEquals(3, subtasks.size)
        subtasks.forEach {
            assertEquals("parent-1", it.parentTaskId)
            assertEquals("Work", it.category)
            assertEquals(Priority.High, it.priority)
        }
        assertEquals("Part 1", subtasks[0].title)
        assertEquals("Part 2", subtasks[1].title)
        assertEquals("Part 3", subtasks[2].title)
    }
}
