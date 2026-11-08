package com.chronos.hsc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StudyTopicChain::class,
        DailyTaskSlice::class,
        UserStats::class,
        ExamCountdownEntity::class,
        NeuroSessionEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(RoomTypeConverters::class)
abstract class StudyDatabase : RoomDatabase() {

    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile
        private var INSTANCE: StudyDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_topic_chains` (
                        `chainId` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `paper` TEXT NOT NULL,
                        `chapterNumber` INTEGER NOT NULL,
                        `chapterName` TEXT NOT NULL,
                        `totalDays` INTEGER NOT NULL,
                        `distributionCurve` TEXT NOT NULL,
                        `energyLevel` TEXT NOT NULL,
                        `dailyProblemTarget` INTEGER NOT NULL,
                        `createdAtEpochDay` INTEGER NOT NULL,
                        `bossMaxHp` INTEGER NOT NULL,
                        `bossBonusShieldHp` INTEGER NOT NULL DEFAULT 0,
                        `isChainCompleted` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`chainId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_task_slices` (
                        `sliceId` TEXT NOT NULL,
                        `chainId` TEXT NOT NULL,
                        `sliceNumber` INTEGER NOT NULL,
                        `totalSlicesInChain` INTEGER NOT NULL,
                        `displayTitle` TEXT NOT NULL,
                        `shortBadge` TEXT NOT NULL,
                        `phaseTag` TEXT NOT NULL,
                        `scheduledDateEpochDay` INTEGER NOT NULL,
                        `problemsCompleted` INTEGER NOT NULL DEFAULT 0,
                        `problemsTarget` INTEGER NOT NULL DEFAULT 5,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `completedAtTimestamp` INTEGER,
                        `isRetentionEcho` INTEGER NOT NULL DEFAULT 0,
                        `echoIntervalDays` INTEGER,
                        `sliceEnergyOverride` TEXT,
                        `xpMultiplier` INTEGER NOT NULL DEFAULT 1,
                        `originalScheduledEpochDay` INTEGER NOT NULL DEFAULT 0,
                        `shiftLagCount` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`sliceId`),
                        FOREIGN KEY(`chainId`) REFERENCES `study_topic_chains`(`chainId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_task_slices_chainId` ON `daily_task_slices` (`chainId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_task_slices_scheduledDateEpochDay` ON `daily_task_slices` (`scheduledDateEpochDay`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_stats` (
                        `id` INTEGER NOT NULL,
                        `totalXp` INTEGER NOT NULL,
                        `currentLevel` INTEGER NOT NULL,
                        `streakDays` INTEGER NOT NULL,
                        `lastStudyEpochDay` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exam_countdowns` (
                        `examId` TEXT NOT NULL,
                        `examTitle` TEXT NOT NULL,
                        `targetDateEpochDay` INTEGER NOT NULL,
                        `createdAtEpochDay` INTEGER NOT NULL,
                        `isPrimaryPinned` INTEGER NOT NULL,
                        PRIMARY KEY(`examId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `neuro_sessions` (
                        `sessionId` TEXT NOT NULL,
                        `drillType` TEXT NOT NULL,
                        `scoreMetric` REAL NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`sessionId`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `daily_task_slices` ADD COLUMN `originalScheduledEpochDay` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `daily_task_slices` ADD COLUMN `shiftLagCount` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `daily_task_slices` SET `originalScheduledEpochDay` = `scheduledDateEpochDay` WHERE `originalScheduledEpochDay` = 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `study_topic_chains` ADD COLUMN `linkedPreviousChainId` TEXT DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): StudyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "chronos_hsc_study_engine.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
