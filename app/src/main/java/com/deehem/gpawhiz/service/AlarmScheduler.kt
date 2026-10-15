package com.deehem.gpawhiz.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import android.widget.Toast
import com.deehem.gpawhiz.data.Exam
import com.deehem.gpawhiz.data.StudySession
import com.deehem.gpawhiz.data.TimetableSlot
import com.deehem.gpawhiz.receiver.AlarmReceiver
import java.util.Calendar
import java.util.Date

object AlarmScheduler {

    fun scheduleAlarm(context: Context, slot: TimetableSlot) {
        if (!slot.alertEnabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fireAt = nextWeeklyAlarmTimeMillis(
            nowMillis = System.currentTimeMillis(),
            dayOfWeek = slot.dayOfWeek,
            startTime = slot.startTime,
            leadMinutes = 60
        )

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_LECTURE_ALARM
            putExtra("slotId", slot.id)
            putExtra("courseCode", slot.courseCode)
            putExtra("venue", slot.venue)
            putExtra("startTime", slot.startTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRequestCode(AppAlarmType.LECTURE, slot.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        fireAt,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        fireAt,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    fireAt,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // AlarmManager fallback on restrictive devices
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                fireAt,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, slot: TimetableSlot) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRequestCode(AppAlarmType.LECTURE, slot.id),
            Intent(context, AlarmReceiver::class.java).apply { action = ACTION_LECTURE_ALARM },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelLegacyLectureAlarm(context: Context, slotId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slotId,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleOneShotStudyAlarm(context: Context, session: StudySession, nowMillis: Long = System.currentTimeMillis()) {
        if (!session.reminderEnabled || session.status != "Upcoming") return
        val fireAt = oneShotReminderTimeMillis(session.date, session.startTime, 15, nowMillis) ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_STUDY_ALARM
            putExtra("isStudySession", true)
            putExtra("courseCode", session.courseCode)
            putExtra("startTime", session.startTime)
            putExtra("duration", session.durationMinutes)
            putExtra("sessionId", session.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRequestCode(AppAlarmType.STUDY, session.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleAlarmManager(context, fireAt, pendingIntent)
    }

    fun cancelStudyAlarm(context: Context, session: StudySession) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_STUDY_ALARM }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRequestCode(AppAlarmType.STUDY, session.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelLegacyStudyAlarm(context: Context, sessionId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "study_$sessionId".hashCode(),
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleStudySessionAlarms(
        context: Context,
        sessions: List<StudySession>,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        sessions.forEach { session ->
            cancelLegacyStudyAlarm(context, session.id)
            cancelStudyAlarm(context, session)
        }
        nextStudySessionsToArm(sessions, nowMillis).forEach { session ->
            scheduleOneShotStudyAlarm(context, session, nowMillis)
        }
    }

    fun rescheduleAll(context: Context, slots: List<TimetableSlot>) {
        for (slot in slots) {
            cancelAlarm(context, slot)
            scheduleAlarm(context, slot)
        }
    }

    fun scheduleExamAlarm(context: Context, exam: Exam) {
        if (!exam.alertEnabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val parts = exam.time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = exam.date
        // Normalize date part
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)

        // Exam reminder 2 hours before
        calendar.add(Calendar.HOUR_OF_DAY, -2)

        if (calendar.timeInMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_EXAM_ALARM
            putExtra("isExam", true)
            putExtra("courseCode", exam.courseCode)
            putExtra("startTime", exam.time)
            putExtra("examId", exam.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRequestCode(AppAlarmType.EXAM, exam.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun cancelExamAlarm(context: Context, exam: Exam) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_EXAM_ALARM }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRequestCode(AppAlarmType.EXAM, exam.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelLegacyExamAlarm(context: Context, examId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "exam_$examId".hashCode(),
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun scheduleAlarmManager(context: Context, fireAt: Long, pendingIntent: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent)
        }
    }

    private const val ACTION_LECTURE_ALARM = "com.deehem.gpawhiz.ALARM_LECTURE"
    private const val ACTION_STUDY_ALARM = "com.deehem.gpawhiz.ALARM_STUDY"
    private const val ACTION_EXAM_ALARM = "com.deehem.gpawhiz.ALARM_EXAM"

    fun exportExamToCalendar(context: Context, exam: Exam) {
        val parts = exam.time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = exam.date
        // Normalize
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)

        val startTimeMillis = calendar.timeInMillis
        val endTimeMillis = startTimeMillis + (3 * 60 * 60 * 1000) // Default 3 hours for exam

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "${exam.courseCode} Examination")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
            putExtra(CalendarContract.Events.DESCRIPTION, "Final Examination for ${exam.courseCode}. Imported from GPA Whiz.")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
            Toast.makeText(context, "Adding exam to calendar...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open calendar.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Export class timetable slot to Android Native Calendar provider
     */
    fun exportToNativeCalendar(context: Context, slot: TimetableSlot) {
        val startHourMin = slot.startTime.split(":")
        val endHourMin = slot.endTime.split(":")
        
        val startHour = startHourMin.getOrNull(0)?.toIntOrNull() ?: 8
        val startMin = startHourMin.getOrNull(1)?.toIntOrNull() ?: 0
        val endHour = endHourMin.getOrNull(0)?.toIntOrNull() ?: 10
        val endMin = endHourMin.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            val targetDay = when (slot.dayOfWeek) {
                1 -> Calendar.MONDAY
                2 -> Calendar.TUESDAY
                3 -> Calendar.WEDNESDAY
                4 -> Calendar.THURSDAY
                5 -> Calendar.FRIDAY
                6 -> Calendar.SATURDAY
                else -> Calendar.SUNDAY
            }
            set(Calendar.DAY_OF_WEEK, targetDay)
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMin)
            set(Calendar.SECOND, 0)
        }
        
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 7)
        }

        val startTimeMillis = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, endHour)
        calendar.set(Calendar.MINUTE, endMin)
        val endTimeMillis = calendar.timeInMillis

        // Launch Native Calendar event editor intent (no heavy calendar write permissions needed!)
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "${slot.courseCode} Lecture")
            putExtra(CalendarContract.Events.EVENT_LOCATION, slot.venue)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
            putExtra(CalendarContract.Events.DESCRIPTION, "Timetable slot imported from GPA Whiz Nigeria App.")
            // Recurrence weekly
            putExtra(CalendarContract.Events.RRULE, "FREQ=WEEKLY;BYDAY=" + when(slot.dayOfWeek) {
                1 -> "MO"
                2 -> "TU"
                3 -> "WE"
                4 -> "TH"
                5 -> "FR"
                6 -> "SA"
                else -> "SU"
            })
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
            Toast.makeText(context, "Redirecting to your Native Calendar app...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open Native Calendar app.", Toast.LENGTH_LONG).show()
        }
    }
}
