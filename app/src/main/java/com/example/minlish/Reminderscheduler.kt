package com.example.minlish

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ReminderScheduler {

    private const val REQUEST_CODE = 2001

    // Lên lịch báo thức lặp lại mỗi ngày vào giờ cố định
    // AlarmManager = "đồng hồ hẹn giờ" của Android
    fun scheduleDailyReminder(context: Context, hour: Int = 20, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tính thời điểm báo thức đầu tiên
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            // Nếu giờ đã qua hôm nay → lên lịch cho ngày mai
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // setRepeating: lặp lại mỗi 24 giờ
        // Lưu ý: từ Android 6.0+, setRepeating không chính xác 100%
        // để chính xác hơn cần dùng setExactAndAllowWhileIdle nhưng phức tạp hơn
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,        // loại: thức máy dậy nếu đang ngủ
            calendar.timeInMillis,           // thời điểm đầu tiên
            AlarmManager.INTERVAL_DAY,       // lặp mỗi 24 giờ
            pendingIntent
        )
    }

    // Hủy báo thức nếu cần
    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}