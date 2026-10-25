package app.horae.ui.rule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.horae.container
import app.horae.domain.Anchor
import app.horae.domain.Bell
import app.horae.domain.Clock
import app.horae.domain.DayRule
import app.horae.domain.Feast
import app.horae.domain.Hour
import app.horae.domain.Hue
import app.horae.domain.Preset
import app.horae.domain.Season
import app.horae.notify.Bells
import app.horae.ui.Home
import app.horae.ui.Words
import app.horae.ui.components.Btn
import app.horae.ui.components.Card
import app.horae.ui.components.Chip
import app.horae.ui.components.Field
import app.horae.ui.components.Flow
import app.horae.ui.components.Gap
import app.horae.ui.components.Kind
import app.horae.ui.components.Label
import app.horae.ui.components.LocalToast
import app.horae.ui.components.Sheet
import app.horae.ui.components.Stepper
import app.horae.ui.components.TopBar
import app.horae.ui.theme.C
import app.horae.ui.theme.T
import app.horae.ui.today.hm
import java.time.LocalDate

private sealed interface Open {
    data class Hour(val h: app.horae.domain.Hour) : Open
    data object Presets : Open
    data class Feast(val f: app.horae.domain.Feast) : Open
    data class Season(val s: app.horae.domain.Season) : Open
}

@Composable
fun RuleScreen(home: Home) {
    val w = home.w
    val s = home.s
    val ctx = LocalContext.current
    val c = ctx.container
    var open by remember { mutableStateOf<Open?>(null) }
    val perDay = s.hours.filter { it.on }.sumOf { h -> h.steps.sumOf { it.seconds() } } / 60

    Column(Modifier.fillMaxSize()) {
        TopBar(w.t("Устав", "The rule"), w.n(s.hours.count { it.on }, "час", "часа", "часов", "hour", "hours") + w.t(" · около $perDay мин в день", " · about $perDay min a day"))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(w.t("Сколько часов, когда они звонят и из чего состоят — решаешь ты. Коснись часа, чтобы настроить.", "How many hours, when they ring and what they hold is yours to decide. Tap an hour to set it."), style = T.Small)
            Gap(8)
            val today = home.date
            for ((i, h) in s.hours.withIndex()) HourCard(home, h, i, s.hours.size, today, onOpen = { open = Open.Hour(h) })
            Gap(4)
            Btn("+ " + w.t("Добавить час", "Add an hour"), Modifier.fillMaxWidth(), Kind.GHOST) {
                open = Open.Hour(Hour(name = "", anchor = Anchor.SUN, pos = 6.0, bell = Bell.HAND))
            }
            Gap(8)
            Btn(w.t("Готовые уставы", "Ready-made rules"), Modifier.fillMaxWidth(), Kind.SOFT) { open = Open.Presets }

            Label(w.t("ПРАВИЛА ДНЯ", "RULES OF THE DAY"))
            Text(w.t("На весь день, а не на час: без мяса, без ленты, пешком. Отмечаются на сегодняшней странице.", "For the whole day rather than an hour: no meat, no feeds, on foot. Ticked on today's page."), style = T.Small)
            for (r in s.rules) RuleRow(w, r)
            var rule by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Field(rule, { rule = it }, w.t("новое правило", "a new rule")) }
                Text("+", style = T.Title.copy(color = if (rule.isBlank()) C.vellumEdge else C.vermilion), modifier = Modifier.clip(CircleShape).clickable(enabled = rule.isNotBlank()) {
                    val r = DayRule(text = rule.trim()); rule = ""
                    c.write { repo.save(r) }
                }.padding(horizontal = 14.dp))
            }

            Label(w.t("КРАСНЫЕ ДНИ", "RED-LETTER DAYS"))
            Text(w.t("Свои святцы: годовщины и памятные дни пишутся на странице красными чернилами.", "Your own calendar of saints: anniversaries and days to remember are written in red."), style = T.Small)
            for (f in s.feasts) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { open = Open.Feast(f) }.padding(vertical = 8.dp)) {
                Text(Words.roman(f.dom) + " · " + w.dateShort(LocalDate.of(2024, f.month, f.dom)).substringAfter(' '), style = T.BodyM.copy(color = C.vermilion), modifier = Modifier.width(130.dp))
                Text(f.name, style = T.Body)
            }
            Text("+ " + w.t("красный день", "a red-letter day"), style = T.BodyM.copy(color = C.vermilion), modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { open = Open.Feast(Feast(month = home.date.monthValue, dom = home.date.dayOfMonth, name = "")) }.padding(vertical = 8.dp))

            Label(w.t("СЕЗОНЫ", "SEASONS"))
            Text(w.t("Сорок дней чего-то — и страницы окрашиваются в цвет сезона.", "Forty days of something, and the pages take on the season's colour."), style = T.Small)
            for (se in s.seasons) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { open = Open.Season(se) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(C.hue(se.hue)))
                Column(Modifier.padding(start = 10.dp)) {
                    Text(se.name, style = T.Body)
                    Text(w.dateShort(LocalDate.ofEpochDay(se.start)) + " — " + w.dateShort(LocalDate.ofEpochDay(se.start + se.days - 1)), style = T.Small)
                }
            }
            Text("+ " + w.t("сезон", "a season"), style = T.BodyM.copy(color = C.vermilion), modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { open = Open.Season(Season(name = "", hue = Hue.VIOLET, start = home.today, days = 40)) }.padding(vertical = 8.dp))
            Gap(30)
        }
    }

    when (val o = open) {
        is Open.Hour -> HourEditor(home, o.h, onClose = { open = null })
        Open.Presets -> PresetSheet(home, onClose = { open = null })
        is Open.Feast -> FeastSheet(w, o.f, onClose = { open = null })
        is Open.Season -> SeasonSheet(home, o.s, onClose = { open = null })
        null -> Unit
    }
}

