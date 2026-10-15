package com.deehem.gpawhiz

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.deehem.gpawhiz.data.AppDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudySessionMigrationTest {
    @Test
    fun roomOpensVersionTenSchemaAndPreservesSemestersAndCoursesInVersionTwelve() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val databaseName = "academic-calendar-migration-10-to-11.db"
        context.deleteDatabase(databaseName)
        val databaseFile = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()

        val schema = JSONObject(findVersionTenSchema().readText()).getJSONObject("database")
        val entities = schema.getJSONArray("entities")
        val oldDatabase = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
        try {
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                oldDatabase.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val indices = entity.optJSONArray("indices") ?: JSONArray()
                for (indexInTable in 0 until indices.length()) {
                    oldDatabase.execSQL(indices.getJSONObject(indexInTable).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                }
            }
            oldDatabase.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            oldDatabase.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, '${schema.getString("identityHash")}')")
            oldDatabase.execSQL("PRAGMA user_version = 10")
            oldDatabase.execSQL("INSERT INTO semesters (id, name, gradingScale, rank) VALUES (1, 'Semester 1', 5.0, 0)")
            oldDatabase.execSQL("INSERT INTO courses (id, semesterId, code, title, units, score, grade, isCarryOver, assessmentBreakdown) VALUES (3, 1, 'MAT 101', 'Mathematics', 3, 0, 'F', 0, NULL)")
        } finally {
            oldDatabase.close()
        }

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12)
            .allowMainThreadQueries()
            .build()
        try {
            val semester = migrated.gpaDao().getAllSemesters().first().single()
            assertEquals(1, semester.id)
            assertNull(semester.timelineStartDate)
            assertEquals(3, migrated.gpaDao().getAllCourses().first().single().id)
            migrated.openHelper.readableDatabase.query("PRAGMA user_version").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(12, cursor.getInt(0))
            }
            migrated.openHelper.readableDatabase.query("PRAGMA foreign_key_list(academic_periods)").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("CASCADE", cursor.getString(cursor.getColumnIndexOrThrow("on_delete")))
                assertEquals("semester_id", cursor.getString(cursor.getColumnIndexOrThrow("from")))
            }
            val periodId = migrated.gpaDao().insertAcademicPeriod(
                com.deehem.gpawhiz.data.AcademicPeriod(
                    semesterId = semester.id,
                    sortOrder = 0,
                    type = "LECTURES",
                    weeks = 1,
                    startDate = 1_792_000_000_000L,
                    endDate = 1_792_604_800_000L
                )
            )
            assertTrue(periodId > 0)
            val storedPeriod = migrated.gpaDao().getAcademicPeriodsForSemester(semester.id).first().single()
            assertEquals(1_792_000_000_000L, storedPeriod.startDate)
            assertEquals(1_792_604_800_000L, storedPeriod.endDate)
            migrated.gpaDao().deleteSemester(semester)
            migrated.openHelper.readableDatabase.query("SELECT COUNT(*) FROM academic_periods").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        } finally {
            migrated.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun roomOpensVersionNineSchemaAndMigratesStudySessionsAndCalendarToVersionTwelve() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val databaseName = "study-session-migration-9-to-10.db"
        context.deleteDatabase(databaseName)
        val databaseFile = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()

        val schema = JSONObject(findVersionNineSchema().readText()).getJSONObject("database")
        val entities = schema.getJSONArray("entities")
        val oldDatabase = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
        try {
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val tableName = entity.getString("tableName")
                val createTable = entity.getString("createSql").replace("\${TABLE_NAME}", tableName)
                oldDatabase.execSQL(createTable)
            }
            for (index in 0 until entities.length()) {
                val indices = entities.getJSONObject(index).optJSONArray("indices") ?: JSONArray()
                for (indexInTable in 0 until indices.length()) {
                    val createIndex = indices.getJSONObject(indexInTable)
                        .getString("createSql")
                        .replace("\${TABLE_NAME}", entities.getJSONObject(index).getString("tableName"))
                    oldDatabase.execSQL(createIndex)
                }
            }
            oldDatabase.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            oldDatabase.execSQL(
                "INSERT INTO room_master_table (id, identity_hash) VALUES (42, '${schema.getString("identityHash")}')"
            )
            oldDatabase.execSQL("PRAGMA user_version = 9")
            oldDatabase.execSQL("INSERT INTO semesters (id, name, gradingScale, rank) VALUES (1, 'Semester 1', 5.0, 0)")
            oldDatabase.execSQL(
                "INSERT INTO courses (id, semesterId, code, title, units, score, grade, isCarryOver, assessmentBreakdown) VALUES (3, 1, 'MAT 101', 'Mathematics', 3, 0, 'F', 0, NULL)"
            )
            oldDatabase.execSQL(
                "INSERT INTO study_sessions (id, courseId, courseCode, date, startTime, durationMinutes, status, isRecurring, dayOfWeek, reminderEnabled) VALUES (7, 3, 'MAT 101', 1792000000000, '08:30', 45, 'Upcoming', 0, NULL, 1)"
            )
        } finally {
            oldDatabase.close()
        }

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12)
            .allowMainThreadQueries()
            .build()
        try {
            val sessions = migrated.gpaDao().getStudySessionsDirect()
            val session = sessions.single()
            assertEquals(7, session.id)
            assertEquals(3, session.courseId)
            assertEquals("MAT 101", session.courseCode)
            assertEquals(1_792_000_000_000L, session.date)
            assertEquals("08:30", session.startTime)
            assertEquals(45, session.durationMinutes)
            assertNull(session.seriesId)
            assertEquals("NONE", session.recurrence)
            migrated.openHelper.readableDatabase.query("PRAGMA user_version").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(12, cursor.getInt(0))
            }
        } finally {
            migrated.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun findVersionNineSchema(): File {
        var directory: File? = File(System.getProperty("user.dir") ?: ".").canonicalFile
        repeat(8) {
            val current = directory ?: return@repeat
            val candidates = listOf(
                File(current, "schemas/com.deehem.gpawhiz.data.AppDatabase/9.json"),
                File(current, "app/schemas/com.deehem.gpawhiz.data.AppDatabase/9.json")
            )
            candidates.firstOrNull(File::isFile)?.let { return it }
            directory = current.parentFile
        }
        error("Could not find the checked-in version 9 Room schema.")
    }

    private fun findVersionTenSchema(): File {
        var directory: File? = File(System.getProperty("user.dir") ?: ".").canonicalFile
        repeat(8) {
            val current = directory ?: return@repeat
            val candidates = listOf(
                File(current, "schemas/com.deehem.gpawhiz.data.AppDatabase/10.json"),
                File(current, "app/schemas/com.deehem.gpawhiz.data.AppDatabase/10.json")
            )
            candidates.firstOrNull(File::isFile)?.let { return it }
            directory = current.parentFile
        }
        error("Could not find the checked-in version 10 Room schema.")
    }
}
