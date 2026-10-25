package app.horae.domain

import java.io.InputStream
import kotlin.math.abs

/** Where a reading comes from. */
enum class Source { PSALM, ECCL, DESERT, KEMPIS, STOIC, TAO, DHAMMA, ZEN, SUFI }

data class Reading(val src: Source, val refEn: String, val refRu: String, val en: String, val ru: String) {
    fun ref(ru: Boolean) = if (ru) refRu else refEn
    fun text(ru: Boolean) = if (ru) this.ru else en
}

/** Built-in question banks. */
enum class Bank { MORNING, WORK, NOON, EVENING, DEEP }

data class Question(val bank: Bank, val en: String, val ru: String)

/** The readings and questions shipped with the book (assets/readings.tsv, assets/questions.tsv). */
class Library(val readings: List<Reading>, val questions: List<Question>) {

    /** A reading for this hour of this day: the same all day, a different one tomorrow. */
    fun reading(step: Step, day: Long, salt: Long): Reading? {
        if (step.mode == OWN) {
            if (step.list.isEmpty()) return null
            val t = step.list[pick(day, salt, step.list.size)]
            return Reading(Source.PSALM, "", "", t, t)
        }
        val src = step.mode.split(',').mapNotNull { m -> Source.entries.firstOrNull { it.name == m } }.toSet()
        val pool = if (src.isEmpty()) readings else readings.filter { it.src in src }
        return pool.getOrNull(pick(day, salt, pool.size))
    }

    /** The question asked: the step's own, one of its rotation, or one from a bank. */
    fun question(step: Step, day: Long, salt: Long, ru: Boolean): String {
        if (step.text.isNotBlank()) return step.text
        if (step.list.isNotEmpty()) return step.list[pick(day, salt, step.list.size)]
        val bank = Bank.entries.firstOrNull { it.name == step.mode } ?: Bank.EVENING
        val pool = questions.filter { it.bank == bank }
        val q = pool.getOrNull(pick(day, salt, pool.size)) ?: return ""
        return if (ru) q.ru else q.en
    }

    companion object {
        const val OWN = "OWN"

        fun pick(day: Long, salt: Long, size: Int): Int {
            if (size <= 0) return 0
            var x = day * 6364136223846793005L + salt * 1442695040888963407L
            x = x xor (x ushr 29)
            x *= -4658895280553007687L
            x = x xor (x ushr 32)
            return (abs(x) % size).toInt()
        }

        fun read(readings: InputStream, questions: InputStream): Library {
            val r = readings.bufferedReader().readLines().mapNotNull { line ->
                val f = line.split('\t')
                if (f.size < 5 || line.startsWith("#")) return@mapNotNull null
                val src = Source.entries.firstOrNull { it.name == f[0] } ?: return@mapNotNull null
                Reading(src, f[1], f[2], f[3], f[4])
            }
            val q = questions.bufferedReader().readLines().mapNotNull { line ->
                val f = line.split('\t')
                if (f.size < 3 || line.startsWith("#")) return@mapNotNull null
                val bank = Bank.entries.firstOrNull { it.name == f[0] } ?: return@mapNotNull null
                Question(bank, f[1], f[2])
            }
            return Library(r, q)
        }
    }
}

/** The steps of each ready-made evening review. */
object Reviews {
    fun steps(e: Examen, ru: Boolean): List<String> = when (e) {
        Examen.IGNATIUS -> if (ru) listOf(
            "Побудь в благодарности. За что сегодня?",
            "Попроси ясности: увидеть день как есть, без оправданий и без суда.",
            "Пройди день час за часом. Где была жизнь? Где тяжесть?",
            "Что хочется исправить, а что простить — себе или другому?",
            "С чем идёшь в завтрашний день?",
        ) else listOf(
            "Rest in gratitude. For what, today?",
            "Ask for clear sight: to see the day as it was, without excuses and without a verdict.",
            "Walk through the day hour by hour. Where was life? Where was weight?",
            "What do you want to mend, and what to forgive, in yourself or another?",
            "What do you take into tomorrow?",
        )
        Examen.SENECA -> if (ru) listOf(
            "Какую свою слабость сегодня удалось вылечить?",
            "Какому пороку удалось противостоять?",
            "В чём сегодня стало лучше, чем вчера?",
        ) else listOf(
            "Which of your faults did you heal today?",
            "Which failing did you stand against?",
            "In what are you better than yesterday?",
        )
        Examen.FRANKLIN -> if (ru) listOf("Что хорошего сегодня сделано?", "Что было бы сделать лучше?") else listOf("What good have I done today?", "What could I have done better?")
        Examen.OWN -> emptyList()
    }
}

/** The kind phrases of a loving-kindness sitting and the circle they travel. */
object Metta {
    fun phrases(ru: Boolean) = if (ru) listOf("Пусть тебе будет безопасно.", "Пусть тебе будет спокойно.", "Пусть у тебя будут силы.", "Пусть тебе будет легко.")
    else listOf("May you be safe.", "May you be at peace.", "May you be strong.", "May you live with ease.")

    fun circle(ru: Boolean) = if (ru) listOf("Себе", "Близкому человеку", "Тому, кого едва знаешь", "Тому, с кем трудно", "Всем живым существам")
    else listOf("Yourself", "Someone dear", "Someone you barely know", "Someone difficult", "All living beings")
}
