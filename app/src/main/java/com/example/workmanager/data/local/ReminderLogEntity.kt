package com.example.workmanager.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.workmanager.domain.model.LogStatus

@Entity(
    tableName = "reminder_logs",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("reminderId")]
)
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    /** epochMillis */
    val scheduledTime: Long,
    val status: LogStatus,
    val actedAt: Long? = null
)