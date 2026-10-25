package app.horae.ui.rule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.horae.domain.Bank
import app.horae.domain.Beads
import app.horae.domain.Breath
import app.horae.domain.Codec
import app.horae.domain.Examen
import app.horae.domain.Kind
import app.horae.domain.Library
import app.horae.domain.Metta
import app.horae.domain.Reviews
import app.horae.domain.Sit
import app.horae.domain.Source
import app.horae.domain.Step
import app.horae.ui.Words
import app.horae.ui.components.Btn
import app.horae.ui.components.Card
import app.horae.ui.components.Chip
import app.horae.ui.components.Field
import app.horae.ui.components.Flow
import app.horae.ui.components.Gap
import app.horae.ui.components.Label
import app.horae.ui.components.Sheet
import app.horae.ui.components.Stepper
import app.horae.ui.components.Toggle
import app.horae.ui.theme.C
import app.horae.ui.theme.T

/** A fresh step of [k] with sensible settings to start from. */
fun defaultStep(k: Kind, ru: Boolean): Step = when (k) {
    Kind.BREATH -> Step(k, min = 3, mode = Breath.EVEN.name)
    Kind.SIT -> Step(k, min = 10, mode = Sit.BREATH.name)
    Kind.METTA -> Step(k, min = 10, list = Metta.phrases(ru))
    Kind.BEADS -> Step(k, n = 33, mode = Beads.PLAIN.name, text = if (ru) "Господи, помилуй" else "Lord, have mercy")
    Kind.READ -> Step(k)
    Kind.QUESTION -> Step(k, mode = Bank.EVENING.name)
    Kind.THANKS -> Step(k, n = 3)
    Kind.EXAMEN -> Step(k, mode = Examen.IGNATIUS.name)
    Kind.BOWS -> Step(k, n = 12, mode = "BOW")
    Kind.WALK -> Step(k, min = 5, n = 3)
    Kind.GAZE -> Step(k, min = 3, mode = "CANDLE")
    Kind.SILENCE -> Step(k, min = 5, mode = "FACE")
    Kind.CUSTOM -> Step(k, text = if (ru) "Своя практика" else "Own practice", list = listOf(Codec.custom(if (ru) "Встань у окна" else "Stand by the window", 60)))
}

private fun lines(s: String) = s.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

/** Picks the kind of a new practice. */
@Composable
fun KindPicker(w: Words, onClose: () -> Unit, onPick: (Kind) -> Unit) {
    Sheet(w.t("Какая практика?", "Which practice?"), onClose, w.t("Отмена", "Cancel")) {
        for (k in Kind.entries) {
            Card(Modifier.padding(vertical = 4.dp), onClick = { onPick(k) }) {
                Text(w.kind(k), style = T.BodyM)
                Text(w.kindSub(k), style = T.Small)
            }
        }
    }
}

