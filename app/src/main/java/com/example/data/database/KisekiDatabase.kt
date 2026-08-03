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
import com.example.data.entity.ActivityTask
import com.example.data.entity.Category
import com.example.data.entity.TaskGroup
import com.example.data.dao.TaskGroupDao
import com.example.data.dao.TaskGroupTemplateDao
import com.example.data.entity.TaskGroupTemplate

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

@Database(entities = [ActivityTask::class, Category::class, TaskGroup::class, TaskGroupTemplate::class], version = 7, exportSchema = false)
@TypeConverters(Converters::class)
abstract class KisekiDatabase : RoomDatabase() {
    abstract fun activityTaskDao(): ActivityTaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun taskGroupDao(): TaskGroupDao
    abstract fun taskGroupTemplateDao(): TaskGroupTemplateDao

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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
