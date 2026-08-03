package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.database.KisekiDatabase
import com.example.data.entity.ActivityTask
import com.example.data.entity.Category
import com.example.data.entity.TaskGroup
import com.example.data.entity.TaskGroupTemplate
import com.example.data.model.Priority
import com.example.data.model.RepeatType
import com.example.util.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class BackupData(
    val categories: List<Category>,
    val tasks: List<ActivityTask>,
    val groups: List<TaskGroup> = emptyList(),
    val templates: List<com.example.data.entity.TaskGroupTemplate> = emptyList()
)

class BackupRepository(private val context: Context) {

    private val db = KisekiDatabase.getDatabase(context)

    suspend fun exportBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val tasks = db.activityTaskDao().getAllTasksOneShot()
            val categories = db.categoryDao().getAllCategoriesOneShot()
            val groups = db.taskGroupDao().getAllGroupsOneShot()
            val templates = db.taskGroupTemplateDao().getAllTemplatesOneShot()

            val rootJson = JSONObject().apply {
                put("app", "Kiseki")
                put("version", 1)
                put("exportedAt", System.currentTimeMillis())

                val categoriesArray = JSONArray()
                categories.forEach { cat ->
                    val catObj = JSONObject().apply {
                        put("id", cat.id)
                        put("name", cat.name)
                        put("colorHex", cat.colorHex)
                        put("isDefault", cat.isDefault)
                    }
                    categoriesArray.put(catObj)
                }
                put("categories", categoriesArray)

                val groupsArray = JSONArray()
                groups.forEach { group ->
                    val groupObj = JSONObject().apply {
                        put("id", group.id)
                        put("name", group.name)
                        put("color", group.color ?: JSONObject.NULL)
                        put("createdAt", group.createdAt)
                        put("updatedAt", group.updatedAt)
                    }
                    groupsArray.put(groupObj)
                }
                put("groups", groupsArray)

                val templatesArray = JSONArray()
                templates.forEach { tmpl ->
                    val tmplObj = JSONObject().apply {
                        put("id", tmpl.id)
                        put("name", tmpl.name)
                        put("color", tmpl.color ?: JSONObject.NULL)
                        put("createdAt", tmpl.createdAt)
                        put("itemsJson", tmpl.itemsJson)
                    }
                    templatesArray.put(tmplObj)
                }
                put("templates", templatesArray)

                val tasksArray = JSONArray()
                tasks.forEach { task ->
                    val taskObj = JSONObject().apply {
                        put("id", task.id)
                        put("title", task.title)
                        put("description", task.description)
                        put("category", task.category)
                        put("priority", task.priority.name)
                        put("isCompleted", task.isCompleted)
                        put("completedAt", task.completedAt ?: JSONObject.NULL)
                        put("createdAt", task.createdAt)
                        put("dueDate", task.dueDate ?: JSONObject.NULL)
                        put("repeatType", task.repeatType.name)
                        put("parentTaskId", task.parentTaskId ?: JSONObject.NULL)
                        put("customDays", task.customDays ?: JSONObject.NULL)
                        put("isReminderEnabled", task.isReminderEnabled)
                        put("reminderTime", task.reminderTime ?: JSONObject.NULL)
                        put("groupId", task.groupId ?: JSONObject.NULL)
                    }
                    tasksArray.put(taskObj)
                }
                put("tasks", tasksArray)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            } ?: return@withContext Result.failure(Exception("Could not open output stream for file URI"))

