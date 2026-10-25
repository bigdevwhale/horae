package app.horae.ui.book

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.horae.container
import app.horae.domain.Gloss
import app.horae.domain.Kind
import app.horae.domain.Sun
import app.horae.ui.Home
import app.horae.ui.Words
import app.horae.ui.art.Illum
import app.horae.ui.components.Btn
import app.horae.ui.components.Field
import app.horae.ui.components.Gap
import app.horae.ui.components.Kind as BtnKind
import app.horae.ui.components.LocalToast
import app.horae.ui.components.Sheet
import app.horae.ui.components.TextKey
import app.horae.ui.components.TopBar
import app.horae.ui.theme.C
import app.horae.ui.theme.T
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

/** A leaf of the book: a month's calendar page, or a day. */
sealed interface Leaf {
    data class Month(val first: LocalDate) : Leaf
    data class Day(val day: Long) : Leaf
}

fun leaves(first: Long, today: Long): List<Leaf> {
    val out = ArrayList<Leaf>()
    for (d in first..today) {
        val date = LocalDate.ofEpochDay(d)
        if (d == first || date.dayOfMonth == 1) out += Leaf.Month(date.withDayOfMonth(1))
        out += Leaf.Day(d)
    }
    return out
}

@Composable
fun BookScreen(home: Home) {
    val w = home.w
    val first = minOf(home.s.firstDay ?: home.today, home.today)
    val leaves = remember(first, home.today) { leaves(first, home.today) }
    val pager = rememberPagerState(initialPage = leaves.lastIndex) { leaves.size }
    val scope = rememberCoroutineScope()
    var shelf by remember { mutableStateOf(false) }
    val year = when (val l = leaves.getOrNull(pager.currentPage)) { is Leaf.Month -> l.first.year; is Leaf.Day -> LocalDate.ofEpochDay(l.day).year; null -> home.date.year }

    Column(Modifier.fillMaxSize()) {
        TopBar(w.t("Книга часов", "Book of hours"), w.t("Год ", "Year ") + Words.roman(year) + " · " + w.t("листай назад", "swipe back through it")) {
            TextKey(w.t("Полка", "Shelf")) { shelf = true }
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1) { i ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 6.dp)) {
                when (val l = leaves[i]) {
                    is Leaf.Month -> MonthLeaf(home, l.first)
                    is Leaf.Day -> Page(home, l.day) { if (l.day < home.today) GlossEditor(home, l.day) else InkWet(home) }
                }
                Gap(20)
            }
        }
    }
    if (shelf) Shelf(home, onClose = { shelf = false }) { y ->
        shelf = false
        val target = leaves.indexOfFirst { (it is Leaf.Month && it.first.year == y) || (it is Leaf.Day && LocalDate.ofEpochDay(it.day).year == y) }
        if (target >= 0) scope.launch { pager.scrollToPage(target) }
    }
}

@Composable
private fun InkWet(home: Home) {
    Text(home.w.t("Чернила этой страницы ещё не высохли. Завтра её уже нельзя будет переписать — только оставить глоссу на полях.", "The ink on this page is still wet. Tomorrow it can't be rewritten, only glossed in the margin."), style = T.Small.copy(fontSize = 13.sp), modifier = Modifier.padding(top = 14.dp))
}

/** A note from the future in the margin of a dried page. */
@Composable
private fun GlossEditor(home: Home, day: Long) {
    val w = home.w
    val c = LocalContext.current.container
    val toast = LocalToast.current
    var open by remember(day) { mutableStateOf(false) }
    var text by remember(day) { mutableStateOf("") }
    if (!open) {
        Text("+ " + w.t("глосса на полях", "a gloss in the margin"), style = T.Body.copy(color = C.vermilion, fontSize = 16.sp), modifier = Modifier.padding(top = 12.dp).clip(RoundedCornerShape(8.dp)).clickable { open = true }.padding(vertical = 6.dp))
        return
    }
    Gap(10)
    Field(text, { text = it }, w.t("Что видно отсюда, из сегодня?", "What can you see from here, from today?"), minLines = 2, style = T.Italic)
    Gap(6)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Btn(w.t("Отмена", "Cancel"), Modifier.weight(1f), BtnKind.GHOST) { open = false; text = "" }
        Btn(w.t("Вписать", "Write it"), Modifier.weight(1f), enabled = text.isNotBlank()) {
            val g = Gloss(day = day, at = System.currentTimeMillis(), text = text.trim())
            c.write { repo.gloss(g) }
            open = false; text = ""
            toast.show(w.t("Глосса на полях", "Glossed"))
        }
    }
}

