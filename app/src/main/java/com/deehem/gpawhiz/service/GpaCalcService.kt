package com.deehem.gpawhiz.service

import com.deehem.gpawhiz.data.Course
import com.deehem.gpawhiz.data.Semester

object GpaCalcService {

    fun gradeToPoints(grade: String, scale: Double = 5.0): Double {
        val g = grade.trim().uppercase()
        return if (scale == 4.0) {
            when (g) {
                "A" -> 4.0
                "B" -> 3.0
                "C" -> 2.0
                "D" -> 1.0
                else -> 0.0
            }
        } else {
            when (g) {
                "A" -> 5.0
                "B" -> 4.0
                "C" -> 3.0
                "D" -> 2.0
                "E" -> 1.0
                else -> 0.0
            }
        }
    }

    fun calculateTotalUnits(courses: List<Course>): Int {
        return courses.filter { !it.grade.equals("Awaiting Grade", ignoreCase = true) && !it.grade.equals("AR", ignoreCase = true) }
            .sumOf { it.units }
    }

    fun calculateSgpa(courses: List<Course>, scale: Double = 5.0): Double {
        val validCourses = courses.filter {
            !it.grade.equals("Awaiting Grade", ignoreCase = true) && !it.grade.equals("AR", ignoreCase = true)
        }
        val totalUnits = validCourses.sumOf { it.units }
        if (totalUnits == 0) return 0.0
        val totalPoints = validCourses.sumOf { it.units * gradeToPoints(it.grade, scale) }
        return totalPoints / totalUnits
    }

    fun calculateCgpa(semesters: List<Semester>, allCourses: List<Course>): Double {
        val validCourses = allCourses.filter {
            !it.grade.equals("Awaiting Grade", ignoreCase = true) && !it.grade.equals("AR", ignoreCase = true)
        }
        val totalUnits = validCourses.sumOf { it.units }
        if (totalUnits == 0) return 0.0
        val semScaleMap = semesters.associate { it.id to it.gradingScale }
        val totalPoints = validCourses.sumOf { c ->
            val scale = semScaleMap[c.semesterId] ?: 5.0
            c.units * gradeToPoints(c.grade, scale)
        }
        return totalPoints / totalUnits
    }

    fun getClassification(cgpa: Double, scale: Double = 5.0): String {
        return if (scale == 4.0) {
            when {
                cgpa >= 3.5 -> "First Class"
                cgpa >= 3.0 -> "Second Class Upper"
                cgpa >= 2.0 -> "Second Class Lower"
                cgpa >= 1.0 -> "Third Class"
                else -> "Pass"
            }
        } else {
            when {
                cgpa >= 4.5 -> "First Class"
                cgpa >= 3.5 -> "Second Class Upper"
                cgpa >= 2.4 -> "Second Class Lower"
                cgpa >= 1.5 -> "Third Class"
                else -> "Pass"
            }
        }
    }

    fun getColorForCgpa(cgpa: Double, scale: Double = 5.0): String {
        return if (scale == 4.0) {
            when {
                cgpa >= 3.5 -> "#10B981"
                cgpa >= 3.0 -> "#3B82F6"
                cgpa >= 2.0 -> "#F59E0B"
                else -> "#EF4444"
            }
        } else {
            when {
                cgpa >= 4.5 -> "#10B981"
                cgpa >= 3.5 -> "#3B82F6"
                cgpa >= 2.4 -> "#F59E0B"
                else -> "#EF4444"
            }
        }
    }

    fun getStandingForCgpa(cgpa: Double, scale: Double = 5.0): String {
        return getClassification(cgpa, scale)
    }
}
