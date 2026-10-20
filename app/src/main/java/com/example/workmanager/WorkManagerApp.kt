package com.example.workmanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
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
        val request = PeriodicWorkRequestBuilder<ReconcileWorker>(24, TimeUnit.HOURS).build()
        // KEEP: nếu đã có thì giữ nguyên, không đặt lại đồng hồ mỗi lần mở app
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReconcileWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}