package com.example.workmanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.workmanager.notification.NotificationHelper
import com.example.workmanager.worker.ReconcileWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class WorkManagerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: NotificationHelper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
        scheduleDailyReconcile()
    }

    private fun scheduleDailyReconcile() {
        val request = PeriodicWorkRequestBuilder<ReconcileWorker>(
            RECONCILE_INTERVAL_HOURS, TimeUnit.HOURS
        )
            // Khi Result.retry(): giãn cách tuyến tính 30s, 60s, 90s...
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build()
        // UPDATE: áp dụng chu kỳ mới nhưng không reset đồng hồ mỗi lần mở app
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReconcileWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private companion object {
        /** 12h: rút ngắn thời gian chuỗi nhắc bị đứt (do OEM kill app) được tự phục hồi. */
        const val RECONCILE_INTERVAL_HOURS = 12L
    }
}