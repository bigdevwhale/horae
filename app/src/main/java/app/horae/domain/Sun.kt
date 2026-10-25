package app.horae.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Sunrise, noon and sunset for a place, by the NOAA approximation; good to a minute or two, no network. */
object Sun {
    data class Day(val rise: Instant?, val noon: Instant, val set: Instant?, val polar: Polar = Polar.NONE)

    enum class Polar { NONE, DAY, NIGHT }

    private fun rad(d: Double) = d * PI / 180
    private fun deg(r: Double) = r * 180 / PI

    fun day(date: LocalDate, lat: Double, lon: Double): Day {
        val g = 2 * PI / 365 * (date.dayOfYear - 1)
        val eq = 229.18 * (0.000075 + 0.001868 * cos(g) - 0.032077 * sin(g) - 0.014615 * cos(2 * g) - 0.040849 * sin(2 * g))
        val decl = 0.006918 - 0.399912 * cos(g) + 0.070257 * sin(g) - 0.006758 * cos(2 * g) + 0.000907 * sin(2 * g) -
            0.002697 * cos(3 * g) + 0.00148 * sin(3 * g)
        val midnight = date.atStartOfDay().toInstant(ZoneOffset.UTC)
        fun at(minutes: Double): Instant = midnight.plusMillis((minutes * 60_000).toLong())
        val noon = at(720 - 4 * lon - eq)
        val x = cos(rad(90.833)) / (cos(rad(lat)) * cos(decl)) - tan(rad(lat)) * tan(decl)
        if (x > 1) return Day(null, noon, null, Polar.NIGHT)
        if (x < -1) return Day(null, noon, null, Polar.DAY)
        val ha = deg(acos(x))
        return Day(at(720 - 4 * (lon + ha) - eq), noon, at(720 - 4 * (lon - ha) - eq))
    }

    /** Where the sun stands in the zodiac on [date], 0 = the first degree of Aries, in degrees. */
    fun longitude(date: LocalDate): Double {
        val d = date.toEpochDay() - 10957.5 // days since J2000
        val l = (280.460 + 0.9856474 * d).mod(360.0)
        val m = rad((357.528 + 0.9856003 * d).mod(360.0))
        return (l + 1.915 * sin(m) + 0.020 * sin(2 * m)).mod(360.0)
    }
}
