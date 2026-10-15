package com.deehem.gpawhiz.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "scholarships")
data class Scholarship(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val organization: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val currency: String = "NGN",
    val applicationUrl: String = "",
    val organizationWebsite: String = "",
    val contactEmail: String = "",
    val notes: String = "",
    val openingDate: Long? = null,
    val deadlineDate: Long? = null,
    val expectedFeedbackDate: Long? = null,
    val testDate: Long? = null,
    val interviewDate: Long? = null,
    val followUpDate: Long? = null,
    val status: String = ScholarshipStatus.NOT_STARTED,
    val minCgpa: Double? = null,
    val minScale: Double = 5.0,
    val outcome: String? = null,
    val awardAmount: Double? = null,
    val awardCurrency: String? = null,
    val awardDate: Long? = null,
    val awardNotes: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val dateApplied: Long? = null
) {
    val effectiveAmount: Double
        get() = if (awardAmount != null && awardAmount > 0.0) awardAmount else amount

    val effectiveCurrency: String
        get() = (awardCurrency?.takeIf { it.isNotBlank() } ?: currency).trim().uppercase()
}

data class PredefinedRequirement(
    val title: String,
    val category: String,
    val defaultDetails: String = ""
)


@Entity(
    tableName = "scholarship_requirements",
    foreignKeys = [
        ForeignKey(
            entity = Scholarship::class,
            parentColumns = ["id"],
            childColumns = ["scholarshipId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["scholarshipId"])]
)
data class ScholarshipRequirement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scholarshipId: Int,
    val title: String,
    val category: String = RequirementCategory.OTHER,
    val status: String = RequirementStatus.NOT_STARTED,
    val details: String = "",
    val deadline: Long? = null,
    val notes: String = ""
)

@Entity(
    tableName = "scholarship_timeline_events",
    foreignKeys = [
        ForeignKey(
            entity = Scholarship::class,
            parentColumns = ["id"],
            childColumns = ["scholarshipId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["scholarshipId"])]
)
data class ScholarshipTimelineEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scholarshipId: Int,
    val date: Long,
    val title: String,
    val description: String = "",
    val isAutomatic: Boolean = false
)

@Entity(
    tableName = "scholarship_reminders",
    foreignKeys = [
        ForeignKey(
            entity = Scholarship::class,
            parentColumns = ["id"],
            childColumns = ["scholarshipId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["scholarshipId"])]
)
data class ScholarshipReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scholarshipId: Int,
    val eventType: String,
    val reminderTime: Long,
    val offsetDays: Int = 0,
    val isEnabled: Boolean = true,
    val notes: String = ""
)

@Entity(
    tableName = "exchange_rates",
    primaryKeys = ["fromCurrency", "toCurrency"]
)
data class ExchangeRate(
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val sourceDescription: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "essay_drafts",
    foreignKeys = [
        ForeignKey(
            entity = Scholarship::class,
            parentColumns = ["id"],
            childColumns = ["scholarshipId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["scholarshipId"])]
)
data class EssayDraft(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val scholarshipId: Int? = null,
    val wordLimit: Int? = null,
    val lastModified: Long = System.currentTimeMillis()
)

object ScholarshipStatus {
    const val NOT_STARTED = "Not Started"
    const val PREPARING = "In Preparation"
    const val IN_PROGRESS = "In Progress"
    const val READY_TO_SUBMIT = "Ready to Submit"
    const val SUBMITTED = "Submitted"
    const val ASSESSMENT = "Assessment"
    const val INTERVIEW = "Interview"
    const val AWAITING_RESULT = "Awaiting Result"
    const val AWARDED = "Awarded"
    const val REJECTED = "Rejected"
    const val WITHDRAWN = "Withdrawn"
    const val EXPIRED = "Expired"

    val ALL = listOf(
        NOT_STARTED,
        PREPARING,
        IN_PROGRESS,
        READY_TO_SUBMIT,
        SUBMITTED,
        ASSESSMENT,
        INTERVIEW,
        AWAITING_RESULT,
        AWARDED,
        REJECTED,
        WITHDRAWN,
        EXPIRED
    )

    val ACTIVE = setOf(
        NOT_STARTED,
        PREPARING,
        IN_PROGRESS,
        READY_TO_SUBMIT,
        SUBMITTED,
        ASSESSMENT,
        INTERVIEW,
        AWAITING_RESULT
    )
}

object RequirementStatus {
    const val NOT_STARTED = "Not Started"
    const val IN_PROGRESS = "In Progress"
    const val COMPLETED = "Completed"
    const val SUBMITTED = "Submitted"
    const val NOT_APPLICABLE = "Not Applicable"
}

object RequirementCategory {
    const val DOCUMENT = "Document"
    const val ESSAY = "Essay"
    const val RECOMMENDATION = "Recommendation"
    const val TRANSCRIPT = "Transcript"
    const val TEST_SCORE = "Test Score"
    const val OTHER = "Other"
}
