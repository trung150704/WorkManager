package com.example.workmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.workmanager.domain.model.ReminderType

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val type: ReminderType,
    val hour: Int,
    val minute: Int,
    /** Bitmask: bit0 = Thứ 2 ... bit6 = Chủ nhật */
    val daysOfWeek: Int,
    /** epochDay */
    val startDate: Long,
    /** epochDay, null = không kết thúc */
    val endDate: Long? = null,
    val dosage: String? = null,
    val remainingQty: Int? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)