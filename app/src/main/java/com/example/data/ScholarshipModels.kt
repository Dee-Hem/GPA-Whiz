package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "scholarships")
data class Scholarship(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val organization: String,
    val description: String = "",
    val amount: Double = 0.0,
    val currency: String = "₦", // Default Naira, supports $, £, €, CAD, etc.
    val applicationUrl: String = "",
    val organizationWebsite: String = "",
    val contactEmail: String = "",
    val notes: String = "",
    val openingDate: Long? = null, // Epoch millis
    val deadlineDate: Long? = null, // Epoch millis
    val expectedFeedbackDate: Long? = null, // Epoch millis
    val testDate: Long? = null, // Epoch millis
    val interviewDate: Long? = null, // Epoch millis
    val followUpDate: Long? = null, // Epoch millis
    val status: String = ScholarshipStatus.NOT_STARTED,
    val minCgpa: Double? = null, // For local eligibility comparison
    val minScale: Double = 5.0,
    val outcome: String? = null, // Awarded, Rejected, Waitlisted, Withdrawn, Other
    val awardAmount: Double? = null,
    val awardCurrency: String? = null,
    val awardDate: Long? = null,
    val awardNotes: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val dateApplied: Long? = null
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
    val deadline: Long? = null, // Epoch millis
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
    val date: Long = System.currentTimeMillis(),
    val title: String,
    val description: String = "",
    val isAutomatic: Boolean = true
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
    val eventType: String, // e.g. "Deadline", "Interview", "Expected Result"
    val reminderTime: Long, // Epoch millis
    val offsetDays: Int = 0, // e.g. 30, 14, 7, 3, 1, 0
    val isEnabled: Boolean = true,
    val notes: String = ""
)

object ScholarshipStatus {
    const val NOT_STARTED = "Not Started"
    const val PREPARING = "Preparing"
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

    val ACTIVE = listOf(
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

    val ALL = listOf(
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        SUBMITTED,
        NOT_APPLICABLE
    )
}

object RequirementCategory {
    const val ACADEMIC = "Academic"
    const val PERSONAL_DOCS = "Personal Documents"
    const val WRITTEN_MATERIALS = "Written Materials"
    const val REFERENCES = "References"
    const val FINANCIAL = "Financial"
    const val OTHER = "Other"

