package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FormatUtils {
    private val localeId = Locale("id", "ID")
    private val wibZone: TimeZone = TimeZone.getTimeZone("Asia/Jakarta")

    fun formatRupiah(amount: Long): String {
        val nf = NumberFormat.getNumberInstance(localeId)
        val prefix = if (amount < 0) "-Rp" else "Rp"
        return "$prefix${nf.format(kotlin.math.abs(amount))}"
    }

    fun formatAttendanceCode(sequenceNumber: Long): String {
        return "FNC" + sequenceNumber.toString().padStart(4, '0')
    }

    fun formatDayName(millis: Long): String {
        val cal = Calendar.getInstance(wibZone).apply { timeInMillis = millis }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Minggu"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> "-"
        }
    }

    fun formatIndonesianDate(millis: Long): String {
        val cal = Calendar.getInstance(wibZone).apply { timeInMillis = millis }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = when (cal.get(Calendar.MONTH)) {
            0 -> "Januari"
            1 -> "Februari"
            2 -> "Maret"
            3 -> "April"
            4 -> "Mei"
            5 -> "Juni"
            6 -> "Juli"
            7 -> "Agustus"
            8 -> "September"
            9 -> "Oktober"
            10 -> "November"
            else -> "Desember"
        }
        val year = cal.get(Calendar.YEAR)
        return "$day $month $year"
    }

    fun formatTimeWib(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", localeId).apply { timeZone = wibZone }
        return "${sdf.format(Date(millis))} WIB"
    }

    fun formatDateTimeWib(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", localeId).apply { timeZone = wibZone }
        return "${sdf.format(Date(millis))} WIB"
    }

    fun formatDateKey(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = wibZone }
        return sdf.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM", localeId).apply { timeZone = wibZone }
        return sdf.format(Date(millis))
    }

    fun formatCompactDateForCode(millis: Long): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = wibZone }
        return sdf.format(Date(millis))
    }

    fun startOfDayMillis(millis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance(wibZone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun endOfDayMillis(millis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance(wibZone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun daysAgoStartMillis(daysAgo: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance(wibZone).apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
