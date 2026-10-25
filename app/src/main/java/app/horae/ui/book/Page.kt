package app.horae.ui.book

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.horae.domain.Beads
import app.horae.domain.Breath
import app.horae.domain.Done
import app.horae.domain.Entry
import app.horae.domain.Examen
import app.horae.domain.Kind
import app.horae.domain.Sit
import app.horae.ui.Home
import app.horae.ui.Words
import app.horae.ui.art.Drollery
import app.horae.ui.art.Illum
import app.horae.ui.theme.C
import app.horae.ui.theme.Garamond
import app.horae.ui.theme.T
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/** How richly a page is illuminated: 0 red ink, 1 lapis, 2 gold; -1 for a page with nothing kept. */
fun Home.level(day: Long): Int {
    val kept = s.on(day).map { it.hourId }.distinct().size
    if (kept == 0) return -1
    val planned = slots(LocalDate.ofEpochDay(day)).count { it.at != null }.coerceAtLeast(1)
    val r = kept.toFloat() / planned
    return when { r < .34f -> 0; r < .67f -> 1; else -> 2 }
}

/** Which creatures come to a page's margin: chosen by what was written there, or by the page's own lot. */
fun Home.drolleries(day: Long): List<Drollery> {
    val entries = s.on(day)
    if (entries.isEmpty()) return emptyList()
    val text = entries.flatMap { e -> e.items.map { it.text + " " + it.word } }.joinToString(" ").lowercase()
    val out = mutableListOf<Drollery>()
    // The noonday demon visits a page whose noon hour was kept.
    if (entries.any { e -> s.hours.firstOrNull { it.id == e.hourId }?.let { it.anchor == app.horae.domain.Anchor.SUN && it.pos == 6.0 } == true }) out += Drollery.DEMON
    val words = listOf(
        Drollery.HARE to listOf("работ", "дела", "спеш", "бег", "work", "rush", "busy", "job"),
        Drollery.BIRD to listOf("небо", "утр", "птиц", "свобод", "лёгк", "sky", "morning", "bird", "free", "light"),
        Drollery.FISH to listOf("вод", "море", "дожд", "река", "плав", "water", "sea", "rain", "river", "swim"),
        Drollery.CAT to listOf("дом", "кот", "сон", "отдых", "тепл", "home", "cat", "sleep", "rest", "warm"),
        Drollery.SNAIL to listOf("медлен", "устал", "тихо", "терпен", "slow", "tired", "quiet", "patien"),
    )
    for ((d, keys) in words) if (out.size < 2 && keys.any { it in text }) out += d
    if (out.size < 2) {
        val pool = listOf(Drollery.SNAIL, Drollery.HARE, Drollery.BIRD, Drollery.FISH, Drollery.CAT).filter { it !in out }
        out += pool[(abs(day * 7919 + s.salt) % pool.size).toInt()]
    }
    return out.take(2)
}

private val HM = DateTimeFormatter.ofPattern("H:mm")

