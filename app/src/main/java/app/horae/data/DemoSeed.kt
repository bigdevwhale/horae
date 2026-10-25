package app.horae.data

import app.horae.AppContainer
import app.horae.domain.Breath
import app.horae.domain.Clock
import app.horae.domain.DayRule
import app.horae.domain.Done
import app.horae.domain.Entry
import app.horae.domain.Examen
import app.horae.domain.Feast
import app.horae.domain.Gloss
import app.horae.domain.Hue
import app.horae.domain.Kept
import app.horae.domain.Kind
import app.horae.domain.Preset
import app.horae.domain.Reviews
import app.horae.domain.Season
import app.horae.domain.day
import java.time.LocalDate
import java.time.ZoneId
import app.horae.localized
import app.horae.ru
import kotlin.random.Random

/** Six weeks of a Benedictine rule kept the way a person keeps it: mostly, not always. */
object DemoSeed {
    private val answersRu = listOf(
        "Дописать главу и не открывать почту до обеда.", "Скучно разбирать счета. Но после станет легко.", "Хочется на море. А здесь — окно и клён.",
        "Позвонить отцу.", "Отчёт для Лены, честно и без спешки.", "Тело тяжёлое, но голова ясная.", "Ленту. Она съедает утро.",
        "Медленно, медленно — и всё успелось.", "Дождь весь день, и это хорошо.", "Кот весь вечер спал на коленях.",
    )
    private val answersEn = listOf(
        "Finish the chapter and not open mail before lunch.", "Sorting the bills is dull. Afterwards it'll be light.", "I want the sea. Here: a window and a maple.",
        "Call my father.", "The report for Lena, honestly and without rushing.", "The body heavy, the head clear.", "The feed. It eats the morning.",
        "Slowly, slowly, and everything got done.", "Rain all day, and that's good.", "The cat slept on my lap while I worked.",
    )
    private val thanksRu = listOf("Тёплый хлеб утром", "Звонок от сестры", "Солнце на подоконнике", "Тихая дорога домой", "Что колено не болит", "Смех в трамвае", "Первый снег", "Хорошая книга", "Суп у мамы", "Ровный день")
    private val thanksEn = listOf("Warm bread in the morning", "A call from my sister", "Sun on the windowsill", "A quiet way home", "My knee not hurting", "Laughter on the tram", "The first snow", "A good book", "Mum's soup", "An even day")

    suspend fun seedIfEmpty(c: AppContainer) {
        if (c.repo.refresh().entries.isNotEmpty()) return
        val ru = c.app.localized().ru
        val zone = ZoneId.systemDefault()
        val place = Cities.default
        c.settings.update { it.copy(onboarded = true, place = place) }
        c.repo.preset(Preset.BENEDICT, ru)
        val s0 = c.repo.refresh()
        val today = System.currentTimeMillis().day(zone)
        val r = Random(42)
        c.repo.save(DayRule(text = if (ru) "Без ленты новостей" else "No news feed"))
        c.repo.save(DayRule(text = if (ru) "Пешком до работы" else "Walk to work"))
        c.repo.save(Season(name = if (ru) "Сорок дней тишины" else "Forty days of quiet", hue = Hue.VIOLET, start = today - 11, days = 40))
        val d = LocalDate.ofEpochDay(today)
        c.repo.save(Feast(month = d.monthValue, dom = (d.dayOfMonth - 3).coerceAtLeast(1), name = if (ru) "День рождения Ани" else "Anya's birthday"))
        c.repo.save(Feast(month = d.plusMonths(1).monthValue, dom = 2, name = if (ru) "Год без сигарет" else "A year without cigarettes"))
        val rules = c.repo.refresh().rules

        val keep = mapOf(0 to .5, 1 to .85, 2 to .6, 3 to .7, 4 to .5, 5 to .75, 6 to .85)
        val entries = ArrayList<Entry>()
        val kept = ArrayList<Kept>()
        val now = System.currentTimeMillis()
        for (day in today - 44..today) {
            val date = LocalDate.ofEpochDay(day)
            val slots = Clock.day(s0.hours, date, zone, place)
            for (slot in slots) {
                val at = slot.at ?: continue
                if (at > now) continue
                // Today's afternoon hour is missed, so the page shows a blank.
                if (day == today && slot.hour.ord == 4) continue
                if (r.nextDouble() > (keep[slot.hour.ord] ?: .6)) continue
                val brief = slot.hour.brief.isNotEmpty() && r.nextDouble() < .15
                val steps = if (brief) slot.hour.brief else slot.hour.steps
                val items = steps.mapIndexed { i, st ->
                    when (st.kind) {
                        Kind.READ -> c.library.reading(st, day, s0.salt + slot.hour.id * 31 + i).let { rd ->
                            Done(Kind.READ, 120, mode = rd?.ref(ru) ?: "", text = rd?.text(ru) ?: "", word = if (r.nextDouble() < .3) rd?.text(ru)?.split(' ')?.filter { it.length > 4 }?.randomOrNull(r)?.trim(',', '.', ';', '—') ?: "" else "")
                        }
                        Kind.QUESTION -> Done(Kind.QUESTION, 90, text = (if (ru) answersRu else answersEn).random(r), word = c.library.question(st, day, s0.salt + slot.hour.id * 31 + i, ru))
                        Kind.THANKS -> Done(Kind.THANKS, 60, st.n, text = (if (ru) thanksRu else thanksEn).shuffled(r).take(maxOf(1, st.n)).joinToString("\n"))
                        Kind.EXAMEN -> {
                            val e = Examen.entries.firstOrNull { it.name == st.mode } ?: Examen.IGNATIUS
                            val notes = Reviews.steps(e, ru).indices.mapNotNull { if (r.nextDouble() < .4) (if (ru) answersRu else answersEn).random(r) else null }
                            Done(Kind.EXAMEN, 300, Reviews.steps(e, ru).size, e.name, notes.joinToString("\n"))
                        }
                        Kind.BREATH -> Done(Kind.BREATH, st.min * 60, (st.min * 60 / Breath.of(st.mode).cycle).toInt(), st.mode)
                        else -> Done(st.kind, st.min * 60, st.n, st.mode)
                    }
                }
                entries += Entry(day = day, hourId = slot.hour.id, hourName = slot.hour.name, at = at + r.nextLong(2, 25) * 60_000L, brief = brief, items = items)
            }
            for (rule in rules) if (r.nextDouble() < .7) kept += Kept(day, rule.id)
        }
        val glosses = listOf(
            Gloss(day = today - 30, at = now - 3 * 86_400_000L, text = if (ru) "Смешно читать: тогда казалось, что это навсегда." else "Funny to read now: back then it felt like for ever."),
            Gloss(day = today - 17, at = now - 86_400_000L, text = if (ru) "Вот здесь всё и началось." else "This is where it all began."),
        )
        c.repo.seed(entries, glosses, kept)
    }
}
