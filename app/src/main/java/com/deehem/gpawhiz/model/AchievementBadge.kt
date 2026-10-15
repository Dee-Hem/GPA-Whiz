package com.deehem.gpawhiz.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.deehem.gpawhiz.data.Course
import com.deehem.gpawhiz.data.Scholarship
import com.deehem.gpawhiz.data.ScholarshipStatus
import com.deehem.gpawhiz.data.Semester
import com.deehem.gpawhiz.data.StudentProfile
import com.deehem.gpawhiz.data.StudySession
import com.deehem.gpawhiz.data.StudyStatus
import com.deehem.gpawhiz.service.GpaCalcService

data class BadgeDefinition(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

data class BadgeStatus(
    val badge: BadgeDefinition,
    val isUnlocked: Boolean,
    val progressHint: String
)

object AchievementBadges {

    val DEANS_LIST = BadgeDefinition(
        id = "deans_list",
        title = "Dean's List",
        description = "SGPA of 4.50+ in any term on 5.0 scale (3.50+ on 4.0 scale).",
        icon = Icons.Default.School
    )

    val PERFECT_TERM = BadgeDefinition(
        id = "perfect_term",
        title = "Perfect Term",
        description = "Achieve the maximum possible SGPA in any term.",
        icon = Icons.Default.Star
    )

    val CREDIT_CRUSHER = BadgeDefinition(
        id = "credit_crusher",
        title = "Credit Crusher",
        description = "Pass 100 or more academic credits.",
        icon = Icons.Default.WorkspacePremium
    )

    val HALFWAY_THERE = BadgeDefinition(
        id = "halfway_there",
        title = "Halfway There",
        description = "Complete 50% of your required graduation credits.",
        icon = Icons.Default.Flag
    )

    val COMEBACK_KID = BadgeDefinition(
        id = "comeback_kid",
        title = "Comeback Kid",
        description = "CGPA rose by 0.20 or more from one semester to the next.",
        icon = Icons.Default.TrendingUp
    )

    val CLEAN_SHEET = BadgeDefinition(
        id = "clean_sheet",
        title = "Clean Sheet",
        description = "No carry-over courses across at least 2 completed semesters.",
        icon = Icons.Default.Verified
    )

    val SCHOLARSHIP_FINALIST = BadgeDefinition(
        id = "scholarship_finalist",
        title = "Scholarship Finalist",
        description = "At least one scholarship with status Awaiting Result.",
        icon = Icons.Default.HourglassTop
    )

    val SCHOLARSHIP_WINNER = BadgeDefinition(
        id = "scholarship_winner",
        title = "Scholarship Winner",
        description = "At least one scholarship with status Awarded.",
        icon = Icons.Default.EmojiEvents
    )

    val FOCUS_STARTER = BadgeDefinition(
        id = "focus_starter",
        title = "Focus Starter",
        description = "Log at least 10 hours (600 mins) of completed study sessions.",
        icon = Icons.Default.SelfImprovement
    )

    val ALL = listOf(
        DEANS_LIST,
        PERFECT_TERM,
        CREDIT_CRUSHER,
        HALFWAY_THERE,
        COMEBACK_KID,
        CLEAN_SHEET,
        SCHOLARSHIP_FINALIST,
        SCHOLARSHIP_WINNER,
        FOCUS_STARTER
    )

    fun evaluateAll(
        profile: StudentProfile,
        semesters: List<Semester>,
        courses: List<Course>,
        scholarships: List<Scholarship>,
        studySessions: List<StudySession>
    ): List<BadgeStatus> {
        val scale = profile.gradingScale
        val isScale4 = scale <= 4.0
        val deansThreshold = if (isScale4) 3.50 else 4.50

        // 1. Graded courses
        val gradedCourses = courses.filter {
            !it.grade.equals("Awaiting Grade", ignoreCase = true) &&
            !it.grade.equals("AR", ignoreCase = true)
        }

        // Passed courses (grade != F)
        val passedCourses = gradedCourses.filter { !it.grade.equals("F", ignoreCase = true) }
        val passedCredits = passedCourses.sumOf { it.units }

        // Semesters with at least 1 graded course
        val completedSemesters = semesters.filter { sem ->
            gradedCourses.any { it.semesterId == sem.id }
        }.sortedBy { it.rank }

        // SGPAs for completed semesters
        val termSgpas = completedSemesters.map { sem ->
            val semCourses = gradedCourses.filter { it.semesterId == sem.id }
            GpaCalcService.calculateSgpa(semCourses, sem.gradingScale)
        }

        val maxSgpa = termSgpas.maxOrNull() ?: 0.0

        // 1. Dean's List
        val deansUnlocked = maxSgpa >= deansThreshold
        val deansHint = if (deansUnlocked) {
            "Unlocked! Best term: %.2f".format(maxSgpa)
        } else {
            "Best term: %.2f / %.2f needed".format(maxSgpa, deansThreshold)
        }

        // 2. Perfect Term
        val perfectThreshold = scale
        val perfectUnlocked = termSgpas.any { it >= perfectThreshold - 0.001 }
        val perfectHint = if (perfectUnlocked) {
            "Unlocked! Maximum term GPA attained."
        } else {
            "Best term: %.2f / %.1f needed".format(maxSgpa, perfectThreshold)
        }

        // 3. Credit Crusher
        val creditUnlocked = passedCredits >= 100
        val creditHint = if (creditUnlocked) {
            "Unlocked! $passedCredits credits passed."
        } else {
            "$passedCredits of 100 credits"
        }

        // 4. Halfway There
        val reqCredits = profile.totalRequiredCredits
        val halfwayTarget = (reqCredits / 2.0).toInt().coerceAtLeast(1)
        val halfwayUnlocked = passedCredits >= halfwayTarget
        val halfwayHint = if (halfwayUnlocked) {
            "Unlocked! $passedCredits of $halfwayTarget credits (50%)."
        } else {
            "$passedCredits of $halfwayTarget credits (50%)"
        }

        // 5. Comeback Kid
        var bestJump = 0.0
        var comebackUnlocked = false
        if (completedSemesters.size >= 2) {
            var previousCgpa: Double? = null
            for (i in 1..completedSemesters.size) {
                val subSemesters = completedSemesters.take(i)
                val subCourses = gradedCourses.filter { c -> subSemesters.any { it.id == c.semesterId } }
                val currentCgpa = GpaCalcService.calculateCgpa(subSemesters, subCourses)
                if (previousCgpa != null) {
                    val delta = currentCgpa - previousCgpa
                    if (delta > bestJump) bestJump = delta
                    if (delta >= 0.20) comebackUnlocked = true
                }
                previousCgpa = currentCgpa
            }
        }
        val comebackHint = if (comebackUnlocked) {
            "Unlocked! CGPA jumped by +%.2f.".format(bestJump)
        } else if (completedSemesters.size < 2) {
            "Complete at least 2 terms"
        } else {
            "Best term jump: +%.2f (+0.20 needed)".format(bestJump.coerceAtLeast(0.0))
        }

        // 6. Clean Sheet
        val hasCarryOver = courses.any { it.isCarryOver || it.grade.uppercase() == "F" }
        val cleanUnlocked = completedSemesters.size >= 2 && !hasCarryOver
        val cleanHint = if (cleanUnlocked) {
            "Unlocked! Zero carry-overs across ${completedSemesters.size} terms."
        } else if (hasCarryOver) {
            "Carry-over course recorded"
        } else {
            "${completedSemesters.size} of 2 completed terms"
        }

        // 7. Scholarship Finalist
        val finalistUnlocked = scholarships.any {
            it.status.equals(ScholarshipStatus.AWAITING_RESULT, ignoreCase = true)
        }
        val finalistHint = if (finalistUnlocked) {
            "Unlocked! Application awaiting result."
        } else {
            "0 of 1 scholarship awaiting result"
        }

        // 8. Scholarship Winner
        val winnerUnlocked = scholarships.any {
            it.status.equals(ScholarshipStatus.AWARDED, ignoreCase = true)
        }
        val winnerHint = if (winnerUnlocked) {
            "Unlocked! Scholarship awarded."
        } else {
            "0 of 1 scholarship awarded"
        }

        // 9. Focus Starter
        val completedFocusMinutes = studySessions
            .filter { it.status.equals(StudyStatus.COMPLETED, ignoreCase = true) }
            .sumOf { it.durationMinutes }
        val focusUnlocked = completedFocusMinutes >= 600
        val focusHours = completedFocusMinutes / 60
        val focusMins = completedFocusMinutes % 60
        val focusHint = if (focusUnlocked) {
            "Unlocked! ${focusHours}h ${focusMins}m of focused study logged."
        } else {
            "${focusHours}h ${focusMins}m of 10h logged"
        }

        return listOf(
            BadgeStatus(DEANS_LIST, deansUnlocked, deansHint),
            BadgeStatus(PERFECT_TERM, perfectUnlocked, perfectHint),
            BadgeStatus(CREDIT_CRUSHER, creditUnlocked, creditHint),
            BadgeStatus(HALFWAY_THERE, halfwayUnlocked, halfwayHint),
            BadgeStatus(COMEBACK_KID, comebackUnlocked, comebackHint),
            BadgeStatus(CLEAN_SHEET, cleanUnlocked, cleanHint),
            BadgeStatus(SCHOLARSHIP_FINALIST, finalistUnlocked, finalistHint),
            BadgeStatus(SCHOLARSHIP_WINNER, winnerUnlocked, winnerHint),
            BadgeStatus(FOCUS_STARTER, focusUnlocked, focusHint)
        )
    }
}