/** What each month's labour is in a book of hours' calendar. */
private fun labour(w: Words, m: Int) = when (m) {
    1 -> w.t("Январь пирует у огня", "January feasts by the fire")
    2 -> w.t("Февраль греет руки у очага", "February warms its hands at the hearth")
    3 -> w.t("Март обрезает лозу", "March prunes the vines")
    4 -> w.t("Апрель сажает сад", "April plants the garden")
    5 -> w.t("Май выезжает в поле верхом", "May rides out into the fields")
    6 -> w.t("Июнь косит сено", "June mows the hay")
    7 -> w.t("Июль жнёт хлеб", "July reaps the corn")
    8 -> w.t("Август молотит зерно", "August threshes the grain")
    9 -> w.t("Сентябрь давит виноград", "September treads the grapes")
    10 -> w.t("Октябрь сеет озимые", "October sows the winter wheat")
    11 -> w.t("Ноябрь собирает жёлуди", "November gathers acorns")
    else -> w.t("Декабрь печёт хлеб к празднику", "December bakes the feast-day bread")
}

private val SIGNS = listOf("♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓").map { "$it︎" }

/** The calendar leaf that opens each month: its name, its labour, the zodiac arch with the sun, and its count. */
@Composable
internal fun MonthLeaf(home: Home, first: LocalDate) {
    val w = home.w
    val s = home.s
    val start = first.toEpochDay()
    val end = first.plusMonths(1).toEpochDay() - 1
    val entries = (start..end).flatMap { s.on(it) }
    val items = entries.flatMap { it.items }
    val minutes = (items.sumOf { it.seconds } + 30) / 60
    val beads = items.filter { it.kind == Kind.BEADS }.sumOf { it.n }
    val pages = (start..end).count { s.on(it).isNotEmpty() }
    val tm = rememberTextMeasurer()
    val signStyle = T.Body.copy(fontSize = 15.sp, color = C.goldHi)
    val mid = first.withDayOfMonth(15)
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            // The arch of heaven over the month, its signs, and the sun where it stands.
            val c = Offset(size.width / 2, size.height * .98f)
            val r = minOf(size.width * .46f, size.height * .92f)
            drawArc(Brush.verticalGradient(listOf(C.lapisDeep, C.lapis), c.y - r, c.y), 180f, 180f, true, c - Offset(r, r), Size(r * 2, r * 2))
            drawArc(C.gold, 180f, 180f, false, c - Offset(r, r), Size(r * 2, r * 2), style = Stroke(r * .03f))
            drawArc(C.goldHi.copy(alpha = .4f), 180f, 180f, false, c - Offset(r * .72f, r * .72f), Size(r * 1.44f, r * 1.44f), style = Stroke(r * .01f))
            val lon = Sun.longitude(mid)
            val sign = (lon / 30).toInt()
            for (k in -2..2) {
                val idx = ((sign + k) % 12 + 12) % 12
                val a = PI + (k + 2.5) / 5.0 * PI
                val p = Offset(c.x + cos(a).toFloat() * r * .86f, c.y + sin(a).toFloat() * r * .86f)
                val m = tm.measure(SIGNS[idx], signStyle.copy(color = if (k == 0) C.goldHi else C.goldHi.copy(alpha = .45f)))
                drawText(m, topLeft = Offset(p.x - m.size.width / 2f, p.y - m.size.height / 2f))
            }
            val within = (lon % 30) / 30
            val sa = PI + (2 + within) / 5.0 * PI
            val sp = Offset(c.x + cos(sa).toFloat() * r * .5f, c.y + sin(sa).toFloat() * r * .5f)
            Illum.bezant(this, sp, r * .09f)
            for (i in 0 until 12) {
                val a = i * PI.toFloat() / 6
                drawLine(C.gold, Offset(sp.x + cos(a) * r * .11f, sp.y + sin(a) * r * .11f), Offset(sp.x + cos(a) * r * .16f, sp.y + sin(a) * r * .16f), r * .015f)
            }
            val stars = kotlin.random.Random(first.monthValue)
            repeat(14) {
                val a = PI + (stars.nextDouble() * .9 + .05) * PI
                val rr = r * (.15f + stars.nextFloat() * .5f)
                drawCircle(C.goldHi.copy(alpha = .6f), r * .01f, Offset(c.x + cos(a).toFloat() * rr, c.y + sin(a).toFloat() * rr))
            }
        }
        Gap(14)
        Text(w.month(first), style = T.Title.copy(fontSize = 40.sp, lineHeight = 44.sp, color = C.vermilion))
        Text(Words.roman(first.year), style = T.Rubric.copy(fontSize = 15.sp, color = C.lapis))
        Gap(8)
        Text(labour(w, first.monthValue), style = T.Italic.copy(color = C.inkSoft, textAlign = TextAlign.Center))
        Gap(18)
        if (entries.isNotEmpty()) {
            Text(
                w.n(entries.size, "час соблюдён", "часа соблюдено", "часов соблюдено", "hour kept", "hours kept") + " · " +
                    w.n(pages, "страница", "страницы", "страниц", "page", "pages") + " · " + w.t("$minutes мин в практике", "$minutes min in practice") +
                    if (beads > 0) w.t(" · бусин: $beads", " · beads: $beads") else "",
                style = T.Small.copy(textAlign = TextAlign.Center, color = C.inkSoft),
            )
        }
        val feasts = s.feasts.filter { it.month == first.monthValue }
        if (feasts.isNotEmpty()) {
            Gap(14)
            Text(w.t("КРАСНЫЕ ДНИ", "RED-LETTER DAYS"), style = T.Rubric)
            for (f in feasts) Text("${Words.roman(f.dom)} · ${f.name}", style = T.Body.copy(color = C.vermilion))
        }
    }
}

