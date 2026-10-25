package app.horae.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The kinds of practice an hour can be made of. */
enum class Kind {
    BREATH, SIT, METTA, BEADS, READ, QUESTION, THANKS, EXAMEN, BOWS, WALK, GAZE, SILENCE, CUSTOM;

    /** Practices measured in minutes rather than in counts or words. */
    val timed get() = this == SIT || this == METTA || this == WALK || this == GAZE || this == SILENCE
}

/** How an hour knows when it is: by the sun's unequal hours, by the clock, or only when opened by hand. */
enum class Anchor { SUN, CLOCK, HAND }

/** What rings an hour. */
enum class Bell { GREAT, SMALL, HAND, BOWL, WOOD, NONE }

/** The colour of a season, after the liturgical colours. */
enum class Hue { GREEN, VIOLET, WHITE, RED, ROSE }

/** Breathing patterns: seconds of in, hold, out, hold. */
enum class Breath(val inhale: Double, val hold: Double, val exhale: Double, val rest: Double) {
    BOX(4.0, 4.0, 4.0, 4.0), RELAX(4.0, 7.0, 8.0, 0.0), EVEN(5.5, 0.0, 5.5, 0.0), CALM(4.0, 0.0, 6.0, 0.0), NADI(4.0, 4.0, 4.0, 0.0), OWN(4.0, 2.0, 6.0, 0.0);

    val cycle get() = inhale + hold + exhale + rest

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name } ?: EVEN

        /** "4-7-8-0" as written by hand for [OWN]; anything unreadable falls back to OWN's defaults. */
        fun phases(b: Breath, own: String): DoubleArray {
            if (b != OWN) return doubleArrayOf(b.inhale, b.hold, b.exhale, b.rest)
            val p = own.split('-', ' ', '/').mapNotNull { it.trim().replace(',', '.').toDoubleOrNull() }
            if (p.isEmpty() || p.any { it < 0 || it > 30 } || p.sum() < 2) return doubleArrayOf(OWN.inhale, OWN.hold, OWN.exhale, OWN.rest)
            return DoubleArray(4) { p.getOrElse(it) { 0.0 } }
        }
    }
}

/** What a sitting attends to. */
enum class Sit { BREATH, BODY, NOTING, OPEN }

/** How the beads look and where the heavier marker beads sit. */
enum class Beads(val count: Int, val every: Int) { LESTOVKA(100, 25), MALA(108, 27), TASBIH(99, 33), ROSARY(50, 10), PLAIN(33, 0) }

/** Ready-made orders of the evening review. */
enum class Examen { IGNATIUS, SENECA, FRANKLIN, OWN }

/**
 * One practice inside an hour, with its settings. The fields mean different things for different kinds:
 * [min] minutes (or breathing minutes when [n] is 0), [n] a count (cycles, beads, bows, lines),
 * [mode] the variant (a [Breath], [Sit], [Beads], [Examen] or a reading source), [text] a phrase or question,
 * [list] phrases, questions, steps or own texts, and [every] a marker or an interval bell.
 */
data class Step(
    val kind: Kind,
    val min: Int = 0,
    val n: Int = 0,
    val mode: String = "",
    val text: String = "",
    val list: List<String> = emptyList(),
    val every: Int = 0,
) {
    /** Roughly how long the step takes, for the hour's total. */
    fun seconds(): Int = when (kind) {
        Kind.BREATH -> if (n > 0) (n * Breath.phases(Breath.of(mode), text).sum()).toInt() else min * 60
        Kind.BEADS -> n * 4
        Kind.READ -> 120
        Kind.QUESTION -> 90
        Kind.THANKS -> 30 * maxOf(1, n)
        Kind.EXAMEN -> 60 * maxOf(3, list.size)
        Kind.BOWS -> n * 6
        Kind.CUSTOM -> list.sumOf { Codec.customSeconds(it).coerceAtLeast(20) }
        else -> min * 60
    }
}

/** What was actually done in one practice, as it goes into the book. */
data class Done(
    val kind: Kind,
    val seconds: Int = 0,
    val n: Int = 0,
    val mode: String = "",
    /** Answers, thanks, notes: lines separated by \n. */
    val text: String = "",
    /** The word chosen in lectio, or the reference of a reading. */
    val word: String = "",
)

