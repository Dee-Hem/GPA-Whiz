package com.example.service

import com.example.data.Course
import com.example.data.Semester

object GpaCalcService {

    // Color definitions for performance indicators
    const val COLOR_FIRST_CLASS = "#0F9D58"  // Emerald Green
    const val COLOR_SECOND_UPPER = "#1A73E8" // Deep Blue
    const val COLOR_SECOND_LOWER = "#F4B400" // Amber/Yellow
    const val COLOR_THIRD_CLASS = "#DB4437"  // Crimson Red

    /**
     * Map percentage score to Letter Grade based on scale
     */
    fun getGradeForScore(score: Int, scale: Double): String {
        return if (scale >= 5.0) {
            when (score) {
                in 70..100 -> "A"
                in 60..69 -> "B"
                in 50..59 -> "C"
                in 45..49 -> "D"
                in 40..44 -> "E"
                else -> "F"
            }
        } else {
            when (score) {
                in 70..100 -> "A"
                in 60..69 -> "B"
                in 50..59 -> "C"
                in 45..49 -> "D"
                else -> "F"
            }
        }
    }

    /**
     * Map Letter Grade to numeric quality points
     */
    fun getPointsForGrade(grade: String, scale: Double): Double {
        return if (scale >= 5.0) {
            when (grade.uppercase()) {
                "A" -> 5.0
                "B" -> 4.0
                "C" -> 3.0
                "D" -> 2.0
                "E" -> 1.0
                else -> 0.0
            }
        } else {
            when (grade.uppercase()) {
                "A" -> 4.0
                "B" -> 3.0
                "C" -> 2.0
                "D" -> 1.0
                else -> 0.0
            }
        }
    }

    /**
     * Get hex color code for academic standing based on CGPA and scale
     */
    fun getColorForCgpa(cgpa: Double, scale: Double): String {
        return if (scale >= 5.0) {
            when {
                cgpa >= 4.50 -> COLOR_FIRST_CLASS  // 4.50 - 5.00
                cgpa >= 3.50 -> COLOR_SECOND_UPPER // 3.50 - 4.49
                cgpa >= 2.40 -> COLOR_SECOND_LOWER // 2.40 - 3.49
                else -> COLOR_THIRD_CLASS          // Below 2.40
            }
        } else {
            when {
                cgpa >= 3.50 -> COLOR_FIRST_CLASS  // 3.50 - 4.00
                cgpa >= 3.00 -> COLOR_SECOND_UPPER // 3.00 - 3.49
                cgpa >= 2.00 -> COLOR_SECOND_LOWER // 2.00 - 2.99
                else -> COLOR_THIRD_CLASS          // Below 2.00
            }
        }
    }

    /**
     * Get label for academic standing based on CGPA and scale
     */
    fun getStandingForCgpa(cgpa: Double, scale: Double): String {
        return if (scale >= 5.0) {
            when {
                cgpa >= 4.50 -> "First Class (Distinction)"
                cgpa >= 3.50 -> "Second Class Upper (Credit)"
                cgpa >= 2.40 -> "Second Class Lower (Pass)"
                else -> "Third Class / Pass (Probation)"
            }
        } else {
            when {
                cgpa >= 3.50 -> "First Class (Distinction)"
                cgpa >= 3.00 -> "Second Class Upper (Credit)"
                cgpa >= 2.00 -> "Second Class Lower (Pass)"
                else -> "Third Class / Pass (Probation)"
            }
        }
    }

    /**
     * Get short label for academic classification (First Class, etc.)
     */
    fun getClassification(cgpa: Double, scale: Double): String {
        return if (scale >= 5.0) {
            when {
                cgpa >= 4.50 -> "First Class"
                cgpa >= 3.50 -> "Second Class Upper"
                cgpa >= 2.40 -> "Second Class Lower"
                else -> "Third Class / Pass"
            }
        } else {
            when {
                cgpa >= 3.50 -> "First Class"
                cgpa >= 3.00 -> "Second Class Upper"
                cgpa >= 2.00 -> "Second Class Lower"
                else -> "Third Class / Pass"
            }
        }
    }