/** Bound years on a shelf, the current one still open on the desk. */
@Composable
private fun Shelf(home: Home, onClose: () -> Unit, onYear: (Int) -> Unit) {
    val w = home.w
    val years = (home.s.entries.keys.map { LocalDate.ofEpochDay(it).year } + home.date.year).distinct().sorted()
    Sheet(w.t("Полка", "Shelf"), onClose, w.t("Закрыть", "Close")) {
        Text(w.t("Каждый год 31 декабря книга переплетается в кожу и встаёт на полку. Нынешняя ещё лежит на пюпитре.", "Each year on 31 December the book is bound in leather and goes on the shelf. This year's still lies on the lectern."), style = T.Small)
        Gap(16)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            val tm = rememberTextMeasurer()
            for ((i, y) in years.withIndex()) {
                val current = y == home.date.year
                val count = home.s.entries.filterKeys { LocalDate.ofEpochDay(it).year == y }.values.sumOf { it.size }
                val leather = listOf(Color(0xFF5A2E1C), Color(0xFF2E3A52), Color(0xFF3B4A2E), Color(0xFF5A1E2A))[i % 4]
                Box(Modifier.width(54.dp).height((150 + (count / 40).coerceAtMost(60)).dp).clip(RoundedCornerShape(4.dp)).clickable { onYear(y) }) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRoundRect(Brush.horizontalGradient(listOf(leather.copy(alpha = .8f), leather, leather.copy(alpha = .7f))), cornerRadius = CornerRadius(6f))
                        for (k in listOf(.12f, .2f, .8f, .88f)) drawLine(C.gold, Offset(size.width * .1f, size.height * k), Offset(size.width * .9f, size.height * k), 3f)
                        val m = tm.measure(Words.roman(y), T.Rubric.copy(color = C.goldHi, fontSize = 11.sp, letterSpacing = 0.sp))
                        drawText(m, topLeft = Offset(size.width / 2 - m.size.width / 2f, size.height * .45f))
                        if (current) drawRoundRect(C.goldHi, style = Stroke(4f), cornerRadius = CornerRadius(6f))
                    }
                }
            }
        }
        Gap(10)
        for (y in years) {
            val count = home.s.entries.filterKeys { LocalDate.ofEpochDay(it).year == y }.values.sumOf { it.size }
            Text("${Words.roman(y)} · $y — " + w.n(count, "час", "часа", "часов", "hour", "hours") + if (y == home.date.year) w.t(" · ещё пишется", " · still being written") else w.t(" · переплетена", " · bound"), style = T.Body.copy(fontSize = 16.sp), modifier = Modifier.clickable { onYear(y) }.padding(vertical = 4.dp))
        }
    }
}
