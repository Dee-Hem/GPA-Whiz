package com.deehem.gpawhiz.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.deehem.gpawhiz.MainActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isStudy = intent.getBooleanExtra("isStudySession", false)
        val isExam = intent.getBooleanExtra("isExam", false)
        val courseCode = intent.getStringExtra("courseCode") ?: "Your Class"
        val time = intent.getStringExtra("startTime") ?: "Soon"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = when {
            isExam -> "exam_alerts"
            isStudy -> "study_alerts"
            else -> "timetable_alerts"
        }
        val channelName = when {
            isExam -> "Examination Alerts"
            isStudy -> "Study Planner Alerts"
            else -> "Class Schedule Alerts"
        }
        val channelDesc = when {
            isExam -> "Critical reminders for your university examinations"
            isStudy -> "Reminders for your scheduled study sessions"
            else -> "Fires 1 hour before your scheduled Nigerian university classes"
        }

        // Create channel for Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
                enableVibration(true)
                // Set sound to alarm sound for real ringing
                val alarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                setSound(alarmUri, android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open app on click
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            isExam -> "Exam Today: $courseCode"
            isStudy -> "Study Time: $courseCode"
            else -> "Upcoming Class: $courseCode"
        }
        val content = when {
            isExam -> "Your examination starts in 2 hours ($time). Good luck, you've got this!"
            isStudy -> "Your study session starts in 15 minutes ($time). Ready to focus?"
            else -> {
                val venue = intent.getStringExtra("venue") ?: "Unknown Venue"
                "Starting in 1 hour ($time) at $venue. Keep shining!"
            }
        }

        val alarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmUri)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = when {
            isExam -> intent.getIntExtra("examId", 0).plus(200000)
            isStudy -> intent.getIntExtra("sessionId", 0).plus(100000)
            else -> courseCode.hashCode()
        }
        notificationManager.notify(notificationId, notification)
    }
}
