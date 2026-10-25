package app.horae.ui

import app.horae.domain.Anchor
import app.horae.domain.Bank
import app.horae.domain.Beads
import app.horae.domain.Bell
import app.horae.domain.Breath
import app.horae.domain.Examen
import app.horae.domain.Hour
import app.horae.domain.Hue
import app.horae.domain.Kind
import app.horae.domain.Library
import app.horae.domain.Preset
import app.horae.domain.Sit
import app.horae.domain.Source
import app.horae.domain.Step
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/** The book's words that come from data rather than resources, in both languages. */
class Words(val ru: Boolean) {
    fun t(r: String, e: String) = if (ru) r else e
    val locale: Locale = if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH

    fun plural(n: Int, one: String, few: String, many: String): String {
        val a = abs(n) % 100; val b = a % 10
        return when { a in 11..19 -> many; b == 1 -> one; b in 2..4 -> few; else -> many }
    }

    fun n(n: Int, one: String, few: String, many: String, en1: String, enN: String) = if (ru) "$n ${plural(n, one, few, many)}" else "$n ${if (n == 1) en1 else enN}"

    fun min(m: Int) = t("$m мин", "$m min")
    fun clock(m: Int) = "%d:%02d".format(m / 60, m % 60)

    fun kind(k: Kind) = when (k) {
        Kind.BREATH -> t("Дыхание", "Breathing"); Kind.SIT -> t("Медитация", "Meditation"); Kind.METTA -> t("Метта", "Loving-kindness")
        Kind.BEADS -> t("Чётки", "Beads"); Kind.READ -> t("Чтение", "Reading"); Kind.QUESTION -> t("Вопрос", "Question")
        Kind.THANKS -> t("Благодарность", "Gratitude"); Kind.EXAMEN -> t("Разбор дня", "Review of the day"); Kind.BOWS -> t("Поклоны", "Bows")
        Kind.WALK -> t("Ходьба", "Walking"); Kind.GAZE -> t("Созерцание", "Gazing"); Kind.SILENCE -> t("Молчание", "Silence")
        Kind.CUSTOM -> t("Своя практика", "Own practice")
    }

    fun kindSub(k: Kind) = when (k) {
        Kind.BREATH -> t("квадрат, 4-7-8, ровное, своё", "box, 4-7-8, even, your own")
        Kind.SIT -> t("дыхание, тело, отмечание, открытость", "breath, body, noting, open")
        Kind.METTA -> t("добрые фразы по кругу людей", "kind phrases round a circle of people")
        Kind.BEADS -> t("лестовка, мала, тасбих; кнопки громкости", "lestovka, mala, tasbih; volume keys")
        Kind.READ -> t("псалмы, стоики, Дао, Дхаммапада, свои тексты", "psalms, stoics, Tao, Dhammapada, your own")
        Kind.QUESTION -> t("ответ ложится на страницу", "the answer goes onto the page")
        Kind.THANKS -> t("одна, три или пять строк", "one, three or five lines")
        Kind.EXAMEN -> t("Игнатий, Сенека, Франклин или свои шаги", "Ignatius, Seneca, Franklin or your own steps")
        Kind.BOWS -> t("поясные или земные, счёт касанием", "from the waist or to the ground, tap to count")
        Kind.WALK -> t("медленная ходьба под мерный звук", "slow walking to a steady tick")
        Kind.GAZE -> t("пламя свечи или окно", "a candle flame or a window")
        Kind.SILENCE -> t("телефон экраном вниз", "the phone face down")
        Kind.CUSTOM -> t("свои шаги с таймером", "your own steps with timers")
    }

    fun bell(b: Bell) = when (b) {
        Bell.GREAT -> t("Большой колокол", "Great bell"); Bell.SMALL -> t("Малый колокол", "Small bell"); Bell.HAND -> t("Колокольчик", "Handbell")
        Bell.BOWL -> t("Поющая чаша", "Singing bowl"); Bell.WOOD -> t("Деревянное било", "Wooden board"); Bell.NONE -> t("Без звона", "No bell")
    }

    fun hue(h: Hue) = when (h) {
        Hue.GREEN -> t("Зелёный — рядовое время", "Green — ordinary time"); Hue.VIOLET -> t("Фиолетовый — пост, сосредоточение", "Violet — a fast, a gathering in")
        Hue.WHITE -> t("Белый с золотом — праздник", "White and gold — a feast"); Hue.RED -> t("Красный — огонь, решимость", "Red — fire, resolve")
        Hue.ROSE -> t("Розовый — передышка посреди поста", "Rose — a breath in the middle of a fast")
    }

