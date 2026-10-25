package app.horae.domain

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.roundToInt

/** Where the book is kept, for the sun. */
data class Place(val lat: Double, val lon: Double, val name: String = "")

/**
 * The old reckoning: the light from sunrise to sunset is twelve hours, and so is the dark, so a summer day
 * hour is long and a winter one short. Where the sun doesn't rise or set, it falls back to six and eighteen.
 */
class SunDay(val date: LocalDate, val rise: Long, val set: Long, val prevSet: Long, val nextRise: Long, val polar: Boolean) {
    val dayHour get() = (set - rise) / 12.0
    val nightHour get() = (nextRise - set) / 12.0
    private val prevNightHour get() = (rise - prevSet) / 12.0

    /** The moment of position [pos] in unequal hours after sunrise. */
    fun at(pos: Double): Long = when {
        pos < 0 -> rise + (pos * prevNightHour).toLong()
        pos <= 12 -> rise + (pos * dayHour).toLong()
        else -> set + ((pos - 12) * nightHour).toLong()
    }

    /** The position of [t] in unequal hours after sunrise, from -12 to 24. */
    fun pos(t: Long): Double = when {
        t < rise -> -(rise - t) / prevNightHour
        t <= set -> (t - rise) / dayHour
        else -> 12 + (t - set) / nightHour
    }
}

object Clock {
    private fun edges(date: LocalDate, zone: ZoneId, p: Place): Triple<Long, Long, Boolean> {
        val d = Sun.day(date, p.lat, p.lon)
        if (d.rise != null && d.set != null) return Triple(d.rise.toEpochMilli(), d.set.toEpochMilli(), false)
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        return Triple(start + 6 * 3_600_000L, start + 18 * 3_600_000L, true)
    }

    fun sun(date: LocalDate, zone: ZoneId, p: Place): SunDay {
        val (rise, set, polar) = edges(date, zone, p)
        val prev = edges(date.minusDays(1), zone, p)
        val next = edges(date.plusDays(1), zone, p)
        return SunDay(date, rise, set, prev.second, next.first, polar)
    }

    /** When [h] falls on the page of [date]; null for an hour opened only by hand. */
    fun at(h: Hour, date: LocalDate, zone: ZoneId, p: Place, sun: SunDay = sun(date, zone, p)): Long? = when (h.anchor) {
        Anchor.SUN -> sun.at(h.pos)
        Anchor.CLOCK -> date.atStartOfDay(zone).toInstant().toEpochMilli() + h.clock * 60_000L
        Anchor.HAND -> null
    }

    data class Slot(val hour: Hour, val at: Long?, val date: LocalDate)

    /** The page's hours in the order they fall; hours by hand go last. */
    fun day(hours: List<Hour>, date: LocalDate, zone: ZoneId, p: Place): List<Slot> {
        val sun = sun(date, zone, p)
        return hours.filter { it.on && it.keeps(date) }.map { Slot(it, at(it, date, zone, p, sun), date) }
            .sortedWith(compareBy<Slot> { it.at == null }.thenBy { it.at ?: 0 }.thenBy { it.hour.ord })
    }

    /** The next bell after [now], looking from yesterday's late hours a week ahead. */
    fun next(hours: List<Hour>, now: Long, zone: ZoneId, p: Place): Slot? {
        val today = LocalDate.ofEpochDay(now.day(zone))
        for (i in -1..8) {
            val s = day(hours, today.plusDays(i.toLong()), zone, p).firstOrNull { it.at != null && it.at > now && it.hour.bell != Bell.NONE }
            if (s != null) return s
        }
        return null
    }

    /**
     * Which hour is open at [now]: from its moment until the next hour's, but no longer than three hours.
     * Late night hours of yesterday's page count too.
     */
    fun open(hours: List<Hour>, now: Long, zone: ZoneId, p: Place): Slot? {
        val today = LocalDate.ofEpochDay(now.day(zone))
        val slots = (day(hours, today.minusDays(1), zone, p) + day(hours, today, zone, p) + day(hours, today.plusDays(1), zone, p)).filter { it.at != null }
        for ((i, s) in slots.withIndex()) {
            val end = minOf(slots.getOrNull(i + 1)?.at ?: Long.MAX_VALUE, s.at!! + 3 * 3_600_000L)
            if (now >= s.at && now < end) return s
        }
        return null
    }

    /** The number of the hour, as the bell counts it: Terce rings three. */
    fun number(h: Hour): Int = when (h.anchor) {
        Anchor.SUN -> (floor(h.pos.mod(12.0)).toInt()).let { if (it == 0) 12 else it }
        Anchor.CLOCK -> ((h.clock / 60) % 12).let { if (it == 0) 12 else it }
        Anchor.HAND -> 1
    }

    fun strokes(h: Hour) = if (h.strokes > 0) h.strokes else number(h)

    /** Positions offered for an hour by the sun, every half hour from six night hours before dawn to midnight. */
    val positions: List<Double> = (-12..36).map { it / 2.0 }

    fun round(pos: Double) = (pos * 2).roundToInt() / 2.0
}
