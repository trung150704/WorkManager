package com.example.workmanager.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.workmanager.data.repository.ReminderRepository
import com.example.workmanager.domain.scheduler.ReminderScheduler
import com.example.workmanager.domain.util.ReminderTimeUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Chạy mỗi 24h: ghi MISSED cho lần quên và đặt lại lịch bị mất. */
@HiltWorker
class ReconcileWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()

            val missed = repository.markMissed(now)

            var rescheduled = 0
            for (reminder in repository.getActive()) {
                val hasNext = ReminderTimeUtils.computeNextTrigger(reminder, now) != null
                if (hasNext && !scheduler.isScheduled(reminder.id)) {
                    scheduler.schedule(reminder)
                    rescheduled++
                }
            }
            Log.d(TAG, "Đối soát xong: MISSED mới=$missed, lịch được đặt lại=$rescheduled")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Đối soát lỗi, sẽ thử lại", e)
            Result.retry()   // BackoffPolicy mặc định của WorkManager sẽ giãn cách các lần thử
        }
    }

    companion object {
        const val UNIQUE_NAME = "daily_reconcile"
        private const val TAG = "ReconcileWorker"
    }
}