            Result.success(tasks.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateAndReadBackup(uri: Uri): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext Result.failure(Exception("Unable to read selected file"))

            if (jsonString.length > 10_000_000) {
                return@withContext Result.failure(Exception("Backup file exceeds maximum allowed size"))
            }

            val rootObj = JSONObject(jsonString)

            if (!rootObj.has("tasks") && !rootObj.has("categories")) {
                return@withContext Result.failure(Exception("Invalid backup file: Missing Kiseki data structure"))
            }

            val categoriesList = mutableListOf<Category>()
            if (rootObj.has("categories")) {
                val catArray = rootObj.getJSONArray("categories")
                if (catArray.length() > 1000) {
                    return@withContext Result.failure(Exception("Too many categories in backup"))
                }
                for (i in 0 until catArray.length()) {
                    val catObj = catArray.getJSONObject(i)
                    val id = catObj.optString("id")
                    val name = catObj.optString("name")
                    if (id.isNotBlank() && name.isNotBlank()) {
                        categoriesList.add(
                            Category(
                                id = id,
                                name = name,
                                colorHex = catObj.optString("colorHex", "#6750A4"),
                                isDefault = catObj.optBoolean("isDefault", false)
                            )
                        )
                    }
                }
            }

            val groupsList = mutableListOf<TaskGroup>()
            if (rootObj.has("groups")) {
                val groupArray = rootObj.getJSONArray("groups")
                if (groupArray.length() > 1000) {
                    return@withContext Result.failure(Exception("Too many groups in backup"))
                }
                for (i in 0 until groupArray.length()) {
                    val groupObj = groupArray.getJSONObject(i)
                    val id = groupObj.optString("id")
                    val name = groupObj.optString("name")
                    if (id.isNotBlank() && name.isNotBlank()) {
                        groupsList.add(
                            TaskGroup(
                                id = id,
                                name = name,
                                color = if (groupObj.isNull("color")) null else groupObj.optInt("color"),
                                createdAt = groupObj.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = groupObj.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }

            val tasksList = mutableListOf<ActivityTask>()
            if (rootObj.has("tasks")) {
                val taskArray = rootObj.getJSONArray("tasks")
                if (taskArray.length() > 20000) {
                    return@withContext Result.failure(Exception("Too many tasks in backup"))
                }
                for (i in 0 until taskArray.length()) {
                    val taskObj = taskArray.getJSONObject(i)
                    val id = taskObj.optString("id")
                    val title = taskObj.optString("title")
                    if (id.isNotBlank() && title.isNotBlank()) {
                        val priorityStr = taskObj.optString("priority", "Medium")
                        val priority = try {
                            Priority.valueOf(priorityStr)
                        } catch (e: Exception) {
                            Priority.Medium
                        }

                        val repeatStr = taskObj.optString("repeatType", "None")
                        val repeatType = try {
                            RepeatType.valueOf(repeatStr)
                        } catch (e: Exception) {
                            RepeatType.None
                        }

                        val completedAt = if (taskObj.isNull("completedAt")) null else taskObj.optLong("completedAt")
                        val dueDate = if (taskObj.isNull("dueDate")) null else taskObj.optLong("dueDate")
                        val reminderTime = if (taskObj.isNull("reminderTime")) null else taskObj.optLong("reminderTime")
                        val parentTaskId = if (taskObj.isNull("parentTaskId")) null else taskObj.optString("parentTaskId")
                        val customDays = if (taskObj.isNull("customDays")) null else taskObj.optInt("customDays")
                        val groupId = if (taskObj.isNull("groupId")) null else taskObj.optString("groupId")

                        tasksList.add(
                            ActivityTask(
                                id = id,
                                title = title,
                                description = taskObj.optString("description", ""),
                                category = taskObj.optString("category", ""),
                                priority = priority,
                                isCompleted = taskObj.optBoolean("isCompleted", false),
                                completedAt = if (completedAt == 0L && taskObj.isNull("completedAt")) null else completedAt,
                                createdAt = taskObj.optLong("createdAt", System.currentTimeMillis()),
                                dueDate = if (dueDate == 0L && taskObj.isNull("dueDate")) null else dueDate,
                                repeatType = repeatType,
                                parentTaskId = parentTaskId,
                                customDays = customDays,
                                isReminderEnabled = taskObj.optBoolean("isReminderEnabled", false),
                                reminderTime = if (reminderTime == 0L && taskObj.isNull("reminderTime")) null else reminderTime,
                                groupId = groupId
                            )
                        )
                    }
                }
            }

            val templatesList = mutableListOf<TaskGroupTemplate>()
            if (rootObj.has("templates")) {
                val tmplArray = rootObj.getJSONArray("templates")
                if (tmplArray.length() > 1000) {
                    return@withContext Result.failure(Exception("Too many templates in backup"))
                }
                for (i in 0 until tmplArray.length()) {
                    val tmplObj = tmplArray.getJSONObject(i)
                    val id = tmplObj.optString("id")
                    val name = tmplObj.optString("name")
                    if (id.isNotBlank() && name.isNotBlank()) {
                        templatesList.add(
                            TaskGroupTemplate(
                                id = id,
                                name = name,
                                color = if (tmplObj.isNull("color")) null else tmplObj.optInt("color"),
                                createdAt = tmplObj.optLong("createdAt", System.currentTimeMillis()),
                                itemsJson = tmplObj.optString("itemsJson", "[]")
                            )
                        )
                    }
                }
            }

            Result.success(BackupData(categories = categoriesList, tasks = tasksList, groups = groupsList, templates = templatesList))
        } catch (e: Exception) {
            Result.failure(Exception("File validation failed: ${e.localizedMessage ?: "Invalid JSON backup format"}"))
        }
    }

    suspend fun restoreReplace(backupData: BackupData): Result<Int> = withContext(Dispatchers.IO) {
        try {
            db.withTransaction {
                db.activityTaskDao().deleteAllTasks()
                db.taskGroupDao().deleteAllGroups()
                db.taskGroupTemplateDao().deleteAllTemplates()
                db.categoryDao().deleteAllCategories()

                if (backupData.groups.isNotEmpty()) {
                    db.taskGroupDao().insertAll(backupData.groups)
                }
                if (backupData.templates.isNotEmpty()) {
                    db.taskGroupTemplateDao().insertAll(backupData.templates)
                }
                if (backupData.categories.isNotEmpty()) {
                    db.categoryDao().insertAll(backupData.categories)
                }
                if (backupData.tasks.isNotEmpty()) {
                    db.activityTaskDao().insertAll(backupData.tasks)
                }
            }

            val categoryRepo = CategoryRepository(db.categoryDao(), db.activityTaskDao())
            categoryRepo.ensureDefaultCategories()

            // Reschedule active reminders
            val allTasks = db.activityTaskDao().getAllTasksOneShot()
            allTasks.forEach { task ->
                ReminderScheduler.scheduleOrCancelReminder(context, task)
            }

            Result.success(backupData.tasks.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreMerge(backupData: BackupData): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var mergedCount = 0
            db.withTransaction {
                val existingGroups = db.taskGroupDao().getAllGroupsOneShot()
                val existingGroupIds = existingGroups.map { it.id }.toSet()
                val existingGroupNames = existingGroups.map { it.name.lowercase() }.toSet()

                val newGroupsToInsert = backupData.groups.filter { group ->
                    !existingGroupIds.contains(group.id) && !existingGroupNames.contains(group.name.lowercase())
                }
                if (newGroupsToInsert.isNotEmpty()) {
                    db.taskGroupDao().insertAll(newGroupsToInsert)
                }

                val existingTemplates = db.taskGroupTemplateDao().getAllTemplatesOneShot()
                val existingTemplateIds = existingTemplates.map { it.id }.toSet()
                val existingTemplateNames = existingTemplates.map { it.name.lowercase() }.toSet()

                val newTemplatesToInsert = backupData.templates.filter { tmpl ->
                    !existingTemplateIds.contains(tmpl.id) && !existingTemplateNames.contains(tmpl.name.lowercase())
                }
                if (newTemplatesToInsert.isNotEmpty()) {
                    db.taskGroupTemplateDao().insertAll(newTemplatesToInsert)
                }

                val existingCategories = db.categoryDao().getAllCategoriesOneShot()
                val existingCategoryIds = existingCategories.map { it.id }.toSet()
                val existingCategoryNames = existingCategories.map { it.name.lowercase() }.toSet()

                val newCategoriesToInsert = backupData.categories.filter { cat ->
                    !existingCategoryIds.contains(cat.id) && !existingCategoryNames.contains(cat.name.lowercase())
                }
                if (newCategoriesToInsert.isNotEmpty()) {
                    db.categoryDao().insertAll(newCategoriesToInsert)
                }

                val existingTasks = db.activityTaskDao().getAllTasksOneShot()
                val existingTaskMap = existingTasks.associateBy { it.id }

                val tasksToInsert = mutableListOf<ActivityTask>()

                for (backupTask in backupData.tasks) {
                    val existing = existingTaskMap[backupTask.id]
                    if (existing == null) {
                        tasksToInsert.add(backupTask)
                        mergedCount++
                    } else {
                        val existingTime = existing.completedAt ?: existing.createdAt
                        val backupTime = backupTask.completedAt ?: backupTask.createdAt
                        if (backupTime > existingTime) {
                            tasksToInsert.add(backupTask)
                            mergedCount++
                        }
                    }
                }

                if (tasksToInsert.isNotEmpty()) {
                    db.activityTaskDao().insertAll(tasksToInsert)
                }
            }

            val categoryRepo = CategoryRepository(db.categoryDao(), db.activityTaskDao())
            categoryRepo.ensureDefaultCategories()

            // Reschedule active reminders
            val allTasks = db.activityTaskDao().getAllTasksOneShot()
            allTasks.forEach { task ->
                ReminderScheduler.scheduleOrCancelReminder(context, task)
            }

            Result.success(mergedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