@Entity(tableName = "hour")
data class Hour(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ord: Int = 0,
    val name: String,
    val anchor: Anchor = Anchor.SUN,
    /** For [Anchor.SUN]: unequal hours after sunrise; 12 is sunset, more is into the night, less than 0 before dawn. */
    val pos: Double = 0.0,
    /** For [Anchor.CLOCK]: minute of the day. */
    val clock: Int = 8 * 60,
    val bell: Bell = Bell.SMALL,
    /** Strokes of the bell; 0 rings the number of the hour. */
    val strokes: Int = 3,
    /** Weekdays it keeps, bit 0 = Monday. */
    val days: Int = 0b1111111,
    val steps: List<Step> = emptyList(),
    /** The short rule for a busy day or the road. */
    val brief: List<Step> = emptyList(),
    val on: Boolean = true,
) {
    fun keeps(date: LocalDate) = days and (1 shl (date.dayOfWeek.value - 1)) != 0
}

@Entity(tableName = "entry")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch day of the page it belongs to. */
    val day: Long,
    val hourId: Long,
    val hourName: String,
    val at: Long,
    val brief: Boolean = false,
    val items: List<Done> = emptyList(),
)

/** A note in the margin of a page whose ink has already dried. */
@Entity(tableName = "gloss")
data class Gloss(@PrimaryKey(autoGenerate = true) val id: Long = 0, val day: Long, val at: Long, val text: String)

/** A red-letter day of one's own, every year. */
@Entity(tableName = "feast")
data class Feast(@PrimaryKey(autoGenerate = true) val id: Long = 0, val month: Int, val dom: Int, val name: String, val since: Int = 0)

/** A season of one's own: forty days of something, coloured. */
@Entity(tableName = "season")
data class Season(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val hue: Hue, val start: Long, val days: Int) {
    operator fun contains(day: Long) = day >= start && day < start + days
}

/** A rule for the whole day (no meat, no feeds), ticked or not. */
@Entity(tableName = "rule")
data class DayRule(@PrimaryKey(autoGenerate = true) val id: Long = 0, val text: String, val on: Boolean = true)

@Entity(tableName = "kept", primaryKeys = ["day", "rule"])
data class Kept(val day: Long, val rule: Long)

@Entity(tableName = "meta")
data class MetaRow(@PrimaryKey val k: String, val v: String)

fun Long.day(zone: ZoneId): Long = Instant.ofEpochMilli(this).atZone(zone).toLocalDate().toEpochDay()
fun Long.date(): LocalDate = LocalDate.ofEpochDay(this)

/**
 * Steps and records stored as text. Separators are control characters, which never come from the keyboard;
 * anything typed has them stripped on the way in.
 */
object Codec {
    private const val F = '\u001F'
    private const val L = '\u001E'
    private const val S = '\u001D'

    fun clean(s: String) = s.filter { it != F && it != L && it != S }

    fun steps(list: List<Step>): String = list.joinToString(S.toString()) { s ->
        listOf(s.kind.name, s.min.toString(), s.n.toString(), clean(s.mode), clean(s.text), s.list.joinToString(L.toString()) { clean(it) }, s.every.toString()).joinToString(F.toString())
    }

    fun steps(s: String): List<Step> = if (s.isEmpty()) emptyList() else s.split(S).mapNotNull { part ->
        val f = part.split(F)
        val kind = Kind.entries.firstOrNull { it.name == f.getOrNull(0) } ?: return@mapNotNull null
        Step(kind, f.int(1), f.int(2), f.getOrElse(3) { "" }, f.getOrElse(4) { "" }, f.getOrElse(5) { "" }.let { if (it.isEmpty()) emptyList() else it.split(L) }, f.int(6))
    }

    fun done(list: List<Done>): String = list.joinToString(S.toString()) { d ->
        listOf(d.kind.name, d.seconds.toString(), d.n.toString(), clean(d.mode), clean(d.text), clean(d.word)).joinToString(F.toString())
    }

    fun done(s: String): List<Done> = if (s.isEmpty()) emptyList() else s.split(S).mapNotNull { part ->
        val f = part.split(F)
        val kind = Kind.entries.firstOrNull { it.name == f.getOrNull(0) } ?: return@mapNotNull null
        Done(kind, f.int(1), f.int(2), f.getOrElse(3) { "" }, f.getOrElse(4) { "" }, f.getOrElse(5) { "" })
    }

    /** A step of an own practice is "text|seconds". */
    fun custom(text: String, seconds: Int) = "${text.replace('|', '/')}|$seconds"
    fun customText(s: String) = s.substringBeforeLast('|', s)
    fun customSeconds(s: String) = s.substringAfterLast('|', "").toIntOrNull() ?: 0

    private fun List<String>.int(i: Int) = getOrNull(i)?.toIntOrNull() ?: 0
}
