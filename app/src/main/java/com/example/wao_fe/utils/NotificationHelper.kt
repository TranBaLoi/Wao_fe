package com.example.wao_fe.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.wao_fe.MainActivity
import com.example.wao_fe.R

object NotificationHelper {

    private const val CHANNEL_ID_REMINDERS = "channel_reminders_v2"
    private const val CHANNEL_ID_ALERTS = "channel_alerts_v2"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Nhắc nhở hàng ngày",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh thông báo nhắc nhở uống nước, ghi nhật ký bữa ăn"
                enableVibration(true)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Cảnh báo quan trọng",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh thông báo cảnh báo như vượt quá calo"
                enableVibration(true)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    fun showReminderNotification(context: Context, title: String, message: String, notificationId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Thay đổi icon cho phù hợp vói app của bạn
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) // Yêu cầu popup trên một số dòng máy
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                // Ignore if missing POST_NOTIFICATIONS permission
            }
        }
    }

    fun showAlertNotification(context: Context, title: String, message: String, notificationId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Thay đổi icon cho phù hợp vói app của bạn
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) // Yêu cầu popup trên một số dòng máy
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                // Ignore if missing POST_NOTIFICATIONS permission
            }
        }
    }
}
