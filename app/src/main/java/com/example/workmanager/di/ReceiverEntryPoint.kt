package com.example.workmanager.di

import com.example.workmanager.data.repository.ReminderRepository
import com.example.workmanager.domain.scheduler.ReminderScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.example.workmanager.notification.NotificationHelper

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverEntryPoint {
    fun reminderRepository(): ReminderRepository
    fun reminderScheduler(): ReminderScheduler
    fun notificationHelper(): NotificationHelper
}