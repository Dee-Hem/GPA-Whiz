package com.example.service

import com.example.data.Scholarship
import com.example.data.ScholarshipRequirement
import com.example.data.ScholarshipStatus
import com.example.data.ScholarshipTimelineEvent
import com.example.data.StudentProfile
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ScholarshipXlsxGenerator {

    private val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val sdfDateTime = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())

    /**
     * Generate 4-Sheet Comprehensive Scholarship Workbook (.xlsx)
     * Sheet 1: Scholarship Dashboard
     * Sheet 2: Requirements
     * Sheet 3: Application Timeline
     * Sheet 4: Summary
     */
    fun generateScholarshipWorkbook(
        student: StudentProfile,
        calculatedCgpa: Double,
        scholarships: List<Scholarship>,
        allRequirements: List<ScholarshipRequirement>,
        allTimelineEvents: List<ScholarshipTimelineEvent>,
        outputStream: OutputStream
    ) {
        val zip = ZipOutputStream(outputStream)

        // 1. [Content_Types].xml
        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
        writeZipEntry(zip, "[Content_Types].xml", contentTypesXml)

        // 2. _rels/.rels
        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
        writeZipEntry(zip, "_rels/.rels", relsXml)

        // 3. xl/_rels/workbook.xml.rels
        val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
        writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml)

        // 4. xl/workbook.xml
        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Scholarship Dashboard" sheetId="1" r:id="rId1"/>
    <sheet name="Requirements" sheetId="2" r:id="rId2"/>
    <sheet name="Application Timeline" sheetId="3" r:id="rId3"/>
    <sheet name="Summary" sheetId="4" r:id="rId4"/>
  </sheets>
