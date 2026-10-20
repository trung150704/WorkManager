package com.example.workmanager.data.repository

import com.example.workmanager.data.local.ReminderEntity
import com.example.workmanager.data.local.ReminderLogItem
import com.example.workmanager.domain.model.LogStatus
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeAll(): Flow<List<ReminderEntity>>
    fun observe(id: Long): Flow<ReminderEntity?>
    fun observeLogs(): Flow<List<ReminderLogItem>>
    suspend fun getActive(): List<ReminderEntity>
    suspend fun add(reminder: ReminderEntity): Long
    suspend fun update(reminder: ReminderEntity)
    suspend fun delete(reminder: ReminderEntity)
    suspend fun setActive(id: Long, isActive: Boolean)

    /**
     * Ghi log phản hồi từ thông báo.
     * @return số thuốc còn lại sau khi trừ (chỉ khi TAKEN và là thuốc có theo dõi số lượng), ngược lại null.
     */
    suspend fun recordAction(reminderId: Long, scheduledTime: Long, status: LogStatus): Int?

    /** Ghi MISSED cho các lần nhắc đã qua mà không có phản hồi. @return số dòng đã ghi. */
    suspend fun markMissed(now: Long): Int
}