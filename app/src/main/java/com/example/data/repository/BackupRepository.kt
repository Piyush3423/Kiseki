package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.database.KisekiDatabase
import com.example.data.entity.ActivityTask
import com.example.data.entity.Category
import com.example.data.entity.DailyScore
import com.example.data.entity.EndOfDayReview
import com.example.data.entity.FocusSession
import com.example.data.entity.PersonalBest
import com.example.data.entity.TaskGroup
import com.example.data.entity.TaskGroupTemplate
import com.example.data.entity.XpEvent
import com.example.data.model.Priority
import com.example.data.model.RepeatType
import com.example.util.ReminderScheduler
import com.example.widget.KisekiWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class BackupData(
    val categories: List<Category> = emptyList(),
    val tasks: List<ActivityTask> = emptyList(),
    val groups: List<TaskGroup> = emptyList(),
    val templates: List<TaskGroupTemplate> = emptyList(),
    val dailyScores: List<DailyScore> = emptyList(),
    val xpEvents: List<XpEvent> = emptyList(),
    val personalBests: List<PersonalBest> = emptyList(),
    val endOfDayReviews: List<EndOfDayReview> = emptyList(),
    val focusSessions: List<FocusSession> = emptyList()
)

class BackupRepository(private val context: Context) {

    private val db = KisekiDatabase.getDatabase(context)

    suspend fun exportBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val tasks = db.activityTaskDao().getAllTasksOneShot()
            val categories = db.categoryDao().getAllCategoriesOneShot()
            val groups = db.taskGroupDao().getAllGroupsOneShot()
            val templates = db.taskGroupTemplateDao().getAllTemplatesOneShot()
            val dailyScores = db.dailyScoreDao().getAllScores()
            val xpEvents = db.xpEventDao().getAllEvents()
            val personalBests = db.personalBestDao().getAllRecordsOneShot()
            val endOfDayReviews = db.endOfDayReviewDao().getAllReviews()
            val focusSessions = db.focusSessionDao().getAllSessions()

