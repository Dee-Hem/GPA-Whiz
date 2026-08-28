package com.example.data

import androidx.room.Entity
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
    val graduationYear: String = "",
    val gradingScale: Double = 5.0, // Default 5.0 or 4.0
    val targetCgpa: Double = 4.5
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

@Entity(tableName = "timetable_slots")
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseCode: String,
    val venue: String,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, ..., 7 = Sunday
    val startTime: String, // "HH:MM" e.g. "08:00"
    val endTime: String, // "HH:MM" e.g. "10:00"
    val alertEnabled: Boolean = true
)
