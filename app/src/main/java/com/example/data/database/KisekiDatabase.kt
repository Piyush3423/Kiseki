package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ActivityTaskDao
import com.example.data.dao.CategoryDao
import com.example.data.dao.DailyScoreDao
import com.example.data.dao.XpEventDao
import com.example.data.entity.ActivityTask
import com.example.data.entity.Category
import com.example.data.entity.DailyScore
import com.example.data.entity.TaskGroup
import com.example.data.entity.TaskGroupTemplate
import com.example.data.entity.XpEvent

import com.example.data.dao.EndOfDayReviewDao
import com.example.data.dao.FocusSessionDao
import com.example.data.dao.PersonalBestDao
import com.example.data.dao.TaskGroupDao
import com.example.data.dao.TaskGroupTemplateDao
import com.example.data.entity.EndOfDayReview
import com.example.data.entity.FocusSession
import com.example.data.entity.PersonalBest

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activity_tasks ADD COLUMN parentTaskId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE activity_tasks ADD COLUMN customDays TEXT DEFAULT NULL")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `isDefault` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activity_tasks ADD COLUMN completedAt INTEGER DEFAULT NULL")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activity_tasks ADD COLUMN isReminderEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE activity_tasks ADD COLUMN reminderTime INTEGER DEFAULT NULL")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `task_groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `color` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("ALTER TABLE activity_tasks ADD COLUMN groupId TEXT DEFAULT NULL")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `task_group_templates` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `color` INTEGER, `createdAt` INTEGER NOT NULL, `itemsJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_scores` (`date` TEXT NOT NULL, `score` INTEGER NOT NULL, `completionScore` REAL NOT NULL, `priorityPerformance` REAL NOT NULL, `onTimeScore` REAL NOT NULL, `consistencyScore` REAL NOT NULL, PRIMARY KEY(`date`))")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `xp_events` (`id` TEXT NOT NULL, `amount` INTEGER NOT NULL, `eventType` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `taskId` TEXT, `date` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `personal_bests` (`recordKey` TEXT NOT NULL, `value` INTEGER NOT NULL, `dateAchieved` TEXT NOT NULL, `previousValue` INTEGER NOT NULL DEFAULT 0, `acknowledged` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`recordKey`))")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activity_tasks ADD COLUMN rescheduleCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE activity_tasks ADD COLUMN missCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE activity_tasks ADD COLUMN lateCompletionCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE activity_tasks ADD COLUMN frictionScore REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE activity_tasks ADD COLUMN frictionSuppressedUntil INTEGER DEFAULT NULL")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `end_of_day_reviews` (`date` TEXT NOT NULL, `completedTasks` INTEGER NOT NULL, `totalTasks` INTEGER NOT NULL, `score` INTEGER NOT NULL, `rank` TEXT NOT NULL, `xpEarned` INTEGER NOT NULL, `obstacles` TEXT NOT NULL, `note` TEXT DEFAULT NULL, `reviewedAt` INTEGER NOT NULL, PRIMARY KEY(`date`))")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activity_tasks ADD COLUMN estimatedDurationMinutes INTEGER DEFAULT NULL")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `focus_sessions` (
                `id` TEXT NOT NULL,
                `taskId` TEXT NOT NULL,
                `startTime` INTEGER NOT NULL,
                `endTime` INTEGER NOT NULL,
                `duration` INTEGER NOT NULL,
                `completed` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
    }
}

@Database(entities = [ActivityTask::class, Category::class, TaskGroup::class, TaskGroupTemplate::class, DailyScore::class, XpEvent::class, PersonalBest::class, EndOfDayReview::class, FocusSession::class], version = 14, exportSchema = false)
@TypeConverters(Converters::class)
abstract class KisekiDatabase : RoomDatabase() {
    abstract fun activityTaskDao(): ActivityTaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun taskGroupDao(): TaskGroupDao
    abstract fun taskGroupTemplateDao(): TaskGroupTemplateDao
    abstract fun dailyScoreDao(): DailyScoreDao
    abstract fun xpEventDao(): XpEventDao
    abstract fun personalBestDao(): PersonalBestDao
    abstract fun endOfDayReviewDao(): EndOfDayReviewDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        @Volatile
        private var INSTANCE: KisekiDatabase? = null

        fun getDatabase(context: Context): KisekiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KisekiDatabase::class.java,
                    "kiseki_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
