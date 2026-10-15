package com.deehem.gpawhiz.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

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
    val rank: Int = 0,
    val timelineStartDate: Long? = null
)

@Entity(
    tableName = "academic_periods",
    foreignKeys = [
        ForeignKey(
            entity = Semester::class,
            parentColumns = ["id"],
            childColumns = ["semester_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["semester_id"])]
)
data class AcademicPeriod(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "semester_id")
    val semesterId: Int,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    val type: String,
    val label: String? = null,
    val weeks: Int,
    val startDate: Long? = null,
    val endDate: Long? = null
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
    val isCarryOver: Boolean = false,
    val assessmentBreakdown: String? = null // Optional JSON breakdown of Continuous Assessment (CA)
)

data class AssessmentComponent(
    val name: String,
    val maxMarks: Int,
    val scoreEarned: Int? = null
) {
    companion object {
        fun listToJson(components: List<AssessmentComponent>): String {
            val arr = org.json.JSONArray()
            for (c in components) {
                val obj = org.json.JSONObject()
                obj.put("name", c.name)
                obj.put("maxMarks", c.maxMarks)
                if (c.scoreEarned != null) obj.put("scoreEarned", c.scoreEarned)
                arr.put(obj)
            }
            return arr.toString()
        }

        fun listFromJson(jsonStr: String?): List<AssessmentComponent> {
            if (jsonStr.isNullOrBlank()) return emptyList()
            val list = mutableListOf<AssessmentComponent>()
            try {
                val arr = org.json.JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val score = if (obj.has("scoreEarned") && !obj.isNull("scoreEarned")) obj.getInt("scoreEarned") else null
                    list.add(
                        AssessmentComponent(
                            name = obj.getString("name"),
                            maxMarks = obj.getInt("maxMarks"),
                            scoreEarned = score
                        )
                    )
                }
            } catch (_: Exception) { }
            return list
        }
    }
}

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
    val reminderEnabled: Boolean = true,
    @ColumnInfo(name = "series_id", defaultValue = "NULL") val seriesId: String? = null,
    @ColumnInfo(defaultValue = "'NONE'") val recurrence: String = StudyRecurrence.NONE
)

object StudyRecurrence {
    const val NONE = "NONE"
    const val WEEKLY = "WEEKLY"
}

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
    val alertEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "NULL") val endTime: String? = null,
    @ColumnInfo(defaultValue = "''") val venue: String = "",
    @ColumnInfo(defaultValue = "''") val seatNumber: String = ""
)
