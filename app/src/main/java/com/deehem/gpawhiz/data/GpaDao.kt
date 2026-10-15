package com.deehem.gpawhiz.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GpaDao {
    // Student Profile
    @Query("SELECT * FROM student_profile WHERE id = 1 LIMIT 1")
    fun getStudentProfile(): Flow<StudentProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentProfile(profile: StudentProfile)

    // Semesters
    @Query("SELECT * FROM semesters ORDER BY rank ASC, id ASC")
    fun getAllSemesters(): Flow<List<Semester>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: Semester): Long

    @Update
    suspend fun updateSemester(semester: Semester)

    @Delete
    suspend fun deleteSemester(semester: Semester)

    @Query("DELETE FROM semesters")
    suspend fun clearAllSemesters()

    @Query("SELECT * FROM academic_periods WHERE semester_id = :semesterId ORDER BY sort_order ASC")
    fun getAcademicPeriodsForSemester(semesterId: Int): Flow<List<AcademicPeriod>>

    @Query("SELECT * FROM academic_periods ORDER BY semester_id ASC, sort_order ASC")
    suspend fun getAcademicPeriodsDirect(): List<AcademicPeriod>

    @Query("SELECT * FROM academic_periods ORDER BY semester_id ASC, sort_order ASC")
    fun getAllAcademicPeriods(): Flow<List<AcademicPeriod>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicPeriod(period: AcademicPeriod): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicPeriods(periods: List<AcademicPeriod>)

    @Update
    suspend fun updateAcademicPeriod(period: AcademicPeriod)

    @Query("DELETE FROM academic_periods WHERE semester_id = :semesterId")
    suspend fun deleteAcademicPeriodsForSemester(semesterId: Int)

    @Query("DELETE FROM academic_periods")
    suspend fun clearAllAcademicPeriods()

    // Courses
    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId")
    fun getCoursesForSemester(semesterId: Int): Flow<List<Course>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Query("DELETE FROM courses WHERE semesterId = :semesterId")
    suspend fun deleteCoursesForSemester(semesterId: Int)

    @Query("DELETE FROM courses")
    suspend fun clearAllCourses()

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun getCourseCount(): Int

    // Timetable Slots
    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllTimetableSlots(): Flow<List<TimetableSlot>>

    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getTimetableSlotsDirect(): List<TimetableSlot>

    @Query("SELECT * FROM timetable_slots WHERE id = :id LIMIT 1")
    suspend fun getTimetableSlotById(id: Int): TimetableSlot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlot(slot: TimetableSlot): Long

    @Update
    suspend fun updateTimetableSlot(slot: TimetableSlot)

    @Delete
    suspend fun deleteTimetableSlot(slot: TimetableSlot)

    @Query("DELETE FROM timetable_slots")
    suspend fun clearAllTimetableSlots()

    // Study Sessions
    @Query("SELECT * FROM study_sessions ORDER BY date ASC, startTime ASC")
    fun getAllStudySessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions ORDER BY date ASC, startTime ASC")
    suspend fun getStudySessionsDirect(): List<StudySession>

    @Query("SELECT * FROM study_sessions WHERE id = :id LIMIT 1")
    suspend fun getStudySessionById(id: Int): StudySession?

    @Query("SELECT * FROM study_sessions WHERE series_id = :seriesId ORDER BY date ASC, startTime ASC")
    suspend fun getStudySessionsBySeriesId(seriesId: String): List<StudySession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySessions(sessions: List<StudySession>): List<Long>

    @Update
    suspend fun updateStudySession(session: StudySession)

    @Update
    suspend fun updateStudySessions(sessions: List<StudySession>)

    @Delete
    suspend fun deleteStudySession(session: StudySession)

    @Delete
    suspend fun deleteStudySessions(sessions: List<StudySession>)

    @Query("DELETE FROM study_sessions")
    suspend fun clearAllStudySessions()

    // Exams
    @Query("SELECT * FROM exams ORDER BY date ASC, time ASC")
    fun getAllExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams ORDER BY date ASC, time ASC")
    suspend fun getExamsDirect(): List<Exam>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam): Long

    @Update
    suspend fun updateExam(exam: Exam)

    @Delete
    suspend fun deleteExam(exam: Exam)

    @Query("DELETE FROM exams")
    suspend fun clearAllExams()
}
