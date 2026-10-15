package com.deehem.gpawhiz.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.deehem.gpawhiz.data.AppDatabase
import com.deehem.gpawhiz.data.StudySession
import com.deehem.gpawhiz.data.TimetableSlot
import com.deehem.gpawhiz.service.AlarmScheduler
import com.deehem.gpawhiz.service.studySessionsEligibleForRecovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmRecovery.rescheduleAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

object AlarmRecovery {
    suspend fun rescheduleAll(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val dao = AppDatabase.getDatabase(context).gpaDao()
        rescheduleLectureSlots(context, dao.getTimetableSlotsDirect())
        rescheduleExams(context, dao.getExamsDirect())
        rescheduleStudySessions(context, dao.getStudySessionsDirect(), nowMillis)
    }

    private fun rescheduleLectureSlots(context: Context, slots: List<TimetableSlot>) {
        slots.forEach { slot ->
            AlarmScheduler.cancelLegacyLectureAlarm(context, slot.id)
            AlarmScheduler.cancelAlarm(context, slot)
            AlarmScheduler.scheduleAlarm(context, slot)
        }
    }

    private fun rescheduleExams(context: Context, exams: List<com.deehem.gpawhiz.data.Exam>) {
        exams.forEach { exam ->
            AlarmScheduler.cancelLegacyExamAlarm(context, exam.id)
            AlarmScheduler.cancelExamAlarm(context, exam)
            AlarmScheduler.scheduleExamAlarm(context, exam)
        }
    }

    private fun rescheduleStudySessions(
        context: Context,
        sessions: List<StudySession>,
        nowMillis: Long
    ) {
        sessions.forEach { session ->
            AlarmScheduler.cancelLegacyStudyAlarm(context, session.id)
            AlarmScheduler.cancelStudyAlarm(context, session)
        }
        studySessionsEligibleForRecovery(sessions, nowMillis).forEach { session ->
            AlarmScheduler.scheduleOneShotStudyAlarm(context, session, nowMillis)
        }
    }
}
