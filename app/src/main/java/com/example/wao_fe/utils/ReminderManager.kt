package com.example.wao_fe.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.wao_fe.receivers.AlarmReceiver
import java.util.Calendar

object ReminderManager {

    /**
     * Lên lịch báo thức chính xác (Exact Alarm)
     * Thậm chí khi app bị clear memory (vuốt chớ) thì AlarmManager của hệ điều hành vẫn kích hoạt.
     */
    fun scheduleExactAlarm(context: Context, id: Int, hour: Int, minute: Int, title: String, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Yêu cầu quyền cho Android 12+ (API 31)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("ReminderManager", "Thiếu quyền sử dụng báo thức chính xác (Exact Alarms).")
                return
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_ID", id)
            putExtra("EXTRA_HOUR", hour)
            putExtra("EXTRA_MINUTE", minute)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_MESSAGE", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Nếu thời gian đã trôi qua trong ngày hôm nay, bộ hẹn giờ sẽ tự động nhảy sang ngày hôm sau
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        try {
            // setExactAndAllowWhileIdle sẽ hẹn báo thức cực kì đúng giờ bất chấp chế độ Doze của điện thoại
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("ReminderManager", "Đã đặt báo thức: $title lúc \${calendar.time}")
        } catch (e: SecurityException) {
            Log.e("ReminderManager", "SecurityException: \${e.message}")
        }
    }

    /**
     * Khởi tạo toàn bộ các nhắc nhở cơ bản cho 1 ngày
     */
    fun setupAllReminders(context: Context) {
        // Nhắc bữa ăn
        scheduleExactAlarm(context, 101, 8, 0, "Nhắc nhở bữa sáng", "Bạn đã ăn sáng chưa? Hãy ghi lại nhật ký món ăn để tiếp thêm năng lượng nhé!")
        scheduleExactAlarm(context, 102, 12, 0, "Nhắc nhở bữa trưa", "Nạp năng lượng bữa trưa ngay và đừng quên ghi vào nhật ký calo!")
        scheduleExactAlarm(context, 103, 19, 0, "Nhắc nhở bữa tối", "Bữa tối rất quan trọng, hãy kiểm tra nhật ký xem hôm nay bạn đã ăn bao nhiêu calo rồi.")

        // Nhắc uống nước
        scheduleExactAlarm(context, 201, 9, 30, "Uống nước thôi!", "1 ly nước buổi sáng giúp cơ thể khỏe khoắn hơn.")
        scheduleExactAlarm(context, 202, 11, 30, "Bạn uống nước chưa?", "Gần đến bữa trưa, hãy làm một ly nước mát nhé.")
        scheduleExactAlarm(context, 203, 15, 0, "Giải lao uống nước", "Uống một chút nước để tăng sức tập trung buổi chiều nào.")
        scheduleExactAlarm(context, 204, 17, 30, "Bổ sung nước", "Ngày sắp hết rồi, hãy bổ sung nước cho đủ KPI hôm nay.")
    }
}