    val ALL = listOf(
        ACADEMIC,
        PERSONAL_DOCS,
        WRITTEN_MATERIALS,
        REFERENCES,
        FINANCIAL,
        OTHER
    )
}

data class PredefinedRequirement(
    val title: String,
    val category: String,
    val defaultDetails: String = ""
)

object PredefinedRequirements {
    val LIST = listOf(
        // Academic
        PredefinedRequirement("Academic Transcript", RequirementCategory.ACADEMIC, "Official or unofficial university transcript showing grades and CGPA."),
        PredefinedRequirement("Academic Certificate", RequirementCategory.ACADEMIC, "Degree or diploma certificate copy."),
        PredefinedRequirement("Proof of Enrollment", RequirementCategory.ACADEMIC, "Official letter confirming current student registration."),
        PredefinedRequirement("Admission Letter", RequirementCategory.ACADEMIC, "University admission confirmation letter."),
        PredefinedRequirement("English Proficiency Certificate", RequirementCategory.ACADEMIC, "IELTS, TOEFL, or official English language instruction certificate."),
        PredefinedRequirement("English Test Score", RequirementCategory.ACADEMIC, "Standardized English language test score report."),
        PredefinedRequirement("Standardized Test Score", RequirementCategory.ACADEMIC, "GRE, GMAT, SAT, or equivalent test score record."),
        PredefinedRequirement("Student ID", RequirementCategory.ACADEMIC, "Copy of current student identification card."),

        // Personal Documents
        PredefinedRequirement("International Passport", RequirementCategory.PERSONAL_DOCS, "Passport data page copy valid for the required duration."),
        PredefinedRequirement("National ID", RequirementCategory.PERSONAL_DOCS, "National identity card, NIN slip, or government ID."),
        PredefinedRequirement("Birth Certificate", RequirementCategory.PERSONAL_DOCS, "Official birth certificate or declaration of age."),
        PredefinedRequirement("Passport Photograph", RequirementCategory.PERSONAL_DOCS, "Recent passport-sized photograph on plain background."),
        PredefinedRequirement("Certificate of Origin", RequirementCategory.PERSONAL_DOCS, "State or local government certificate of origin."),
        PredefinedRequirement("Proof of Address", RequirementCategory.PERSONAL_DOCS, "Utility bill or formal proof of residence."),
        PredefinedRequirement("Proof of Residence", RequirementCategory.PERSONAL_DOCS, "Document verifying physical address."),

        // Written Materials
        PredefinedRequirement("CV / Resume", RequirementCategory.WRITTEN_MATERIALS, "Updated academic and extracurricular curriculum vitae."),
        PredefinedRequirement("Personal Statement", RequirementCategory.WRITTEN_MATERIALS, "Statement describing academic background, goals, and motivations."),
        PredefinedRequirement("Statement of Purpose", RequirementCategory.WRITTEN_MATERIALS, "Detailed statement outlining future academic and career objectives."),
        PredefinedRequirement("Motivation Letter", RequirementCategory.WRITTEN_MATERIALS, "Letter expressing reasons for choosing this scholarship program."),
        PredefinedRequirement("Essay", RequirementCategory.WRITTEN_MATERIALS, "Required thematic essay as specified by the scholarship provider."),
        PredefinedRequirement("Leadership Essay", RequirementCategory.WRITTEN_MATERIALS, "Essay describing leadership experience and impact."),
        PredefinedRequirement("Personal Essay", RequirementCategory.WRITTEN_MATERIALS, "Reflective personal essay."),
        PredefinedRequirement("Cover Letter", RequirementCategory.WRITTEN_MATERIALS, "Formal cover letter for the scholarship application."),
        PredefinedRequirement("Study Plan", RequirementCategory.WRITTEN_MATERIALS, "Detailed outline of intended academic curriculum and timelines."),
        PredefinedRequirement("Research Proposal", RequirementCategory.WRITTEN_MATERIALS, "Structured academic research proposal and methodology."),

        // References
        PredefinedRequirement("Recommendation Letter", RequirementCategory.REFERENCES, "Confidential letter of recommendation from an academic referee."),
        PredefinedRequirement("Academic Reference", RequirementCategory.REFERENCES, "Reference letter from a professor or lecturer."),
        PredefinedRequirement("Professional Reference", RequirementCategory.REFERENCES, "Reference from an employer, internship supervisor, or mentor."),
        PredefinedRequirement("Recommendation Form", RequirementCategory.REFERENCES, "Specific completed evaluation form provided by scholarship committee."),

        // Financial
        PredefinedRequirement("Financial Need Statement", RequirementCategory.FINANCIAL, "Explanation of financial circumstances and need for assistance."),
        PredefinedRequirement("Bank Statement", RequirementCategory.FINANCIAL, "Stamped bank statement covering recent months."),
        PredefinedRequirement("Income Documents", RequirementCategory.FINANCIAL, "Salary payslips or proof of earnings."),
        PredefinedRequirement("Financial Documents", RequirementCategory.FINANCIAL, "Supporting financial declarations and records."),
        PredefinedRequirement("Family Income Information", RequirementCategory.FINANCIAL, "Statement or evidence of parental/guardian financial background."),

        // Other
        PredefinedRequirement("Portfolio", RequirementCategory.OTHER, "Sample of creative, technical, or research work."),
        PredefinedRequirement("Medical Certificate", RequirementCategory.OTHER, "Medical fitness report or health certificate."),
        PredefinedRequirement("Application Form", RequirementCategory.OTHER, "Completed and signed scholarship application document."),
        PredefinedRequirement("Scholarship Form", RequirementCategory.OTHER, "Official application form required by the foundation."),
        PredefinedRequirement("Consent Form", RequirementCategory.OTHER, "Signed guardian or applicant consent form."),
        PredefinedRequirement("Signature", RequirementCategory.OTHER, "Digital or signed authorization."),
        PredefinedRequirement("Other", RequirementCategory.OTHER, "Custom scholarship requirement.")
    )
}
