package com.example.workmanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    // REPLACE để chức năng "Hoàn tác xoá" chèn lại đúng id cũ
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getById(id: Long): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders ORDER BY hour ASC, minute ASC")
    fun getAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isActive = 1")
    suspend fun getActive(): List<ReminderEntity>

    @Query("UPDATE reminders SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)
}