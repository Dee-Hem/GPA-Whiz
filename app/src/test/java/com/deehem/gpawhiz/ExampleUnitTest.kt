package com.deehem.gpawhiz

import com.deehem.gpawhiz.data.AssessmentComponent
import com.deehem.gpawhiz.data.Course
import com.deehem.gpawhiz.data.EssayDraft
import com.deehem.gpawhiz.data.Semester
import com.deehem.gpawhiz.data.StudentProfile
import com.deehem.gpawhiz.data.TimetableSlot
import com.deehem.gpawhiz.service.BackupRestoreHelper
import com.deehem.gpawhiz.service.GpaCalcService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
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