/** Every setting of one step, by its kind. */
@Composable
fun StepEditor(w: Words, initial: Step, onClose: () -> Unit, onSave: (Step) -> Unit) {
    var s by remember { mutableStateOf(initial) }
    Sheet(w.kind(s.kind), onClose, w.t("Отмена", "Cancel")) {
        Text(w.kindSub(s.kind), style = T.Small)
        when (s.kind) {
            Kind.BREATH -> {
                Label(w.t("СХЕМА", "PATTERN"))
                Flow { for (b in Breath.entries) Chip(w.breath(b), s.mode == b.name) { s = s.copy(mode = b.name) } }
                if (s.mode == Breath.OWN.name) {
                    Gap(8)
                    Field(s.text, { s = s.copy(text = it) }, w.t("вдох-задержка-выдох-пауза, например 4-2-6-0", "in-hold-out-rest, e.g. 4-2-6-0"))
                }
                val ph = Breath.phases(Breath.of(s.mode), s.text)
                Text(w.t("Цикл: вдох ${ph[0]} · задержка ${ph[1]} · выдох ${ph[2]} · пауза ${ph[3]} с", "A cycle: in ${ph[0]} · hold ${ph[1]} · out ${ph[2]} · rest ${ph[3]} s"), style = T.Small, modifier = Modifier.padding(top = 6.dp))
                Label(w.t("СКОЛЬКО", "HOW LONG"))
                Flow {
                    Chip(w.t("по минутам", "by minutes"), s.n == 0) { s = s.copy(n = 0, min = s.min.coerceAtLeast(1)) }
                    Chip(w.t("по циклам", "by cycles"), s.n > 0) { s = s.copy(n = if (s.n > 0) s.n else 10) }
                }
                if (s.n > 0) Stepper(w.t("Циклов", "Cycles"), s.n, 1..200) { s = s.copy(n = it) }
                else Stepper(w.t("Минут", "Minutes"), s.min, 1..60) { s = s.copy(min = it) }
            }
            Kind.SIT -> {
                Label(w.t("ВНИМАНИЕ", "ATTENTION"))
                Flow { for (m in Sit.entries) Chip(w.sit(m), s.mode == m.name) { s = s.copy(mode = m.name) } }
                Label(w.t("ВРЕМЯ", "TIME"))
                Stepper(w.t("Минут", "Minutes"), s.min, 1..120) { s = s.copy(min = it) }
                Stepper(w.t("Колокол каждые", "A bell every"), s.every, 0..60, suffix = w.t(" мин", " min")) { s = s.copy(every = it) }
                Text(w.t("0 — только в начале и в конце", "0 rings only at the start and the end"), style = T.Small)
            }
            Kind.METTA -> {
                Stepper(w.t("Минут", "Minutes"), s.min, 1..60) { s = s.copy(min = it) }
                Text(w.t("Круг: себе → близкому → едва знакомому → трудному → всем живым. Время делится поровну.", "The circle: yourself → someone dear → a stranger → someone difficult → all beings. The time is shared out evenly."), style = T.Small)
                Label(w.t("ФРАЗЫ, ПО ОДНОЙ НА СТРОКЕ", "PHRASES, ONE PER LINE"))
                var text by remember { mutableStateOf(s.list.joinToString("\n")) }
                Field(text, { text = it; s = s.copy(list = lines(it)) }, Metta.phrases(w.ru).joinToString("\n"), minLines = 4)
            }
            Kind.BEADS -> {
                Label(w.t("ЧЁТКИ", "BEADS"))
                Flow { for (b in Beads.entries) Chip(w.beads(b), s.mode == b.name) { s = s.copy(mode = b.name, n = b.count, every = b.every) } }
                Stepper(w.t("Бусин", "Beads"), s.n, 1..1000) { s = s.copy(n = it) }
                Stepper(w.t("Разделитель каждые", "A marker every"), s.every, 0..200) { s = s.copy(every = it) }
                Label(w.t("ЧТО ПОВТОРЯТЬ", "WHAT TO REPEAT"))
                Field(s.text, { s = s.copy(text = it) }, w.t("молитва, мантра, имя, слово…", "a prayer, a mantra, a name, a word…"), minLines = 2)
                Text(w.t("Во время практики бусины двигаются касанием или кнопками громкости — можно не смотреть на экран.", "During the practice the beads move with a tap or the volume keys, so you needn't look at the screen."), style = T.Small, modifier = Modifier.padding(top = 6.dp))
            }
            Kind.READ -> {
                val chosen = s.mode.split(',').filter { it.isNotEmpty() }.toSet()
                Label(w.t("ОТКУДА", "FROM WHERE"))
                Flow {
                    Chip(w.t("Всё подряд", "All of them"), chosen.isEmpty()) { s = s.copy(mode = "") }
                    for (src in Source.entries) Chip(w.source(src), src.name in chosen) {
                        val next = (if (src.name in chosen) chosen - src.name else chosen - Library.OWN + src.name)
                        s = s.copy(mode = next.joinToString(","))
                    }
                    Chip(w.t("Свои тексты", "Your own texts"), Library.OWN in chosen) { s = s.copy(mode = Library.OWN) }
                }
                if (s.mode == Library.OWN) {
                    Label(w.t("СВОИ ТЕКСТЫ, ПО ОДНОМУ НА СТРОКЕ", "YOUR TEXTS, ONE PER LINE"))
                    var text by remember { mutableStateOf(s.list.joinToString("\n")) }
                    Field(text, { text = it; s = s.copy(list = lines(it)) }, w.t("стихи, молитвы, цитаты…", "verses, prayers, quotes…"), minLines = 5, max = 6000)
                }
                Gap(6)
                Toggle(w.t("Lectio divina", "Lectio divina"), w.t("читать медленно и выбрать слово, которое отозвалось; оно ляжет на страницу красным", "read slowly and choose the word that speaks; it goes onto the page in red"), s.n == 1) { s = s.copy(n = if (it) 1 else 0) }
                Text(w.t("Каждый день — новый текст, весь день — один и тот же.", "A new text each day, the same one all day."), style = T.Small)
            }
            Kind.QUESTION -> {
                Label(w.t("СВОЙ ВОПРОС", "YOUR OWN QUESTION"))
                Field(s.text, { s = s.copy(text = it) }, w.t("оставь пустым, чтобы вопросы менялись", "leave empty for changing questions"), minLines = 2)
                if (s.text.isBlank()) {
                    Label(w.t("ИЛИ СВОЙ СПИСОК, ПО ОДНОМУ НА СТРОКЕ", "OR YOUR OWN LIST, ONE PER LINE"))
                    var text by remember { mutableStateOf(s.list.joinToString("\n")) }
                    Field(text, { text = it; s = s.copy(list = lines(it)) }, w.t("каждый день следующий", "a different one each day"), minLines = 3, max = 4000)
                    if (s.list.isEmpty()) {
                        Label(w.t("ИЛИ ВСТРОЕННЫЕ", "OR BUILT-IN"))
                        Flow { for (b in Bank.entries) Chip(w.bank(b), s.mode == b.name) { s = s.copy(mode = b.name) } }
                    }
                }
            }
            Kind.THANKS -> Stepper(w.t("Строк", "Lines"), s.n.coerceAtLeast(1), 1..7) { s = s.copy(n = it) }
            Kind.EXAMEN -> {
                Flow { for (e in Examen.entries) Chip(w.examen(e), s.mode == e.name) { s = s.copy(mode = e.name) } }
                val e = Examen.entries.firstOrNull { it.name == s.mode } ?: Examen.IGNATIUS
                if (e == Examen.OWN) {
                    Label(w.t("ШАГИ, ПО ОДНОМУ НА СТРОКЕ", "STEPS, ONE PER LINE"))
                    var text by remember { mutableStateOf(s.list.joinToString("\n")) }
                    Field(text, { text = it; s = s.copy(list = lines(it)) }, w.t("Что было хорошо?\nЧто было трудно?", "What was good?\nWhat was hard?"), minLines = 4)
                } else {
                    Gap(8)
                    for ((i, p) in Reviews.steps(e, w.ru).withIndex()) Text("${i + 1}. $p", style = T.Body.copy(color = C.inkSoft), modifier = Modifier.padding(vertical = 2.dp))
                }
            }
            Kind.BOWS -> {
                Flow {
                    Chip(w.t("поясные", "from the waist"), s.mode != "GROUND") { s = s.copy(mode = "BOW") }
                    Chip(w.t("земные", "to the ground"), s.mode == "GROUND") { s = s.copy(mode = "GROUND") }
                }
                Stepper(w.t("Поклонов", "Bows"), s.n.coerceAtLeast(1), 1..500) { s = s.copy(n = it) }
            }
            Kind.WALK -> {
                Stepper(w.t("Минут", "Minutes"), s.min, 1..60) { s = s.copy(min = it) }
                Stepper(w.t("Звук шага каждые", "A step sound every"), s.n, 0..10, suffix = w.t(" с", " s")) { s = s.copy(n = it) }
            }
            Kind.GAZE -> {
                Flow {
                    Chip(w.t("свеча", "a candle"), s.mode != "WINDOW") { s = s.copy(mode = "CANDLE") }
                    Chip(w.t("окно", "a window"), s.mode == "WINDOW") { s = s.copy(mode = "WINDOW") }
                }
                Stepper(w.t("Минут", "Minutes"), s.min, 1..60) { s = s.copy(min = it) }
            }
            Kind.SILENCE -> {
                Stepper(w.t("Минут", "Minutes"), s.min, 1..120) { s = s.copy(min = it) }
                Toggle(w.t("Только экраном вниз", "Only face down"), w.t("время идёт, пока телефон лежит экраном вниз", "the time runs while the phone lies face down"), s.mode != "FREE") { s = s.copy(mode = if (it) "FACE" else "FREE") }
            }
            Kind.CUSTOM -> {
                Label(w.t("НАЗВАНИЕ", "NAME"))
                Field(s.text, { s = s.copy(text = it) }, w.t("например, «Утренний чай»", "e.g. Morning tea"))
                Label(w.t("ШАГИ: ТЕКСТ | СЕКУНДЫ", "STEPS: TEXT | SECONDS"))
                var text by remember { mutableStateOf(s.list.joinToString("\n") { "${Codec.customText(it)} | ${Codec.customSeconds(it)}" }) }
                Field(text, { t ->
                    text = t
                    s = s.copy(list = lines(t).map { l -> Codec.custom(l.substringBeforeLast('|').trim(), l.substringAfterLast('|', "0").trim().toIntOrNull() ?: 0) })
                }, w.t("Налей чай | 0\nПросто пей и смотри | 180", "Pour the tea | 0\nJust drink and look | 180"), minLines = 4, max = 3000)
                Text(w.t("0 секунд — шаг ждёт кнопки «Дальше».", "0 seconds waits for the Next button."), style = T.Small)
            }
        }
        Gap(20)
        Btn(w.t("Готово", "Done"), Modifier.fillMaxWidth()) { onSave(s) }
        Gap(12)
    }
}

