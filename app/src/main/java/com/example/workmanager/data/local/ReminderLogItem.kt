package com.example.workmanager.data.local

import androidx.room.Embedded

/** Một dòng log kèm tên nhắc nhở (kết quả JOIN, không phải bảng mới). */
data class ReminderLogItem(
    @Embedded val log: ReminderLogEntity,
    val reminderTitle: String
)