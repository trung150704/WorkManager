package com.example.workmanager.domain.util

import com.example.workmanager.data.local.ReminderLogEntity
import com.example.workmanager.domain.model.LogStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayStat(val date: LocalDate, val taken: Int, val notTaken: Int)

data class WeeklyStats(
    val days: List<DayStat> = emptyList(),
    val taken: Int = 0,
    val skipped: Int = 0,
    val missed: Int = 0
) {
    val total: Int get() = taken + skipped + missed

    /** Tỉ lệ tuân thủ 0..100, null nếu chưa có dữ liệu. */
    val ratePercent: Int? get() = if (total == 0) null else taken * 100 / total
}

object LogStats {

    private fun priority(status: LogStatus): Int = when (status) {
        LogStatus.TAKEN -> 3
        LogStatus.SKIPPED -> 2
        LogStatus.MISSED -> 1
        LogStatus.SNOOZED -> 0
    }

    /**
     * Một lần nhắc có thể có nhiều dòng log (hoãn rồi uống, bỏ lỡ rồi bấm muộn...).
     * Gộp theo (reminderId, scheduledTime) và lấy trạng thái "cuối cùng" theo độ ưu tiên.
     */
    fun finalLogs(logs: List<ReminderLogEntity>): List<ReminderLogEntity> =
        logs.groupBy { it.reminderId to it.scheduledTime }
            .map { (_, group) -> group.maxBy { entry -> priority(entry.status) } }

    fun weekly(
        logs: List<ReminderLogEntity>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): WeeklyStats {
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        // Lần nhắc mới chỉ bị hoãn, chưa có kết quả cuối: không tính vào tỉ lệ
        val finals = finalLogs(logs).filter { it.status != LogStatus.SNOOZED }
        val byDate = finals.groupBy {
            Instant.ofEpochMilli(it.scheduledTime).atZone(zone).toLocalDate()
        }

        val dayStats = days.map { d ->
            val list = byDate[d].orEmpty()
            DayStat(
                date = d,
                taken = list.count { it.status == LogStatus.TAKEN },
                notTaken = list.count { it.status != LogStatus.TAKEN }
            )
        }
        val window = days.flatMap { byDate[it].orEmpty() }
        return WeeklyStats(
            days = dayStats,
            taken = window.count { it.status == LogStatus.TAKEN },
            skipped = window.count { it.status == LogStatus.SKIPPED },
            missed = window.count { it.status == LogStatus.MISSED }
        )
    }
}