/** One page of the book: the day's kept hours written out, blanks for the missed ones, and its margins. */
@Composable
fun Page(home: Home, day: Long, modifier: Modifier = Modifier, glossEditor: @Composable () -> Unit = {}) {
    val w = home.w
    val s = home.s
    val date = LocalDate.ofEpochDay(day)
    val entries = s.on(day).sortedBy { it.at }
    val level = home.level(day)
    val season = s.season(day)
    val feast = s.feast(date)
    val keptIds = entries.map { it.hourId }.toSet()
    val missed = if (day < home.today || day == home.today) home.slots(date).filter { it.at != null && it.at < home.now && it.hour.id !in keptIds } else emptyList()
    val drolls = home.drolleries(day)

    Box(
        modifier.fillMaxWidth().clipToBounds().drawBehind {
            // Ruled frame, double, in red ink; the season's colour runs along the top.
            val m = 10.dp.toPx()
            drawRect(C.vermilion.copy(alpha = .55f), Offset(m, m), androidx.compose.ui.geometry.Size(size.width - 2 * m, size.height - 2 * m), style = Stroke(1.2.dp.toPx()))
            drawRect(C.vermilion.copy(alpha = .35f), Offset(m + 4.dp.toPx(), m + 4.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 2 * m - 8.dp.toPx(), size.height - 2 * m - 8.dp.toPx()), style = Stroke(.8.dp.toPx()))
            if (season != null) drawRect(C.hue(season.hue).copy(alpha = .8f), Offset(m, m), androidx.compose.ui.geometry.Size(size.width - 2 * m, 5.dp.toPx()))
            // The vine down the outer margin.
            if (level >= 0) Illum.vine(this, size.width - 24.dp.toPx(), 60.dp.toPx(), size.height - 90.dp.toPx(), 22.dp.toPx(), (level + 1) / 3f, day.toInt())
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 44.dp, top = 22.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Words.roman(date.dayOfMonth), style = T.Title.copy(color = C.vermilion, fontSize = 30.sp))
                Text("  " + w.date(date).replaceFirstChar { it.uppercase() }, style = T.Rubric.copy(color = C.inkSoft, fontSize = 14.sp), modifier = Modifier.padding(bottom = 5.dp))
            }
            if (feast != null) Text(feast.name, style = T.Heading.copy(color = C.vermilion, fontSize = 20.sp))
            if (season != null) Text(season.name + " · " + w.t("день ${day - season.start + 1}", "day ${day - season.start + 1}"), style = T.Small.copy(color = C.hue(season.hue)))
            Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(C.rule))

            if (entries.isEmpty() && missed.isEmpty()) {
                Text(
                    if (day == home.today) w.t("Страница ещё чиста. Чернила ждут первого часа.", "The page is still clean. The ink waits for the first hour.")
                    else w.t("В этот день книгу не открывали.", "The book wasn't opened this day."),
                    style = T.Italic.copy(color = C.faded), modifier = Modifier.padding(vertical = 18.dp),
                )
            }
            for ((i, e) in entries.withIndex()) EntryBlock(home, e, if (i == 0) level else -1)
            for (m in missed) Lacuna(m.hour.name, m.at?.let { Instant.ofEpochMilli(it).atZone(home.zone).format(HM) } ?: "")

            val rules = s.rules.filter { s.kept(day, it.id) }
            if (rules.isNotEmpty()) {
                Text(w.t("СОБЛЮДЕНО: ", "KEPT: ") + rules.joinToString(" · ") { it.text }, style = T.Rubric.copy(color = C.malachite, letterSpacing = .6.sp), modifier = Modifier.padding(top = 10.dp))
            }
            if (s.silenceBroken(day)) Text(w.t("молчание нарушено", "silence broken"), style = T.Small.copy(fontStyle = FontStyle.Italic), modifier = Modifier.padding(top = 4.dp))

            val gl = s.glosses[day].orEmpty()
            if (gl.isNotEmpty()) {
                Text(w.t("ГЛОССЫ", "GLOSSES"), style = T.Rubric, modifier = Modifier.padding(top = 14.dp, bottom = 2.dp))
                for (g in gl) {
                    val on = Instant.ofEpochMilli(g.at).atZone(home.zone).toLocalDate()
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = C.vermilion)) { append("¶ ") }
                            append(g.text)
                            withStyle(SpanStyle(color = C.faded, fontSize = 13.sp)) { append("  — " + w.dateShort(on)) }
                        },
                        style = T.Italic.copy(fontSize = 16.sp, color = C.inkSoft), modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
            glossEditor()

            if (drolls.isNotEmpty()) {
                Canvas(Modifier.fillMaxWidth().height(70.dp).padding(top = 8.dp)) {
                    val ground = size.height - 4.dp.toPx()
                    drawLine(C.inkSoft.copy(alpha = .5f), Offset(0f, ground), Offset(size.width, ground), 1.dp.toPx())
                    val sz = 46.dp.toPx()
                    Illum.drollery(this, drolls[0], Offset(size.width * .22f, ground), sz)
                    if (drolls.size > 1) Illum.drollery(this, drolls[1], Offset(size.width * .72f, ground), sz, flip = true)
                }
            }
        }
    }
}

@Composable
private fun EntryBlock(home: Home, e: Entry, level: Int) {
    val w = home.w
    val time = Instant.ofEpochMilli(e.at).atZone(home.zone).format(HM)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 6.dp)) {
        // The left margin carries the marks of the practices.
        Box(Modifier.width(28.dp).fillMaxHeight()) {
            Canvas(Modifier.fillMaxHeight().width(28.dp)) {
                val u = 22.dp.toPx()
                var y = 4.dp.toPx()
                for (d in e.items.filter { it.kind !in setOf(Kind.QUESTION) }.distinctBy { it.kind }.take(4)) {
                    if (y > size.height - u * .4f) break
                    Illum.trace(this, d.kind, Offset(size.width / 2, y), u, d.n, (d.seconds + 30) / 60)
                    y += u * 1.25f
                }
            }
        }
        Column(Modifier.weight(1f).padding(start = 6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                if (level >= 0) {
                    Initial(e.hourName.first().uppercaseChar(), level)
                    Column(Modifier.padding(start = 10.dp)) { Head(e, time, w); Body(home, e.items) }
                } else Column { Head(e, time, w); Body(home, e.items) }
            }
        }
    }
}

@Composable
private fun Head(e: Entry, time: String, w: Words) {
    Text(
        e.hourName.uppercase() + "  " + time + if (e.brief) w.t(" · кратко", " · short") else "",
        style = T.Rubric.copy(fontSize = 13.sp),
    )
}

