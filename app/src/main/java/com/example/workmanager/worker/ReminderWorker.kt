package com.example.workmanager.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.workmanager.data.repository.ReminderRepository
import com.example.workmanager.domain.scheduler.ReminderScheduler
import com.example.workmanager.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_REMINDER_ID, -1L)
        val scheduledTime = inputData.getLong(KEY_SCHEDULED_TIME, System.currentTimeMillis())
        val isSnooze = inputData.getBoolean(KEY_IS_SNOOZE, false)
        if (id == -1L) return Result.failure()

        val reminder = repository.observe(id).first()
        if (reminder == null || !reminder.isActive) {
            Log.d(TAG, "Bỏ qua id=$id (đã xoá hoặc đã tắt)")
            return Result.success()
        }
        val lateMs = System.currentTimeMillis() - scheduledTime
        Log.d(TAG, "Nhắc id=${reminder.id} snooze=$isSnooze, trễ ${lateMs}ms so với dự kiến")

        // Đặt lần kế tiếp TRƯỚC khi hiện thông báo: nếu tiến trình bị kill giữa chừng
        // thì chuỗi nhắc vẫn không bị đứt. Snooze chỉ hiện thông báo, chuỗi chính chạy độc lập.
        if (!isSnooze) {
            scheduler.scheduleNext(reminder, maxOf(System.currentTimeMillis(), scheduledTime))
        }
        notificationHelper.show(reminder, scheduledTime)
        return Result.success()
    }

    companion object {
        const val KEY_REMINDER_ID = "reminderId"
        const val KEY_SCHEDULED_TIME = "scheduledTime"
        const val KEY_IS_SNOOZE = "isSnooze"
        private const val TAG = "ReminderWorker"
    }
}