    fun breath(b: Breath) = when (b) {
        Breath.BOX -> t("Квадрат 4-4-4-4", "Box 4-4-4-4"); Breath.RELAX -> t("4-7-8, успокоение", "4-7-8, settling"); Breath.EVEN -> t("Ровное 5,5", "Even 5.5")
        Breath.CALM -> t("Долгий выдох 4-6", "Long exhale 4-6"); Breath.NADI -> t("Попеременное, нади шодхана", "Alternate nostril"); Breath.OWN -> t("Своё", "Your own")
    }

    fun sit(s: Sit) = when (s) {
        Sit.BREATH -> t("Внимание к дыханию", "Attention to breath"); Sit.BODY -> t("Сканирование тела", "Body scan")
        Sit.NOTING -> t("Отмечание", "Noting"); Sit.OPEN -> t("Открытое внимание", "Open awareness")
    }

    fun beads(b: Beads) = when (b) {
        Beads.LESTOVKA -> t("Лестовка", "Lestovka"); Beads.MALA -> t("Мала, 108", "Mala, 108"); Beads.TASBIH -> t("Тасбих, 99", "Tasbih, 99")
        Beads.ROSARY -> t("Розарий, 5 десятков", "Rosary, five decades"); Beads.PLAIN -> t("Простые", "Plain")
    }

    fun examen(e: Examen) = when (e) {
        Examen.IGNATIUS -> t("Экзамен Игнатия", "Ignatian examen"); Examen.SENECA -> t("Вечерний отчёт Сенеки", "Seneca's evening account")
        Examen.FRANKLIN -> t("Вопрос Франклина", "Franklin's question"); Examen.OWN -> t("Свои шаги", "Your own steps")
    }

    fun source(s: Source) = when (s) {
        Source.PSALM -> t("Псалмы", "Psalms"); Source.ECCL -> t("Екклесиаст", "Ecclesiastes"); Source.DESERT -> t("Отцы пустыни", "Desert fathers")
        Source.KEMPIS -> t("Фома Кемпийский", "Thomas à Kempis"); Source.STOIC -> t("Стоики", "Stoics"); Source.TAO -> t("Дао", "Tao")
        Source.DHAMMA -> t("Дхаммапада", "Dhammapada"); Source.ZEN -> t("Дзен", "Zen"); Source.SUFI -> t("Суфии", "Sufis")
    }

    fun bank(b: Bank) = when (b) {
        Bank.MORNING -> t("Утренние", "Morning"); Bank.WORK -> t("О работе", "On work"); Bank.NOON -> t("Бес полуденный", "The noonday demon")
        Bank.EVENING -> t("Вечерние", "Evening"); Bank.DEEP -> t("Глубокие", "Deep")
    }

    fun preset(p: Preset) = when (p) {
        Preset.BENEDICT -> t("Бенедиктинский", "Benedictine"); Preset.STOIC -> t("Стоический", "Stoic"); Preset.ZEN -> t("Дзенский день", "A zen day")
        Preset.HESYCHAST -> t("Исихастский", "Hesychast"); Preset.METTA -> t("День метты", "A day of metta"); Preset.FIVE -> t("Пять минут", "Five minutes")
        Preset.BLANK -> t("С чистого листа", "A blank page")
    }

    fun presetSub(p: Preset) = when (p) {
        Preset.BENEDICT -> t("семь часов по солнцу: псалмы, благодарность, бес полуденный, свеча, экзамен", "seven hours by the sun: psalms, thanks, the noonday demon, a candle, examen")
        Preset.STOIC -> t("утреннее размышление, полдень, вечерний отчёт Сенеки", "morning reflection, noon, Seneca's evening account")
        Preset.ZEN -> t("дзадзэн на рассвете и вечером, кинхин, деревянное било", "zazen at dawn and evening, kinhin, the wooden board")
        Preset.HESYCHAST -> t("поклоны, лестовка с Иисусовой молитвой, отцы пустыни", "bows, the lestovka with the Jesus prayer, desert fathers")
        Preset.METTA -> t("метта утром и вечером, дыхание в полдень, Дхаммапада", "metta morning and evening, breath at noon, Dhammapada")
        Preset.FIVE -> t("без религии: дыхание, вопрос, три благодарности", "no religion: breathing, a question, three thanks")
        Preset.BLANK -> t("один вечерний час — дальше сами", "one evening hour, and the rest is yours")
    }