            val rootJson = JSONObject().apply {
                put("app", "Kiseki")
                put("version", 2)
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
                        put("rescheduleCount", task.rescheduleCount)
                        put("missCount", task.missCount)
                        put("lateCompletionCount", task.lateCompletionCount)
                        put("frictionScore", task.frictionScore.toDouble())
                        put("frictionSuppressedUntil", task.frictionSuppressedUntil ?: JSONObject.NULL)
                        put("estimatedDurationMinutes", task.estimatedDurationMinutes ?: JSONObject.NULL)
                    }
                    tasksArray.put(taskObj)
                }
                put("tasks", tasksArray)

                val dailyScoresArray = JSONArray()
                dailyScores.forEach { ds ->
                    val dsObj = JSONObject().apply {
                        put("date", ds.date)
                        put("score", ds.score)
                        put("completionScore", ds.completionScore.toDouble())
                        put("priorityPerformance", ds.priorityPerformance.toDouble())
                        put("onTimeScore", ds.onTimeScore.toDouble())
                        put("consistencyScore", ds.consistencyScore.toDouble())
                    }
                    dailyScoresArray.put(dsObj)
                }
                put("dailyScores", dailyScoresArray)

                val xpEventsArray = JSONArray()
                xpEvents.forEach { xp ->
                    val xpObj = JSONObject().apply {
                        put("id", xp.id)
                        put("amount", xp.amount)
                        put("eventType", xp.eventType)
                        put("timestamp", xp.timestamp)
                        put("taskId", xp.taskId ?: JSONObject.NULL)
                        put("date", xp.date)
                    }
                    xpEventsArray.put(xpObj)
                }
                put("xpEvents", xpEventsArray)

                val pbArray = JSONArray()
                personalBests.forEach { pb ->
                    val pbObj = JSONObject().apply {
                        put("recordKey", pb.recordKey)
                        put("value", pb.value)
                        put("dateAchieved", pb.dateAchieved)
                        put("previousValue", pb.previousValue)
                        put("acknowledged", pb.acknowledged)
                    }
                    pbArray.put(pbObj)
                }
                put("personalBests", pbArray)

                val reviewsArray = JSONArray()
                endOfDayReviews.forEach { rev ->
                    val revObj = JSONObject().apply {
                        put("date", rev.date)
                        put("completedTasks", rev.completedTasks)
                        put("totalTasks", rev.totalTasks)
                        put("score", rev.score)
                        put("rank", rev.rank)
                        put("xpEarned", rev.xpEarned)
                        val obsArray = JSONArray()
                        rev.obstacles.forEach { obsArray.put(it) }
                        put("obstacles", obsArray)
                        put("note", rev.note ?: JSONObject.NULL)
                        put("reviewedAt", rev.reviewedAt)
                    }
                    reviewsArray.put(revObj)
                }
                put("endOfDayReviews", reviewsArray)

                val sessionsArray = JSONArray()
                focusSessions.forEach { sess ->
                    val sessObj = JSONObject().apply {
                        put("id", sess.id)
                        put("taskId", sess.taskId)
                        put("startTime", sess.startTime)
                        put("endTime", sess.endTime)
                        put("duration", sess.duration)
                        put("completed", sess.completed)
                    }
                    sessionsArray.put(sessObj)
                }
                put("focusSessions", sessionsArray)
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

            if (jsonString.length > 20_000_000) {
                return@withContext Result.failure(Exception("Backup file exceeds maximum allowed size"))
            }

            val rootObj = JSONObject(jsonString)

            if (!rootObj.has("tasks") && !rootObj.has("categories")) {
                return@withContext Result.failure(Exception("Invalid backup file: Missing Kiseki data structure"))
            }

            val categoriesList = mutableListOf<Category>()
            if (rootObj.has("categories")) {
                val catArray = rootObj.getJSONArray("categories")
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
                for (i in 0 until taskArray.length()) {
                    val taskObj = taskArray.getJSONObject(i)
                    val id = taskObj.optString("id")
                    val title = taskObj.optString("title")
                    if (id.isNotBlank() && title.isNotBlank()) {
                        val priorityStr = taskObj.optString("priority", "Medium")
                        val priority = try {
                            Priority.valueOf(priorityStr)
                        } catch (_: Exception) {
                            Priority.Medium
                        }

                        val repeatStr = taskObj.optString("repeatType", "None")
                        val repeatType = try {
                            RepeatType.valueOf(repeatStr)
                        } catch (_: Exception) {
                            RepeatType.None
                        }

                        val completedAt = if (taskObj.isNull("completedAt")) null else taskObj.optLong("completedAt")
                        val dueDate = if (taskObj.isNull("dueDate")) null else taskObj.optLong("dueDate")
                        val reminderTime = if (taskObj.isNull("reminderTime")) null else taskObj.optLong("reminderTime")
                        val parentTaskId = if (taskObj.isNull("parentTaskId")) null else taskObj.optString("parentTaskId")
                        val customDays = if (taskObj.isNull("customDays")) null else taskObj.optInt("customDays")
                        val groupId = if (taskObj.isNull("groupId")) null else taskObj.optString("groupId")
                        val frictionSuppressedUntil = if (taskObj.isNull("frictionSuppressedUntil")) null else taskObj.optLong("frictionSuppressedUntil")
                        val estimatedDurationMinutes = if (taskObj.isNull("estimatedDurationMinutes")) null else taskObj.optInt("estimatedDurationMinutes")

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
                                groupId = groupId,
                                rescheduleCount = taskObj.optInt("rescheduleCount", 0),
                                missCount = taskObj.optInt("missCount", 0),
                                lateCompletionCount = taskObj.optInt("lateCompletionCount", 0),
                                frictionScore = taskObj.optDouble("frictionScore", 0.0).toFloat(),
                                frictionSuppressedUntil = frictionSuppressedUntil,
                                estimatedDurationMinutes = estimatedDurationMinutes
                            )
                        )
                    }
                }
            }

            val templatesList = mutableListOf<TaskGroupTemplate>()
            if (rootObj.has("templates")) {
                val tmplArray = rootObj.getJSONArray("templates")
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

            val dailyScoresList = mutableListOf<DailyScore>()
            if (rootObj.has("dailyScores")) {
                val dsArray = rootObj.getJSONArray("dailyScores")
                for (i in 0 until dsArray.length()) {
                    val dsObj = dsArray.getJSONObject(i)
                    val date = dsObj.optString("date")
                    if (date.isNotBlank()) {
                        dailyScoresList.add(
                            DailyScore(
                                date = date,
                                score = dsObj.optInt("score", 0),
                                completionScore = dsObj.optDouble("completionScore", 0.0).toFloat(),
                                priorityPerformance = dsObj.optDouble("priorityPerformance", 0.0).toFloat(),
                                onTimeScore = dsObj.optDouble("onTimeScore", 0.0).toFloat(),
                                consistencyScore = dsObj.optDouble("consistencyScore", 0.0).toFloat()
                            )
                        )
                    }
                }
            }

            val xpEventsList = mutableListOf<XpEvent>()
            if (rootObj.has("xpEvents")) {
                val xpArray = rootObj.getJSONArray("xpEvents")
                for (i in 0 until xpArray.length()) {
                    val xpObj = xpArray.getJSONObject(i)
                    val id = xpObj.optString("id")
                    val eventType = xpObj.optString("eventType")
                    val date = xpObj.optString("date")
                    if (id.isNotBlank() && eventType.isNotBlank() && date.isNotBlank()) {
                        xpEventsList.add(
                            XpEvent(
                                id = id,
                                amount = xpObj.optInt("amount", 0),
                                eventType = eventType,
                                timestamp = xpObj.optLong("timestamp", System.currentTimeMillis()),
                                taskId = if (xpObj.isNull("taskId")) null else xpObj.optString("taskId"),
                                date = date
                            )
                        )
                    }
                }
            }

            val personalBestsList = mutableListOf<PersonalBest>()
            if (rootObj.has("personalBests")) {
                val pbArray = rootObj.getJSONArray("personalBests")
                for (i in 0 until pbArray.length()) {
                    val pbObj = pbArray.getJSONObject(i)
                    val recordKey = pbObj.optString("recordKey")
                    val dateAchieved = pbObj.optString("dateAchieved")
                    if (recordKey.isNotBlank() && dateAchieved.isNotBlank()) {
                        personalBestsList.add(
                            PersonalBest(
                                recordKey = recordKey,
                                value = pbObj.optInt("value", 0),
                                dateAchieved = dateAchieved,
                                previousValue = pbObj.optInt("previousValue", 0),
                                acknowledged = pbObj.optBoolean("acknowledged", false)
                            )
                        )
                    }
                }
            }

            val reviewsList = mutableListOf<EndOfDayReview>()
            if (rootObj.has("endOfDayReviews")) {
                val revArray = rootObj.getJSONArray("endOfDayReviews")
                for (i in 0 until revArray.length()) {
                    val revObj = revArray.getJSONObject(i)
                    val date = revObj.optString("date")
                    val rank = revObj.optString("rank")
                    if (date.isNotBlank() && rank.isNotBlank()) {
                        val obstaclesList = mutableListOf<String>()
                        if (revObj.has("obstacles")) {
                            val obsVal = revObj.get("obstacles")
                            if (obsVal is JSONArray) {
                                for (j in 0 until obsVal.length()) {
                                    obstaclesList.add(obsVal.getString(j))
                                }
                            } else if (obsVal is String) {
                                try {
                                    val obsArr = JSONArray(obsVal)
                                    for (j in 0 until obsArr.length()) {
                                        obstaclesList.add(obsArr.getString(j))
                                    }
                                } catch (_: Exception) {
                                    if (obsVal.isNotBlank()) obstaclesList.add(obsVal)
                                }
                            }
                        }

                        reviewsList.add(
                            EndOfDayReview(
                                date = date,
                                completedTasks = revObj.optInt("completedTasks", 0),
                                totalTasks = revObj.optInt("totalTasks", 0),
                                score = revObj.optInt("score", 0),
                                rank = rank,
                                xpEarned = revObj.optInt("xpEarned", 0),
                                obstacles = obstaclesList,
                                note = if (revObj.isNull("note")) null else revObj.optString("note"),
                                reviewedAt = revObj.optLong("reviewedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }

            val focusSessionsList = mutableListOf<FocusSession>()
            if (rootObj.has("focusSessions")) {
                val sessArray = rootObj.getJSONArray("focusSessions")
                for (i in 0 until sessArray.length()) {
                    val sessObj = sessArray.getJSONObject(i)
                    val id = sessObj.optString("id")
                    val taskId = sessObj.optString("taskId")
                    if (id.isNotBlank() && taskId.isNotBlank()) {
                        focusSessionsList.add(
                            FocusSession(
                                id = id,
                                taskId = taskId,
                                startTime = sessObj.optLong("startTime", 0L),
                                endTime = sessObj.optLong("endTime", 0L),
                                duration = sessObj.optLong("duration", 0L),
                                completed = sessObj.optBoolean("completed", false)
                            )
                        )
                    }
                }
            }

            Result.success(
                BackupData(
                    categories = categoriesList,
                    tasks = tasksList,
                    groups = groupsList,
                    templates = templatesList,
                    dailyScores = dailyScoresList,
                    xpEvents = xpEventsList,
                    personalBests = personalBestsList,
                    endOfDayReviews = reviewsList,
                    focusSessions = focusSessionsList
                )
            )
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
                db.dailyScoreDao().deleteAllScores()
                db.xpEventDao().deleteAllEvents()
                db.personalBestDao().deleteAllRecords()
                db.endOfDayReviewDao().deleteAllReviews()
                db.focusSessionDao().deleteAllSessions()

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
                if (backupData.dailyScores.isNotEmpty()) {
                    db.dailyScoreDao().insertScores(backupData.dailyScores)
                }
                if (backupData.xpEvents.isNotEmpty()) {
                    db.xpEventDao().insertEvents(backupData.xpEvents)
                }
                if (backupData.personalBests.isNotEmpty()) {
                    db.personalBestDao().insertAll(backupData.personalBests)
                }
                if (backupData.endOfDayReviews.isNotEmpty()) {
                    db.endOfDayReviewDao().insertReviews(backupData.endOfDayReviews)
                }
                if (backupData.focusSessions.isNotEmpty()) {
                    db.focusSessionDao().insertSessions(backupData.focusSessions)
                }
            }

            val categoryRepo = CategoryRepository(db.categoryDao(), db.activityTaskDao())
            categoryRepo.ensureDefaultCategories()

            // Reschedule active reminders
            val allTasks = db.activityTaskDao().getAllTasksOneShot()
            allTasks.forEach { task ->
                ReminderScheduler.scheduleOrCancelReminder(context, task)
            }

            KisekiWidgetUpdater.updateAllWidgets(context)

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

                if (backupData.dailyScores.isNotEmpty()) {
                    db.dailyScoreDao().insertScores(backupData.dailyScores)
                }
                if (backupData.xpEvents.isNotEmpty()) {
                    db.xpEventDao().insertEvents(backupData.xpEvents)
                }
                if (backupData.personalBests.isNotEmpty()) {
                    db.personalBestDao().insertAll(backupData.personalBests)
                }
                if (backupData.endOfDayReviews.isNotEmpty()) {
                    db.endOfDayReviewDao().insertReviews(backupData.endOfDayReviews)
                }
                if (backupData.focusSessions.isNotEmpty()) {
                    db.focusSessionDao().insertSessions(backupData.focusSessions)
                }
            }

            val categoryRepo = CategoryRepository(db.categoryDao(), db.activityTaskDao())
            categoryRepo.ensureDefaultCategories()

            // Reschedule active reminders
            val allTasks = db.activityTaskDao().getAllTasksOneShot()
            allTasks.forEach { task ->
                ReminderScheduler.scheduleOrCancelReminder(context, task)
            }

            KisekiWidgetUpdater.updateAllWidgets(context)

            Result.success(mergedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
