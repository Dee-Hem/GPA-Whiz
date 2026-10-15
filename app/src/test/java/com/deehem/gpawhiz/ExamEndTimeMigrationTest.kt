package com.deehem.gpawhiz

import androidx.sqlite.db.SupportSQLiteDatabase
import com.deehem.gpawhiz.data.AppDatabase
import android.database.sqlite.SQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExamEndTimeMigrationTest {
    @Test
    fun migrationAddsOptionalFieldsAndPreservesExistingExamRows() {
        val database = SQLiteDatabase.create(null)
        database.execSQL(
            "CREATE TABLE exams (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, courseId INTEGER NOT NULL, courseCode TEXT NOT NULL, date INTEGER NOT NULL, time TEXT NOT NULL, alertEnabled INTEGER NOT NULL DEFAULT 1)"
        )
        try {
            database.execSQL(
                "INSERT INTO exams (id, courseId, courseCode, date, time, alertEnabled) VALUES (1, 4, 'CSC 204', 1800000000000, '09:00', 1)"
            )
            val supportDatabase = Proxy.newProxyInstance(
                SupportSQLiteDatabase::class.java.classLoader,
                arrayOf(SupportSQLiteDatabase::class.java)
            ) { _, method, args ->
                if (method.name == "execSQL") {
                    database.execSQL(args?.get(0) as String)
                    null
                } else {
                    throw UnsupportedOperationException("Unexpected SQLite operation: ${method.name}")
                }
            } as SupportSQLiteDatabase

            AppDatabase.MIGRATION_8_9.migrate(supportDatabase)

            database.rawQuery(
                "SELECT id, courseId, courseCode, date, time, alertEnabled, endTime, venue, seatNumber FROM exams WHERE id = 1",
                null
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
                assertEquals(4, cursor.getInt(1))
                assertEquals("CSC 204", cursor.getString(2))
                assertEquals(1_800_000_000_000L, cursor.getLong(3))
                assertEquals("09:00", cursor.getString(4))
                assertEquals(1, cursor.getInt(5))
                assertTrue(cursor.isNull(6))
                assertEquals("", cursor.getString(7))
                assertEquals("", cursor.getString(8))
                assertFalse(cursor.moveToNext())
            }
        } finally {
            database.close()
        }
    }
}
