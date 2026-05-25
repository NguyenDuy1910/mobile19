package com.example.minlish

// ════════════════════════════════════════════════════════════════
//  ReminderReceiver.kt — Updated
//
//  Thay đổi so với bản cũ:
//  1. Show notification với số từ due chính xác
//  2. Sau khi bắn xong → tự gọi SmartReminderScheduler.schedule()
//     để lên lịch lần tiếp theo (dựa trên từ due tiếp theo)
//  3. Dùng goAsync() để coroutine không bị kill trước khi xong
// ════════════════════════════════════════════════════════════════

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // goAsync() giúp coroutine chạy xong trước khi hệ thống kill receiver
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)

                // Đếm từ due để hiện trong notification
                val dueCount = db.studySessionDao().getDueCount(System.currentTimeMillis())

                // Hiện notification (trên main thread)
                CoroutineScope(Dispatchers.Main).launch {
                    NotificationHelper.showReminder(context, dueCount)
                }

                // Lên lịch lần tiếp theo dựa trên từ due tiếp theo
                SmartReminderScheduler.schedule(context)

            } finally {
                pendingResult.finish()
            }
        }
    }
}