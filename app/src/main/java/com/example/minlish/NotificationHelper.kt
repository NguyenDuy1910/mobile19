package com.example.minlish

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID   = "minlish_reminder"
    private const val CHANNEL_NAME = "Nhắc học từ vựng"
    private const val NOTIFICATION_ID = 1001

    // Tạo channel — bắt buộc từ Android 8.0 trở lên
    // Channel giống như "danh mục" notification, người dùng có thể tắt từng channel
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT // độ ưu tiên trung bình
            ).apply {
                description = "Nhắc bạn ôn từ vựng hàng ngày"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // Hiển thị notification
    fun showReminder(context: Context, dueCount: Int) {
        // Khi nhấn notification → mở MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // PendingIntent = "ý định trì hoãn" — cho phép hệ thống mở app sau này
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = if (dueCount > 0) {
            "Bạn có $dueCount từ cần ôn hôm nay! 📚"
        } else {
            "Đừng quên học từ vựng hôm nay! 💪"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MinLish — Nhắc học")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // tự xóa khi người dùng nhấn vào
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}