    /** Where an hour stands by the sun, in words. */
    fun pos(p: Double): String {
        fun hours(x: Double): String {
            val whole = floor(x).toInt()
            val half = x - whole >= .5
            val num = if (half) (if (whole == 0) "½" else "$whole½") else "$whole"
            return if (ru) "$num ${if (half) "часа" else plural(whole, "час", "часа", "часов")}" else "$num ${if (!half && whole == 1) "hour" else "hours"}"
        }
        return when {
            p == 0.0 -> t("рассвет", "sunrise")
            p == 6.0 -> t("полдень", "noon")
            p == 12.0 -> t("закат", "sunset")
            p == 18.0 -> t("полночь", "midnight")
            p < 0 -> t("за ${hours(-p)} до рассвета", "${hours(-p)} before sunrise")
            p < 12 && p == floor(p) -> t("${ordinal(p.toInt())} час дня", "the ${ordinalEn(p.toInt())} hour")
            p < 12 -> t("${hours(p)} после рассвета", "${hours(p)} after sunrise")
            else -> t("${hours(p - 12)} после заката", "${hours(p - 12)} after sunset")
        }
    }

    fun ordinal(n: Int) = listOf("", "первый", "второй", "третий", "четвёртый", "пятый", "шестой", "седьмой", "восьмой", "девятый", "десятый", "одиннадцатый", "двенадцатый").getOrElse(n) { "$n-й" }
    fun ordinalEn(n: Int) = listOf("", "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth", "eleventh", "twelfth").getOrElse(n) { "${n}th" }

    fun anchor(h: Hour) = when (h.anchor) {
        Anchor.SUN -> pos(h.pos)
        Anchor.CLOCK -> t("в ${clock(h.clock)}", "at ${clock(h.clock)}")
        Anchor.HAND -> t("когда откроешь", "when you open it")
    }

    fun days(mask: Int): String {
        if (mask == 0b1111111) return t("каждый день", "every day")
        if (mask == 0b0011111) return t("по будням", "on weekdays")
        if (mask == 0b1100000) return t("по выходным", "at weekends")
        return (0..6).filter { mask and (1 shl it) != 0 }.joinToString(", ") { weekday(it + 1) }
    }

    fun weekday(dow: Int): String = java.time.DayOfWeek.of(dow).getDisplayName(TextStyle.SHORT, locale)

    fun date(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern(if (ru) "EEEE, d MMMM" else "EEEE, d MMMM", locale))
    fun dateShort(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMMM", locale))
    fun month(d: LocalDate): String = d.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.uppercase() }

    /** What one step amounts to, in a line. */
    fun summary(s: Step, lib: Library? = null): String {
        val parts = mutableListOf(kind(s.kind))
        when (s.kind) {
            Kind.BREATH -> { parts += breath(Breath.of(s.mode)).substringBefore(','); parts += if (s.n > 0) n(s.n, "цикл", "цикла", "циклов", "cycle", "cycles") else min(s.min) }
            Kind.SIT -> { parts += sit(Sit.entries.firstOrNull { it.name == s.mode } ?: Sit.BREATH); parts += min(s.min) }
            Kind.METTA, Kind.GAZE, Kind.SILENCE -> parts += min(s.min)
            Kind.WALK -> parts += min(s.min)
            Kind.BEADS -> { parts += "${s.n}"; if (s.text.isNotBlank()) parts += "«${s.text.take(24)}${if (s.text.length > 24) "…" else ""}»" }
            Kind.READ -> parts += if (s.mode == Library.OWN) t("свои тексты", "your own texts") else s.mode.split(',').mapNotNull { m -> Source.entries.firstOrNull { it.name == m } }.let { if (it.isEmpty()) t("всё подряд", "all of them") else it.joinToString(", ") { x -> source(x) } }
            Kind.QUESTION -> parts += when {
                s.text.isNotBlank() -> "«${s.text.take(30)}${if (s.text.length > 30) "…" else ""}»"
                s.list.isNotEmpty() -> n(s.list.size, "вопрос", "вопроса", "вопросов", "question", "questions")
                else -> bank(Bank.entries.firstOrNull { it.name == s.mode } ?: Bank.EVENING)
            }
            Kind.THANKS -> parts += n(maxOf(1, s.n), "строка", "строки", "строк", "line", "lines")
            Kind.EXAMEN -> parts += examen(Examen.entries.firstOrNull { it.name == s.mode } ?: Examen.IGNATIUS)
            Kind.BOWS -> parts += "${s.n}"
            Kind.CUSTOM -> parts += n(s.list.size, "шаг", "шага", "шагов", "step", "steps")
        }
        return parts.joinToString(" · ")
    }

    fun total(steps: List<Step>): String {
        val m = (steps.sumOf { it.seconds() } + 30) / 60
        return if (m < 1) t("меньше минуты", "under a minute") else t("около $m мин", "about $m min")
    }

    companion object {
        fun roman(n: Int): String {
            val v = listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
            var x = n
            val sb = StringBuilder()
            for ((k, s) in v) while (x >= k) { sb.append(s); x -= k }
            return sb.toString()
        }
    }
}