/** A decorated capital starting the page's writing. */
@Composable
private fun Initial(letter: Char, level: Int) {
    Box(Modifier.size(54.dp).padding(top = 2.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(50.dp)) { Illum.initialGround(this, Rect(0f, 0f, size.width, size.height), level) }
        Text(
            letter.toString(),
            style = T.Title.copy(
                fontFamily = Garamond, fontSize = 40.sp, lineHeight = 40.sp, textAlign = TextAlign.Center,
                color = when (level) { 0 -> C.vermilion; 1 -> C.vellum; else -> C.lapisDeep },
            ),
        )
    }
}

@Composable
private fun Body(home: Home, items: List<Done>) {
    val w = home.w
    for (d in items) {
        when (d.kind) {
            Kind.READ -> {
                Text(
                    buildAnnotatedString {
                        append("«${d.text}»")
                        if (d.mode.isNotEmpty()) withStyle(SpanStyle(color = C.faded, fontSize = 13.sp, fontStyle = FontStyle.Normal)) { append("  ${d.mode}") }
                    },
                    style = T.Italic.copy(fontSize = 16.sp, lineHeight = 21.sp, color = C.inkSoft), modifier = Modifier.padding(vertical = 2.dp),
                )
                if (d.word.isNotEmpty()) Text(w.t("слово: ", "the word: ") + d.word, style = T.Body.copy(color = C.vermilion, fontSize = 16.sp))
            }
            Kind.QUESTION -> {
                Text(d.word, style = T.Italic.copy(fontSize = 15.sp, lineHeight = 19.sp, color = C.faded))
                if (d.text.isNotEmpty()) Text(d.text, style = T.Body.copy(fontSize = 17.sp, lineHeight = 22.sp), modifier = Modifier.padding(bottom = 2.dp))
            }
            Kind.THANKS -> for (l in d.text.split('\n').filter { it.isNotBlank() }) Text(
                buildAnnotatedString { withStyle(SpanStyle(color = C.gold)) { append("❦ ") }; append(l) },
                style = T.Body.copy(fontSize = 17.sp, lineHeight = 22.sp),
            )
            Kind.EXAMEN -> {
                Text(w.examen(Examen.entries.firstOrNull { it.name == d.mode } ?: Examen.IGNATIUS), style = T.Small.copy(fontStyle = FontStyle.Italic))
                for (l in d.text.split('\n').filter { it.isNotBlank() }) Text(l, style = T.Body.copy(fontSize = 17.sp, lineHeight = 22.sp))
            }
            else -> Text(line(w, d), style = T.Body.copy(fontSize = 16.sp, color = C.inkSoft))
        }
    }
    if (items.isEmpty()) Text(w.t("час отмечен", "the hour marked"), style = T.Italic.copy(fontSize = 16.sp, color = C.faded))
}

/** One line for a practice measured rather than written. */
fun line(w: Words, d: Done): String {
    val m = (d.seconds + 30) / 60
    val mins = if (m > 0) w.min(m) else w.t("меньше минуты", "under a minute")
    return when (d.kind) {
        Kind.BREATH -> w.kind(d.kind) + " · " + w.breath(Breath.of(d.mode)).substringBefore(',') + " · " + w.n(d.n, "цикл", "цикла", "циклов", "cycle", "cycles")
        Kind.SIT -> w.kind(d.kind) + " · " + w.sit(Sit.entries.firstOrNull { it.name == d.mode } ?: Sit.BREATH).lowercase() + " · " + mins
        Kind.BEADS -> w.beads(Beads.entries.firstOrNull { it.name == d.mode } ?: Beads.PLAIN) + " · ${d.n}" + if (d.text.isNotBlank()) " · «${d.text}»" else ""
        Kind.BOWS -> w.kind(d.kind) + " · ${d.n}"
        Kind.CUSTOM -> (d.text.ifBlank { w.kind(d.kind) }) + " · " + mins
        else -> w.kind(d.kind) + " · " + mins
    }
}

/** A missed hour: its name in faded red and an empty space, as a scribe left room for what never came. */
@Composable
private fun Lacuna(name: String, time: String) {
    Column(Modifier.fillMaxWidth().padding(start = 34.dp, top = 6.dp, bottom = 6.dp)) {
        Text("${name.uppercase()}  $time", style = T.Rubric.copy(color = C.vermilion.copy(alpha = .35f), fontSize = 13.sp))
        Canvas(Modifier.fillMaxWidth().height(22.dp)) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx()))
            for (y in listOf(.35f, .8f)) drawLine(C.rule, Offset(0f, size.height * y), Offset(size.width * (if (y > .5f) .6f else 1f), size.height * y), 1.dp.toPx(), pathEffect = dash)
        }
    }
}
