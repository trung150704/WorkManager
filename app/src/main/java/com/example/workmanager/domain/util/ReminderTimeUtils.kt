package com.example.workmanager.domain.util

import com.example.workmanager.data.local.ReminderEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object ReminderTimeUtils {

    /** bit0 = Thứ 2 ... bit6 = Chủ nhật (DayOfWeek.value: Mon=1..Sun=7) */
    fun daysToMask(days: Set<DayOfWeek>): Int =
        days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

    fun maskToDays(mask: Int): Set<DayOfWeek> =
        DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()

    /**
     * Thời điểm nhắc kế tiếp (epochMillis), LUÔN lớn hơn [now].
     * Trả null nếu: không chọn ngày nào, hoặc đã vượt endDate.
     * Không xét isActive (việc đó do scheduler quyết định).
     */
    fun computeNextTrigger(
        reminder: ReminderEntity,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long? {
        if (reminder.daysOfWeek == 0) return null

        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val start = LocalDate.ofEpochDay(reminder.startDate)
        val firstDay = if (start.isAfter(today)) start else today
        val end = reminder.endDate?.let { LocalDate.ofEpochDay(it) }
        val time = LocalTime.of(reminder.hour, reminder.minute)

        // 8 ngày là đủ để gặp lại mọi thứ trong tuần (kể cả hôm nay đã qua giờ)
        for (i in 0..7) {
            val date = firstDay.plusDays(i.toLong())
            if (end != null && date.isAfter(end)) return null
            if (reminder.daysOfWeek and (1 shl (date.dayOfWeek.value - 1)) == 0) continue

            val triggerMillis = ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()
            if (triggerMillis > now) return triggerMillis
        }
        return null
    }
    /**
     * Mọi thời điểm nhắc trong khoảng (fromExclusive, toInclusive], theo thứ tự tăng dần.
     * Có xét startDate, endDate, daysOfWeek. Dùng để tìm các lần nhắc đã qua mà không có phản hồi.
     */
    fun occurrencesBetween(
        reminder: ReminderEntity,
        fromExclusive: Long,
        toInclusive: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<Long> {
        if (reminder.daysOfWeek == 0 || toInclusive <= fromExclusive) return emptyList()

        val start = LocalDate.ofEpochDay(reminder.startDate)
        val end = reminder.endDate?.let { LocalDate.ofEpochDay(it) }
        val time = LocalTime.of(reminder.hour, reminder.minute)
        val fromDate = Instant.ofEpochMilli(fromExclusive).atZone(zone).toLocalDate()
        val toDate = Instant.ofEpochMilli(toInclusive).atZone(zone).toLocalDate()

        var date = if (start.isAfter(fromDate)) start else fromDate
        val result = mutableListOf<Long>()
        while (!date.isAfter(toDate)) {
            if (end != null && date.isAfter(end)) break
            if (reminder.daysOfWeek and (1 shl (date.dayOfWeek.value - 1)) != 0) {
                val t = ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()
                if (t > fromExclusive && t <= toInclusive) result += t
            }
            date = date.plusDays(1)
        }
        return result
    }
}