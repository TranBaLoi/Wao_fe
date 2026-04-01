package com.example.wao_fe.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.wao_fe.utils.ReminderManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Thiết bị vừa khởi động lại, đặt lại toàn bộ báo thức.")
            ReminderManager.setupAllReminders(context)
        }
    }
}

