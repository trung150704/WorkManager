package com.example.workmanager.domain.scheduler

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.workmanager.data.local.ReminderEntity
import com.example.workmanager.domain.util.ReminderTimeUtils
import com.example.workmanager.worker.ReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderScheduler {

    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun schedule(reminder: ReminderEntity) {
        enqueue(reminder, System.currentTimeMillis(), ExistingWorkPolicy.REPLACE)
    }

    override fun scheduleNext(reminder: ReminderEntity, after: Long) {
        enqueue(reminder, after, ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    override fun snooze(reminder: ReminderEntity, scheduledTime: Long, delayMillis: Long) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_REMINDER_ID to reminder.id,
                    ReminderWorker.KEY_SCHEDULED_TIME to scheduledTime,
                    ReminderWorker.KEY_IS_SNOOZE to true
                )
            )
            .addTag(TAG_ALL)
            .build()
        workManager.enqueueUniqueWork(snoozeName(reminder.id), ExistingWorkPolicy.REPLACE, request)
        Log.d(TAG, "id=${reminder.id}: hoãn ${delayMillis / 1000}s")
    }

    override fun cancel(reminderId: Long) {
        workManager.cancelUniqueWork(uniqueName(reminderId))
        workManager.cancelUniqueWork(snoozeName(reminderId))
    }

    private fun enqueue(reminder: ReminderEntity, from: Long, policy: ExistingWorkPolicy) {
        val trigger = ReminderTimeUtils.computeNextTrigger(reminder, from)
        if (trigger == null) {
            if (policy == ExistingWorkPolicy.REPLACE) cancel(reminder.id)
            Log.d(TAG, "id=${reminder.id}: không còn lần nhắc nào")
            return
        }

        val delay = (trigger - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_REMINDER_ID to reminder.id,
                    ReminderWorker.KEY_SCHEDULED_TIME to trigger
                )
            )
            .addTag(TAG_ALL)
            .build()

        workManager.enqueueUniqueWork(uniqueName(reminder.id), policy, request)
        Log.d(TAG, "id=${reminder.id}: hẹn sau ${delay / 1000}s (lúc $trigger), policy=$policy")
    }

    private fun uniqueName(id: Long) = "reminder_$id"
    private fun snoozeName(id: Long) = "snooze_$id"

    companion object {
        const val TAG_ALL = "reminder"
        private const val TAG = "ReminderScheduler"
    }

    override suspend fun isScheduled(reminderId: Long): Boolean = withContext(Dispatchers.IO) {
        // get() chặn luồng nên phải chạy trên IO
        workManager.getWorkInfosForUniqueWork(uniqueName(reminderId))
            .get()
            .any { !it.state.isFinished }
    }
}