package com.example.data

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
        ExchangeRate::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gpaDao(): GpaDao
    abstract fun scholarshipDao(): ScholarshipDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Safely create exchange_rates table
                database.execSQL("CREATE TABLE IF NOT EXISTS exchange_rates (fromCurrency TEXT NOT NULL, toCurrency TEXT NOT NULL, rate REAL NOT NULL, sourceDescription TEXT NOT NULL DEFAULT '', lastUpdated INTEGER NOT NULL, PRIMARY KEY(fromCurrency, toCurrency))")
                
                // Safely add missing scholarship columns if they don't exist
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
                try {
                    database.execSQL("ALTER TABLE exchange_rates ADD COLUMN sourceDescription TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) { }

                // Ensure scholarship columns exist in case they were missed in 2->3
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
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
