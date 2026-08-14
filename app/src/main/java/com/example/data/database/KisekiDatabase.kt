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

import com.example.data.dao.PersonalBestDao
import com.example.data.dao.TaskGroupDao
import com.example.data.dao.TaskGroupTemplateDao
import com.example.data.entity.PersonalBest

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

@Database(entities = [ActivityTask::class, Category::class, TaskGroup::class, TaskGroupTemplate::class, DailyScore::class, XpEvent::class, PersonalBest::class], version = 11, exportSchema = false)
@TypeConverters(Converters::class)
abstract class KisekiDatabase : RoomDatabase() {
    abstract fun activityTaskDao(): ActivityTaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun taskGroupDao(): TaskGroupDao
    abstract fun taskGroupTemplateDao(): TaskGroupTemplateDao
    abstract fun dailyScoreDao(): DailyScoreDao
    abstract fun xpEventDao(): XpEventDao
    abstract fun personalBestDao(): PersonalBestDao

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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
