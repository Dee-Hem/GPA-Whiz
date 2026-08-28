package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.OutputStream

class GpaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val gpaDao = db.gpaDao()
    val scholarshipDao = db.scholarshipDao()

    // Flowing States
    val studentProfile: StateFlow<StudentProfile> = gpaDao.getStudentProfile()
        .map { it ?: StudentProfile() }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StudentProfile()
        )

    val semesters: StateFlow<List<Semester>> = gpaDao.getAllSemesters()
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val courses: StateFlow<List<Course>> = gpaDao.getAllCourses()
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val timetableSlots: StateFlow<List<TimetableSlot>> = gpaDao.getAllTimetableSlots()
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Outstanding Carry-over courses flow
    val carryOverCourses: StateFlow<List<Course>> = gpaDao.getAllCourses()
        .combine(gpaDao.getStudentProfile()) { allCourses, profile ->
            val scale = profile?.gradingScale ?: 5.0
            allCourses.filter { it.grade.uppercase() == "F" || it.isCarryOver }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // SCHOLARSHIP FLOWS
    val scholarships: StateFlow<List<Scholarship>> = scholarshipDao.getAllScholarships()
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val scholarshipRequirements: StateFlow<List<ScholarshipRequirement>> = scholarshipDao.getAllRequirements()
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val scholarshipTimelineEvents: StateFlow<List<ScholarshipTimelineEvent>> = scholarshipDao.getAllTimelineEvents()
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val scholarshipReminders: StateFlow<List<ScholarshipReminder>> = scholarshipDao.getAllReminders()
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI Status Helpers
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage = _uiMessage.asStateFlow()

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    // STUDENT PROFILE CRUD
    fun updateProfile(
        name: String,
        institution: String,
        matric: String,
        faculty: String,
        dept: String,
        level: String,
        gradYear: String,
        scale: Double,
        targetCgpa: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = StudentProfile(
                id = 1,
                fullName = name,
                institution = institution,
                matricNo = matric,
                faculty = faculty,
                department = dept,
                currentLevel = level,
                graduationYear = gradYear,
                gradingScale = scale,
                targetCgpa = targetCgpa
            )
            gpaDao.insertStudentProfile(updated)
            _uiMessage.value = "Student profile saved."
        }
    }

    // SEMESTERS CRUD
    fun addSemester(name: String, scale: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val semSize = semesters.value.size
            val mSemester = Semester(name = name, gradingScale = scale, rank = semSize)
            gpaDao.insertSemester(mSemester)
            _uiMessage.value = "Semester added successfully."
        }
    }

    fun deleteSemester(semester: Semester) {
        viewModelScope.launch(Dispatchers.IO) {
            gpaDao.deleteCoursesForSemester(semester.id)
            gpaDao.deleteSemester(semester)
            _uiMessage.value = "Semester and its courses deleted."
        }
    }

    // COURSES CRUD
    fun addCourse(
        semesterId: Int,
        code: String,
        title: String,
        units: Int,
        score: Int,
        grade: String,
        isCarryOver: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val course = Course(
                semesterId = semesterId,
                code = code.uppercase(),
                title = title,
                units = units,
                score = score,
                grade = grade,
                isCarryOver = isCarryOver || grade.uppercase() == "F"
            )
            gpaDao.insertCourse(course)
            _uiMessage.value = "Course ${code.uppercase()} added."
        }
    }

    fun updateCourse(
        courseId: Int,
        semesterId: Int,
        code: String,
        title: String,
        units: Int,
        score: Int,
        grade: String,
        isCarryOver: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = Course(
                id = courseId,
                semesterId = semesterId,
                code = code.uppercase(),
                title = title,
                units = units,
                score = score,
                grade = grade,
                isCarryOver = isCarryOver || grade.uppercase() == "F"
            )
            gpaDao.insertCourse(updated)
            _uiMessage.value = "Course updated."
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch(Dispatchers.IO) {
            gpaDao.deleteCourse(course)
            _uiMessage.value = "Course deleted."
        }
    }

    // TIMETABLE CRUD
    fun addTimetableSlot(
        context: Context,
        courseCode: String,
        venue: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val slot = TimetableSlot(
                courseCode = courseCode.uppercase(),
                venue = venue,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                alertEnabled = true
            )
            val newId = gpaDao.insertTimetableSlot(slot)
            val savedSlot = slot.copy(id = newId.toInt())
            
            launch(Dispatchers.Main) {
                AlarmScheduler.scheduleAlarm(context, savedSlot)
            }
            _uiMessage.value = "Lecture schedule saved."
        }
    }

    fun deleteTimetableSlot(context: Context, slot: TimetableSlot) {
        viewModelScope.launch(Dispatchers.IO) {
            launch(Dispatchers.Main) {
                AlarmScheduler.cancelAlarm(context, slot)
            }
            gpaDao.deleteTimetableSlot(slot)
            _uiMessage.value = "Lecture schedule cleared."
        }
    }

    fun toggleAlertForSlot(context: Context, slot: TimetableSlot) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = slot.copy(alertEnabled = !slot.alertEnabled)
            gpaDao.updateTimetableSlot(updated)
            
            launch(Dispatchers.Main) {
                if (updated.alertEnabled) {
                    AlarmScheduler.scheduleAlarm(context, updated)
                } else {
                    AlarmScheduler.cancelAlarm(context, updated)
                }
            }
        }
    }

    // SCHOLARSHIPS CRUD
    fun addScholarship(
        name: String,
        organization: String,
        description: String,
        amount: Double,
        currency: String,
        applicationUrl: String,
        organizationWebsite: String,
        contactEmail: String,
        notes: String,
        openingDate: Long?,
        deadlineDate: Long?,
        expectedFeedbackDate: Long?,
        testDate: Long?,
        interviewDate: Long?,
        followUpDate: Long?,
        status: String,
        minCgpa: Double?,
        minScale: Double = 5.0,
        initialRequirements: List<PredefinedRequirement> = emptyList()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val scholarship = Scholarship(
                name = name.trim(),
                organization = organization.trim(),
                description = description.trim(),
                amount = amount,
                currency = currency.ifBlank { "₦" },
                applicationUrl = applicationUrl.trim(),
                organizationWebsite = organizationWebsite.trim(),
                contactEmail = contactEmail.trim(),
                notes = notes.trim(),
                openingDate = openingDate,
                deadlineDate = deadlineDate,
                expectedFeedbackDate = expectedFeedbackDate,
                testDate = testDate,
                interviewDate = interviewDate,
                followUpDate = followUpDate,
                status = status,
                minCgpa = minCgpa,
                minScale = minScale,
                dateAdded = System.currentTimeMillis()
            )
            val newId = scholarshipDao.insertScholarship(scholarship).toInt()

            // Auto-create initial timeline event
            scholarshipDao.insertTimelineEvent(
                ScholarshipTimelineEvent(
                    scholarshipId = newId,
                    date = System.currentTimeMillis(),
                    title = "Scholarship Added",
                    description = "Initial record drafted with status: $status",
                    isAutomatic = true
                )
            )

            // Add any selected initial requirements
            for (req in initialRequirements) {
                scholarshipDao.insertRequirement(
                    ScholarshipRequirement(
                        scholarshipId = newId,
                        title = req.title,
                        category = req.category,
                        status = RequirementStatus.NOT_STARTED,
                        details = req.defaultDetails
                    )
                )
            }

            _uiMessage.value = "Scholarship '${name.trim()}' saved."
        }
    }

    fun updateScholarship(scholarship: Scholarship) {
        viewModelScope.launch(Dispatchers.IO) {
            scholarshipDao.updateScholarship(scholarship)
            _uiMessage.value = "Scholarship details updated."
        }
    }

    fun updateScholarshipStatus(scholarship: Scholarship, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (scholarship.status != newStatus) {
                val updated = scholarship.copy(
                    status = newStatus,
                    dateApplied = if (newStatus == ScholarshipStatus.SUBMITTED && scholarship.dateApplied == null) System.currentTimeMillis() else scholarship.dateApplied
                )
                scholarshipDao.updateScholarship(updated)
                scholarshipDao.insertTimelineEvent(
                    ScholarshipTimelineEvent(
                        scholarshipId = scholarship.id,
                        date = System.currentTimeMillis(),
                        title = "Status Changed",
                        description = "Status updated from '${scholarship.status}' to '$newStatus'",
                        isAutomatic = true
                    )
                )
                _uiMessage.value = "Status updated to '$newStatus'."
            }
        }
    }

    fun recordScholarshipOutcome(
        scholarship: Scholarship,
        outcome: String,
        awardAmount: Double?,
        awardCurrency: String?,
        awardDate: Long?,
        awardNotes: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalStatus = when (outcome.lowercase()) {
                "awarded" -> ScholarshipStatus.AWARDED
                "rejected" -> ScholarshipStatus.REJECTED
                "withdrawn" -> ScholarshipStatus.WITHDRAWN
                else -> scholarship.status
            }
            val updated = scholarship.copy(
                status = finalStatus,
                outcome = outcome,
                awardAmount = awardAmount,
                awardCurrency = awardCurrency ?: scholarship.currency,
                awardDate = awardDate ?: System.currentTimeMillis(),
                awardNotes = awardNotes
            )
            scholarshipDao.updateScholarship(updated)
            scholarshipDao.insertTimelineEvent(
                ScholarshipTimelineEvent(
                    scholarshipId = scholarship.id,
                    date = awardDate ?: System.currentTimeMillis(),
                    title = "Outcome Recorded: $outcome",
                    description = if (outcome.equals("Awarded", ignoreCase = true) && awardAmount != null) {
                        "Awarded funding of ${awardCurrency ?: scholarship.currency}%,.0f. ${awardNotes ?: ""}".trim()
                    } else {
                        awardNotes ?: "Outcome recorded as $outcome"
                    },
                    isAutomatic = true
                )
            )
            _uiMessage.value = "Outcome '$outcome' recorded."
        }
    }

    fun deleteScholarship(scholarship: Scholarship) {
        viewModelScope.launch(Dispatchers.IO) {
            scholarshipDao.deleteRequirementsForScholarship(scholarship.id)
            scholarshipDao.deleteTimelineEventsForScholarship(scholarship.id)
            scholarshipDao.deleteRemindersForScholarship(scholarship.id)
            scholarshipDao.deleteScholarship(scholarship)
            _uiMessage.value = "Scholarship removed."
        }
    }

    // REQUIREMENTS CRUD
    fun addRequirement(
        scholarshipId: Int,
        title: String,
        category: String,
        details: String = "",
        deadline: Long? = null,
        notes: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val req = ScholarshipRequirement(
                scholarshipId = scholarshipId,
                title = title.trim(),
                category = category,
                status = RequirementStatus.NOT_STARTED,
                details = details.trim(),
                deadline = deadline,
                notes = notes.trim()
            )
            scholarshipDao.insertRequirement(req)
            scholarshipDao.insertTimelineEvent(
                ScholarshipTimelineEvent(
                    scholarshipId = scholarshipId,
                    date = System.currentTimeMillis(),
                    title = "Requirement Added",
                    description = "Added requirement '${title.trim()}' ($category)",
                    isAutomatic = true
                )
            )
            _uiMessage.value = "Requirement '${title.trim()}' added."
        }
    }

    fun updateRequirement(requirement: ScholarshipRequirement) {
        viewModelScope.launch(Dispatchers.IO) {
            scholarshipDao.updateRequirement(requirement)
            _uiMessage.value = "Requirement updated."
        }
    }

    fun toggleRequirementStatus(requirement: ScholarshipRequirement) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextStatus = when (requirement.status) {
                RequirementStatus.NOT_STARTED -> RequirementStatus.IN_PROGRESS
                RequirementStatus.IN_PROGRESS -> RequirementStatus.COMPLETED
                RequirementStatus.COMPLETED -> RequirementStatus.NOT_STARTED
                RequirementStatus.SUBMITTED -> RequirementStatus.COMPLETED
                else -> RequirementStatus.NOT_STARTED
            }
            val updated = requirement.copy(status = nextStatus)
            scholarshipDao.updateRequirement(updated)
            if (nextStatus == RequirementStatus.COMPLETED) {
                scholarshipDao.insertTimelineEvent(
                    ScholarshipTimelineEvent(
                        scholarshipId = requirement.scholarshipId,
                        date = System.currentTimeMillis(),
                        title = "Requirement Completed",
                        description = "Completed requirement '${requirement.title}'",
                        isAutomatic = true
                    )
                )
            }
            _uiMessage.value = "${requirement.title}: $nextStatus"
        }
    }

    fun deleteRequirement(requirement: ScholarshipRequirement) {
        viewModelScope.launch(Dispatchers.IO) {
            scholarshipDao.deleteRequirement(requirement)
            _uiMessage.value = "Requirement removed."
        }
    }

    // TIMELINE EVENTS CRUD
    fun addManualTimelineEvent(scholarshipId: Int, title: String, description: String, date: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val event = ScholarshipTimelineEvent(
                scholarshipId = scholarshipId,
                date = date,
                title = title.trim(),
                description = description.trim(),
                isAutomatic = false
            )
            scholarshipDao.insertTimelineEvent(event)
            _uiMessage.value = "Timeline note recorded."
        }
    }

    fun deleteTimelineEvent(event: ScholarshipTimelineEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            scholarshipDao.deleteTimelineEvent(event)
            _uiMessage.value = "Timeline entry removed."
        }
    }

    // REMINDERS
    fun addScholarshipReminder(
        scholarshipId: Int,
        eventType: String,
        reminderTime: Long,
        offsetDays: Int,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val reminder = ScholarshipReminder(
                scholarshipId = scholarshipId,
                eventType = eventType,
                reminderTime = reminderTime,
                offsetDays = offsetDays,
                isEnabled = true,
                notes = notes
            )
            scholarshipDao.insertReminder(reminder)
            _uiMessage.value = "Reminder scheduled."
        }
    }

    fun toggleScholarshipReminder(reminder: ScholarshipReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = reminder.copy(isEnabled = !reminder.isEnabled)
            scholarshipDao.updateReminder(updated)
        }
    }

    fun deleteScholarshipReminder(reminder: ScholarshipReminder) {
        viewModelScope.launch(Dispatchers.IO) {
            scholarshipDao.deleteReminder(reminder)
            _uiMessage.value = "Reminder removed."
        }
    }

    // PDF EXPORTS
    fun exportTranscriptPdf(outputStream: OutputStream) {
        try {
            OfficialTranscriptPdfGenerator.generateTranscriptPdf(
                student = studentProfile.value,
                semesters = semesters.value,
                allCourses = courses.value,
                outputStream = outputStream
            )
            _uiMessage.value = "Transcript PDF compiled successfully."
        } catch (e: Exception) {
            _uiMessage.value = "PDF Export failed: ${e.message}"
        }
    }

    fun exportScholarshipsPdf(outputStream: OutputStream, activeOnly: Boolean = false) {
        try {
            val list = if (activeOnly) {
                scholarships.value.filter { it.status in ScholarshipStatus.ACTIVE }
            } else {
                scholarships.value
            }
            val cgpa = GpaCalcService.calculateCgpa(semesters.value, courses.value)
            ScholarshipPdfGenerator.generateScholarshipReportPdf(
                student = studentProfile.value,
                calculatedCgpa = cgpa,
                scholarships = list,
                allRequirements = scholarshipRequirements.value,
                outputStream = outputStream,
                reportTitle = if (activeOnly) "ACTIVE SCHOLARSHIP APPLICATIONS REPORT" else "COMPLETE SCHOLARSHIP TRACKER REPORT"
            )
            _uiMessage.value = "Scholarship PDF report generated."
        } catch (e: Exception) {
            _uiMessage.value = "Scholarship PDF Export failed: ${e.message}"
        }
    }

    fun exportSingleScholarshipPdf(scholarship: Scholarship, outputStream: OutputStream) {
        try {
            val reqs = scholarshipRequirements.value.filter { it.scholarshipId == scholarship.id }
            val events = scholarshipTimelineEvents.value.filter { it.scholarshipId == scholarship.id }
            val cgpa = GpaCalcService.calculateCgpa(semesters.value, courses.value)
            ScholarshipPdfGenerator.generateSingleScholarshipDossierPdf(
                student = studentProfile.value,
                calculatedCgpa = cgpa,
                scholarship = scholarship,
                requirements = reqs,
                timelineEvents = events,
                outputStream = outputStream
            )
            _uiMessage.value = "Dossier PDF exported."
        } catch (e: Exception) {
            _uiMessage.value = "Dossier PDF Export failed: ${e.message}"
        }
    }

    fun exportScholarshipsXlsx(outputStream: OutputStream) {
        try {
            val cgpa = GpaCalcService.calculateCgpa(semesters.value, courses.value)
            ScholarshipXlsxGenerator.generateScholarshipWorkbook(
                student = studentProfile.value,
                calculatedCgpa = cgpa,
                scholarships = scholarships.value,
                allRequirements = scholarshipRequirements.value,
                allTimelineEvents = scholarshipTimelineEvents.value,
                outputStream = outputStream
            )
            _uiMessage.value = "Scholarship Excel workbook (.xlsx) exported."
        } catch (e: Exception) {
            _uiMessage.value = "Excel Export failed: ${e.message}"
        }
    }

    fun exportSingleScholarshipXlsx(scholarship: Scholarship, outputStream: OutputStream) {
        try {
            val reqs = scholarshipRequirements.value.filter { it.scholarshipId == scholarship.id }
            val events = scholarshipTimelineEvents.value.filter { it.scholarshipId == scholarship.id }
            val cgpa = GpaCalcService.calculateCgpa(semesters.value, courses.value)
            ScholarshipXlsxGenerator.generateSingleScholarshipWorkbook(
                student = studentProfile.value,
                calculatedCgpa = cgpa,
                scholarship = scholarship,
                requirements = reqs,
                timelineEvents = events,
                outputStream = outputStream
            )
            _uiMessage.value = "Scholarship Excel (.xlsx) exported."
        } catch (e: Exception) {
            _uiMessage.value = "Excel Export failed: ${e.message}"
        }
    }

    // DATA PORTABILITY EXPORT
    fun exportBackup(): String {
        return BackupRestoreHelper.exportToJson(
            profile = studentProfile.value,
            semesters = semesters.value,
            courses = courses.value,
            timetable = timetableSlots.value,
            scholarships = scholarships.value,
            requirements = scholarshipRequirements.value,
            timelineEvents = scholarshipTimelineEvents.value,
            reminders = scholarshipReminders.value
        )
    }

    // DATA PORTABILITY IMPORT
    fun restoreBackup(context: Context, jsonString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = BackupRestoreHelper.parseBackupJson(jsonString)
                
                // Nuclear reset tables
                gpaDao.clearAllSemesters()
                gpaDao.clearAllCourses()
                gpaDao.clearAllTimetableSlots()
                scholarshipDao.clearAllScholarships()
                scholarshipDao.clearAllRequirements()
                scholarshipDao.clearAllTimelineEvents()
                scholarshipDao.clearAllReminders()

                // Insert elements
                gpaDao.insertStudentProfile(data.profile)
                for (sem in data.semesters) {
                    gpaDao.insertSemester(sem)
                }
                for (course in data.courses) {
                    gpaDao.insertCourse(course)
                }
                for (slot in data.timetable) {
                    gpaDao.insertTimetableSlot(slot)
                }

                // Insert scholarships & related entities
                for (s in data.scholarships) {
                    scholarshipDao.insertScholarship(s)
                }
                for (req in data.requirements) {
                    scholarshipDao.insertRequirement(req)
                }
                for (t in data.timelineEvents) {
                    scholarshipDao.insertTimelineEvent(t)
                }
                for (r in data.reminders) {
                    scholarshipDao.insertReminder(r)
                }

                // Reschedule all exact timetable alarms
                launch(Dispatchers.Main) {
                    AlarmScheduler.rescheduleAll(context, data.timetable)
                }
                _uiMessage.value = "Database backup restored successfully."
            } catch (e: Exception) {
                _uiMessage.value = "Validation Failed: Corrupted schema! ${e.message}"
            }
        }
    }
}
