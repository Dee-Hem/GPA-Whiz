package com.deehem.gpawhiz

import com.deehem.gpawhiz.data.StudyRecurrence
import com.deehem.gpawhiz.data.StudySession
import com.deehem.gpawhiz.data.StudentProfile
import com.deehem.gpawhiz.service.BackupRestoreHelper
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudySessionBackupTest {
    @Test
    fun backupRoundTripPreservesStudySessionSeriesFields() {
        val session = StudySession(
            id = 11,
            courseId = 4,
            courseCode = "CSC 204",
            date = 1_800_000_000_000,
            startTime = "13:05",
            durationMinutes = 55,
            seriesId = "series-uuid",
            recurrence = StudyRecurrence.WEEKLY
        )
        val backup = BackupRestoreHelper.exportToJson(
            profile = StudentProfile(),
            semesters = emptyList(),
            courses = emptyList(),
            timetable = emptyList(),
            studySessions = listOf(session)
        )

        val restored = BackupRestoreHelper.parseBackupJson(backup).studySessions.single()
        assertEquals(session, restored)
        val record = JSONObject(backup).getJSONArray("studySessions").getJSONObject(0)
        assertEquals("series-uuid", record.getString("series_id"))
        assertEquals("WEEKLY", record.getString("recurrence"))
    }

    @Test
    fun oldBackupWithoutStudySessionArrayRestoresAsEmpty() {
        val backup = JSONObject(emptyBackupJson()).apply { remove("studySessions") }.toString()

        assertEquals(emptyList<StudySession>(), BackupRestoreHelper.parseBackupJson(backup).studySessions)
    }

    @Test
    fun oldStudySessionRecordsWithoutRecurrenceFieldsRestoreAsSingles() {
        val record = JSONObject().apply {
            put("id", 2)
            put("courseId", 1)
            put("courseCode", "ENG 101")
            put("date", 1_800_000_000_000)
            put("startTime", "09:00")
            put("durationMinutes", 60)
            put("status", "Upcoming")
            put("isRecurring", false)
            put("reminderEnabled", true)
        }
        val backup = JSONObject(emptyBackupJson()).put("studySessions", JSONArray().put(record)).toString()

        val restored = BackupRestoreHelper.parseBackupJson(backup).studySessions.single()
        assertNull(restored.seriesId)
        assertEquals(StudyRecurrence.NONE, restored.recurrence)
    }

    private fun emptyBackupJson() = BackupRestoreHelper.exportToJson(
        profile = StudentProfile(),
        semesters = emptyList(),
        courses = emptyList(),
        timetable = emptyList()
    )
}
