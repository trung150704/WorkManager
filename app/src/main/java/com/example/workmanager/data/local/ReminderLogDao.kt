package com.example.workmanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderLogDao {

    @Insert
    suspend fun insert(log: ReminderLogEntity): Long

    @Query("SELECT * FROM reminder_logs WHERE reminderId = :reminderId ORDER BY scheduledTime DESC")
    fun getByReminder(reminderId: Long): Flow<List<ReminderLogEntity>>

    @Query(
        "SELECT * FROM reminder_logs " +
                "WHERE scheduledTime BETWEEN :fromMillis AND :toMillis ORDER BY scheduledTime DESC"
    )
    fun getByDateRange(fromMillis: Long, toMillis: Long): Flow<List<ReminderLogEntity>>

    @Query(
        "SELECT l.*, r.title AS reminderTitle " +
                "FROM reminder_logs AS l INNER JOIN reminders AS r ON l.reminderId = r.id " +
                "ORDER BY l.scheduledTime DESC, l.id DESC"
    )
    fun observeAllWithReminder(): Flow<List<ReminderLogItem>>
    @Query(
        "SELECT * FROM reminder_logs " +
                "WHERE reminderId = :reminderId AND scheduledTime BETWEEN :fromMillis AND :toMillis"
    )
    suspend fun getForReminderInRange(
        reminderId: Long,
        fromMillis: Long,
        toMillis: Long
    ): List<ReminderLogEntity>
}