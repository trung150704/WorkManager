package com.example.workmanager.di

import android.content.Context
import androidx.room.Room
import com.example.workmanager.data.local.AppDatabase
import com.example.workmanager.data.local.ReminderDao
import com.example.workmanager.data.local.ReminderLogDao
import com.example.workmanager.data.repository.ReminderRepository
import com.example.workmanager.data.repository.ReminderRepositoryImpl
import com.example.workmanager.domain.scheduler.ReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.workmanager.domain.scheduler.WorkManagerReminderScheduler

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "nhacviec.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideReminderLogDao(db: AppDatabase): ReminderLogDao = db.reminderLogDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingModule {

    @Binds
    @Singleton
    abstract fun bindRepository(impl: ReminderRepositoryImpl): ReminderRepository

    // Tuần 2: đổi sang WorkManagerReminderScheduler
    @Binds
    @Singleton
    abstract fun bindScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler
}