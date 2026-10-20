package com.example.workmanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.workmanager.di.ReceiverEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Đặt lại toàn bộ lịch khi: khởi động máy, đổi giờ hệ thống, đổi múi giờ, cập nhật app.
 * Lịch dùng thời điểm tuyệt đối tính theo múi giờ lúc lên lịch,
 * nên sau các sự kiện này phải tính lại cho chính xác.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action !in HANDLED_ACTIONS) return
        Log.d(TAG, "Nhận $action, đặt lại lịch")

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, ReceiverEntryPoint::class.java
        )

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val active = entryPoint.reminderRepository().getActive()
                val scheduler = entryPoint.reminderScheduler()
                active.forEach { scheduler.schedule(it) }
                Log.d(TAG, "Đã đặt lại ${active.size} nhắc nhở")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi đặt lại lịch", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}