package com.example.minlish

// ════════════════════════════════════════════════════════════════
//  SmartReminderScheduler.kt
//
//  Vấn đề cũ (ReminderScheduler.kt):
//    - Luôn nhắc lúc 20:00 dù có thể không có từ nào due
//    - Dùng setRepeating() — không chính xác từ Android 6.0+
//
//  Giải pháp mới:
//    1. Tìm nextReviewTime nhỏ nhất trong DB (từ due sớm nhất)
//    2. Nếu thời điểm đó < 8:00 sáng → delay đến 8:00 (tránh nhắc đêm)
//    3. Nếu không có từ nào → fallback về 20:00 hôm nay / ngày mai
//    4. Dùng setExactAndAllowWhileIdle() → chính xác ngay cả khi Doze
//    5. Android 12+: kiểm tra canScheduleExactAlarms() trước khi gọi
//    6. Bắt SecurityException làm fallback an toàn
// ════════════════════════════════════════════════════════════════

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object SmartReminderScheduler {

    private const val TAG           = "SmartReminder"
    private const val REQUEST_CODE  = 3001
    private const val HOUR_MIN      = 8    // Không nhắc trước 8:00 sáng
    private const val HOUR_FALLBACK = 20   // Fallback: 20:00 nếu không có từ nào due

    /**
     * Gọi hàm này tại:
     *  - MainActivity.onCreate()
     *  - Cuối ReminderReceiver.onReceive() (để tự động lên lịch lần tiếp)
     *  - Sau khi người dùng thêm từ mới / hoàn thành session flashcard
     */
    fun schedule(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db  = AppDatabase.getInstance(context)
            val dao = db.vocabularyDao()

            val earliestDue = dao.getEarliestNextReviewTime()
            val now         = System.currentTimeMillis()
            val targetTime  = resolveTargetTime(earliestDue, now)

            Log.d(TAG, "Scheduling reminder at: ${java.util.Date(targetTime)}")
            scheduleExact(context, targetTime)
        }
    }

    /**
     * Tính thời điểm nhắc hợp lý:
     * - Nếu có từ due và thời điểm đó > bây giờ → dùng max(đó, 8:00 sáng hôm đó)
     * - Nếu từ đó đã due rồi (quá khứ) → nhắc sau 1 phút
     * - Nếu không có từ nào → 20:00 hôm nay hoặc ngày mai
     */
    private fun resolveTargetTime(earliestDue: Long?, now: Long): Long {
        if (earliestDue != null && earliestDue > 0) {
            return if (earliestDue <= now) {
                now + 60_000L
            } else {
                val dueDay8am = get8amOfDay(earliestDue)
                maxOf(earliestDue, dueDay8am)
            }
        }
        return getFallbackTime(now)
    }

    private fun get8amOfDay(ts: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, HOUR_MIN)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun getFallbackTime(now: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, HOUR_FALLBACK)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    private fun scheduleExact(context: Context, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    // Android 12+: bắt buộc phải kiểm tra canScheduleExactAlarms()
                    // trước khi gọi setExactAndAllowWhileIdle(), nếu không sẽ crash
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                        )
                        Log.d(TAG, "Exact alarm set (Android 12+)")
                    } else {
                        // Không có quyền → dùng setWindow (window 10 phút, kém chính xác hơn)
                        // User cần vào Settings → Apps → MinLish → Alarms & Reminders để cấp quyền
                        Log.w(TAG, "No SCHEDULE_EXACT_ALARM permission, fallback to setWindow")
                        alarmManager.setWindow(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            10 * 60 * 1000L,
                            pendingIntent
                        )
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    // Android 6–11: không cần permission đặc biệt
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                    Log.d(TAG, "Exact alarm set (Android 6–11)")
                }
                else -> {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                    Log.d(TAG, "Exact alarm set (Android < 6)")
                }
            }
        } catch (e: SecurityException) {
            // Bắt SecurityException phòng trường hợp permission bị thu hồi runtime
            Log.e(TAG, "SecurityException: ${e.message} — fallback to inexact alarm")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}