package com.example

import com.example.data.*
import com.example.service.*
import org.junit.Assert.*
import org.junit.Test

class ScholarshipUnitTest {

    @Test
    fun testEligibilityCheck() {
        val scholarship5Scale = Scholarship(
            name = "MTN Science & Technology Scholarship",
            organization = "MTN Foundation",
            minCgpa = 3.5,
            minScale = 5.0
        )

        // Student with 4.2 on 5.0 scale -> Eligible
        val res1 = ScholarshipCalculationHelper.checkEligibility(
            studentCgpa = 4.2,
            studentScale = 5.0,
            scholarship = scholarship5Scale
        )
        assertEquals(EligibilityStatus.ELIGIBLE, res1.status)
        assertTrue(res1.description.contains("Current CGPA: 4.20"))

        // Student with 3.0 on 5.0 scale -> Ineligible
        val res2 = ScholarshipCalculationHelper.checkEligibility(
            studentCgpa = 3.0,
            studentScale = 5.0,
            scholarship = scholarship5Scale
        )
        assertEquals(EligibilityStatus.INELIGIBLE, res2.status)
        assertTrue(res2.description.contains("Current CGPA: 3.00"))

        // Scholarship with no min CGPA -> Unknown / Verify
        val noMinCgpaScholarship = Scholarship(
            name = "Open Grant",
            organization = "Community Board",
            minCgpa = null
        )
        val res3 = ScholarshipCalculationHelper.checkEligibility(
            studentCgpa = 4.0,
            studentScale = 5.0,
            scholarship = noMinCgpaScholarship
        )
        assertEquals(EligibilityStatus.UNKNOWN, res3.status)
    }

    @Test
    fun testCurrentVsTargetCgpaEligibilityCheck() {
        val scholarship = Scholarship(
            name = "Agbami Medical & Engineering Scholarship",
            organization = "Star Deep Water Petroleum",
            minCgpa = 3.5,
            minScale = 5.0
        )

        // Student with current recorded CGPA of 3.80 vs target 4.50
        val currentCgpa = 3.80
        val targetCgpa = 4.50

        // When comparing with current CGPA
        val currentEligibility = ScholarshipCalculationHelper.checkEligibility(
            studentCgpa = currentCgpa,
            studentScale = 5.0,
            scholarship = scholarship,
            isTarget = false
        )
        assertEquals(EligibilityStatus.ELIGIBLE, currentEligibility.status)
        assertTrue(currentEligibility.description.contains("Current CGPA: 3.80"))

        // When student has not recorded any courses yet (fallback to target CGPA)
        val targetEligibility = ScholarshipCalculationHelper.checkEligibility(
            studentCgpa = targetCgpa,
            studentScale = 5.0,
            scholarship = scholarship,
            isTarget = true
        )
        assertEquals(EligibilityStatus.ELIGIBLE, targetEligibility.status)
        assertTrue(targetEligibility.description.contains("Target CGPA: 4.50"))

        // Cross-scale comparison: 4.0 scale scholarship requiring 3.5 / 4.0
        val usScholarship = Scholarship(
            name = "International Tech Fellowship",
            organization = "Global STEM Foundation",
            minCgpa = 3.5,
            minScale = 4.0
        )

        // Student on 5.0 scale with 4.0 CGPA: 4.0 / 5.0 * 4.0 = 3.20 < 3.50 (Ineligible)
        val crossScaleIneligible = ScholarshipCalculationHelper.checkEligibility(
            studentCgpa = 4.0,
            studentScale = 5.0,
            scholarship = usScholarship
        )
        assertEquals(EligibilityStatus.INELIGIBLE, crossScaleIneligible.status)

        // Student on 5.0 scale with 4.5 CGPA: 4.5 / 5.0 * 4.0 = 3.60 >= 3.50 (Eligible)
        val crossScaleEligible = ScholarshipCalculationHelper.checkEligibility(
            studentCgpa = 4.5,
            studentScale = 5.0,
            scholarship = usScholarship
        )
        assertEquals(EligibilityStatus.ELIGIBLE, crossScaleEligible.status)
    }

    @Test
    fun testFundingSeparationByCurrency() {
        val list = listOf(
            Scholarship(name = "S1", organization = "O1", amount = 200000.0, currency = "₦", status = ScholarshipStatus.AWARDED, awardAmount = 200000.0, awardCurrency = "₦"),
            Scholarship(name = "S2", organization = "O2", amount = 300000.0, currency = "₦", status = ScholarshipStatus.AWARDED, awardAmount = 300000.0, awardCurrency = "₦"),
            Scholarship(name = "S3", organization = "O3", amount = 1500.0, currency = "$", status = ScholarshipStatus.AWARDED, awardAmount = 1500.0, awardCurrency = "$"),
            Scholarship(name = "S4", organization = "O4", amount = 500000.0, currency = "₦", status = ScholarshipStatus.IN_PROGRESS)
        )

        val awardedFunding = ScholarshipCalculationHelper.calculateFundingByCurrency(list, awardedOnly = true)
        assertEquals(500000.0, awardedFunding["₦"] ?: 0.0, 0.01)
        assertEquals(1500.0, awardedFunding["$"] ?: 0.0, 0.01)

        val pipelineFunding = ScholarshipCalculationHelper.calculateFundingByCurrency(
            list.filter { it.status in ScholarshipStatus.ACTIVE },
            awardedOnly = false
        )
        assertEquals(500000.0, pipelineFunding["₦"] ?: 0.0, 0.01)
    }

