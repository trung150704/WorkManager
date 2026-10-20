package com.example.workmanager.domain.util

import com.example.workmanager.data.local.ReminderEntity
import com.example.workmanager.domain.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderTimeUtilsTest {

    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    // 2026-10-19 là Thứ 2
    private val monday = LocalDate.of(2026, 10, 19)

    private fun millis(date: LocalDate, h: Int, m: Int = 0): Long =
        LocalDateTime.of(date, LocalTime.of(h, m))
            .atZone(zone).toInstant().toEpochMilli()

    private fun reminder(
        mask: Int = ReminderTimeUtils.daysToMask(DayOfWeek.entries.toSet()),
        hour: Int = 8,
        minute: Int = 0,
        start: LocalDate = monday,
        end: LocalDate? = null
    ) = ReminderEntity(
        title = "Test",
        type = ReminderType.MEDICINE,
        hour = hour,
        minute = minute,
        daysOfWeek = mask,
        startDate = start.toEpochDay(),
        endDate = end?.toEpochDay()
    )

    @Test
    fun sameDay_notPassedYet() {
        val now = millis(monday, 7)
        val result = ReminderTimeUtils.computeNextTrigger(reminder(), now, zone)
        assertEquals(millis(monday, 8), result)
    }

    @Test
    fun sameDay_alreadyPassed_goesToTomorrow() {
        val now = millis(monday, 9)
        val result = ReminderTimeUtils.computeNextTrigger(reminder(), now, zone)
        assertEquals(millis(monday.plusDays(1), 8), result)
    }

    @Test
    fun skipsToNextSelectedDay() {
        val mask = ReminderTimeUtils.daysToMask(setOf(DayOfWeek.WEDNESDAY))
        val now = millis(monday, 9)
        val result = ReminderTimeUtils.computeNextTrigger(reminder(mask = mask), now, zone)
        assertEquals(millis(monday.plusDays(2), 8), result)
    }

    @Test
    fun sameWeekdayAlreadyPassed_goesToNextWeek() {
        val mask = ReminderTimeUtils.daysToMask(setOf(DayOfWeek.MONDAY))
        val now = millis(monday, 9)
        val result = ReminderTimeUtils.computeNextTrigger(reminder(mask = mask), now, zone)
        assertEquals(millis(monday.plusDays(7), 8), result)
    }

    @Test
    fun afterEndDate_returnsNull() {
        val now = millis(monday, 9)
        val result = ReminderTimeUtils.computeNextTrigger(reminder(end = monday), now, zone)
        assertNull(result)
    }

    @Test
    fun noDaysSelected_returnsNull() {
        val now = millis(monday, 7)
        val result = ReminderTimeUtils.computeNextTrigger(reminder(mask = 0), now, zone)
        assertNull(result)
    }

    @Test
    fun startDateInFuture_waitsForStartDate() {
        val start = monday.plusDays(3)
        val now = millis(monday, 7)
        val result = ReminderTimeUtils.computeNextTrigger(reminder(start = start), now, zone)
        assertEquals(millis(start, 8), result)
    }

    @Test
    fun bitmask_roundTrip() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
        val mask = ReminderTimeUtils.daysToMask(days)
        assertEquals(0b1010001, mask)
        assertEquals(days, ReminderTimeUtils.maskToDays(mask))
    }
    @Test
    fun occurrencesBetween_everyDay_listsAllInOrder() {
        val from = millis(monday, 0)
        val to = millis(monday.plusDays(2), 23)
        val result = ReminderTimeUtils.occurrencesBetween(reminder(), from, to, zone)
        assertEquals(
            listOf(
                millis(monday, 8),
                millis(monday.plusDays(1), 8),
                millis(monday.plusDays(2), 8)
            ),
            result
        )
    }

    @Test
    fun occurrencesBetween_fromIsExclusive() {
        val from = millis(monday, 8)   // đúng giờ nhắc đầu tiên
        val to = millis(monday.plusDays(1), 23)
        val result = ReminderTimeUtils.occurrencesBetween(reminder(), from, to, zone)
        assertEquals(listOf(millis(monday.plusDays(1), 8)), result)
    }

    @Test
    fun occurrencesBetween_respectsEndDate() {
        val to = millis(monday.plusDays(10), 23)
        val result = ReminderTimeUtils.occurrencesBetween(
            reminder(end = monday.plusDays(1)), millis(monday, 0), to, zone
        )
        assertEquals(2, result.size)
    }

    @Test
    fun occurrencesBetween_respectsDaysMask() {
        val mask = ReminderTimeUtils.daysToMask(setOf(DayOfWeek.WEDNESDAY))
        val result = ReminderTimeUtils.occurrencesBetween(
            reminder(mask = mask), millis(monday, 0), millis(monday.plusDays(6), 23), zone
        )
        assertEquals(listOf(millis(monday.plusDays(2), 8)), result)
    }
}