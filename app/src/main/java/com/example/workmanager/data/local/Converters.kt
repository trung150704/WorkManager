package com.example.workmanager.data.local

import androidx.room.TypeConverter
import com.example.workmanager.domain.model.LogStatus
import com.example.workmanager.domain.model.ReminderType

class Converters {
    @TypeConverter
    fun fromReminderType(value: ReminderType): String = value.name

    @TypeConverter
    fun toReminderType(value: String): ReminderType = ReminderType.valueOf(value)

    @TypeConverter
    fun fromLogStatus(value: LogStatus): String = value.name

    @TypeConverter
    fun toLogStatus(value: String): LogStatus = LogStatus.valueOf(value)
}