/** A list of steps with order, removal and adding. */
@Composable
fun StepList(w: Words, steps: List<Step>, onEdit: (Int) -> Unit, onChange: (List<Step>) -> Unit, onAdd: () -> Unit) {
    for ((i, s) in steps.withIndex()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(10.dp)).clickable { onEdit(i) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${i + 1}.", style = T.BodyM.copy(color = C.vermilion), modifier = Modifier.padding(end = 8.dp))
            Column(Modifier.weight(1f)) { Text(w.summary(s), style = T.Body) }
            Small("↑", i > 0) { onChange(steps.toMutableList().apply { add(i - 1, removeAt(i)) }) }
            Small("↓", i < steps.lastIndex) { onChange(steps.toMutableList().apply { add(i + 1, removeAt(i)) }) }
            Small("✕", true) { onChange(steps.toMutableList().apply { removeAt(i) }) }
        }
    }
    Text("+ " + w.t("практика", "a practice"), style = T.BodyM.copy(color = C.vermilion), modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAdd).padding(vertical = 8.dp))
}

@Composable
private fun Small(t: String, on: Boolean, onClick: () -> Unit) {
    Text(t, style = T.Body.copy(color = if (on) C.inkSoft else C.vellumEdge), modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = on, onClick = onClick).padding(horizontal = 9.dp, vertical = 4.dp))
}
