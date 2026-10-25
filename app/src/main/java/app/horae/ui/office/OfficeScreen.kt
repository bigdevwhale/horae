package app.horae.ui.office

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.horae.container
import app.horae.domain.Done
import app.horae.domain.Entry
import app.horae.domain.Hour
import app.horae.domain.Step
import app.horae.ui.Home
import app.horae.ui.art.Illum
import app.horae.ui.components.Btn
import app.horae.ui.components.Gap
import app.horae.ui.components.Kind
import app.horae.ui.components.night
import app.horae.ui.theme.C
import app.horae.ui.theme.T

/**
 * An hour being kept: its practices one after another in a dark room with one flame, then the page is written.
 * Leaving half way still writes what was done.
 */
@Composable
fun OfficeScreen(home: Home, hourId: Long, day: Long, brief: Boolean, onClose: () -> Unit) {
    val c = LocalContext.current.container
    val hour = home.s.hours.firstOrNull { it.id == hourId } ?: run { onClose(); return }
    val steps = if (brief) hour.brief else hour.steps
    val w = home.w
    var at by remember(hourId, brief) { mutableIntStateOf(-1) }
    val done = remember(hourId, brief) { mutableStateListOf<Done>() }
    var saved by remember(hourId, brief) { mutableIntStateOf(0) }

    fun save() {
        if (saved != 0) return
        saved = 1
        val e = Entry(day = day, hourId = hour.id, hourName = hour.name, at = System.currentTimeMillis(), brief = brief, items = done.toList())
        c.write { repo.keep(e) }
    }

    fun close() {
        if (done.isNotEmpty() && at < steps.size) save()
        c.beadsInHand = false
        onClose()
    }

    BackHandler { close() }
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false; c.beadsInHand = false; c.sfx.stop() }
    }

    fun next(d: Done?) {
        if (d != null) done += d
        at += 1
        if (at >= steps.size) {
            save()
            c.sfx.stroke(hour.bell)
            c.sfx.feel(60, 120)
        }
    }

    Box(Modifier.fillMaxSize().night().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    hour.name.uppercase() + if (at in steps.indices) "  ·  ${at + 1}/${steps.size}" else "",
                    style = T.Rubric.copy(color = C.candle.copy(alpha = .7f)), modifier = Modifier.weight(1f),
                )
                Text(
                    if (at >= steps.size) w.t("Закрыть", "Close") else w.t("Выйти", "Leave"), style = T.Button.copy(color = C.faded),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { close() }.padding(10.dp),
                )
            }
            AnimatedContent(at, Modifier.weight(1f).fillMaxWidth(), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "step") { i ->
                when {
                    i < 0 -> Intro(home, hour, steps, brief) {
                        c.sfx.stroke(hour.bell)
                        if (steps.isEmpty()) { at = 0; save() } else at = 0
                    }
                    i >= steps.size -> Kept(home, hour, done)
                    else -> StepView(home, hour, steps[i], day, i) { next(it) }
                }
            }
        }
    }
}

@Composable
private fun Intro(home: Home, hour: Hour, steps: List<Step>, brief: Boolean, onStart: () -> Unit) {
    val w = home.w
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Canvas(Modifier.size(90.dp, 140.dp)) { Illum.candle(this, Offset(size.width / 2, size.height), size.height * .95f, 0f) }
        Gap(12)
        Text(hour.name, style = T.Big.copy(textAlign = TextAlign.Center))
        Text(
            (if (brief) w.t("Краткое правило · ", "Short rule · ") else "") + w.anchor(hour) + " · " + w.total(steps),
            style = T.Small.copy(color = C.candle.copy(alpha = .6f), textAlign = TextAlign.Center),
        )
        Gap(22)
        for ((i, s) in steps.withIndex()) {
            Text("${i + 1}. ${w.summary(s)}", style = T.Body.copy(color = C.candle), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp))
        }
        if (steps.isEmpty()) Text(
            w.t("В этом часе пока нет практик. Можно просто отметить его — или добавить практики в уставе.", "This hour has no practices yet. You can simply mark it, or add practices in the rule."),
            style = T.Italic.copy(color = C.candle.copy(alpha = .7f), textAlign = TextAlign.Center),
        )
        Gap(28)
        Btn(if (steps.isEmpty()) w.t("Отметить час", "Mark the hour") else w.t("Начать", "Begin"), Modifier.fillMaxWidth(), Kind.CANDLE, onClick = onStart)
    }
}

@Composable
private fun Kept(home: Home, hour: Hour, done: List<Done>) {
    val w = home.w
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Canvas(Modifier.size(120.dp)) {
            Illum.rosette(this, center, size.minDimension * .3f, C.gold)
        }
        Gap(16)
        Text(w.t("Час соблюдён", "The hour is kept"), style = T.Big.copy(textAlign = TextAlign.Center))
        Gap(6)
        val minutes = (done.sumOf { it.seconds } + 30) / 60
        Text(
            w.t("${hour.name} вписан в сегодняшнюю страницу", "${hour.name} is written on today's page") + if (minutes > 0) w.t(" · $minutes мин", " · $minutes min") else "",
            style = T.Body.copy(color = C.candle.copy(alpha = .75f), textAlign = TextAlign.Center),
        )
    }
}
