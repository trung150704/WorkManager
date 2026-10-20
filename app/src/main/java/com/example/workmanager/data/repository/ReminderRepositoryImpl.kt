package com.example.workmanager.data.repository

import com.example.workmanager.data.local.ReminderDao
import com.example.workmanager.data.local.ReminderEntity
import com.example.workmanager.data.local.ReminderLogDao
import com.example.workmanager.data.local.ReminderLogEntity
import com.example.workmanager.data.local.ReminderLogItem
import com.example.workmanager.domain.model.LogStatus
import com.example.workmanager.domain.model.ReminderType
import com.example.workmanager.domain.scheduler.ReminderScheduler
import com.example.workmanager.domain.util.ReminderTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao,
    private val logDao: ReminderLogDao,
    private val scheduler: ReminderScheduler
) : ReminderRepository {

    override fun observeAll(): Flow<List<ReminderEntity>> = dao.getAll()

    override fun observe(id: Long): Flow<ReminderEntity?> = dao.getById(id)

    override fun observeLogs(): Flow<List<ReminderLogItem>> = logDao.observeAllWithReminder()

    override suspend fun getActive(): List<ReminderEntity> = dao.getActive()

    override suspend fun add(reminder: ReminderEntity): Long {
        val id = dao.insert(reminder)
        val saved = reminder.copy(id = id)
        if (saved.isActive) scheduler.schedule(saved)
        return id
    }

    override suspend fun update(reminder: ReminderEntity) {
        dao.update(reminder)
        if (reminder.isActive) scheduler.schedule(reminder) else scheduler.cancel(reminder.id)
    }

    override suspend fun delete(reminder: ReminderEntity) {
        dao.delete(reminder)
        scheduler.cancel(reminder.id)
    }

    override suspend fun setActive(id: Long, isActive: Boolean) {
        dao.setActive(id, isActive)
        if (isActive) {
            dao.getById(id).first()?.let { scheduler.schedule(it) }
        } else {
            scheduler.cancel(id)
        }
    }

    override suspend fun recordAction(
        reminderId: Long,
        scheduledTime: Long,
        status: LogStatus
    ): Int? {
        logDao.insert(
            ReminderLogEntity(
                reminderId = reminderId,
                scheduledTime = scheduledTime,
                status = status,
                actedAt = System.currentTimeMillis()
            )
        )
        if (status != LogStatus.TAKEN) return null

        val reminder = dao.getById(reminderId).first() ?: return null
        val qty = reminder.remainingQty
        if (reminder.type != ReminderType.MEDICINE || qty == null || qty <= 0) return null

        val newQty = qty - 1
        // dao.update (không qua scheduler) để không đặt lại lịch
        dao.update(reminder.copy(remainingQty = newQty))
        return newQty
    }

    override suspend fun markMissed(now: Long): Int {
        val windowEnd = now - MISSED_GRACE_MS
        val windowStartLimit = now - LOOKBACK_MS
        var count = 0

        for (reminder in dao.getActive()) {
            // Không tính các lần nhắc trước khi nhắc nhở được tạo
            val from = maxOf(windowStartLimit, reminder.createdAt)
            val occurrences = ReminderTimeUtils.occurrencesBetween(reminder, from, windowEnd)
            if (occurrences.isEmpty()) continue

            val existing = logDao.getForReminderInRange(
                reminder.id, occurrences.first(), occurrences.last()
            )
            // SNOOZED một mình chưa phải kết quả cuối, vẫn coi là chưa xử lý
            val resolved = existing
                .filter { it.status != LogStatus.SNOOZED }
                .map { it.scheduledTime }
                .toSet()

            for (t in occurrences) {
                if (t !in resolved) {
                    logDao.insert(
                        ReminderLogEntity(
                            reminderId = reminder.id,
                            scheduledTime = t,
                            status = LogStatus.MISSED,
                            actedAt = null
                        )
                    )
                    count++
                }
            }
        }
        return count
    }

    companion object {
        /** Chờ 60 phút sau giờ nhắc mới coi là bỏ lỡ. Muốn thử nhanh thì đổi tạm thành 60_000L. */
        private const val MISSED_GRACE_MS = 60 * 60_000L
        private const val LOOKBACK_MS = 7 * 24 * 60 * 60_000L
    }
}