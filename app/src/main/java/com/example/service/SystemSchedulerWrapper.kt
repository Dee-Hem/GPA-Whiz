package com.example.service

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.widget.Toast
import com.example.data.TimetableSlot
import java.util.Calendar

object SystemSchedulerWrapper {

    /**
     * Set a native system clock alarm for a course lecture.
     * Uses explicit AlarmClock ACTION_SET_ALARM intent redirection.
     */
    fun setSystemAlarm(context: Context, slot: TimetableSlot) {
        try {
            val parts = slot.startTime.split(":")
            val startHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val startMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            // Alarm set to 60 minutes before lecture
            var alertHour = startHour
            var alertMin = startMinute - 60
            if (alertMin < 0) {
                alertHour -= 1
                alertMin += 60
            }
            if (alertHour < 0) {
                alertHour = 23
            }

            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, alertHour)
                putExtra(AlarmClock.EXTRA_MINUTES, alertMin)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Class Alert: ${slot.courseCode} at ${slot.venue}")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false) // Explicitly open the native clock app
                
                // Set custom repeat days based on dayOfWeek conversion
                val alarmDays = arrayListOf(when (slot.dayOfWeek) {
                    1 -> Calendar.MONDAY
                    2 -> Calendar.TUESDAY
                    3 -> Calendar.WEDNESDAY
                    4 -> Calendar.THURSDAY
                    5 -> Calendar.FRIDAY
                    6 -> Calendar.SATURDAY
                    else -> Calendar.SUNDAY
                })
                putExtra(AlarmClock.EXTRA_DAYS, alarmDays)
                
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)
            Toast.makeText(context, "Redirecting to Device Alarm App...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "System Alarm Setup failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Set up and redirect user to native calendar with complete lecture data.
     */
    fun redirectToSystemCalendar(context: Context, slot: TimetableSlot) {
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
            val rRuleDay = when(slot.dayOfWeek) {
                1 -> "MO"
                2 -> "TU"
                3 -> "WE"
                4 -> "TH"
                5 -> "FR"
                6 -> "SA"
                else -> "SU"
            }
            putExtra(CalendarContract.Events.RRULE, "FREQ=WEEKLY;BYDAY=$rRuleDay")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
            Toast.makeText(context, "Redirecting to your Device Calendar App...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch Device Calendar app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
