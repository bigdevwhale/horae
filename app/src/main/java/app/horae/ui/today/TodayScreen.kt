package app.horae.ui.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import app.horae.domain.Anchor
import app.horae.domain.Clock
import app.horae.ui.Home
import app.horae.ui.Words
import app.horae.ui.art.Illum
import app.horae.ui.components.Btn
import app.horae.ui.components.Card
import app.horae.ui.components.Gap
import app.horae.ui.components.Kind
import app.horae.ui.components.Label
import app.horae.ui.theme.C
import app.horae.ui.theme.T
import java.time.Instant
import java.time.format.DateTimeFormatter

private val HM = DateTimeFormatter.ofPattern("H:mm")

fun Home.hm(t: Long): String = Instant.ofEpochMilli(t).atZone(zone).format(HM)

@Composable
fun TodayScreen(home: Home, onHour: (Long, Long, Boolean) -> Unit, onSettings: () -> Unit) {
    val silence = home.silence()
    if (silence != null) { Silence(home, silence.first, silence.second); return }
    val w = home.w
    val s = home.s
    val date = home.date
    val feast = s.feast(date)
    val season = s.season(home.today)
    val open = home.open()
    val openKept = open != null && home.kept(open.date.toEpochDay(), open.hour.id)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(w.date(date).replaceFirstChar { it.uppercase() }, style = T.Title)
                if (feast != null) Text(feast.name, style = T.Heading.copy(color = C.vermilion))
                if (season != null) {
                    val n = (home.today - season.start + 1).toInt()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(C.hue(season.hue)))
                        Text("  ${season.name} · ${w.t("день $n из ${season.days}", "day $n of ${season.days}")}", style = T.Small.copy(color = C.inkSoft))
                    }
                }
            }
            Icon(Icons.Outlined.Settings, w.t("Настройки", "Settings"), tint = C.faded, modifier = Modifier.clip(CircleShape).clickable(onClick = onSettings).padding(8.dp).size(24.dp))
        }

        Dial(home)

        if (open != null && !openKept) {
            Card(bg = C.ink.copy(alpha = .9f), onClick = { onHour(open.hour.id, open.date.toEpochDay(), false) }) {
                Text(w.t("ОТКРЫТ ЧАС", "AN HOUR IS OPEN"), style = T.Rubric.copy(color = C.goldHi))
                Text(open.hour.name, style = T.Title.copy(color = C.vellum))
                Text(open.hour.steps.joinToString(" · ") { w.kind(it.kind) }.ifEmpty { w.t("пустой час — добавь практики в уставе", "an empty hour — add practices in the rule") } + " · " + w.total(open.hour.steps), style = T.Small.copy(color = C.vellumEdge))
                Gap(8)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn(w.t("Войти", "Enter"), Modifier.weight(1f), Kind.CANDLE) { onHour(open.hour.id, open.date.toEpochDay(), false) }
                    if (open.hour.brief.isNotEmpty()) Btn(w.t("Краткое правило", "Short rule"), Modifier.weight(1f), Kind.DARK) { onHour(open.hour.id, open.date.toEpochDay(), true) }
                }
            }
        }

        Label(w.t("ЧАСЫ ЭТОГО ДНЯ", "THE HOURS OF THIS DAY"))
        val slots = home.slots(date)
        if (slots.isEmpty()) Text(w.t("Сегодня по уставу часов нет. Устав можно поменять на соседней вкладке.", "No hours today by the rule. The rule is on the next tab."), style = T.Italic.copy(color = C.faded))
        for (slot in slots) HourRow(home, slot, open, onHour)

        if (s.rules.any { it.on }) {
            Label(w.t("ПРАВИЛА ДНЯ", "RULES OF THE DAY"))
            val c = LocalContext.current.container
            for (r in s.rules.filter { it.on }) {
                val kept = s.kept(home.today, r.id)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { c.write { repo.toggle(home.today, r.id) } }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(22.dp)) { Illum.tick(this, center, size.minDimension / 2.2f, kept) }
                    Text(r.text, style = T.Body.copy(color = if (kept) C.ink else C.inkSoft), modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
        Gap(24)
    }
}

@Composable
private fun HourRow(home: Home, slot: Clock.Slot, open: Clock.Slot?, onHour: (Long, Long, Boolean) -> Unit) {
    val w = home.w
    val day = slot.date.toEpochDay()
    val kept = home.kept(day, slot.hour.id)
    val isOpen = open?.hour?.id == slot.hour.id && open.date == slot.date
    val past = slot.at != null && slot.at < home.now && !isOpen
    val (status, color) = when {
        kept -> w.t("соблюдён", "kept") to C.vermilion
        isOpen -> w.t("открыт", "open") to C.lapis
        past -> w.t("пропущен — пустое место на странице", "missed — a blank on the page") to C.faded
        slot.at == null -> w.t("по желанию", "when you wish") to C.faded
        else -> "" to C.faded
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onHour(slot.hour.id, day, false) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(slot.at?.let { home.hm(it) } ?: "—", style = T.BodyM.copy(color = if (kept) C.vermilion else C.inkSoft), modifier = Modifier.width(58.dp))
        Column(Modifier.weight(1f)) {
            Text(slot.hour.name, style = T.Heading.copy(fontSize = 19.sp, color = if (past && !kept) C.faded else C.ink))
            val sub = listOfNotNull(if (slot.hour.anchor == Anchor.SUN) w.pos(slot.hour.pos) else null, w.total(slot.hour.steps), status.ifEmpty { null }).joinToString(" · ")
            Text(sub, style = T.Small.copy(color = color))
        }
        Canvas(Modifier.size(22.dp)) {
            if (kept) Illum.tick(this, center, size.minDimension / 2.2f, true)
            else if (isOpen) drawCircle(C.lapis, size.minDimension / 3)
        }
    }
}

