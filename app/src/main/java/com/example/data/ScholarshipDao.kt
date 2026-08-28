package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScholarshipDao {

    // Scholarships
    @Query("SELECT * FROM scholarships ORDER BY deadlineDate ASC, id DESC")
    fun getAllScholarships(): Flow<List<Scholarship>>

    @Query("SELECT * FROM scholarships WHERE id = :id LIMIT 1")
    fun getScholarshipById(id: Int): Flow<Scholarship?>

    @Query("SELECT * FROM scholarships WHERE id = :id LIMIT 1")
    suspend fun getScholarshipDirect(id: Int): Scholarship?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScholarship(scholarship: Scholarship): Long

    @Update
    suspend fun updateScholarship(scholarship: Scholarship)

    @Delete
    suspend fun deleteScholarship(scholarship: Scholarship)

    @Query("DELETE FROM scholarships WHERE id = :id")
    suspend fun deleteScholarshipById(id: Int)

    @Query("DELETE FROM scholarships")
    suspend fun clearAllScholarships()

    // Requirements
    @Query("SELECT * FROM scholarship_requirements ORDER BY id ASC")
    fun getAllRequirements(): Flow<List<ScholarshipRequirement>>

    @Query("SELECT * FROM scholarship_requirements WHERE scholarshipId = :scholarshipId ORDER BY id ASC")
    fun getRequirementsForScholarship(scholarshipId: Int): Flow<List<ScholarshipRequirement>>

    @Query("SELECT * FROM scholarship_requirements WHERE scholarshipId = :scholarshipId ORDER BY id ASC")
    suspend fun getRequirementsForScholarshipDirect(scholarshipId: Int): List<ScholarshipRequirement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequirement(requirement: ScholarshipRequirement): Long

    @Update
    suspend fun updateRequirement(requirement: ScholarshipRequirement)

    @Delete
    suspend fun deleteRequirement(requirement: ScholarshipRequirement)

    @Query("DELETE FROM scholarship_requirements WHERE scholarshipId = :scholarshipId")
    suspend fun deleteRequirementsForScholarship(scholarshipId: Int)

    @Query("DELETE FROM scholarship_requirements")
    suspend fun clearAllRequirements()

    // Timeline Events
    @Query("SELECT * FROM scholarship_timeline_events ORDER BY date DESC, id DESC")
    fun getAllTimelineEvents(): Flow<List<ScholarshipTimelineEvent>>

    @Query("SELECT * FROM scholarship_timeline_events WHERE scholarshipId = :scholarshipId ORDER BY date DESC, id DESC")
    fun getTimelineEventsForScholarship(scholarshipId: Int): Flow<List<ScholarshipTimelineEvent>>

    @Query("SELECT * FROM scholarship_timeline_events WHERE scholarshipId = :scholarshipId ORDER BY date DESC, id DESC")
    suspend fun getTimelineEventsForScholarshipDirect(scholarshipId: Int): List<ScholarshipTimelineEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimelineEvent(event: ScholarshipTimelineEvent): Long

    @Update
    suspend fun updateTimelineEvent(event: ScholarshipTimelineEvent)

    @Delete
    suspend fun deleteTimelineEvent(event: ScholarshipTimelineEvent)

    @Query("DELETE FROM scholarship_timeline_events WHERE scholarshipId = :scholarshipId")
    suspend fun deleteTimelineEventsForScholarship(scholarshipId: Int)

    @Query("DELETE FROM scholarship_timeline_events")
    suspend fun clearAllTimelineEvents()

    // Reminders
    @Query("SELECT * FROM scholarship_reminders ORDER BY reminderTime ASC")
    fun getAllReminders(): Flow<List<ScholarshipReminder>>

    @Query("SELECT * FROM scholarship_reminders WHERE scholarshipId = :scholarshipId ORDER BY reminderTime ASC")
    fun getRemindersForScholarship(scholarshipId: Int): Flow<List<ScholarshipReminder>>

    @Query("SELECT * FROM scholarship_reminders WHERE scholarshipId = :scholarshipId ORDER BY reminderTime ASC")
    suspend fun getRemindersForScholarshipDirect(scholarshipId: Int): List<ScholarshipReminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ScholarshipReminder): Long

    @Update
    suspend fun updateReminder(reminder: ScholarshipReminder)

    @Delete
    suspend fun deleteReminder(reminder: ScholarshipReminder)

    @Query("DELETE FROM scholarship_reminders WHERE scholarshipId = :scholarshipId")
    suspend fun deleteRemindersForScholarship(scholarshipId: Int)

    @Query("DELETE FROM scholarship_reminders")
    suspend fun clearAllReminders()
}
