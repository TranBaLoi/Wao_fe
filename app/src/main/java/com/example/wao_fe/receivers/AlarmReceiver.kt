package com.example.wao_fe.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.wao_fe.utils.NotificationHelper
import com.example.wao_fe.utils.ReminderManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("EXTRA_ID", 0)
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Nhắc nhở Wao"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Đã đến lúc thực hiện mục tiêu của bạn!"
        
        // 1. Gửi thông báo ngay lập tức
        NotificationHelper.showReminderNotification(context, title, message, id)

        // 2. Cài đặt lại đúng mốc giờ này cho ngày hôm sau
        val hour = intent.getIntExtra("EXTRA_HOUR", -1)
        val minute = intent.getIntExtra("EXTRA_MINUTE", -1)
        
        if (hour != -1 && minute != -1) {
            ReminderManager.scheduleExactAlarm(context, id, hour, minute, title, message)
        }
    }
}

