package com.example.service

import com.example.data.*
import org.json.JSONArray
import org.json.JSONObject

object BackupRestoreHelper {

    fun exportToJson(
        profile: StudentProfile,
        semesters: List<Semester>,
        courses: List<Course>,
        timetable: List<TimetableSlot>,
        scholarships: List<Scholarship> = emptyList(),
        requirements: List<ScholarshipRequirement> = emptyList(),
        timelineEvents: List<ScholarshipTimelineEvent> = emptyList(),
        reminders: List<ScholarshipReminder> = emptyList()
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", 2)
        root.put("exportedAt", System.currentTimeMillis())

        // Profile block
        val profileJson = JSONObject().apply {
            put("fullName", profile.fullName)
            put("institution", profile.institution)
            put("matricNo", profile.matricNo)
            put("faculty", profile.faculty)
            put("department", profile.department)
            put("currentLevel", profile.currentLevel)
            put("graduationYear", profile.graduationYear)
            put("gradingScale", profile.gradingScale)
            put("targetCgpa", profile.targetCgpa)
        }
        root.put("profile", profileJson)

        // Semesters list
        val semestersArray = JSONArray()
        for (semester in semesters) {
            val semJson = JSONObject().apply {
                put("id", semester.id)
                put("name", semester.name)
                put("gradingScale", semester.gradingScale)
                put("rank", semester.rank)
            }
            semestersArray.put(semJson)
        }
        root.put("semesters", semestersArray)

        // Courses list
        val coursesArray = JSONArray()
        for (course in courses) {
            val courseJson = JSONObject().apply {
                put("id", course.id)
                put("semesterId", course.semesterId)
                put("code", course.code)
                put("title", course.title)
                put("units", course.units)
                put("score", course.score)
                put("grade", course.grade)
                put("isCarryOver", course.isCarryOver)
            }
            coursesArray.put(courseJson)
        }
        root.put("courses", coursesArray)

        // Timetable list
        val timetableArray = JSONArray()
        for (slot in timetable) {
            val slotJson = JSONObject().apply {
                put("id", slot.id)
                put("courseCode", slot.courseCode)
                put("venue", slot.venue)
                put("dayOfWeek", slot.dayOfWeek)
                put("startTime", slot.startTime)
                put("endTime", slot.endTime)
                put("alertEnabled", slot.alertEnabled)
            }
            timetableArray.put(slotJson)
        }
        root.put("timetable", timetableArray)

        // Scholarships list
        val scholarshipsArray = JSONArray()
        for (s in scholarships) {
            val sJson = JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("organization", s.organization)
                put("description", s.description)
                put("amount", s.amount)
                put("currency", s.currency)
                put("applicationUrl", s.applicationUrl)
                put("organizationWebsite", s.organizationWebsite)
                put("contactEmail", s.contactEmail)
                put("notes", s.notes)
                if (s.openingDate != null) put("openingDate", s.openingDate)
                if (s.deadlineDate != null) put("deadlineDate", s.deadlineDate)
                if (s.expectedFeedbackDate != null) put("expectedFeedbackDate", s.expectedFeedbackDate)
                if (s.testDate != null) put("testDate", s.testDate)
                if (s.interviewDate != null) put("interviewDate", s.interviewDate)
                if (s.followUpDate != null) put("followUpDate", s.followUpDate)
                put("status", s.status)
                if (s.minCgpa != null) put("minCgpa", s.minCgpa)
                put("minScale", s.minScale)
                if (s.outcome != null) put("outcome", s.outcome)
                if (s.awardAmount != null) put("awardAmount", s.awardAmount)
                if (s.awardCurrency != null) put("awardCurrency", s.awardCurrency)
                if (s.awardDate != null) put("awardDate", s.awardDate)
                if (s.awardNotes != null) put("awardNotes", s.awardNotes)
                put("dateAdded", s.dateAdded)
                if (s.dateApplied != null) put("dateApplied", s.dateApplied)
            }
            scholarshipsArray.put(sJson)
        }
        root.put("scholarships", scholarshipsArray)

        // Requirements list
        val reqsArray = JSONArray()
        for (req in requirements) {
            val reqJson = JSONObject().apply {
                put("id", req.id)
                put("scholarshipId", req.scholarshipId)
                put("title", req.title)
                put("category", req.category)
                put("status", req.status)
                put("details", req.details)
                if (req.deadline != null) put("deadline", req.deadline)
                put("notes", req.notes)
            }
            reqsArray.put(reqJson)
        }
        root.put("requirements", reqsArray)

