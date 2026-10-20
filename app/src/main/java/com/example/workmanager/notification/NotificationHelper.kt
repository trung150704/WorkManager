package com.example.workmanager.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.workmanager.MainActivity
import com.example.workmanager.R
import com.example.workmanager.data.local.ReminderEntity
import com.example.workmanager.domain.model.ReminderType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.workmanager.receiver.ReminderActionReceiver

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** minSdk 26 nên channel luôn tồn tại. Gọi nhiều lần vẫn an toàn. */
    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)

        val medicine = NotificationChannel(
            CHANNEL_MEDICINE,
            context.getString(R.string.channel_medicine_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_medicine_desc)
            enableVibration(true)
        }

        val study = NotificationChannel(
            CHANNEL_STUDY,
            context.getString(R.string.channel_study_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_study_desc)
        }
        val stock = NotificationChannel(
            CHANNEL_STOCK,
            context.getString(R.string.channel_stock_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_stock_desc)
        }

        manager.createNotificationChannels(listOf(medicine, study, stock))
    }

    @SuppressLint("MissingPermission")
    fun show(reminder: ReminderEntity, scheduledTime: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Chưa có quyền POST_NOTIFICATIONS, bỏ qua thông báo id=${reminder.id}")
            return
        }

        val isMedicine = reminder.type == ReminderType.MEDICINE

        val body = buildString {
            append(
                context.getString(
                    if (isMedicine) R.string.notif_text_medicine else R.string.notif_text_study
                )
            )
            reminder.dosage?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
            if (reminder.note.isNotBlank()) append("\n").append(reminder.note)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takenLabel = context.getString(
            if (isMedicine) R.string.action_taken_medicine else R.string.action_taken_study
        )

        val notification = NotificationCompat.Builder(
            context,
            if (isMedicine) CHANNEL_MEDICINE else CHANNEL_STUDY
        )
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(reminder.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (isMedicine) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, takenLabel,
                actionIntent(reminder, scheduledTime, ReminderActionReceiver.ACTION_TAKEN, 1))
            .addAction(0, context.getString(R.string.action_snooze),
                actionIntent(reminder, scheduledTime, ReminderActionReceiver.ACTION_SNOOZE, 2))
            .addAction(0, context.getString(R.string.action_skip),
                actionIntent(reminder, scheduledTime, ReminderActionReceiver.ACTION_SKIP, 3))
            .build()

        NotificationManagerCompat.from(context).notify(reminder.id.toInt(), notification)
    }

    private fun actionIntent(
        reminder: ReminderEntity,
        scheduledTime: Long,
        action: String,
        slot: Int
    ): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderActionReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        // requestCode khác nhau cho mỗi (nhắc nhở, nút), nếu không các nút sẽ đè lên nhau
        return PendingIntent.getBroadcast(
            context,
            reminder.id.toInt() * 10 + slot,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun showLowStock(reminder: ReminderEntity, remaining: Int) {
        if (!hasPermission()) return

        val text = if (remaining <= 0) {
            context.getString(R.string.stock_empty_text)
        } else {
            context.getString(R.string.stock_low_text, remaining)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            LOW_STOCK_BASE + reminder.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_STOCK)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.stock_low_title, reminder.title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(LOW_STOCK_BASE + reminder.id.toInt(), notification)
    }
    companion object {
        const val CHANNEL_MEDICINE = "medicine_reminders"
        const val CHANNEL_STUDY = "study_reminders"
        private const val TAG = "NotificationHelper"
        const val CHANNEL_STOCK = "stock_alerts"
        private const val LOW_STOCK_BASE = 1_000_000
    }
}