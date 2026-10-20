package com.example.workmanager.domain.util

import com.example.workmanager.data.local.ReminderLogEntity
import com.example.workmanager.domain.model.LogStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class LogStatsTest {

    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val today = LocalDate.of(2026, 10, 21)

    private fun time(date: LocalDate, hour: Int = 8): Long =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, 0))
            .atZone(zone).toInstant().toEpochMilli()

    private fun log(date: LocalDate, status: LogStatus, reminderId: Long = 1) =
        ReminderLogEntity(
            reminderId = reminderId,
            scheduledTime = time(date),
            status = status
        )

    @Test
    fun takenWinsOverMissedForSameOccurrence() {
        val logs = listOf(
            log(today, LogStatus.MISSED),
            log(today, LogStatus.TAKEN)
        )
        val finals = LogStats.finalLogs(logs)
        assertEquals(1, finals.size)
        assertEquals(LogStatus.TAKEN, finals.first().status)
    }

    @Test
    fun snoozedOnlyIsNotCountedInRate() {
        val stats = LogStats.weekly(listOf(log(today, LogStatus.SNOOZED)), today, zone)
        assertEquals(0, stats.total)
        assertNull(stats.ratePercent)
    }

    @Test
    fun rateIsTakenOverAllResolved() {
        val logs = listOf(
            log(today, LogStatus.TAKEN, reminderId = 1),
            log(today, LogStatus.TAKEN, reminderId = 2),
            log(today.minusDays(1), LogStatus.MISSED, reminderId = 1)
        )
        val stats = LogStats.weekly(logs, today, zone)
        assertEquals(2, stats.taken)
        assertEquals(1, stats.missed)
        assertEquals(66, stats.ratePercent)
    }

    @Test
    fun weeklyHasSevenDaysEndingToday() {
        val stats = LogStats.weekly(emptyList(), today, zone)
        assertEquals(7, stats.days.size)
        assertEquals(today, stats.days.last().date)
        assertEquals(today.minusDays(6), stats.days.first().date)
    }

    @Test
    fun logsOlderThanSevenDaysAreIgnored() {
        val old = log(today.minusDays(10), LogStatus.TAKEN)
        assertEquals(0, LogStats.weekly(listOf(old), today, zone).total)
    }
}