package com.deehem.gpawhiz.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "student_profile")
data class StudentProfile(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "",
    val institution: String = "",
    val matricNo: String = "",
    val faculty: String = "",
    val department: String = "",
    val currentLevel: String = "100L",
    val academicSession: String = "",
    val currentSemesterId: Int = 0,
    val graduationYear: String = "",
    val gradingScale: Double = 5.0, // Default 5.0 or 4.0
    val targetCgpa: Double = 4.5,
    val totalRequiredCredits: Int = 120
)

@Entity(tableName = "semesters")
data class Semester(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val gradingScale: Double = 5.0, // Scale for this semester (5.0 or 4.0)
    val rank: Int = 0
)

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val semesterId: Int,
    val code: String,
    val title: String,
    val units: Int,
    val score: Int, // 0 - 100
    val grade: String, // "A", "B", "C", "D", "E", "F"
    val isCarryOver: Boolean = false
)

@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["courseId"])]
)
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseId: Int,
    val courseCode: String,
    val date: Long, // Epoch millis
    val startTime: String, // "HH:MM"
    val durationMinutes: Int,
    val status: String = "Upcoming",
    val isRecurring: Boolean = false,
    val dayOfWeek: Int? = null, // 1-7 (Mon-Sun)
    val reminderEnabled: Boolean = true
)

object StudyStatus {
    const val UPCOMING = "Upcoming"
    const val COMPLETED = "Completed"
    const val POSTPONED = "Postponed"
    const val CANCELLED = "Cancelled"
}

@Entity(tableName = "timetable_slots")
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseId: Int? = null,
    val courseCode: String,
    val venue: String,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, ..., 7 = Sunday
    val startTime: String, // "HH:MM" e.g. "08:00"
    val endTime: String, // "HH:MM" e.g. "10:00"
    val alertEnabled: Boolean = true
)

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["courseId"])]
)
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseId: Int,
    val courseCode: String,
    val date: Long,
    val time: String,
    val alertEnabled: Boolean = true
)
