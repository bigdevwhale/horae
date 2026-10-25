package app.horae.ui.rule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.horae.container
import app.horae.domain.Anchor
import app.horae.domain.Bell
import app.horae.domain.Clock
import app.horae.domain.Hour
import app.horae.domain.Step
import app.horae.notify.Bells
import app.horae.ui.Home
import app.horae.ui.components.Btn
import app.horae.ui.components.Chip
import app.horae.ui.components.Field
import app.horae.ui.components.Flow
import app.horae.ui.components.Gap
import app.horae.ui.components.Kind
import app.horae.ui.components.Label
import app.horae.ui.components.Sheet
import app.horae.ui.components.Stepper
import app.horae.ui.components.Toggle
import app.horae.ui.theme.C
import app.horae.ui.theme.T
import app.horae.ui.today.hm

/** What the editor of steps is busy with: which list, which step (-1 for a new one of [kind]). */
private data class Editing(val brief: Boolean, val index: Int, val step: Step)

@Composable
fun HourEditor(home: Home, initial: Hour, onClose: () -> Unit) {
    val w = home.w
    val ctx = LocalContext.current
    val c = ctx.container
    var h by remember { mutableStateOf(initial) }
    var picking by remember { mutableStateOf<Boolean?>(null) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun save() {
        val clean = h.copy(name = h.name.trim().ifEmpty { w.t("Час", "Hour") })
        c.write { repo.save(clean); Bells.plan(ctx) }
        onClose()
    }

    Sheet(if (initial.id == 0L) w.t("Новый час", "A new hour") else initial.name, onClose, w.t("Отмена", "Cancel")) {
        Label(w.t("НАЗВАНИЕ", "NAME"))
        Field(h.name, { h = h.copy(name = it) }, w.t("Хвалитны, Вечерня, Утро…", "Lauds, Vespers, Morning…"))

        Label(w.t("КОГДА", "WHEN"))
        Flow {
            Chip(w.t("по солнцу", "by the sun"), h.anchor == Anchor.SUN) { h = h.copy(anchor = Anchor.SUN) }
            Chip(w.t("по часам", "by the clock"), h.anchor == Anchor.CLOCK) { h = h.copy(anchor = Anchor.CLOCK) }
            Chip(w.t("вручную", "by hand"), h.anchor == Anchor.HAND) { h = h.copy(anchor = Anchor.HAND) }
        }
        Gap(6)
        when (h.anchor) {
            Anchor.SUN -> {
                Stepper(w.pos(h.pos).replaceFirstChar { it.uppercase() }, (h.pos * 2).toInt(), -12..36, show = { v -> (if (v % 2 == 0) "${v / 2}" else "${if (v < 0 && v / 2 == 0) "-" else ""}${v / 2}½") }) { h = h.copy(pos = it / 2.0) }
                val at = Clock.at(h, home.date, home.zone, home.place)
                Text(
                    w.t("Неравные часы: от восхода до заката — двенадцать, от заката до восхода — двенадцать. Сегодня это ", "Unequal hours: twelve from sunrise to sunset, twelve from sunset to sunrise. Today that is ") + (at?.let { home.hm(it) } ?: "—") + ".",
                    style = T.Small,
                )
                Gap(4)
                Flow {
                    for ((p, label) in listOf(-3.0 to w.t("ночью", "night"), 0.0 to w.t("рассвет", "sunrise"), 3.0 to w.t("третий", "terce"), 6.0 to w.t("полдень", "noon"), 9.0 to w.t("девятый", "none"), 12.0 to w.t("закат", "sunset"), 13.5 to w.t("повечерие", "compline")))
                        Chip(label, h.pos == p) { h = h.copy(pos = p) }
                }
            }
            Anchor.CLOCK -> Stepper(w.t("Время", "Time"), h.clock, 0..(24 * 60 - 15), step = 15, show = { w.clock(it) }) { h = h.copy(clock = it) }
            Anchor.HAND -> Text(w.t("Такой час не звонит. Его открывают из списка, когда захочется.", "Such an hour doesn't ring. Open it from the list whenever you like."), style = T.Small)
        }

        Label(w.t("ДНИ", "DAYS"))
        Flow {
            for (d in 0..6) {
                val on = h.days and (1 shl d) != 0
                Chip(w.weekday(d + 1), on) { h = h.copy(days = h.days xor (1 shl d)) }
            }
        }

        if (h.anchor != Anchor.HAND) {
            Label(w.t("КОЛОКОЛ", "BELL"))
            Flow { for (b in Bell.entries) Chip(w.bell(b), h.bell == b) { h = h.copy(bell = b); if (b != Bell.NONE) c.sfx.stroke(b) } }
            if (h.bell != Bell.NONE) {
                Stepper(w.t("Ударов", "Strokes"), h.strokes, 0..12) { h = h.copy(strokes = it) }
                Text(if (h.strokes == 0) w.t("0 — по номеру часа: сейчас это ${Clock.number(h)}", "0 rings the number of the hour: ${Clock.number(h)} now") else w.t("Тихое уведомление после звона откроет час.", "A quiet notification after the bell opens the hour."), style = T.Small)
                Gap(6)
                Btn(w.t("Послушать", "Listen"), Modifier.fillMaxWidth(), Kind.GHOST) { c.sfx.ring(h.bell, Clock.strokes(h)) }
            }
        }

        Label(w.t("ПРАКТИКИ · ", "PRACTICES · ") + w.total(h.steps).uppercase())
        StepList(w, h.steps, onEdit = { editing = Editing(false, it, h.steps[it]) }, onChange = { h = h.copy(steps = it) }, onAdd = { picking = false })

        Label(w.t("КРАТКОЕ ПРАВИЛО", "THE SHORT RULE"))
        Text(w.t("Для занятого дня и дороги: минута вместо пропуска.", "For a busy day or the road: a minute instead of a blank."), style = T.Small)
        StepList(w, h.brief, onEdit = { editing = Editing(true, it, h.brief[it]) }, onChange = { h = h.copy(brief = it) }, onAdd = { picking = true })

        Gap(10)
        Toggle(w.t("Час включён", "The hour is on"), null, h.on) { h = h.copy(on = it) }
        Gap(16)
        Btn(w.t("Сохранить", "Save"), Modifier.fillMaxWidth()) { save() }
        if (initial.id != 0L) {
            Gap(8)
            if (!confirmDelete) Btn(w.t("Убрать час из устава", "Remove the hour"), Modifier.fillMaxWidth(), Kind.GHOST) { confirmDelete = true }
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(w.t("Оставить", "Keep it"), Modifier.weight(1f), Kind.GHOST) { confirmDelete = false }
                Btn(w.t("Убрать", "Remove"), Modifier.weight(1f)) { c.write { repo.delete(initial); Bells.plan(ctx) }; onClose() }
            }
            Text(w.t("Записи этого часа в книге останутся.", "This hour's writing stays in the book."), style = T.Small)
        }
        Gap(16)
    }

    picking?.let { brief ->
        KindPicker(w, onClose = { picking = null }) { k ->
            picking = null
            editing = Editing(brief, -1, defaultStep(k, w.ru))
        }
    }
    editing?.let { e ->
        StepEditor(w, e.step, onClose = { editing = null }) { s ->
            val list = (if (e.brief) h.brief else h.steps).toMutableList()
            if (e.index < 0) list += s else list[e.index] = s
            h = if (e.brief) h.copy(brief = list) else h.copy(steps = list)
            editing = null
        }
    }
}
