package com.accounts.app.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 金额工具：一律以「分」(Long) 参与运算，仅在展示层格式化 */
object Money {

    fun format(cents: Long): String {
        val nf = NumberFormat.getNumberInstance(Locale.CHINA).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return nf.format(cents.toDouble() / 100.0)
    }

    /** 展示为不带千分位/小数的整元，用于金额输入框实时预览 */
    fun formatPlain(cents: Long): String {
        val yuan = cents / 100
        val fen = cents % 100
        return if (fen == 0L) yuan.toString() else "${yuan}.${(fen / 10)}${(fen % 10)}"
    }

    /** 解析输入文本（允许含 ¥ ￥ 逗号）→ 分 */
    fun parse(text: String): Long {
        val cleaned = text.trim()
            .replace(",", "")
            .replace("¥", "")
            .replace("￥", "")
        if (cleaned.isEmpty()) return 0
        return try {
            BigDecimal(cleaned).movePointRight(2).toLong()
        } catch (e: NumberFormatException) {
            0
        }
    }
}

/** 日期工具：一律用本地时区墙钟时间（改系统时间后旧数据不变，见需求文档 Q1 答复） */
object Days {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun nowMillis(): Long = System.currentTimeMillis()

    fun ofDay(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    /** 某月的 [开始, 结束) 毫秒区间 */
    fun monthRange(month: YearMonth): LongArray =
        longArrayOf(ofDay(month.atDay(1)), ofDay(month.plusMonths(1).atDay(1)))

    fun dayOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** "11月12日 周二" */
    fun dayLabel(millis: Long): String =
        DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINESE).format(dayOf(millis))

    /** "今天 12:30" */
    fun todayTimeLabel(millis: Long): String {
        val date = dayOf(millis)
        val today = LocalDate.now()
        val hhmm = DateTimeFormatter.ofPattern("HH:mm").format(
            Instant.ofEpochMilli(millis).atZone(zone)
        )
        return if (date == today) "今天 $hhmm" else "${DateTimeFormatter.ofPattern("M月d日").format(date)} $hhmm"
    }

    fun monthLabel(month: YearMonth): String =
        DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINESE).format(month)
}
