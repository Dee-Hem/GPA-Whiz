package com.deehem.gpawhiz.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScholarshipDao {

    // Scholarships
    @Query("SELECT * FROM scholarships ORDER BY dateAdded DESC")
    fun getAllScholarships(): Flow<List<Scholarship>>

    @Query("SELECT * FROM scholarships WHERE id = :id LIMIT 1")
    suspend fun getScholarshipById(id: Int): Scholarship?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScholarship(scholarship: Scholarship): Long

    @Update
    suspend fun updateScholarship(scholarship: Scholarship)

    @Delete
    suspend fun deleteScholarship(scholarship: Scholarship)

    @Query("DELETE FROM scholarships")
    suspend fun clearAllScholarships()

    // Requirements
    @Query("SELECT * FROM scholarship_requirements ORDER BY deadline ASC, id ASC")
    fun getAllRequirements(): Flow<List<ScholarshipRequirement>>

    @Query("SELECT * FROM scholarship_requirements WHERE scholarshipId = :scholarshipId ORDER BY deadline ASC, id ASC")
    fun getRequirementsForScholarship(scholarshipId: Int): Flow<List<ScholarshipRequirement>>

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
    @Query("SELECT * FROM scholarship_timeline_events ORDER BY date ASC, id ASC")
    fun getAllTimelineEvents(): Flow<List<ScholarshipTimelineEvent>>

    @Query("SELECT * FROM scholarship_timeline_events WHERE scholarshipId = :scholarshipId ORDER BY date ASC, id ASC")
    fun getTimelineEventsForScholarship(scholarshipId: Int): Flow<List<ScholarshipTimelineEvent>>

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

    @Query("SELECT * FROM scholarship_reminders WHERE scholarshipId = :scholarshipId")
    fun getRemindersForScholarship(scholarshipId: Int): Flow<List<ScholarshipReminder>>

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

    // Essay Drafts
    @Query("SELECT * FROM essay_drafts ORDER BY lastModified DESC")
    fun getAllDrafts(): Flow<List<EssayDraft>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: EssayDraft): Long

    @Update
    suspend fun updateDraft(draft: EssayDraft)

    @Delete
    suspend fun deleteDraft(draft: EssayDraft)

    @Query("DELETE FROM essay_drafts")
    suspend fun clearAllDrafts()

    // Exchange Rates
    @Query("SELECT * FROM exchange_rates ORDER BY fromCurrency ASC, toCurrency ASC")
    fun getAllExchangeRates(): Flow<List<ExchangeRate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRate(rate: ExchangeRate)

    @Delete
    suspend fun deleteExchangeRate(rate: ExchangeRate)

    @Query("DELETE FROM exchange_rates")
    suspend fun clearAllExchangeRates()
}
