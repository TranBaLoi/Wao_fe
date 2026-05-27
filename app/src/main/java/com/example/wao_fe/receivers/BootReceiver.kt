package com.example.wao_fe.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.wao_fe.utils.ReminderManager

/**
 * BroadcastReceiver nhận sự kiện thiết bị khởi động lại.
 * Dùng để thiết lập lại các lịch nhắc nhở sau khi điện thoại reboot.
 */
class BootReceiver : BroadcastReceiver() {
    /**

     * Sau khi máy khởi động lại, hàm đăng ký lại toàn bộ lịch nhắc mặc định.

     */

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Thiết bị vừa khởi động lại, đặt lại toàn bộ báo thức.")
            ReminderManager.setupAllReminders(context)
        }
    }
}