@Composable
private fun Dial(home: Home) {
    val w = home.w
    val sun = Clock.sun(home.date, home.zone, home.place)
    // Before dawn the dial still shows last night: position measured from yesterday's sunrise.
    val nowPos = if (home.now < sun.rise) 12 + (Clock.sun(home.date.minusDays(1), home.zone, home.place).let { y -> (home.now - y.set) / y.nightHour }) else sun.pos(home.now)
    val marks = home.slots(home.date).filter { it.hour.anchor == Anchor.SUN }.map { s ->
        val kept = home.kept(home.today, s.hour.id)
        (((s.hour.pos % 24) + 24) % 24) to if (kept) C.vermilion else C.lapis
    }
    val tm = rememberTextMeasurer()
    val numStyle = T.Small.copy(fontSize = 12.sp, color = C.inkSoft)
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(250.dp)) {
            val r = size.minDimension / 2 * .92f
            Illum.dial(this, center, r, nowPos, marks) { i, at ->
                val m = tm.measure(Words.roman(i), numStyle)
                drawText(m, topLeft = Offset(at.x - m.size.width / 2f, at.y - m.size.height / 2f))
            }
        }
    }
    val dayMin = (sun.dayHour / 60_000).toInt()
    val nightMin = (sun.nightHour / 60_000).toInt()
    val line = if (sun.polar) w.t("Солнце сегодня не встаёт и не садится — часы идут по шести и восемнадцати.", "The sun neither rises nor sets today, so the hours run from six to eighteen.")
    else w.t(
        "Восход ${home.hm(sun.rise)}, закат ${home.hm(sun.set)}. Час дня длится $dayMin мин, час ночи — $nightMin.",
        "Sunrise ${home.hm(sun.rise)}, sunset ${home.hm(sun.set)}. A day hour lasts $dayMin min, a night hour $nightMin.",
    )
    Text(line, style = T.Small.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp))
    Gap(6)
}

/** The book closed with its clasps: nothing to do until the first hour of tomorrow. */
@Composable
private fun Silence(home: Home, day: Long, until: Long) {
    val w = home.w
    val c = LocalContext.current.container
    Column(Modifier.fillMaxSize().background(C.night).statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Canvas(Modifier.size(200.dp, 250.dp)) {
            val bw = size.width * .8f; val bh = size.height * .9f
            val l = (size.width - bw) / 2; val t = (size.height - bh) / 2
            drawRoundRect(Brush.linearGradient(listOf(Color(0xFF5A2E1C), Color(0xFF3A1D12)), Offset(l, t), Offset(l + bw, t + bh)), Offset(l, t), Size(bw, bh), CornerRadius(10f))
            drawRoundRect(Color(0xFF7A4A2A), Offset(l + bw * .08f, t + bh * .06f), Size(bw * .84f, bh * .88f), CornerRadius(6f), style = Stroke(3f))
            for (i in 0..4) drawLine(Color(0xFF2A140C), Offset(l, t + bh * (.15f + i * .17f)), Offset(l + bw * .06f, t + bh * (.15f + i * .17f)), 4f)
            for (y in listOf(.3f, .7f)) {
                drawRoundRect(C.gold, Offset(l + bw * .9f, t + bh * y - 12f), Size(bw * .18f, 24f), CornerRadius(4f))
                drawCircle(C.goldLo, 5f, Offset(l + bw * 1.02f, t + bh * y))
            }
            Illum.rosette(this, Offset(l + bw / 2, t + bh / 2), bw * .14f, C.gold)
        }
        Gap(20)
        Text(w.t("Великое молчание", "The great silence"), style = T.Big)
        Text(w.t("Последний час дня соблюдён. Книга закрыта до первого часа — до ${home.hm(until)}.", "The last hour of the day is kept. The book is closed until the first hour, at ${home.hm(until)}."), style = T.Body.copy(color = C.candle, textAlign = TextAlign.Center))
        Spacer(Modifier.height(40.dp))
        Text(
            w.t("Нарушить молчание — долгое нажатие", "Break the silence — long press"),
            style = T.Small.copy(color = C.faded),
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).combinedClickable(onClick = {}, onLongClick = {
                c.write { repo.breakSilence(day) }
            }).padding(12.dp),
        )
    }
}