@Composable
private fun HourCard(home: Home, h: Hour, i: Int, count: Int, today: LocalDate, onOpen: () -> Unit) {
    val w = home.w
    val ctx = LocalContext.current
    val c = ctx.container
    val at = Clock.at(h, today, home.zone, home.place)
    Card(Modifier.padding(vertical = 4.dp), bg = if (h.on) C.card else C.vellumDeep.copy(alpha = .5f), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(h.name, style = T.Heading.copy(color = if (h.on) C.ink else C.faded))
                Text(
                    listOfNotNull(w.anchor(h), at?.let { w.t("сегодня ", "today ") + home.hm(it) }, w.days(h.days).takeIf { h.days != 0b1111111 }, w.bell(h.bell).lowercase().takeIf { h.anchor != Anchor.HAND }, if (!h.on) w.t("выключен", "off") else null).joinToString(" · "),
                    style = T.Small,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("↑", style = T.Body.copy(color = if (i > 0) C.inkSoft else C.vellumEdge), modifier = Modifier.clip(CircleShape).clickable(enabled = i > 0) { c.write { repo.move(h, -1) } }.padding(horizontal = 10.dp, vertical = 2.dp))
                Text("↓", style = T.Body.copy(color = if (i < count - 1) C.inkSoft else C.vellumEdge), modifier = Modifier.clip(CircleShape).clickable(enabled = i < count - 1) { c.write { repo.move(h, 1) } }.padding(horizontal = 10.dp, vertical = 2.dp))
            }
        }
        if (h.steps.isNotEmpty()) {
            Gap(4)
            for (s in h.steps) Text("· " + w.summary(s), style = T.Body.copy(fontSize = 16.sp, color = C.inkSoft))
            Text(w.total(h.steps) + if (h.brief.isNotEmpty()) w.t(" · есть краткое правило", " · has a short rule") else "", style = T.Small.copy(color = C.vermilion))
        } else Text(w.t("пока пустой — коснись, чтобы добавить практики", "empty so far — tap to add practices"), style = T.Small.copy(color = C.vermilion))
    }
}

@Composable
private fun RuleRow(w: Words, r: DayRule) {
    val c = LocalContext.current.container
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(r.text, style = T.Body.copy(color = if (r.on) C.ink else C.faded), modifier = Modifier.weight(1f).clickable { c.write { repo.save(r.copy(on = !r.on)) } })
        Text(if (r.on) w.t("вкл", "on") else w.t("выкл", "off"), style = T.Small.copy(color = if (r.on) C.vermilion else C.faded), modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { c.write { repo.save(r.copy(on = !r.on)) } }.padding(8.dp))
        Text("✕", style = T.Body.copy(color = C.faded), modifier = Modifier.clip(CircleShape).clickable { c.write { repo.delete(r) } }.padding(8.dp))
    }
}

