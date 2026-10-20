package com.example.workmanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.workmanager.di.ReceiverEntryPoint
import com.example.workmanager.domain.model.LogStatus
import com.example.workmanager.domain.model.StockRules
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, 0L)
        if (reminderId == -1L) return

        NotificationManagerCompat.from(context).cancel(reminderId.toInt())

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, ReceiverEntryPoint::class.java
        )
        val repository = entryPoint.reminderRepository()
        val scheduler = entryPoint.reminderScheduler()
        val notificationHelper = entryPoint.notificationHelper()

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_TAKEN -> {
                        val remaining =
                            repository.recordAction(reminderId, scheduledTime, LogStatus.TAKEN)
                        if (remaining != null && remaining <= StockRules.LOW_THRESHOLD) {
                            repository.observe(reminderId).first()
                                ?.let { notificationHelper.showLowStock(it, remaining) }
                        }
                    }

                    ACTION_SKIP ->
                        repository.recordAction(reminderId, scheduledTime, LogStatus.SKIPPED)

                    ACTION_SNOOZE -> {
                        repository.recordAction(reminderId, scheduledTime, LogStatus.SNOOZED)
                        val reminder = repository.observe(reminderId).first()
                        if (reminder != null && reminder.isActive) {
                            scheduler.snooze(reminder, scheduledTime, SNOOZE_MINUTES * 60_000L)
                        }
                    }
                }
                Log.d(TAG, "action=$action id=$reminderId")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi xử lý action=$action id=$reminderId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TAKEN = "com.example.workmanager.action.TAKEN"
        const val ACTION_SNOOZE = "com.example.workmanager.action.SNOOZE"
        const val ACTION_SKIP = "com.example.workmanager.action.SKIP"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val SNOOZE_MINUTES = 10L
        private const val TAG = "ActionReceiver"
    }
}