    /**
     * Calculate Semester GPA (SGPA)
     */
    fun calculateSgpa(courses: List<Course>, scale: Double): Double {
        val activeCourses = courses.filter { !it.grade.equals("Awaiting Grade", ignoreCase = true) && !it.grade.equals("AR", ignoreCase = true) }
        if (activeCourses.isEmpty()) return 0.0
        var totalPoints = 0.0
        var totalUnits = 0
        for (course in activeCourses) {
            val pts = getPointsForGrade(course.grade, scale)
            totalPoints += pts * course.units
            totalUnits += course.units
        }
        return if (totalUnits == 0) 0.0 else totalPoints / totalUnits
    }

    /**
     * Calculate Cumulative GPA (CGPA) over all semesters
     */
    fun calculateCgpa(semesters: List<Semester>, allCourses: List<Course>): Double {
        if (semesters.isEmpty()) return 0.0
        
        var cumulativePoints = 0.0
        var cumulativeUnits = 0
        
        // Find current student preference scale (default to last semester scale)
        val defaultScale = semesters.lastOrNull()?.gradingScale ?: 5.0

        for (semester in semesters) {
            val semCourses = allCourses.filter { it.semesterId == semester.id && !it.grade.equals("Awaiting Grade", ignoreCase = true) && !it.grade.equals("AR", ignoreCase = true) }
            for (course in semCourses) {
                val pts = getPointsForGrade(course.grade, semester.gradingScale)
                cumulativePoints += pts * course.units
                cumulativeUnits += course.units
            }
        }
        
        return if (cumulativeUnits == 0) 0.0 else cumulativePoints / cumulativeUnits
    }

    /**
     * Calculate total credit units registered
     */
    fun calculateTotalUnits(allCourses: List<Course>): Int {
        return allCourses.filter { !it.grade.equals("Awaiting Grade", ignoreCase = true) && !it.grade.equals("AR", ignoreCase = true) }.sumOf { it.units }
    }

    /**
     * Simulation algorithm for target CGPA
     * Returns the required SGPA in the remaining semesters, or null if impossible.
     */
    fun simulateRequiredSgpa(
        currentCgpa: Double,
        currentUnits: Int,
        targetCgpa: Double,
        remainingSemesters: Int,
        avgUnitsPerSemester: Int,
        scale: Double
    ): TargetSimulationResult {
        if (remainingSemesters <= 0 || avgUnitsPerSemester <= 0) {
            return TargetSimulationResult(
                requiredSgpa = 0.0,
                isPossible = false,
                reason = "Invalid remaining semesters or unit inputs."
            )
        }

        val remainingUnits = remainingSemesters * avgUnitsPerSemester
        val totalUnits = currentUnits + remainingUnits
        val totalNeededGradePoints = targetCgpa * totalUnits
        val currentGradePoints = currentCgpa * currentUnits
        val neededRemainingPoints = totalNeededGradePoints - currentGradePoints

        if (neededRemainingPoints <= 0) {
            return TargetSimulationResult(
                requiredSgpa = 0.0,
                isPossible = true,
                reason = "You have already met or exceeded your target!"
            )
        }

        val requiredSgpa = neededRemainingPoints / remainingUnits
        
        return if (requiredSgpa <= scale) {
            TargetSimulationResult(
                requiredSgpa = requiredSgpa,
                isPossible = true,
                reason = "Attainable. You need an average GPA of %.2f in remaining terms.".format(requiredSgpa)
            )
        } else {
            TargetSimulationResult(
                requiredSgpa = requiredSgpa,
                isPossible = false,
                reason = "Mathematically impossible. Required GPA exceeds scale limit (%.2f).".format(requiredSgpa)
            )
        }
    }
}

data class TargetSimulationResult(
    val requiredSgpa: Double,
    val isPossible: Boolean,
    val reason: String
)
