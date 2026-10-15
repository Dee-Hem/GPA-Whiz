package com.deehem.gpawhiz

import com.deehem.gpawhiz.data.AssessmentComponent
import com.deehem.gpawhiz.data.AcademicPeriod
import com.deehem.gpawhiz.data.Course
import com.deehem.gpawhiz.data.EssayDraft
import com.deehem.gpawhiz.data.Exam
import com.deehem.gpawhiz.data.Semester
import com.deehem.gpawhiz.data.StudentProfile
import com.deehem.gpawhiz.data.TimetableSlot
import com.deehem.gpawhiz.service.BackupRestoreHelper
import com.deehem.gpawhiz.service.GpaCalcService
import com.deehem.gpawhiz.service.OfficialTranscriptPdfGenerator
import com.deehem.gpawhiz.service.formatExamDuration
import com.deehem.gpawhiz.service.formatExamTimeRange
import com.deehem.gpawhiz.service.isExamEndTimeValid
import com.deehem.gpawhiz.service.isExamInProgress
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun transcriptScoreFormattingPreservesEnteredValueAndUsesDashWhenEmpty() {
        assertEquals("59", OfficialTranscriptPdfGenerator.formatScore("59"))
        assertEquals("59.5", OfficialTranscriptPdfGenerator.formatScore("59.5"))
        assertEquals("-", OfficialTranscriptPdfGenerator.formatScore(""))
        assertEquals("-", OfficialTranscriptPdfGenerator.formatScore(null))
    }

    @Test
    fun testAssessmentComponentSerialization() {
        val components = listOf(
            AssessmentComponent("Test 1", 20, 18),
            AssessmentComponent("Assignment", 10, 8),
            AssessmentComponent("Attendance", 10, 10),
            AssessmentComponent("Final Exam", 60, null)
        )
        val json = AssessmentComponent.listToJson(components)
        val parsed = AssessmentComponent.listFromJson(json)

        assertEquals(4, parsed.size)
        assertEquals("Test 1", parsed[0].name)
        assertEquals(20, parsed[0].maxMarks)
        assertEquals(18, parsed[0].scoreEarned)
        assertNull(parsed[3].scoreEarned)
        assertEquals(60, parsed[3].maxMarks)
    }

    @Test
    fun testBackupRestoreBackwardCompatibilityWithOldBackups() {
        // Minimal valid old backup JSON without assessmentBreakdown and without essayDrafts
        val oldJson = """
        {
            "schemaVersion": 6,
            "exportDate": 1700000000000,
            "profile": {
                "fullName": "Olumide Emmanuel",
                "institution": "University of Lagos",
                "matricNo": "190404001",
                "gradingScale": 5.0,
                "targetCgpa": 4.5
            },
            "semesters": [
                { "id": 1, "name": "Year 1 First Semester", "gradingScale": 5.0, "rank": 1 }
            ],
            "courses": [
                { "id": 10, "semesterId": 1, "code": "CSC 101", "title": "Intro to CS", "units": 3, "score": 82, "grade": "A", "isCarryOver": false }
            ],
            "timetable": [],
            "scholarships": []
        }
        """.trimIndent()

        val parsed = BackupRestoreHelper.parseBackupJson(oldJson)
        assertEquals("Olumide Emmanuel", parsed.profile.fullName)
        assertEquals(1, parsed.semesters.size)
        assertEquals(1, parsed.courses.size)
        assertEquals("CSC 101", parsed.courses[0].code)
        assertNull(parsed.courses[0].assessmentBreakdown)
        assertTrue(parsed.drafts.isEmpty())
        assertNull(parsed.exams)
    }

    @Test
    fun testBackupRestoreWithNewFeatures() {
        val breakdown = AssessmentComponent.listToJson(
            listOf(
                AssessmentComponent("Test 1", 20, 16),
                AssessmentComponent("Assignment", 10, 9),
                AssessmentComponent("Final Exam", 70, 55)
            )
        )
        val profile = StudentProfile(id = 1, fullName = "Test Student", gradingScale = 5.0)
        val semesters = listOf(Semester(id = 1, name = "Semester 1", gradingScale = 5.0, rank = 1))
        val courses = listOf(
            Course(id = 1, semesterId = 1, code = "MTH 101", title = "Maths", units = 4, score = 80, grade = "A", assessmentBreakdown = breakdown)
        )
        val drafts = listOf(
            EssayDraft(id = 1, title = "Mastercard Foundation SOP", body = "My academic vision is...", wordLimit = 500)
        )

        val exportedJson = BackupRestoreHelper.exportToJson(
            profile = profile,
            semesters = semesters,
            courses = courses,
            timetable = emptyList(),
            scholarships = emptyList(),
            requirements = emptyList(),
            timelineEvents = emptyList(),
            reminders = emptyList(),
            drafts = drafts
        )

        val restored = BackupRestoreHelper.parseBackupJson(exportedJson)
        assertEquals(1, restored.courses.size)
        assertEquals(breakdown, restored.courses[0].assessmentBreakdown)
        assertEquals(1, restored.drafts.size)
        assertEquals("Mastercard Foundation SOP", restored.drafts[0].title)
        assertEquals(500, restored.drafts[0].wordLimit)
    }

    @Test
    fun academicPeriodDatesRoundTripAndOlderEntriesRemainUnspecified() {
        val profile = StudentProfile(id = 1)
        val semesters = listOf(Semester(id = 1, name = "Semester 1", timelineStartDate = 1_792_000_000_000L))
        val periods = listOf(
            AcademicPeriod(
                id = 5,
                semesterId = 1,
                sortOrder = 0,
                type = "LECTURES",
                weeks = 5,
                startDate = 1_792_000_000_000L,
                endDate = 1_794_678_400_000L
            ),
            AcademicPeriod(id = 6, semesterId = 1, sortOrder = 1, type = "EXAMS", weeks = 3)
        )

        val backup = BackupRestoreHelper.exportToJson(
            profile = profile,
            semesters = semesters,
            courses = emptyList(),
            timetable = emptyList(),
            academicPeriods = periods
        )
        val restored = BackupRestoreHelper.parseBackupJson(backup)

        assertEquals(periods, restored.academicPeriods)
        assertEquals(semesters.single().timelineStartDate, restored.semesters.single().timelineStartDate)
        val olderBackup = JSONObject(backup)
        olderBackup.getJSONArray("academicPeriods").getJSONObject(0).apply {
            remove("startDate")
            remove("endDate")
        }
        val olderPeriod = BackupRestoreHelper.parseBackupJson(olderBackup.toString()).academicPeriods.first()
        assertNull(olderPeriod.startDate)
        assertNull(olderPeriod.endDate)
    }

    @Test
    fun examTimeValidationFormattingAndProgressUseOptionalEndTime() {
        assertTrue(isExamEndTimeValid("09:00", null))
        assertTrue(isExamEndTimeValid("09:00", ""))
        assertTrue(isExamEndTimeValid("09:00", "09:01"))
        assertFalse(isExamEndTimeValid("09:00", "09:00"))
        assertFalse(isExamEndTimeValid("09:00", "08:59"))

        assertEquals("9:00\u00A0AM", formatExamTimeRange("09:00", null))
        assertEquals("9:00 - 11:00\u00A0AM", formatExamTimeRange("09:00", "11:00"))
        assertEquals("2h", formatExamDuration("09:00", "11:00"))
        assertEquals("1h 30m", formatExamDuration("09:00", "10:30"))
        assertNull(formatExamDuration("09:00", null))

        val date = Calendar.getInstance().apply {
            set(2027, Calendar.JANUARY, 12, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val start = date + 9 * 60 * 60 * 1000L
        assertFalse(isExamInProgress(date, "09:00", "11:00", start - 1))
        assertTrue(isExamInProgress(date, "09:00", "11:00", start))
        assertTrue(isExamInProgress(date, "09:00", "11:00", start + 60 * 60 * 1000L))
        assertFalse(isExamInProgress(date, "09:00", "11:00", start + 2 * 60 * 60 * 1000L))
        assertFalse(isExamInProgress(date, "09:00", null, start))
    }

    @Test
    fun examBackupRoundTripPreservesEndTimeAndOptionalFields() {
        val profile = StudentProfile()
        val course = Course(
            id = 4,
            semesterId = 1,
            code = "CSC 204",
            title = "Algorithms",
            units = 3,
            score = 0,
            grade = "F"
        )
        val examWithEnd = Exam(
            id = 8,
            courseId = course.id,
            courseCode = course.code,
            date = 1_800_000_000_000L,
            time = "09:00",
            endTime = "11:00",
            venue = "Hall A",
            seatNumber = "B12"
        )
        val examWithoutEnd = examWithEnd.copy(id = 9, endTime = null, venue = "", seatNumber = "")

        val json = BackupRestoreHelper.exportToJson(
            profile = profile,
            semesters = listOf(Semester(id = 1, name = "Semester 1")),
            courses = listOf(course),
            timetable = emptyList(),
            exams = listOf(examWithEnd, examWithoutEnd)
        )
        val restored = BackupRestoreHelper.parseBackupJson(json).exams.orEmpty()

        assertEquals(2, restored.size)
        assertEquals(examWithEnd, restored[0])
        assertNull(restored[1].endTime)
        assertEquals("", restored[1].venue)
        assertEquals("", restored[1].seatNumber)

        val oldExamBackup = json.replace("\"endTime\": \"11:00\",", "")
        val restoredWithoutEndField = BackupRestoreHelper.parseBackupJson(oldExamBackup).exams.orEmpty().first()
        assertNull(restoredWithoutEndField.endTime)
    }

    @Test
    fun testCgpaProgressionCalculation() {
        val sem1 = Semester(id = 1, name = "Year 1 Sem 1", rank = 1)
        val sem2 = Semester(id = 2, name = "Year 1 Sem 2", rank = 2)

        val course1 = Course(id = 1, semesterId = 1, code = "CSC 101", title = "CS", units = 3, score = 75, grade = "A") // 3 * 5 = 15
        val course2 = Course(id = 2, semesterId = 2, code = "CSC 102", title = "Data", units = 3, score = 65, grade = "B") // 3 * 4 = 12

        val cgpa1 = GpaCalcService.calculateCgpa(listOf(sem1), listOf(course1))
        assertEquals(5.0, cgpa1, 0.01)

        val cgpa2 = GpaCalcService.calculateCgpa(listOf(sem1, sem2), listOf(course1, course2))
        // (15 + 12) / 6 = 27 / 6 = 4.5
        assertEquals(4.5, cgpa2, 0.01)
    }
}
