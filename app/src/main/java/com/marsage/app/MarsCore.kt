package com.marsage.app

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.floor
import kotlin.math.roundToLong

/*
 * MarsAge — ядро пересчёта возраста землянина в марсианский.
 * © 2026 Надиров Илья Никитич. Все права защищены. Санкт-Петербург.
 *
 * Константы: сол = 88 775,244 с; марс. год = 668,5907 сола = 686,9711 земных сут.
 * Дарианский календарь (Гангейл): эпоха — сол 0 «1 Sagittarius 0» (1 марта 1609 UTC),
 * високосные годы: нечётные или кратные 10, кроме кратных 100 и некратных 500;
 * 24 месяца, месяцы 6/12/18/24 — по 27 солов (24-й в високосный год — 28);
 * неделя 7 солов, перезапускается с каждым месяцем. Mars Year: MY1 = 11.04.1955 UTC.
 */
object MarsCore {
    const val SOL_SEC = 88775.244
    const val YEAR_SOLS = 668.5907
    const val YEAR_DAYS = 686.9711

    val EPOCH: Instant = LocalDate.of(1609, 3, 1).atStartOfDay(ZoneOffset.UTC).toInstant()
    val MY1: Instant = LocalDate.of(1955, 4, 11).atStartOfDay(ZoneOffset.UTC).toInstant()
    val MSK: ZoneId = ZoneId.of("Europe/Moscow")

    val MONTHS = listOf(
        "Sagittarius", "Dhanus", "Capricornus", "Makara", "Aquarius", "Kumbha",
        "Pisces", "Mina", "Aries", "Mesha", "Taurus", "Rishabha",
        "Gemini", "Mithuna", "Cancer", "Karka", "Leo", "Simha",
        "Virgo", "Kanya", "Libra", "Tula", "Scorpius", "Vrishika"
    )
    val WEEK = listOf("Sol Solis", "Sol Lunae", "Sol Martis", "Sol Mercurii",
        "Sol Jovis", "Sol Veneris", "Sol Saturni")

    fun isLeap(y: Int) = if (y % 100 == 0 && y % 500 != 0) false else (y % 2 == 1 || y % 10 == 0)

    fun startOfYear(y: Int): Long {
        var s = 0L
        for (i in 1 until y) s += if (isLeap(i)) 669L else 668L
        return s
    }

    fun monthLen(y: Int, m: Int): Int =
        if (m == 6 || m == 12 || m == 18 || m == 24) (if (m == 24 && isLeap(y)) 28 else 27) else 28

    data class DarianDate(val year: Int, val month: Int, val monthName: String,
                          val sol: Long, val weekday: String) {
        override fun toString() = "$sol $monthName $year · $weekday"
    }

    fun darian(t: Instant): DarianDate {
        val secs = Duration.between(EPOCH, t).toMillis() / 1000.0
        val d = floor(secs / SOL_SEC).toLong()
        var y = (d / YEAR_SOLS).toInt()
        while (startOfYear(y + 1) <= d) y++
        while (y > 0 && startOfYear(y) > d) y--
        var doy = d - startOfYear(y)
        var m = 1
        while (doy >= monthLen(y, m)) { doy -= monthLen(y, m); m++ }
        return DarianDate(y, m, MONTHS[m - 1], doy + 1, WEEK[(doy % 7L).toInt()])
    }

    fun marsYear(t: Instant): Pair<Int, Double> {
        val sols = Duration.between(MY1, t).toMillis() / 1000.0 / SOL_SEC
        val y = floor(sols / YEAR_SOLS).toInt() + 1
        return y to (sols - (y - 1) * YEAR_SOLS)
    }

    fun solsBetween(from: Instant, to: Instant): Double =
        Duration.between(from, to).toMillis() / 1000.0 / SOL_SEC

    fun ageYears(birth: Instant, now: Instant) = solsBetween(birth, now) / YEAR_SOLS

    fun fullEarthYears(birth: ZonedDateTime, now: ZonedDateTime): Int {
        var n = 0
        while (!birth.plusYears(n + 1L).isAfter(now)) n++
        return n
    }

    fun birthday(birth: Instant, n: Int): Instant =
        birth.plusSeconds((n * YEAR_SOLS * SOL_SEC).roundToLong())

    fun nextBirthday(birth: Instant, now: Instant): Pair<Int, Instant> {
        val n = floor(ageYears(birth, now)).toInt() + 1
        return n to birthday(birth, n)
    }

    fun solOfDay(birth: Instant, now: Instant): Double {
        val s = solsBetween(birth, now)
        return s - floor(s)
    }

    fun marsClock(frac: Double): Triple<Int, Int, Int> {
        val t = frac * 24.6231 * 3600.0
        return Triple(floor(t / 3600).toInt(), floor(t / 60 % 60).toInt(), floor(t % 60).toInt())
    }

    // Противостояние 19.02.2027 15:44 UTC; сближение 20.02.2027 03:07 МСК (101,4 млн км)
    val OPPOSITION: Instant = ZonedDateTime.of(2027, 2, 19, 15, 44, 0, 0, ZoneOffset.UTC).toInstant()
    val CLOSEST: Instant = ZonedDateTime.of(2027, 2, 20, 3, 7, 0, 0, MSK).toInstant()
    val NEXT_OPP: Instant = ZonedDateTime.of(2029, 3, 25, 5, 0, 0, 0, ZoneOffset.UTC).toInstant()
}