        // Timeline events list
        val timelineArray = JSONArray()
        for (event in timelineEvents) {
            val tJson = JSONObject().apply {
                put("id", event.id)
                put("scholarshipId", event.scholarshipId)
                put("date", event.date)
                put("title", event.title)
                put("description", event.description)
                put("isAutomatic", event.isAutomatic)
            }
            timelineArray.put(tJson)
        }
        root.put("timelineEvents", timelineArray)

        // Reminders list
        val remindersArray = JSONArray()
        for (reminder in reminders) {
            val rJson = JSONObject().apply {
                put("id", reminder.id)
                put("scholarshipId", reminder.scholarshipId)
                put("eventType", reminder.eventType)
                put("reminderTime", reminder.reminderTime)
                put("offsetDays", reminder.offsetDays)
                put("isEnabled", reminder.isEnabled)
                put("notes", reminder.notes)
            }
            remindersArray.put(rJson)
        }
        root.put("reminders", remindersArray)

        return root.toString(4)
    }

    fun parseBackupJson(jsonString: String): BackupData {
        val root = JSONObject(jsonString)

        if (!root.has("schemaVersion")) {
            throw IllegalArgumentException("Invalid backup JSON schema: missing schema version indicator")
        }

        // Parse Profile
        val profJson = root.getJSONObject("profile")
        val profile = StudentProfile(
            id = 1,
            fullName = profJson.optString("fullName", ""),
            institution = profJson.optString("institution", ""),
            matricNo = profJson.optString("matricNo", ""),
            faculty = profJson.optString("faculty", ""),
            department = profJson.optString("department", ""),
            currentLevel = profJson.optString("currentLevel", "100L"),
            graduationYear = profJson.optString("graduationYear", ""),
            gradingScale = profJson.optDouble("gradingScale", 5.0),
            targetCgpa = profJson.optDouble("targetCgpa", 4.5)
        )

        // Parse Semesters
        val semArray = root.getJSONArray("semesters")
        val semestersList = mutableListOf<Semester>()
        for (i in 0 until semArray.length()) {
            val item = semArray.getJSONObject(i)
            semestersList.add(
                Semester(
                    id = item.optInt("id", 0),
                    name = item.optString("name", "Semester $i"),
                    gradingScale = item.optDouble("gradingScale", 5.0),
                    rank = item.optInt("rank", 0)
                )
            )
        }

        // Parse Courses
        val coursesArray = root.getJSONArray("courses")
        val coursesList = mutableListOf<Course>()
        for (i in 0 until coursesArray.length()) {
            val item = coursesArray.getJSONObject(i)
            coursesList.add(
                Course(
                    id = item.optInt("id", 0),
                    semesterId = item.optInt("semesterId", 0),
                    code = item.optString("code", ""),
                    title = item.optString("title", ""),
                    units = item.optInt("units", 1),
                    score = item.optInt("score", 0),
                    grade = item.optString("grade", "F"),
                    isCarryOver = item.optBoolean("isCarryOver", false)
                )
            )
        }

        // Parse Timetable
        val timeArray = root.getJSONArray("timetable")
        val timetableList = mutableListOf<TimetableSlot>()
        for (i in 0 until timeArray.length()) {
            val item = timeArray.getJSONObject(i)
            timetableList.add(
                TimetableSlot(
                    id = item.optInt("id", 0),
                    courseCode = item.optString("courseCode", ""),
                    venue = item.optString("venue", ""),
                    dayOfWeek = item.optInt("dayOfWeek", 1),
                    startTime = item.optString("startTime", "08:00"),
                    endTime = item.optString("endTime", "10:00"),
                    alertEnabled = item.optBoolean("alertEnabled", true)
                )
            )
        }

        // Parse Scholarships (if present, backward compatible)
        val scholarshipsList = mutableListOf<Scholarship>()
        if (root.has("scholarships")) {
            val sArray = root.getJSONArray("scholarships")
            for (i in 0 until sArray.length()) {
                val item = sArray.getJSONObject(i)
                scholarshipsList.add(
                    Scholarship(
                        id = item.optInt("id", 0),
                        name = item.optString("name", ""),
                        organization = item.optString("organization", ""),
                        description = item.optString("description", ""),
                        amount = item.optDouble("amount", 0.0),
                        currency = item.optString("currency", "₦"),
                        applicationUrl = item.optString("applicationUrl", ""),
                        organizationWebsite = item.optString("organizationWebsite", ""),
                        contactEmail = item.optString("contactEmail", ""),
                        notes = item.optString("notes", ""),
                        openingDate = if (item.has("openingDate")) item.optLong("openingDate") else null,
                        deadlineDate = if (item.has("deadlineDate")) item.optLong("deadlineDate") else null,
                        expectedFeedbackDate = if (item.has("expectedFeedbackDate")) item.optLong("expectedFeedbackDate") else null,
                        testDate = if (item.has("testDate")) item.optLong("testDate") else null,
                        interviewDate = if (item.has("interviewDate")) item.optLong("interviewDate") else null,
                        followUpDate = if (item.has("followUpDate")) item.optLong("followUpDate") else null,
                        status = item.optString("status", ScholarshipStatus.NOT_STARTED),
                        minCgpa = if (item.has("minCgpa")) item.optDouble("minCgpa") else null,
                        minScale = item.optDouble("minScale", 5.0),
                        outcome = if (item.has("outcome")) item.optString("outcome") else null,
                        awardAmount = if (item.has("awardAmount")) item.optDouble("awardAmount") else null,
                        awardCurrency = if (item.has("awardCurrency")) item.optString("awardCurrency") else null,
                        awardDate = if (item.has("awardDate")) item.optLong("awardDate") else null,
                        awardNotes = if (item.has("awardNotes")) item.optString("awardNotes") else null,
                        dateAdded = item.optLong("dateAdded", System.currentTimeMillis()),
                        dateApplied = if (item.has("dateApplied")) item.optLong("dateApplied") else null
                    )
                )
            }
        }

        // Parse Requirements
        val requirementsList = mutableListOf<ScholarshipRequirement>()
        if (root.has("requirements")) {
            val reqArray = root.getJSONArray("requirements")
            for (i in 0 until reqArray.length()) {
                val item = reqArray.getJSONObject(i)
                requirementsList.add(
                    ScholarshipRequirement(
                        id = item.optInt("id", 0),
                        scholarshipId = item.optInt("scholarshipId", 0),
                        title = item.optString("title", ""),
                        category = item.optString("category", RequirementCategory.OTHER),
                        status = item.optString("status", RequirementStatus.NOT_STARTED),
                        details = item.optString("details", ""),
                        deadline = if (item.has("deadline")) item.optLong("deadline") else null,
                        notes = item.optString("notes", "")
                    )
                )
            }
        }

        // Parse Timeline Events
        val timelineList = mutableListOf<ScholarshipTimelineEvent>()
        if (root.has("timelineEvents")) {
            val tArray = root.getJSONArray("timelineEvents")
            for (i in 0 until tArray.length()) {
                val item = tArray.getJSONObject(i)
                timelineList.add(
                    ScholarshipTimelineEvent(
                        id = item.optInt("id", 0),
                        scholarshipId = item.optInt("scholarshipId", 0),
                        date = item.optLong("date", System.currentTimeMillis()),
                        title = item.optString("title", ""),
                        description = item.optString("description", ""),
                        isAutomatic = item.optBoolean("isAutomatic", true)
                    )
                )
            }
        }

        // Parse Reminders
        val remindersList = mutableListOf<ScholarshipReminder>()
        if (root.has("reminders")) {
            val rArray = root.getJSONArray("reminders")
            for (i in 0 until rArray.length()) {
                val item = rArray.getJSONObject(i)
                remindersList.add(
                    ScholarshipReminder(
                        id = item.optInt("id", 0),
                        scholarshipId = item.optInt("scholarshipId", 0),
                        eventType = item.optString("eventType", "Deadline"),
                        reminderTime = item.optLong("reminderTime", System.currentTimeMillis()),
                        offsetDays = item.optInt("offsetDays", 0),
                        isEnabled = item.optBoolean("isEnabled", true),
                        notes = item.optString("notes", "")
                    )
                )
            }
        }

        return BackupData(
            profile = profile,
            semesters = semestersList,
            courses = coursesList,
            timetable = timetableList,
            scholarships = scholarshipsList,
            requirements = requirementsList,
            timelineEvents = timelineList,
            reminders = remindersList
        )
    }
}

data class BackupData(
    val profile: StudentProfile,
    val semesters: List<Semester>,
    val courses: List<Course>,
    val timetable: List<TimetableSlot>,
    val scholarships: List<Scholarship> = emptyList(),
    val requirements: List<ScholarshipRequirement> = emptyList(),
    val timelineEvents: List<ScholarshipTimelineEvent> = emptyList(),
    val reminders: List<ScholarshipReminder> = emptyList()
)