</workbook>"""
        writeZipEntry(zip, "xl/workbook.xml", workbookXml)

        // 5. xl/styles.xml
        val stylesXml = buildStylesXml()
        writeZipEntry(zip, "xl/styles.xml", stylesXml)

        // 6. Sheet 1: Scholarship Dashboard
        val sheet1Xml = buildDashboardSheetXml(student, calculatedCgpa, scholarships, allRequirements)
        writeZipEntry(zip, "xl/worksheets/sheet1.xml", sheet1Xml)

        // 7. Sheet 2: Requirements
        val sheet2Xml = buildRequirementsSheetXml(scholarships, allRequirements)
        writeZipEntry(zip, "xl/worksheets/sheet2.xml", sheet2Xml)

        // 8. Sheet 3: Application Timeline
        val sheet3Xml = buildTimelineSheetXml(scholarships, allTimelineEvents)
        writeZipEntry(zip, "xl/worksheets/sheet3.xml", sheet3Xml)

        // 9. Sheet 4: Summary
        val sheet4Xml = buildSummarySheetXml(student, calculatedCgpa, scholarships, allRequirements)
        writeZipEntry(zip, "xl/worksheets/sheet4.xml", sheet4Xml)

        zip.finish()
    }

    /**
     * Generate Single Scholarship Focused Workbook (.xlsx)
     */
    fun generateSingleScholarshipWorkbook(
        student: StudentProfile,
        calculatedCgpa: Double,
        scholarship: Scholarship,
        requirements: List<ScholarshipRequirement>,
        timelineEvents: List<ScholarshipTimelineEvent>,
        outputStream: OutputStream
    ) {
        generateScholarshipWorkbook(
            student = student,
            calculatedCgpa = calculatedCgpa,
            scholarships = listOf(scholarship),
            allRequirements = requirements,
            allTimelineEvents = timelineEvents,
            outputStream = outputStream
        )
    }

    private fun writeZipEntry(zip: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zip.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zip.write(bytes, 0, bytes.size)
        zip.closeEntry()
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun colLetter(colIdx0: Int): String {
        val a = 'A'.code
        return if (colIdx0 < 26) {
            (a + colIdx0).toChar().toString()
        } else {
            val first = (a + (colIdx0 / 26) - 1).toChar()
            val second = (a + (colIdx0 % 26)).toChar()
            "$first$second"
        }
    }

    private fun buildStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="7">
    <font><name val="Calibri"/><sz val="11"/></font> <!-- 0: Regular -->
    <font><b/><name val="Calibri"/><sz val="11"/></font> <!-- 1: Bold -->
    <font><b/><color rgb="FF1B5E20"/><name val="Calibri"/><sz val="16"/></font> <!-- 2: Title Green 16pt -->
    <font><b/><color rgb="FFFFFFFF"/><name val="Calibri"/><sz val="11"/></font> <!-- 3: Header White 11pt -->
    <font><i/><color rgb="FF64748B"/><name val="Calibri"/><sz val="9"/></font> <!-- 4: Muted 9pt -->
    <font><b/><color rgb="FF1B5E20"/><name val="Calibri"/><sz val="12"/></font> <!-- 5: Section 12pt -->
    <font><u/><color rgb="FF1A73E8"/><name val="Calibri"/><sz val="11"/></font> <!-- 6: Link Blue -->
  </fonts>
  <fills count="6">
    <fill><patternFill patternType="none"/></fill> <!-- 0 -->
    <fill><patternFill patternType="gray125"/></fill> <!-- 1 -->
    <fill><patternFill patternType="solid"><fgColor rgb="FF1B5E20"/></patternFill></fill> <!-- 2: Header Green -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFE8F5E9"/></patternFill></fill> <!-- 3: Light Accent Green -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFF1F5F9"/></patternFill></fill> <!-- 4: Card Gray -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFE2E8F0"/></patternFill></fill> <!-- 5: Border Soft Gray -->
  </fills>
  <borders count="3">
    <border><left/><right/><top/><bottom/><diagonal/></border> <!-- 0: None -->
    <border> <!-- 1: Thin Border -->
      <left style="thin"><color rgb="FFCBD5E1"/></left>
      <right style="thin"><color rgb="FFCBD5E1"/></right>
      <top style="thin"><color rgb="FFCBD5E1"/></top>
      <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
    </border>
    <border> <!-- 2: Total Double Bottom -->
      <left/><right/>
      <top style="thin"><color rgb="FFCBD5E1"/></top>
      <bottom style="double"><color rgb="FF1B5E20"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="11">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/> <!-- 0: Regular -->
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0"/> <!-- 1: Bold -->
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0"/> <!-- 2: App Title -->
    <xf numFmtId="0" fontId="4" fillId="0" borderId="0" xfId="0"/> <!-- 3: Subtitle / Note -->
    <xf numFmtId="0" fontId="5" fillId="0" borderId="0" xfId="0"/> <!-- 4: Section Header -->
    <xf numFmtId="0" fontId="3" fillId="2" borderId="1" applyFont="1" applyFill="1" applyBorder="1" xfId="0"/> <!-- 5: Table Header (Green Fill, White Text) -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" applyBorder="1" xfId="0"/> <!-- 6: Cell with Border -->
    <xf numFmtId="0" fontId="1" fillId="0" borderId="1" applyFont="1" applyBorder="1" xfId="0"/> <!-- 7: Cell Bold with Border -->
    <xf numFmtId="0" fontId="1" fillId="3" borderId="1" applyFont="1" applyFill="1" applyBorder="1" xfId="0"/> <!-- 8: Light Green Accent Cell -->
    <xf numFmtId="0" fontId="0" fillId="4" borderId="1" applyFill="1" applyBorder="1" xfId="0"/> <!-- 9: Card Info Cell -->
    <xf numFmtId="0" fontId="6" fillId="0" borderId="1" applyFont="1" applyBorder="1" xfId="0"/> <!-- 10: Link Style -->
  </cellXfs>
</styleSheet>"""
    }

    /**
     * SHEET 1: Scholarship Dashboard
     */
    private fun buildDashboardSheetXml(
        student: StudentProfile,
        calculatedCgpa: Double,
        scholarships: List<Scholarship>,
        allRequirements: List<ScholarshipRequirement>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView tabSelected="1" workbookViewId="0">
      <pane ySplit="16" topLeftCell="A17" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <cols>
    <col min="1" max="1" width="30" customWidth="1"/> <!-- Name -->
    <col min="2" max="2" width="26" customWidth="1"/> <!-- Organization -->
    <col min="3" max="3" width="15" customWidth="1"/> <!-- Deadline -->
    <col min="4" max="4" width="20" customWidth="1"/> <!-- Days Remaining -->
    <col min="5" max="5" width="18" customWidth="1"/> <!-- Application Status -->
    <col min="6" max="6" width="22" customWidth="1"/> <!-- Requirements Progress -->
    <col min="7" max="7" width="16" customWidth="1"/> <!-- Award Amount -->
    <col min="8" max="8" width="10" customWidth="1"/> <!-- Currency -->
    <col min="9" max="9" width="15" customWidth="1"/> <!-- Date Applied -->
    <col min="10" max="10" width="18" customWidth="1"/> <!-- Expected Feedback -->
    <col min="11" max="11" width="14" customWidth="1"/> <!-- Result -->
    <col min="12" max="12" width="32" customWidth="1"/> <!-- Notes -->
    <col min="13" max="13" width="35" customWidth="1"/> <!-- Application URL -->
  </cols>
  <sheetData>
""")

        // Row 1: Title
        sb.append("""    <row r="1">
      <c r="A1" s="2" t="inlineStr"><is><t>GPA WHIZ - SCHOLARSHIP TRACKER</t></is></c>
    </row>
""")

        // Row 2: Subtitle
        val genDateStr = "Exported: ${sdfDateTime.format(Date())}  |  Completely Offline Academic Record"
        sb.append("""    <row r="2">
      <c r="A2" s="3" t="inlineStr"><is><t>$genDateStr</t></is></c>
    </row>
    <row r="3"/>
""")

        // Row 4: Student Profile Header
        sb.append("""    <row r="4">
      <c r="A4" s="4" t="inlineStr"><is><t>STUDENT ACADEMIC PROFILE</t></is></c>
    </row>
""")

        val cgpaDisplay = if (calculatedCgpa > 0.0) {
            "%.2f / %.1f Scale".format(calculatedCgpa, student.gradingScale)
        } else if (student.targetCgpa > 0.0) {
            "%.2f / %.1f Scale (Target)".format(student.targetCgpa, student.gradingScale)
        } else {
            "0.00 / %.1f Scale".format(student.gradingScale)
        }

        // Student Info Fields
        sb.append("""    <row r="5">
      <c r="A5" s="8" t="inlineStr"><is><t>Student Name:</t></is></c>
      <c r="B5" s="7" t="inlineStr"><is><t>${escapeXml(student.fullName.ifBlank { "Not Specified" })}</t></is></c>
      <c r="C5" s="8" t="inlineStr"><is><t>University / Institution:</t></is></c>
      <c r="D5" s="6" t="inlineStr"><is><t>${escapeXml(student.institution.ifBlank { "Not Specified" })}</t></is></c>
      <c r="E5" s="8" t="inlineStr"><is><t>Current CGPA:</t></is></c>
      <c r="F5" s="7" t="inlineStr"><is><t>$cgpaDisplay</t></is></c>
    </row>
    <row r="6">
      <c r="A6" s="8" t="inlineStr"><is><t>Faculty:</t></is></c>
      <c r="B6" s="6" t="inlineStr"><is><t>${escapeXml(student.faculty.ifBlank { "Not Specified" })}</t></is></c>
      <c r="C6" s="8" t="inlineStr"><is><t>Department / Programme:</t></is></c>
      <c r="D6" s="6" t="inlineStr"><is><t>${escapeXml(student.department.ifBlank { "Not Specified" })}</t></is></c>
      <c r="E6" s="8" t="inlineStr"><is><t>Current Level:</t></is></c>
      <c r="F6" s="6" t="inlineStr"><is><t>${escapeXml(student.currentLevel)}</t></is></c>
    </row>
    <row r="7"/>
""")

        // Row 8: Dashboard Summary KPI Section
        sb.append("""    <row r="8">
      <c r="A8" s="4" t="inlineStr"><is><t>APPLICATION PIPELINE SUMMARY</t></is></c>
    </row>
""")

        val stats = ScholarshipCalculationHelper.calculateScholarshipStats(scholarships, allRequirements)

        sb.append("""    <row r="9">
      <c r="A9" s="8" t="inlineStr"><is><t>Total Tracked Scholarships</t></is></c>
      <c r="B9" s="8" t="inlineStr"><is><t>Active Applications</t></is></c>
      <c r="C9" s="8" t="inlineStr"><is><t>Submitted / In Review</t></is></c>
      <c r="D9" s="8" t="inlineStr"><is><t>Awaiting Feedback</t></is></c>
      <c r="E9" s="8" t="inlineStr"><is><t>Awarded</t></is></c>
      <c r="F9" s="8" t="inlineStr"><is><t>Rejected</t></is></c>
    </row>
    <row r="10">
      <c r="A10" s="7"><v>${scholarships.size}</v></c>
      <c r="B10" s="7"><v>${stats.active}</v></c>
      <c r="C10" s="7"><v>${scholarships.count { it.status == ScholarshipStatus.SUBMITTED }}</v></c>
      <c r="D10" s="7"><v>${stats.awaitingResults}</v></c>
      <c r="E10" s="7"><v>${stats.awarded}</v></c>
      <c r="F10" s="7"><v>${stats.rejected}</v></c>
    </row>
    <row r="11">
      <c r="A11" s="9" t="inlineStr"><is><t>Total Awarded Funding:</t></is></c>
      <c r="B11" s="7" t="inlineStr"><is><t>${if (stats.awardedFunding.isNotEmpty()) stats.awardedFunding.entries.joinToString(", ") { "${it.key}%,.0f".format(it.value) } else "None"}</t></is></c>
      <c r="C11" s="9" t="inlineStr"><is><t>Success Rate:</t></is></c>
      <c r="D11" s="7" t="inlineStr"><is><t>%.1f%%</t></is></c>
      <c r="E11" s="9" t="inlineStr"><is><t>Upcoming Deadlines (30d):</t></is></c>
      <c r="F11" s="7"><v>${stats.upcomingDeadlines}</v></c>
    </row>
    <row r="12"/>
    <row r="13"/>
    <row r="14">
      <c r="A14" s="4" t="inlineStr"><is><t>MAIN SCHOLARSHIP TABLE</t></is></c>
    </row>
    <row r="15">
      <c r="A15" s="3" t="inlineStr"><is><t>Use Excel filter headers to search, sort, and organize applications</t></is></c>
    </row>
""")

        // Row 16: Table Headers (13 Columns)
        val headers = listOf(
            "Scholarship Name", "Organization", "Deadline", "Days Remaining",
            "Application Status", "Requirements Progress", "Award Amount", "Currency",
            "Date Applied", "Expected Feedback", "Result", "Notes", "Application URL"
        )

        sb.append("    <row r=\"16\">\n")
        headers.forEachIndexed { idx, h ->
            val cellRef = "${colLetter(idx)}16"
            sb.append("      <c r=\"$cellRef\" s=\"5\" t=\"inlineStr\"><is><t>${escapeXml(h)}</t></is></c>\n")
        }
        sb.append("    </row>\n")

        // Rows 17+: Scholarship Data
        var rowIdx = 17
        for (s in scholarships) {
            val reqs = allRequirements.filter { it.scholarshipId == s.id }
            val progress = ScholarshipCalculationHelper.getRequirementsProgress(reqs)
            val countdown = ScholarshipCalculationHelper.getDeadlineCountdown(s.deadlineDate)
            val progressStr = "${progress.completed}/${progress.total} (${progress.percentage.toInt()}%)"
            val resultStr = s.outcome ?: if (s.status == ScholarshipStatus.AWARDED) "Awarded" else if (s.status == ScholarshipStatus.REJECTED) "Rejected" else "Pending"

            sb.append("    <row r=\"$rowIdx\">\n")
            // A: Name
            sb.append("      <c r=\"A$rowIdx\" s=\"7\" t=\"inlineStr\"><is><t>${escapeXml(s.name)}</t></is></c>\n")
            // B: Organization
            sb.append("      <c r=\"B$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(s.organization)}</t></is></c>\n")
            // C: Deadline
            sb.append("      <c r=\"C$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(ScholarshipCalculationHelper.formatShortDate(s.deadlineDate))}</t></is></c>\n")
            // D: Days Remaining
            sb.append("      <c r=\"D$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(countdown.label)}</t></is></c>\n")
            // E: Application Status
            sb.append("      <c r=\"E$rowIdx\" s=\"7\" t=\"inlineStr\"><is><t>${escapeXml(s.status)}</t></is></c>\n")
            // F: Requirements Progress
            sb.append("      <c r=\"F$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(progressStr)}</t></is></c>\n")
            // G: Award Amount
            if (s.amount > 0) {
                sb.append("      <c r=\"G$rowIdx\" s=\"6\"><v>${s.amount}</v></c>\n")
            } else {
                sb.append("      <c r=\"G$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>—</t></is></c>\n")
            }
            // H: Currency
            sb.append("      <c r=\"H$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(s.currency)}</t></is></c>\n")
            // I: Date Applied
            sb.append("      <c r=\"I$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(ScholarshipCalculationHelper.formatShortDate(s.dateApplied))}</t></is></c>\n")
            // J: Expected Feedback
            sb.append("      <c r=\"J$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(ScholarshipCalculationHelper.formatShortDate(s.expectedFeedbackDate))}</t></is></c>\n")
            // K: Result
            sb.append("      <c r=\"K$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(resultStr)}</t></is></c>\n")
            // L: Notes
            sb.append("      <c r=\"L$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(s.notes)}</t></is></c>\n")
            // M: Application URL
            if (s.applicationUrl.isNotBlank()) {
                sb.append("      <c r=\"M$rowIdx\" s=\"10\" t=\"inlineStr\"><is><t>${escapeXml(s.applicationUrl)}</t></is></c>\n")
            } else {
                sb.append("      <c r=\"M$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>—</t></is></c>\n")
            }
            sb.append("    </row>\n")
            rowIdx++
        }

        val lastRow = if (rowIdx > 17) rowIdx - 1 else 17
        sb.append("""  </sheetData>
  <autoFilter ref="A16:M$lastRow"/>
</worksheet>""")
        return sb.toString()
    }

    /**
     * SHEET 2: Requirements
     */
    private fun buildRequirementsSheetXml(
        scholarships: List<Scholarship>,
        allRequirements: List<ScholarshipRequirement>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView tabSelected="1" workbookViewId="0">
      <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <cols>
    <col min="1" max="1" width="28" customWidth="1"/> <!-- Scholarship Name -->
    <col min="2" max="2" width="26" customWidth="1"/> <!-- Requirement -->
    <col min="3" max="3" width="18" customWidth="1"/> <!-- Category -->
    <col min="4" max="4" width="16" customWidth="1"/> <!-- Status -->
    <col min="5" max="5" width="30" customWidth="1"/> <!-- Details -->
    <col min="6" max="6" width="15" customWidth="1"/> <!-- Due Date -->
    <col min="7" max="7" width="30" customWidth="1"/> <!-- Notes -->
  </cols>
  <sheetData>
""")

        // Header Row
        val headers = listOf(
            "Scholarship Name", "Requirement", "Category", "Status",
            "Details", "Due Date", "Notes"
        )
        sb.append("    <row r=\"1\">\n")
        headers.forEachIndexed { idx, h ->
            val cellRef = "${colLetter(idx)}1"
            sb.append("      <c r=\"$cellRef\" s=\"5\" t=\"inlineStr\"><is><t>${escapeXml(h)}</t></is></c>\n")
        }
        sb.append("    </row>\n")

        val scholarshipsMap = scholarships.associateBy { it.id }
        var rowIdx = 2

        for (req in allRequirements) {
            val parent = scholarshipsMap[req.scholarshipId]
            val sName = parent?.name ?: "Unknown"

            sb.append("    <row r=\"$rowIdx\">\n")
            sb.append("      <c r=\"A$rowIdx\" s=\"7\" t=\"inlineStr\"><is><t>${escapeXml(sName)}</t></is></c>\n")
            sb.append("      <c r=\"B$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(req.title)}</t></is></c>\n")
            sb.append("      <c r=\"C$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(req.category)}</t></is></c>\n")
            sb.append("      <c r=\"D$rowIdx\" s=\"7\" t=\"inlineStr\"><is><t>${escapeXml(req.status)}</t></is></c>\n")
            sb.append("      <c r=\"E$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(req.details)}</t></is></c>\n")
            sb.append("      <c r=\"F$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(ScholarshipCalculationHelper.formatShortDate(req.deadline))}</t></is></c>\n")
            sb.append("      <c r=\"G$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(req.notes)}</t></is></c>\n")
            sb.append("    </row>\n")
            rowIdx++
        }

        val lastRow = if (rowIdx > 2) rowIdx - 1 else 2
        sb.append("""  </sheetData>
  <autoFilter ref="A1:G$lastRow"/>
</worksheet>""")
        return sb.toString()
    }

    /**
     * SHEET 3: Application Timeline
     */
    private fun buildTimelineSheetXml(
        scholarships: List<Scholarship>,
        allTimelineEvents: List<ScholarshipTimelineEvent>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView tabSelected="1" workbookViewId="0">
      <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <cols>
    <col min="1" max="1" width="28" customWidth="1"/> <!-- Scholarship Name -->
    <col min="2" max="2" width="16" customWidth="1"/> <!-- Date -->
    <col min="3" max="3" width="26" customWidth="1"/> <!-- Event -->
    <col min="4" max="4" width="38" customWidth="1"/> <!-- Description -->
    <col min="5" max="5" width="20" customWidth="1"/> <!-- Notes -->
  </cols>
  <sheetData>
""")

        val headers = listOf(
            "Scholarship Name", "Date", "Event", "Description", "Notes"
        )
        sb.append("    <row r=\"1\">\n")
        headers.forEachIndexed { idx, h ->
            val cellRef = "${colLetter(idx)}1"
            sb.append("      <c r=\"$cellRef\" s=\"5\" t=\"inlineStr\"><is><t>${escapeXml(h)}</t></is></c>\n")
        }
        sb.append("    </row>\n")

        val scholarshipsMap = scholarships.associateBy { it.id }
        var rowIdx = 2

        for (event in allTimelineEvents) {
            val parent = scholarshipsMap[event.scholarshipId]
            val sName = parent?.name ?: "Unknown"
            val eventType = if (event.isAutomatic) "System Milestone" else "User Note"

            sb.append("    <row r=\"$rowIdx\">\n")
            sb.append("      <c r=\"A$rowIdx\" s=\"7\" t=\"inlineStr\"><is><t>${escapeXml(sName)}</t></is></c>\n")
            sb.append("      <c r=\"B$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(ScholarshipCalculationHelper.formatShortDate(event.date))}</t></is></c>\n")
            sb.append("      <c r=\"C$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(event.title)}</t></is></c>\n")
            sb.append("      <c r=\"D$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(event.description)}</t></is></c>\n")
            sb.append("      <c r=\"E$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(eventType)}</t></is></c>\n")
            sb.append("    </row>\n")
            rowIdx++
        }

        val lastRow = if (rowIdx > 2) rowIdx - 1 else 2
        sb.append("""  </sheetData>
  <autoFilter ref="A1:E$lastRow"/>
</worksheet>""")
        return sb.toString()
    }

    /**
     * SHEET 4: Summary Statistics & Status Breakdown
     */
    private fun buildSummarySheetXml(
        student: StudentProfile,
        calculatedCgpa: Double,
        scholarships: List<Scholarship>,
        allRequirements: List<ScholarshipRequirement>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="28" customWidth="1"/>
    <col min="2" max="2" width="18" customWidth="1"/>
    <col min="3" max="3" width="18" customWidth="1"/>
    <col min="4" max="4" width="28" customWidth="1"/>
    <col min="5" max="5" width="22" customWidth="1"/>
  </cols>
  <sheetData>
""")

        // Header
        sb.append("""    <row r="1">
      <c r="A1" s="2" t="inlineStr"><is><t>SCHOLARSHIP SUMMARY &amp; ANALYTICS</t></is></c>
    </row>
    <row r="2">
      <c r="A2" s="3" t="inlineStr"><is><t>Generated: ${sdfDateTime.format(Date())} via GPA Whiz</t></is></c>
    </row>
    <row r="3"/>
    <row r="4">
      <c r="A4" s="4" t="inlineStr"><is><t>STATUS DISTRIBUTION</t></is></c>
    </row>
    <row r="5">
      <c r="A5" s="5" t="inlineStr"><is><t>Application Status</t></is></c>
      <c r="B5" s="5" t="inlineStr"><is><t>Count</t></is></c>
      <c r="C5" s="5" t="inlineStr"><is><t>Percentage</t></is></c>
    </row>
""")

        val totalCount = scholarships.size
        val allStatuses = listOf(
            ScholarshipStatus.NOT_STARTED,
            ScholarshipStatus.PREPARING,
            ScholarshipStatus.IN_PROGRESS,
            ScholarshipStatus.READY_TO_SUBMIT,
            ScholarshipStatus.SUBMITTED,
            ScholarshipStatus.ASSESSMENT,
            ScholarshipStatus.INTERVIEW,
            ScholarshipStatus.AWAITING_RESULT,
            ScholarshipStatus.AWARDED,
            ScholarshipStatus.REJECTED,
            ScholarshipStatus.WITHDRAWN,
            ScholarshipStatus.EXPIRED
        )

        var rowIdx = 6
        for (st in allStatuses) {
            val count = scholarships.count { it.status == st }
            val pct = if (totalCount > 0) (count.toDouble() / totalCount.toDouble()) * 100.0 else 0.0

            sb.append("    <row r=\"$rowIdx\">\n")
            sb.append("      <c r=\"A$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>${escapeXml(st)}</t></is></c>\n")
            sb.append("      <c r=\"B$rowIdx\" s=\"6\"><v>$count</v></c>\n")
            sb.append("      <c r=\"C$rowIdx\" s=\"6\" t=\"inlineStr\"><is><t>%.1f%%</t></is></c>\n")
            sb.append("    </row>\n")
            rowIdx++
        }

        // Total Row
        sb.append("""    <row r="$rowIdx">
      <c r="A$rowIdx" s="8" t="inlineStr"><is><t>TOTAL SCHOLARSHIPS</t></is></c>
      <c r="B$rowIdx" s="8"><v>$totalCount</v></c>
      <c r="C$rowIdx" s="8" t="inlineStr"><is><t>100.0%</t></is></c>
    </row>
""")
        rowIdx += 2

        // Financial Breakdown
        sb.append("""    <row r="$rowIdx">
      <c r="A$rowIdx" s="4" t="inlineStr"><is><t>FUNDING BREAKDOWN BY CURRENCY</t></is></c>
    </row>
""")
        rowIdx++
        sb.append("""    <row r="$rowIdx">
      <c r="A$rowIdx" s="5" t="inlineStr"><is><t>Currency</t></is></c>
      <c r="B$rowIdx" s="5" t="inlineStr"><is><t>Awarded Funding</t></is></c>
      <c r="C$rowIdx" s="5" t="inlineStr"><is><t>Active Pipeline Value</t></is></c>
    </row>
""")
        rowIdx++

        val awardedByCurrency = ScholarshipCalculationHelper.calculateFundingByCurrency(scholarships, awardedOnly = true)
        val pipelineByCurrency = ScholarshipCalculationHelper.calculateFundingByCurrency(scholarships.filter { it.status in ScholarshipStatus.ACTIVE }, awardedOnly = false)
        val allCurrencies = (awardedByCurrency.keys + pipelineByCurrency.keys).distinct().ifEmpty { listOf("₦") }

        for (curr in allCurrencies) {
            val awardedAmt = awardedByCurrency[curr] ?: 0.0
            val pipelineAmt = pipelineByCurrency[curr] ?: 0.0

            sb.append("    <row r=\"$rowIdx\">\n")
            sb.append("      <c r=\"A$rowIdx\" s=\"7\" t=\"inlineStr\"><is><t>${escapeXml(curr)}</t></is></c>\n")
            sb.append("      <c r=\"B$rowIdx\" s=\"6\"><v>$awardedAmt</v></c>\n")
            sb.append("      <c r=\"C$rowIdx\" s=\"6\"><v>$pipelineAmt</v></c>\n")
            sb.append("    </row>\n")
            rowIdx++
        }

        rowIdx++
        val stats = ScholarshipCalculationHelper.calculateScholarshipStats(scholarships, allRequirements)
        sb.append("""    <row r="$rowIdx">
      <c r="A$rowIdx" s="4" t="inlineStr"><is><t>KEY PERFORMANCE INDICATORS</t></is></c>
    </row>
""")
        rowIdx++
        sb.append("""    <row r="$rowIdx">
      <c r="A$rowIdx" s="8" t="inlineStr"><is><t>Success Rate (Awarded / Completed):</t></is></c>
      <c r="B$rowIdx" s="7" t="inlineStr"><is><t>%.1f%%</t></is></c>
    </row>
    <row r="${rowIdx + 1}">
      <c r="A${rowIdx + 1}" s="8" t="inlineStr"><is><t>Active Application Rate:</t></is></c>
      <c r="B${rowIdx + 1}" s="7" t="inlineStr"><is><t>${if (totalCount > 0) "%.1f%%".format((stats.active.toDouble() / totalCount.toDouble()) * 100.0) else "0.0%"}</t></is></c>
    </row>
""")

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }
}