@Composable
private fun PresetSheet(home: Home, onClose: () -> Unit) {
    val w = home.w
    val ctx = LocalContext.current
    val c = ctx.container
    val toast = LocalToast.current
    var pick by remember { mutableStateOf<Preset?>(null) }
    Sheet(w.t("Готовые уставы", "Ready-made rules"), onClose, w.t("Закрыть", "Close")) {
        Text(w.t("Устав заменит нынешние часы; книга и всё записанное останутся. Потом любой час можно переделать.", "A rule replaces the current hours; the book and everything written stays. Any hour can be reworked afterwards."), style = T.Small)
        Gap(8)
        for (p in Preset.entries) {
            Card(Modifier.padding(vertical = 4.dp), bg = if (pick == p) C.goldHi.copy(alpha = .5f) else C.card, onClick = { pick = p }) {
                Text(w.preset(p), style = T.Heading)
                Text(w.presetSub(p), style = T.Small)
                if (pick == p) {
                    Gap(8)
                    Btn(w.t("Взять этот устав", "Take this rule"), Modifier.fillMaxWidth()) {
                        c.write { repo.preset(p, w.ru); Bells.plan(ctx) }
                        toast.show(w.t("Устав принят", "The rule is taken"))
                        onClose()
                    }
                }
            }
        }
    }
}

@Composable
private fun FeastSheet(w: Words, initial: Feast, onClose: () -> Unit) {
    val c = LocalContext.current.container
    var f by remember { mutableStateOf(initial) }
    Sheet(w.t("Красный день", "A red-letter day"), onClose, w.t("Отмена", "Cancel")) {
        Field(f.name, { f = f.copy(name = it) }, w.t("день рождения мамы, первый день без сигарет…", "Mum's birthday, the first day without cigarettes…"))
        Gap(8)
        Stepper(w.t("Месяц", "Month"), f.month, 1..12, show = { w.month(LocalDate.of(2024, it, 1)) }) { f = f.copy(month = it, dom = f.dom.coerceAtMost(LocalDate.of(2024, it, 1).lengthOfMonth())) }
        Stepper(w.t("День", "Day"), f.dom, 1..LocalDate.of(2024, f.month, 1).lengthOfMonth()) { f = f.copy(dom = it) }
        Gap(14)
        Btn(w.t("Сохранить", "Save"), Modifier.fillMaxWidth(), enabled = f.name.isNotBlank()) { val x = f.copy(name = f.name.trim()); c.write { repo.save(x) }; onClose() }
        if (initial.id != 0L) { Gap(8); Btn(w.t("Убрать", "Remove"), Modifier.fillMaxWidth(), Kind.GHOST) { c.write { repo.delete(initial) }; onClose() } }
    }
}

@Composable
private fun SeasonSheet(home: Home, initial: Season, onClose: () -> Unit) {
    val w = home.w
    val c = LocalContext.current.container
    var se by remember { mutableStateOf(initial) }
    var offset by remember { mutableIntStateOf((initial.start - home.today).toInt()) }
    Sheet(w.t("Сезон", "A season"), onClose, w.t("Отмена", "Cancel")) {
        Field(se.name, { se = se.copy(name = it) }, w.t("Пост, Сорок дней тишины, Время ученичества…", "Lent, Forty days of quiet, A time of learning…"))
        Label(w.t("ЦВЕТ", "COLOUR"))
        Flow { for (h in Hue.entries) Chip(w.hue(h), se.hue == h, dot = C.hue(h)) { se = se.copy(hue = h) } }
        Label(w.t("КОГДА", "WHEN"))
        Stepper(w.t("Начало", "Starts"), offset, -365..365, show = { w.dateShort(LocalDate.ofEpochDay(home.today + it)) }) { offset = it }
        Stepper(w.t("Дней", "Days"), se.days, 1..365) { se = se.copy(days = it) }
        Gap(14)
        Btn(w.t("Сохранить", "Save"), Modifier.fillMaxWidth(), enabled = se.name.isNotBlank()) {
            val x = se.copy(name = se.name.trim(), start = home.today + offset)
            c.write { repo.save(x) }; onClose()
        }
        if (initial.id != 0L) { Gap(8); Btn(w.t("Убрать", "Remove"), Modifier.fillMaxWidth(), Kind.GHOST) { c.write { repo.delete(initial) }; onClose() } }
    }
}