    @Test
    fun testRequirementsProgress() {
        val reqs = listOf(
            ScholarshipRequirement(scholarshipId = 1, title = "Transcript", status = RequirementStatus.COMPLETED),
            ScholarshipRequirement(scholarshipId = 1, title = "Essay", status = RequirementStatus.IN_PROGRESS),
            ScholarshipRequirement(scholarshipId = 1, title = "Reference Letter", status = RequirementStatus.NOT_STARTED),
            ScholarshipRequirement(scholarshipId = 1, title = "Optional Audio", status = RequirementStatus.NOT_APPLICABLE)
        )

        val progress = ScholarshipCalculationHelper.getRequirementsProgress(reqs)
        assertEquals(4, progress.total) // total reqs including NOT_APPLICABLE
        assertEquals(1, progress.completed)
        assertEquals(1, progress.inProgress)
        assertEquals(1, progress.notStarted)
        assertEquals(33.33f, progress.percentage, 0.5f)
    }

    @Test
    fun testScholarshipStatistics() {
        val scholarships = listOf(
            Scholarship(name = "S1", organization = "O1", status = ScholarshipStatus.PREPARING),
            Scholarship(name = "S2", organization = "O2", status = ScholarshipStatus.SUBMITTED),
            Scholarship(name = "S3", organization = "O3", status = ScholarshipStatus.AWARDED, outcome = "Awarded", awardAmount = 250000.0, awardCurrency = "₦"),
            Scholarship(name = "S4", organization = "O4", status = ScholarshipStatus.REJECTED, outcome = "Rejected")
        )

        val stats = ScholarshipCalculationHelper.calculateStatistics(scholarships, emptyList())
        assertEquals(4, stats.total)
        assertEquals(2, stats.active) // PREPARING, SUBMITTED
        assertEquals(1, stats.awaitingResults)
        assertEquals(1, stats.awarded)
        assertEquals(1, stats.rejected)
        assertEquals(50.0, stats.successRate, 0.01) // 1 awarded out of 2 completed (awarded + rejected)
    }

    @Test
    fun testScholarshipXlsxWorkbookGeneration() {
        val student = StudentProfile(
            fullName = "Tunde Adeleke",
            institution = "University of Lagos",
            faculty = "Engineering",
            department = "Computer Engineering",
            currentLevel = "300L",
            gradingScale = 5.0,
            targetCgpa = 4.65
        )

        val scholarships = listOf(
            Scholarship(
                id = 1,
                name = "NNPC / Chevron National University Scholarship",
                organization = "Chevron Nigeria Limited",
                amount = 250000.0,
                currency = "₦",
                status = ScholarshipStatus.SUBMITTED,
                applicationUrl = "https://example.com/apply"
            )
        )

        val reqs = listOf(
            ScholarshipRequirement(
                id = 1,
                scholarshipId = 1,
                title = "O'Level Result",
                category = RequirementCategory.ACADEMIC,
                status = RequirementStatus.COMPLETED
            )
        )

        val events = listOf(
            ScholarshipTimelineEvent(
                id = 1,
                scholarshipId = 1,
                title = "Application Submitted Online",
                description = "Portal reference #CHV-2026-99"
            )
        )

        val baos = java.io.ByteArrayOutputStream()
        ScholarshipXlsxGenerator.generateScholarshipWorkbook(
            student = student,
            calculatedCgpa = 4.72,
            scholarships = scholarships,
            allRequirements = reqs,
            allTimelineEvents = events,
            outputStream = baos
        )

        val bytes = baos.toByteArray()
        assertTrue("XLSX output should not be empty", bytes.isNotEmpty())

        // Verify ZIP entries
        val zis = java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes))
        val entryNames = mutableListOf<String>()
        var entry = zis.nextEntry
        while (entry != null) {
            entryNames.add(entry.name)
            zis.closeEntry()
            entry = zis.nextEntry
        }

        assertTrue(entryNames.contains("[Content_Types].xml"))
        assertTrue(entryNames.contains("xl/workbook.xml"))
        assertTrue(entryNames.contains("xl/styles.xml"))
        assertTrue(entryNames.contains("xl/worksheets/sheet1.xml"))
        assertTrue(entryNames.contains("xl/worksheets/sheet2.xml"))
        assertTrue(entryNames.contains("xl/worksheets/sheet3.xml"))
        assertTrue(entryNames.contains("xl/worksheets/sheet4.xml"))
    }
}
