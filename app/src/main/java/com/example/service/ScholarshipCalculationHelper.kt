package com.example.service

import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object ScholarshipCalculationHelper {

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val fullDateTimeFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    private val shortDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun formatDate(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "Not set"
        return dateFormatter.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "N/A"
        return shortDateFormatter.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "Not set"
        return fullDateTimeFormatter.format(Date(timestamp))
    }

    /**
     * Compute days difference between target timestamp and today (midnight to midnight).
     */
    fun getDaysDifference(targetDate: Long): Long {
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetCal = Calendar.getInstance().apply {
            timeInMillis = targetDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = targetCal.timeInMillis - nowCal.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffMillis)
    }

    /**
     * Deadline countdown text and urgency indicator.
     */
    fun getDeadlineCountdown(deadlineDate: Long?): DeadlineCountdownResult {
        if (deadlineDate == null || deadlineDate <= 0) {
            return DeadlineCountdownResult("No deadline specified", UrgencyLevel.NONE, 0)
        }

        val days = getDaysDifference(deadlineDate)
        return when {
            days > 1 -> DeadlineCountdownResult("$days days remaining", if (days <= 7) UrgencyLevel.HIGH else if (days <= 14) UrgencyLevel.MEDIUM else UrgencyLevel.LOW, days)
            days == 1L -> DeadlineCountdownResult("Deadline tomorrow", UrgencyLevel.CRITICAL, days)
            days == 0L -> DeadlineCountdownResult("Deadline today!", UrgencyLevel.CRITICAL, days)
            days == -1L -> DeadlineCountdownResult("Deadline passed yesterday", UrgencyLevel.PASSED, days)
            else -> DeadlineCountdownResult("Deadline passed (${-days} days ago)", UrgencyLevel.PASSED, days)
        }
    }

    /**
     * Expected feedback status text and urgency.
     */
    fun getExpectedFeedbackStatus(feedbackDate: Long?, status: String): FeedbackStatusResult {
        if (feedbackDate == null || feedbackDate <= 0) {
            return FeedbackStatusResult("Not specified", false, 0)
        }

        val days = getDaysDifference(feedbackDate)
        return when {
            days > 1 -> FeedbackStatusResult("Result expected in $days days", false, days)
            days == 1L -> FeedbackStatusResult("Result expected tomorrow", false, days)
            days == 0L -> FeedbackStatusResult("Result expected today", false, days)
            days == -1L -> FeedbackStatusResult("Feedback overdue by 1 day", true, days)
            else -> FeedbackStatusResult("Feedback overdue by ${-days} days", true, days)
        }
    }

    /**
     * Calculate requirements completion statistics.
     */
    fun getRequirementsProgress(requirements: List<ScholarshipRequirement>): RequirementsProgress {
        if (requirements.isEmpty()) {
            return RequirementsProgress(0, 0, 0, 0, 0f)
        }
        val activeReqs = requirements.filter { it.status != RequirementStatus.NOT_APPLICABLE }
        if (activeReqs.isEmpty()) {
            return RequirementsProgress(requirements.size, 0, 0, 0, 100f)
        }
        val completed = activeReqs.count { it.status == RequirementStatus.COMPLETED || it.status == RequirementStatus.SUBMITTED }
        val inProgress = activeReqs.count { it.status == RequirementStatus.IN_PROGRESS }
        val notStarted = activeReqs.count { it.status == RequirementStatus.NOT_STARTED }
        val progressPercent = (completed.toFloat() / activeReqs.size.toFloat()) * 100f

        return RequirementsProgress(
            total = requirements.size,
            completed = completed,
            inProgress = inProgress,
            notStarted = notStarted,
            percentage = progressPercent.coerceIn(0f, 100f)
        )
    }

    /**
     * Rule-based next action determination (completely local and deterministic).
     */
    fun determineNextAction(
        scholarship: Scholarship,
        requirements: List<ScholarshipRequirement>
    ): NextActionInfo {
        val incompleteReq = requirements.firstOrNull { 
            it.status != RequirementStatus.COMPLETED && 
            it.status != RequirementStatus.SUBMITTED && 
            it.status != RequirementStatus.NOT_APPLICABLE 
        }

        return when (scholarship.status) {
            ScholarshipStatus.AWARDED -> {
                NextActionInfo(
                    title = "Review Award & Acceptance",
                    description = "Verify award requirements, disbursement timelines, and acceptance confirmation.",
                    type = NextActionType.SUCCESS
                )
            }
            ScholarshipStatus.REJECTED, ScholarshipStatus.WITHDRAWN, ScholarshipStatus.EXPIRED -> {
                NextActionInfo(
                    title = "Application Concluded",
                    description = "Archive notes and review lessons for future scholarship applications.",
                    type = NextActionType.INFO
                )
            }
            ScholarshipStatus.SUBMITTED, ScholarshipStatus.AWAITING_RESULT -> {
                val feedbackStatus = getExpectedFeedbackStatus(scholarship.expectedFeedbackDate, scholarship.status)
                if (feedbackStatus.isOverdue) {
                    NextActionInfo(
                        title = "Follow Up with Provider",
                        description = "Expected feedback date has passed. Check email or portal for admission/award updates.",
                        type = NextActionType.ATTENTION
                    )
                } else if (scholarship.expectedFeedbackDate != null) {
                    NextActionInfo(
                        title = "Awaiting Decision",
                        description = "${feedbackStatus.label}. Keep an eye on your email and portal messages.",
                        type = NextActionType.NEUTRAL
                    )
                } else {
                    NextActionInfo(
                        title = "Awaiting Decision",
                        description = "Application submitted. Record expected outcome date once announced.",
                        type = NextActionType.NEUTRAL
                    )
                }
            }
            ScholarshipStatus.ASSESSMENT -> {
                val testDateStr = formatDate(scholarship.testDate)
                NextActionInfo(
                    title = "Prepare for Assessment / Test",
                    description = if (scholarship.testDate != null) "Test scheduled for $testDateStr. Review syllabus & past questions." else "Prepare materials for the upcoming evaluation.",
                    type = NextActionType.PRIORITY
                )
            }
            ScholarshipStatus.INTERVIEW -> {
                val interviewDateStr = formatDate(scholarship.interviewDate)
                NextActionInfo(
                    title = "Prepare for Interview",
                    description = if (scholarship.interviewDate != null) "Interview date: $interviewDateStr. Practice key talking points and questions." else "Prepare personal presentation and motivation points.",
                    type = NextActionType.PRIORITY
                )
            }
            ScholarshipStatus.READY_TO_SUBMIT -> {
                val deadlineCountdown = getDeadlineCountdown(scholarship.deadlineDate)
                NextActionInfo(
                    title = "Submit Application",
                    description = "All requirements completed! Submit before deadline (${deadlineCountdown.label}).",
                    type = NextActionType.PRIORITY
                )
            }
            else -> { // NOT_STARTED, PREPARING, IN_PROGRESS
                if (incompleteReq != null) {
                    NextActionInfo(
                        title = "Complete ${incompleteReq.title}",
                        description = if (incompleteReq.details.isNotEmpty()) incompleteReq.details else "Category: ${incompleteReq.category}. Mark as completed once prepared.",
                        type = NextActionType.ACTION
                    )
                } else if (requirements.isNotEmpty()) {
                    NextActionInfo(
                        title = "Finalize Submission Package",
                        description = "All current requirements are marked complete. Review application and set status to 'Ready to Submit'.",
                        type = NextActionType.ACTION
                    )
                } else {
                    NextActionInfo(
                        title = "Add Application Requirements",
                        description = "Add required documents (transcripts, essays, references) to track checklist progress.",
                        type = NextActionType.ACTION
                    )
                }
            }
        }
    }

    /**
     * Local eligibility comparison between student CGPA and scholarship min CGPA.
     */
    fun checkEligibility(
        studentCgpa: Double,
        studentScale: Double,
        scholarship: Scholarship,
        isTarget: Boolean = false
    ): EligibilityResult {
        val minCgpa = scholarship.minCgpa
        if (minCgpa == null || minCgpa <= 0.0) {
            return EligibilityResult(
                status = EligibilityStatus.UNKNOWN,
                badgeText = "Verify Criteria",
                description = "No minimum CGPA requirement stated. Check official criteria for specific eligibility rules.",
                colorHex = "#757575"
            )
        }

        // Normalize if scales differ (e.g. 4.0 vs 5.0)
        val normalizedStudentCgpa = if (studentScale != scholarship.minScale && scholarship.minScale > 0) {
            (studentCgpa / studentScale) * scholarship.minScale
        } else {
            studentCgpa
        }

        val cgpaLabel = if (isTarget) "Target CGPA" else "Current CGPA"

        return if (normalizedStudentCgpa >= minCgpa - 0.001) {
            val margin = normalizedStudentCgpa - minCgpa
            EligibilityResult(
                status = EligibilityStatus.ELIGIBLE,
                badgeText = "Eligible (CGPA)",
                description = "Meets CGPA requirement ($cgpaLabel: %.2f / Min: %.2f). Margin: +%.2f".format(studentCgpa, minCgpa, margin),
                colorHex = "#0F9D58"
            )
        } else {
            val deficit = minCgpa - normalizedStudentCgpa
            EligibilityResult(
                status = EligibilityStatus.INELIGIBLE,
                badgeText = "Below Cut-Off",
                description = "Below stated CGPA requirement ($cgpaLabel: %.2f / Min: %.2f). Deficit: -%.2f".format(studentCgpa, minCgpa, deficit),
                colorHex = "#DB4437"
            )
        }
    }

    /**
     * Group funding amounts by currency strictly without mathematically merging them.
     */
    fun calculateFundingByCurrency(
        scholarships: List<Scholarship>,
        awardedOnly: Boolean = false
    ): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for (s in scholarships) {
            if (awardedOnly) {
                if (s.status == ScholarshipStatus.AWARDED || s.outcome.equals("Awarded", ignoreCase = true)) {
                    val amt = s.awardAmount ?: s.amount
                    val curr = (s.awardCurrency ?: s.currency).ifBlank { "₦" }
                    if (amt > 0) {
                        result[curr] = (result[curr] ?: 0.0) + amt
                    }
                }
            } else {
                val amt = s.amount
                val curr = s.currency.ifBlank { "₦" }
                if (amt > 0) {
                    result[curr] = (result[curr] ?: 0.0) + amt
                }
            }
        }
        return result
    }

    /**
     * Compute dashboard analytics
     */
    fun calculateStatistics(
        scholarships: List<Scholarship>,
        requirements: List<ScholarshipRequirement>
    ): ScholarshipStats = calculateScholarshipStats(scholarships, requirements)

    fun calculateScholarshipStats(
        scholarships: List<Scholarship>,
        requirements: List<ScholarshipRequirement>
    ): ScholarshipStats {
        val total = scholarships.size
        val active = scholarships.count { it.status in ScholarshipStatus.ACTIVE }
        val awaitingResults = scholarships.count { it.status == ScholarshipStatus.AWAITING_RESULT || it.status == ScholarshipStatus.SUBMITTED }
        val awarded = scholarships.count { it.status == ScholarshipStatus.AWARDED || it.outcome.equals("Awarded", ignoreCase = true) }
        val rejected = scholarships.count { it.status == ScholarshipStatus.REJECTED || it.outcome.equals("Rejected", ignoreCase = true) }
        
        // Upcoming deadlines: within next 30 days and not passed
        val upcomingDeadlines = scholarships.count { s ->
            s.deadlineDate != null && s.status in ScholarshipStatus.ACTIVE && getDaysDifference(s.deadlineDate) in 0..30
        }

        val completedApps = awarded + rejected
        val successRate = if (completedApps > 0) (awarded.toDouble() / completedApps.toDouble()) * 100.0 else 0.0

        val awardedFunding = calculateFundingByCurrency(scholarships, awardedOnly = true)
        val pipelineFunding = calculateFundingByCurrency(scholarships.filter { it.status in ScholarshipStatus.ACTIVE }, awardedOnly = false)

        return ScholarshipStats(
            total = total,
            active = active,
            upcomingDeadlines = upcomingDeadlines,
            awaitingResults = awaitingResults,
            awarded = awarded,
            rejected = rejected,
            successRate = successRate,
            awardedFunding = awardedFunding,
            pipelineFunding = pipelineFunding
        )
    }

    /**
     * Format currency amount (e.g. "₦1,500,000" or "$5,000")
     */
    fun formatCurrency(amount: Double, currency: String): String {
        return if (amount <= 0.0) {
            "Not specified"
        } else {
            String.format(Locale.getDefault(), "%s %,.0f", currency, amount)
        }
    }
}

