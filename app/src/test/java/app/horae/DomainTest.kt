package app.horae

import app.horae.domain.Anchor
import app.horae.domain.Bank
import app.horae.domain.Bell
import app.horae.domain.Breath
import app.horae.domain.Clock
import app.horae.domain.Codec
import app.horae.domain.Done
import app.horae.domain.Hour
import app.horae.domain.Kind
import app.horae.domain.Library
import app.horae.domain.Place
import app.horae.domain.Preset
import app.horae.domain.Presets
import app.horae.domain.Source
import app.horae.domain.Step
import app.horae.domain.Sun
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {
    private val moscow = Place(55.756, 37.617)
    private val msk = ZoneId.of("Europe/Moscow")

    private fun minutes(t: Long, zone: ZoneId) = java.time.Instant.ofEpochMilli(t).atZone(zone).let { it.hour * 60 + it.minute }

    @Test fun sunriseInMoscowAtMidsummerAndMidwinter() {
        val june = Sun.day(LocalDate.of(2026, 6, 21), moscow.lat, moscow.lon)
        val rise = june.rise!!.atZone(msk); val set = june.set!!.atZone(msk)
        assertTrue("rise ${rise.toLocalTime()}", abs(rise.hour * 60 + rise.minute - (3 * 60 + 44)) <= 5)
        assertTrue("set ${set.toLocalTime()}", abs(set.hour * 60 + set.minute - (21 * 60 + 18)) <= 5)
        val dec = Sun.day(LocalDate.of(2026, 12, 21), moscow.lat, moscow.lon)
        val r2 = dec.rise!!.atZone(msk); val s2 = dec.set!!.atZone(msk)
        assertTrue("rise ${r2.toLocalTime()}", abs(r2.hour * 60 + r2.minute - (8 * 60 + 59)) <= 5)
        assertTrue("set ${s2.toLocalTime()}", abs(s2.hour * 60 + s2.minute - (15 * 60 + 58)) <= 5)
    }

    @Test fun polarNightFallsBackToTheClock() {
        val murmansk = Place(68.97, 33.07)
        val day = Sun.day(LocalDate.of(2026, 12, 21), murmansk.lat, murmansk.lon)
        assertEquals(Sun.Polar.NIGHT, day.polar)
        val sun = Clock.sun(LocalDate.of(2026, 12, 21), msk, murmansk)
        assertTrue(sun.polar)
        assertEquals(6 * 60, minutes(sun.rise, msk))
        assertEquals(18 * 60, minutes(sun.set, msk))
    }

    @Test fun summerDayHoursAreLongAndWinterOnesShort() {
        val june = Clock.sun(LocalDate.of(2026, 6, 21), msk, moscow)
        val dec = Clock.sun(LocalDate.of(2026, 12, 21), msk, moscow)
        assertTrue(june.dayHour / 60_000 > 80)
        assertTrue(dec.dayHour / 60_000 < 37)
        // Noon is the sixth hour, sunset the twelfth.
        assertTrue(abs(june.rise + (june.set - june.rise) / 2 - june.at(6.0)) < 1000)
        assertEquals(june.set, june.at(12.0))
        assertEquals(6.0, june.pos(june.at(6.0)), 1e-6)
        assertEquals(13.5, june.pos(june.at(13.5)), 1e-6)
        assertEquals(-3.0, june.pos(june.at(-3.0)), 1e-6)
    }

    @Test fun hoursFallInOrderAndHandHoursGoLast() {
        val hours = listOf(
            Hour(id = 1, name = "Compline", pos = 13.5),
            Hour(id = 2, name = "Lauds", pos = 0.0),
            Hour(id = 3, name = "By hand", anchor = Anchor.HAND),
            Hour(id = 4, name = "Eight", anchor = Anchor.CLOCK, clock = 8 * 60),
        )
        val slots = Clock.day(hours, LocalDate.of(2026, 10, 25), msk, moscow)
        assertEquals(listOf(2L, 4L, 1L, 3L), slots.map { it.hour.id })
        assertNull(slots.last().at)
    }

    @Test fun weekdaysAndOffHoursAreSkipped() {
        val sundays = Hour(id = 1, name = "Vigils", pos = -3.0, days = 0b1000000)
        val off = Hour(id = 2, name = "Off", pos = 6.0, on = false)
        val sunday = LocalDate.of(2026, 10, 25)
        assertEquals(1, Clock.day(listOf(sundays, off), sunday, msk, moscow).size)
        assertEquals(0, Clock.day(listOf(sundays, off), sunday.plusDays(1), msk, moscow).size)
    }

    @Test fun theNextBellSkipsSilentHours() {
        val hours = listOf(Hour(id = 1, name = "Quiet", pos = 3.0, bell = Bell.NONE), Hour(id = 2, name = "Noon", pos = 6.0))
        val morning = ZonedDateTime.of(2026, 10, 25, 7, 0, 0, 0, msk).toInstant().toEpochMilli()
        val next = Clock.next(hours, morning, msk, moscow)
        assertNotNull(next)
        assertEquals(2L, next!!.hour.id)
    }

    @Test fun anHourStaysOpenUntilTheNextOne() {
        val hours = listOf(Hour(id = 1, name = "Terce", pos = 3.0), Hour(id = 2, name = "Sext", pos = 6.0))
        val sun = Clock.sun(LocalDate.of(2026, 10, 25), msk, moscow)
        assertEquals(1L, Clock.open(hours, sun.at(3.0) + 60_000, msk, moscow)?.hour?.id)
        assertEquals(2L, Clock.open(hours, sun.at(6.0) + 60_000, msk, moscow)?.hour?.id)
        assertNull(Clock.open(hours, sun.at(2.0), msk, moscow))
    }

    @Test fun theBellCountsTheHour() {
        assertEquals(3, Clock.number(Hour(name = "", pos = 3.0)))
        assertEquals(12, Clock.number(Hour(name = "", pos = 12.0)))
        assertEquals(6, Clock.strokes(Hour(name = "", pos = 6.0, strokes = 0)))
        assertEquals(2, Clock.strokes(Hour(name = "", pos = 6.0, strokes = 2)))
    }

    @Test fun stepsAndRecordsSurviveTheCodec() {
        val steps = listOf(
            Step(Kind.BEADS, n = 100, every = 25, mode = "LESTOVKA", text = "Господи, помилуй"),
            Step(Kind.METTA, min = 10, list = listOf("May you be safe.", "May you be at peace.")),
            Step(Kind.CUSTOM, text = "Tea", list = listOf(Codec.custom("Pour | carefully", 0), Codec.custom("Drink", 180))),
            Step(Kind.READ, mode = "PSALM,STOIC", n = 1),
        )
        assertEquals(steps, Codec.steps(Codec.steps(steps)))
        val done = listOf(Done(Kind.THANKS, 60, 2, text = "bread\nrain"), Done(Kind.READ, 120, mode = "Ps 23", text = "The Lord…", word = "still"))
        assertEquals(done, Codec.done(Codec.done(done)))
        assertEquals(emptyList<Step>(), Codec.steps(""))
        assertEquals(180, Codec.customSeconds(steps[2].list[1]))
        assertEquals("Pour / carefully", Codec.customText(steps[2].list[0]))
    }

    @Test fun ownBreathingPatternsAreReadOrFallBack() {
        assertEquals(listOf(4.0, 2.0, 6.0, 1.0), Breath.phases(Breath.OWN, "4-2-6-1").toList())
        assertEquals(listOf(4.0, 0.0, 6.0, 0.0), Breath.phases(Breath.OWN, "4 0 6").toList())
        assertEquals(listOf(4.0, 2.0, 6.0, 0.0), Breath.phases(Breath.OWN, "nonsense").toList())
        assertEquals(listOf(4.0, 7.0, 8.0, 0.0), Breath.phases(Breath.RELAX, "1-1-1-1").toList())
    }

    private fun library() = Library.read(File("src/main/assets/readings.tsv").inputStream(), File("src/main/assets/questions.tsv").inputStream())

    @Test fun theLibraryHasEverySourceAndBank() {
        val lib = library()
        for (s in Source.entries) assertTrue("no $s", lib.readings.count { it.src == s } >= 4)
        for (b in Bank.entries) assertTrue("no $b", lib.questions.count { it.bank == b } >= 5)
        assertTrue(lib.readings.all { it.en.isNotBlank() && it.ru.isNotBlank() && it.refEn.isNotBlank() && it.refRu.isNotBlank() })
    }

    @Test fun aReadingStaysAllDayAndKeepsToItsSources() {
        val lib = library()
        val step = Step(Kind.READ, mode = "STOIC,TAO")
        val a = lib.reading(step, 20000, 7)
        assertEquals(a, lib.reading(step, 20000, 7))
        val seen = (20000L..20060L).mapNotNull { lib.reading(step, it, 7) }
        assertTrue(seen.all { it.src == Source.STOIC || it.src == Source.TAO })
        assertTrue(seen.distinct().size > 10)
        assertEquals("mine", lib.reading(Step(Kind.READ, mode = Library.OWN, list = listOf("mine")), 1, 1)?.en)
        assertNull(lib.reading(Step(Kind.READ, mode = Library.OWN), 1, 1))
    }

    @Test fun questionsComeFromTheStepFirst() {
        val lib = library()
        assertEquals("Mine?", lib.question(Step(Kind.QUESTION, text = "Mine?"), 1, 1, false))
        assertTrue(lib.question(Step(Kind.QUESTION, list = listOf("A?", "B?")), 1, 1, false) in listOf("A?", "B?"))
        val noon = lib.question(Step(Kind.QUESTION, mode = Bank.NOON.name), 1, 1, true)
        assertTrue(lib.questions.any { it.bank == Bank.NOON && it.ru == noon })
    }

    @Test fun everyPresetHasNamedHoursWithPractices() {
        for (p in Preset.entries) for (ru in listOf(true, false)) {
            val hours = Presets.hours(p, ru)
            assertTrue(hours.isNotEmpty())
            assertTrue(hours.all { it.name.isNotBlank() && it.steps.isNotEmpty() })
            assertEquals(hours.indices.toList(), hours.map { it.ord })
        }
    }
}
