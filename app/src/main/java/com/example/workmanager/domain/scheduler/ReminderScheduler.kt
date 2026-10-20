package com.example.workmanager.domain.scheduler

import com.example.workmanager.data.local.ReminderEntity

interface ReminderScheduler {
    /** Người dùng tạo/sửa/bật: thay thế lịch cũ. */
    fun schedule(reminder: ReminderEntity)

    /** Worker gọi sau khi nhắc xong: nối lần kế tiếp mà không huỷ worker đang chạy. */
    fun scheduleNext(reminder: ReminderEntity, after: Long)

    /** Nhắc lại sau [delayMillis]. Chạy riêng, không ảnh hưởng chuỗi chính. */
    fun snooze(reminder: ReminderEntity, scheduledTime: Long, delayMillis: Long)

    fun cancel(reminderId: Long)

    /** True nếu nhắc nhở này còn một work đang chờ hoặc đang chạy. */
    suspend fun isScheduled(reminderId: Long): Boolean
}