enum class UrgencyLevel {
    NONE, LOW, MEDIUM, HIGH, CRITICAL, PASSED
}

data class DeadlineCountdownResult(
    val label: String,
    val urgency: UrgencyLevel,
    val daysRemaining: Long
)

data class FeedbackStatusResult(
    val label: String,
    val isOverdue: Boolean,
    val daysDifference: Long
)

data class RequirementsProgress(
    val total: Int,
    val completed: Int,
    val inProgress: Int,
    val notStarted: Int,
    val percentage: Float
)

enum class NextActionType {
    ACTION, PRIORITY, ATTENTION, NEUTRAL, SUCCESS, INFO
}

data class NextActionInfo(
    val title: String,
    val description: String,
    val type: NextActionType
)

enum class EligibilityStatus {
    ELIGIBLE, INELIGIBLE, UNKNOWN
}

data class EligibilityResult(
    val status: EligibilityStatus,
    val badgeText: String,
    val description: String,
    val colorHex: String
)

data class ScholarshipStats(
    val total: Int,
    val active: Int,
    val upcomingDeadlines: Int,
    val awaitingResults: Int,
    val awarded: Int,
    val rejected: Int,
    val successRate: Double,
    val awardedFunding: Map<String, Double>,
    val pipelineFunding: Map<String, Double>
) {
    val totalApplications: Int get() = total
    val activeApplications: Int get() = active
    val upcomingDeadlinesCount: Int get() = upcomingDeadlines
    val awaitingResultsCount: Int get() = awaitingResults
    val awardedCount: Int get() = awarded
    val totalAwardedFunds: Map<String, Double> get() = awardedFunding
}
