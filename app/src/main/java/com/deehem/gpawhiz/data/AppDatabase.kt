package com.deehem.gpawhiz.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StudentProfile::class,
        Semester::class,
        Course::class,
        TimetableSlot::class,
        Scholarship::class,
        ScholarshipRequirement::class,
        ScholarshipTimelineEvent::class,
        ScholarshipReminder::class,
        ExchangeRate::class,
        StudySession::class,
        Exam::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gpaDao(): GpaDao
    abstract fun scholarshipDao(): ScholarshipDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE timetable_slots ADD COLUMN courseId INTEGER")
                database.execSQL("CREATE TABLE IF NOT EXISTS exams (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, courseId INTEGER NOT NULL, courseCode TEXT NOT NULL, date INTEGER NOT NULL, time TEXT NOT NULL, alertEnabled INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(courseId) REFERENCES courses(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_exams_courseId ON exams (courseId)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS study_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, courseId INTEGER NOT NULL, courseCode TEXT NOT NULL, date INTEGER NOT NULL, startTime TEXT NOT NULL, durationMinutes INTEGER NOT NULL, status TEXT NOT NULL, isRecurring INTEGER NOT NULL, dayOfWeek INTEGER, reminderEnabled INTEGER NOT NULL, FOREIGN KEY(courseId) REFERENCES courses(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_study_sessions_courseId ON study_sessions (courseId)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE student_profile ADD COLUMN academicSession TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE student_profile ADD COLUMN currentSemesterId INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE student_profile ADD COLUMN totalRequiredCredits INTEGER NOT NULL DEFAULT 120")
                } catch (_: Exception) { }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Safely create all possible missing tables for Version 3
                database.execSQL("CREATE TABLE IF NOT EXISTS exchange_rates (fromCurrency TEXT NOT NULL, toCurrency TEXT NOT NULL, rate REAL NOT NULL, sourceDescription TEXT NOT NULL DEFAULT '', lastUpdated INTEGER NOT NULL, PRIMARY KEY(fromCurrency, toCurrency))")
                database.execSQL("CREATE TABLE IF NOT EXISTS scholarship_requirements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, scholarshipId INTEGER NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, status TEXT NOT NULL, details TEXT NOT NULL, deadline INTEGER, notes TEXT NOT NULL, FOREIGN KEY(scholarshipId) REFERENCES scholarships(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS scholarship_timeline_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, scholarshipId INTEGER NOT NULL, date INTEGER NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, isAutomatic INTEGER NOT NULL, FOREIGN KEY(scholarshipId) REFERENCES scholarships(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS scholarship_reminders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, scholarshipId INTEGER NOT NULL, eventType TEXT NOT NULL, reminderTime INTEGER NOT NULL, offsetDays INTEGER NOT NULL, isEnabled INTEGER NOT NULL, notes TEXT NOT NULL, FOREIGN KEY(scholarshipId) REFERENCES scholarships(id) ON UPDATE NO ACTION ON DELETE CASCADE)")

                // Ensure indices exist
                try { database.execSQL("CREATE INDEX IF NOT EXISTS index_scholarship_requirements_scholarshipId ON scholarship_requirements (scholarshipId)") } catch (_: Exception) { }
                try { database.execSQL("CREATE INDEX IF NOT EXISTS index_scholarship_timeline_events_scholarshipId ON scholarship_timeline_events (scholarshipId)") } catch (_: Exception) { }
                try { database.execSQL("CREATE INDEX IF NOT EXISTS index_scholarship_reminders_scholarshipId ON scholarship_reminders (scholarshipId)") } catch (_: Exception) { }

                // Safely add missing scholarship columns
                val columnsToAdd = listOf(
                    "currency" to "TEXT NOT NULL DEFAULT 'NGN'",
                    "awardAmount" to "REAL",
                    "awardCurrency" to "TEXT",
                    "awardDate" to "INTEGER",
                    "awardNotes" to "TEXT",
                    "dateApplied" to "INTEGER"
                )
                
                for ((col, type) in columnsToAdd) {
                    try {
                        database.execSQL("ALTER TABLE scholarships ADD COLUMN $col $type")
                    } catch (_: Exception) { }
                }

                // Normalization updates
                try {
                    database.execSQL("UPDATE scholarships SET currency = 'NGN' WHERE currency = '₦' OR currency IS NULL OR currency = ''")
                    database.execSQL("UPDATE scholarships SET awardCurrency = 'NGN' WHERE awardCurrency = '₦' OR awardCurrency = ''")
                } catch (_: Exception) { }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ensure all Version 3 tables and columns exist in case they were missed
                database.execSQL("CREATE TABLE IF NOT EXISTS exchange_rates (fromCurrency TEXT NOT NULL, toCurrency TEXT NOT NULL, rate REAL NOT NULL, sourceDescription TEXT NOT NULL DEFAULT '', lastUpdated INTEGER NOT NULL, PRIMARY KEY(fromCurrency, toCurrency))")
                
                try {
                    database.execSQL("ALTER TABLE exchange_rates ADD COLUMN sourceDescription TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) { }

                val columnsToAdd = listOf(
                    "currency" to "TEXT NOT NULL DEFAULT 'NGN'",
                    "awardAmount" to "REAL",
                    "awardCurrency" to "TEXT",
                    "awardDate" to "INTEGER",
                    "awardNotes" to "TEXT",
                    "dateApplied" to "INTEGER"
                )
                
                for ((col, type) in columnsToAdd) {
                    try {
                        database.execSQL("ALTER TABLE scholarships ADD COLUMN $col $type")
                    } catch (_: Exception) { }
                }

                try {
                    database.execSQL("UPDATE scholarships SET currency = 'NGN' WHERE currency = '₦' OR currency IS NULL OR currency = ''")
                    database.execSQL("UPDATE scholarships SET awardCurrency = 'NGN' WHERE awardCurrency = '₦' OR awardCurrency = ''")
                } catch (_: Exception) { }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gpa_whiz_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
