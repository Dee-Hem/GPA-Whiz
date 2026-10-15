package com.deehem.gpawhiz.service

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.widget.Toast
import com.deehem.gpawhiz.data.Exam
import com.deehem.gpawhiz.data.StudySession
import com.deehem.gpawhiz.data.TimetableSlot
import java.util.Calendar

object SystemSchedulerWrapper {

    fun redirectToSystemCalendar(context: Context, slot: TimetableSlot) {
        try {
            val parts = slot.startTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val intent = Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, "Class: ${slot.courseCode}")
                .putExtra(CalendarContract.Events.EVENT_LOCATION, slot.venue)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Calendar not supported: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun setSystemAlarm(context: Context, slot: TimetableSlot) {
        try {
            val parts = slot.startTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, "Class: ${slot.courseCode}")
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, min)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not set alarm: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun setSystemAlarmForExam(context: Context, exam: Exam) {
        try {
            val parts = exam.time.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, "Exam: ${exam.courseCode}")
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, min)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not set exam alarm: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun setSystemAlarmForStudy(context: Context, session: StudySession) {
        try {
            val parts = session.startTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, "Study: ${session.courseCode}")
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, min)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not set study alarm: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun redirectToSystemCalendarForStudy(context: Context, session: StudySession) {
        try {
            val parts = session.startTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val cal = Calendar.getInstance().apply {
                timeInMillis = session.date
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
            }
            val intent = Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_CUSTOM_APP_URI, cal.timeInMillis)
                .putExtra(CalendarContract.Events.TITLE, "Study: ${session.courseCode}")
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Calendar not supported: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
