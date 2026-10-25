package app.horae.ui.office

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.horae.container
import app.horae.domain.Beads
import app.horae.domain.Bell
import app.horae.domain.Breath
import app.horae.domain.Codec
import app.horae.domain.Done
import app.horae.domain.Examen
import app.horae.domain.Hour
import app.horae.domain.Kind
import app.horae.domain.Metta
import app.horae.domain.Reviews
import app.horae.domain.Sit
import app.horae.domain.Step
import app.horae.ui.Home
import app.horae.ui.art.Illum
import app.horae.ui.components.Btn
import app.horae.ui.components.Field
import app.horae.ui.components.Flow
import app.horae.ui.components.Gap
import app.horae.ui.components.Kind as BtnKind
import app.horae.ui.theme.C
import app.horae.ui.theme.T
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Milliseconds since the step began, counted only while [running]. */
@Composable
fun rememberElapsed(running: Boolean): Long {
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = -1L
        while (true) {
            withFrameMillis { t ->
                if (last >= 0) elapsed += t - last
                last = t
            }
        }
    }
    return elapsed
}

@Composable
fun StepView(home: Home, hour: Hour, step: Step, day: Long, index: Int, onDone: (Done?) -> Unit) {
    when (step.kind) {
        Kind.BREATH -> BreathStep(home, step, onDone)
        Kind.SIT, Kind.METTA, Kind.GAZE, Kind.WALK, Kind.SILENCE -> TimedStep(home, hour, step, onDone)
        Kind.BEADS -> BeadsStep(home, step, onDone)
        Kind.BOWS -> BowsStep(home, step, onDone)
        Kind.READ -> ReadStep(home, step, day, hour.id * 31 + index, onDone)
        Kind.QUESTION -> QuestionStep(home, step, day, hour.id * 31 + index, onDone)
        Kind.THANKS -> ThanksStep(home, step, onDone)
        Kind.EXAMEN -> ExamenStep(home, step, onDone)
        Kind.CUSTOM -> CustomStep(home, step, onDone)
    }
}

/** The common frame of a step: its name, the work in the middle, and the way on at the bottom. */
@Composable
private fun Frame(home: Home, kind: Kind, sub: String?, scroll: Boolean = false, bottom: @Composable ColumnScope.() -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 8.dp)) {
        Text(home.w.kind(kind), style = T.Heading.copy(color = C.candle))
        if (sub != null) Text(sub, style = T.Small.copy(color = C.candle.copy(alpha = .55f)))
        Column(
            Modifier.weight(1f).fillMaxWidth().then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = if (scroll) Arrangement.Top else Arrangement.Center, content = content,
        )
        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), content = bottom)
    }
}

@Composable
private fun Two(left: String, right: String, onLeft: () -> Unit, onRight: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Btn(left, Modifier.weight(1f), BtnKind.DARK, onClick = onLeft)
        Btn(right, Modifier.weight(1f), BtnKind.CANDLE, onClick = onRight)
    }
}

private fun mmss(ms: Long): String { val s = (ms / 1000).coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }

// ——— Breathing ———

@Composable
private fun BreathStep(home: Home, step: Step, onDone: (Done?) -> Unit) {
    val w = home.w
    val c = LocalContext.current.container
    val b = Breath.of(step.mode)
    val ph = remember(step) { Breath.phases(b, step.text) }
    val cycle = ph.sum() * 1000
    val cycles = if (step.n > 0) step.n else max(1, ceil(step.min * 60_000 / cycle).toInt())
    var running by remember { mutableStateOf(true) }
    val t = rememberElapsed(running)
    val k = (t / cycle).toInt()
    val inCycle = t - k * cycle
    var phase = 0
    var acc = 0.0
    while (phase < 3 && inCycle >= acc + ph[phase] * 1000) { acc += ph[phase] * 1000; phase++ }
    // Phases of zero length are skipped.
    while (phase < 3 && ph[phase] == 0.0) phase++
    val into = ((inCycle - acc) / (ph[phase] * 1000).coerceAtLeast(1.0)).coerceIn(0.0, 1.0)
    val fill = when (phase) { 0 -> into; 1 -> 1.0; 2 -> 1 - into; else -> 0.0 }.toFloat()
    val finished = k >= cycles
    LaunchedEffect(phase, k) { if (!finished && t > 0) c.sfx.feel(if (phase == 0) 40 else 18, if (phase == 0) 90 else 40) }
    LaunchedEffect(finished) { if (finished) { c.sfx.stroke(Bell.BOWL, .5f); onDone(Done(Kind.BREATH, (t / 1000).toInt(), cycles, b.name)) } }
    val left = k % 2 == 0
    val label = when (phase) {
        0 -> w.t("Вдох", "In") + if (b == Breath.NADI) w.t(if (left) " — левой" else " — правой", if (left) " — left" else " — right") else ""
        1 -> w.t("Задержка", "Hold")
        2 -> w.t("Выдох", "Out") + if (b == Breath.NADI) w.t(if (left) " — правой" else " — левой", if (left) " — right" else " — left") else ""
        else -> w.t("Пауза", "Rest")
    }
    val secs = ceil(ph[phase] * (1 - into)).toInt().coerceAtLeast(1)
    Frame(home, Kind.BREATH, w.breath(b) + " · " + w.t("цикл ${min(k + 1, cycles)} из $cycles", "cycle ${min(k + 1, cycles)} of $cycles"), bottom = {
        Two(if (running) w.t("Пауза", "Pause") else w.t("Дальше", "Resume"), w.t("Закончить", "Finish"), { running = !running }) {
            onDone(Done(Kind.BREATH, (t / 1000).toInt(), k, b.name))
        }
    }) {
        Canvas(Modifier.size(260.dp)) {
            val r = size.minDimension / 2
            drawCircle(C.nightLine, r * .95f, center, style = Stroke(2f))
            val rr = r * (.32f + .6f * fill)
            drawCircle(Brush.radialGradient(listOf(C.candleHi.copy(alpha = .9f), C.candle.copy(alpha = .45f), Color.Transparent), center, rr * 1.15f), rr * 1.15f, center)
            drawCircle(C.candle.copy(alpha = .8f), rr, center, style = Stroke(3f))
        }
        Gap(12)
        Text(label, style = T.Big)
        Text("$secs", style = T.Body.copy(color = C.candle.copy(alpha = .6f)))
    }
}

// ——— Timed practices: sitting, metta, gazing, walking, silence ———

@Composable
private fun TimedStep(home: Home, hour: Hour, step: Step, onDone: (Done?) -> Unit) {
    val w = home.w
    val ctx = LocalContext.current
    val c = ctx.container
    val total = max(1, step.min) * 60_000L
    var running by remember { mutableStateOf(true) }
    // Silence counts only while the phone lies face down, when asked so.
    var faceDown by remember { mutableStateOf(false) }
    val needsFace = step.kind == Kind.SILENCE && step.mode != "FREE"
    if (needsFace) {
        DisposableEffect(Unit) {
            val sm = ctx.getSystemService(SensorManager::class.java)
            val acc = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val l = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) { faceDown = e.values[2] < -7f }
                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }
            if (acc != null) sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_NORMAL) else faceDown = true
            onDispose { sm?.unregisterListener(l) }
        }
    }
    val ticking = running && (!needsFace || faceDown)
    val t = rememberElapsed(ticking)
    val left = total - t
    val bell = if (hour.bell == Bell.NONE) Bell.BOWL else hour.bell
    LaunchedEffect(Unit) { if (step.kind == Kind.SIT || step.kind == Kind.METTA) c.sfx.stroke(bell, .6f) }
    // Interval bells while sitting.
    val every = step.every * 60_000L
    val mark = if (every > 0) (t / every).toInt() else 0
    LaunchedEffect(mark) { if (mark > 0 && t < total) { c.sfx.stroke(bell, .45f); c.sfx.feel(40, 60) } }
    // The steady tick of slow walking.
    val pace = step.n * 1000L
    val beat = if (step.kind == Kind.WALK && pace > 0) (t / pace).toInt() else 0
    LaunchedEffect(beat) { if (beat > 0 && t < total) { c.sfx.play(app.horae.sound.Sfx.BEAD, .7f); c.sfx.feel(20, 50) } }
    val finished = t >= total
    LaunchedEffect(finished) {
        if (finished) {
            c.sfx.stroke(bell, .8f); c.sfx.feel(200, 120)
            onDone(Done(step.kind, (total / 1000).toInt(), mode = step.mode))
        }
    }
    val sub = when (step.kind) {
        Kind.SIT -> w.sit(Sit.entries.firstOrNull { it.name == step.mode } ?: Sit.BREATH)
        Kind.GAZE -> if (step.mode == "WINDOW") w.t("в окно", "out of the window") else w.t("на пламя свечи", "into a candle flame")
        Kind.SILENCE -> if (needsFace) w.t("телефон экраном вниз", "phone face down") else w.t("просто тишина", "just quiet")
        else -> null
    }
    Frame(home, step.kind, sub, bottom = {
        Two(if (running) w.t("Пауза", "Pause") else w.t("Дальше", "Resume"), w.t("Закончить", "Finish"), { running = !running }) {
            onDone(Done(step.kind, (t / 1000).toInt(), mode = step.mode))
        }
    }) {
        val frac = (t.toFloat() / total).coerceIn(0f, 1f)
        when (step.kind) {
            Kind.GAZE -> Canvas(Modifier.size(220.dp, 300.dp)) {
                if (step.mode == "WINDOW") window(this, frac) else Illum.candle(this, Offset(size.width / 2, size.height * .98f), size.height * .95f, t / 1000f)
            }
            Kind.METTA -> {
                val phrases = step.list.ifEmpty { Metta.phrases(home.ru) }
                val circle = Metta.circle(home.ru)
                val who = circle[min(circle.lastIndex, (frac * circle.size).toInt())]
                val phrase = phrases[((t / 7000) % phrases.size).toInt()]
                Text(who.uppercase(), style = T.Rubric.copy(color = C.rose))
                Gap(18)
                Text(phrase, style = T.Big.copy(textAlign = TextAlign.Center))
                Gap(30)
                Canvas(Modifier.size(140.dp)) { incense(this, frac, t / 1000f) }
            }
            Kind.WALK -> {
                Canvas(Modifier.size(200.dp)) { steps(this, beat) }
                Gap(8)
                Text(w.t("Медленно. Шаг на каждый звук.", "Slowly. A step to each sound."), style = T.Body.copy(color = C.candle.copy(alpha = .7f), textAlign = TextAlign.Center))
            }
            Kind.SILENCE -> {
                Canvas(Modifier.size(200.dp)) {
                    drawCircle(C.nightLine, size.minDimension * .45f, center, style = Stroke(3f))
                    drawArc(C.candle, -90f, 360f * frac, false, center - Offset(size.minDimension * .45f, size.minDimension * .45f), Size(size.minDimension * .9f, size.minDimension * .9f), style = Stroke(5f, cap = StrokeCap.Round))
                }
                if (needsFace && !faceDown) {
                    Gap(10)
                    Text(w.t("Положи телефон экраном вниз — время пойдёт.", "Lay the phone face down and the time will run."), style = T.Body.copy(color = C.candle, textAlign = TextAlign.Center))
                }
            }
            else -> {
                Canvas(Modifier.size(200.dp, 240.dp)) { incense(this, frac, t / 1000f) }
                Gap(10)
                Text(sitHint(home, Sit.entries.firstOrNull { it.name == step.mode } ?: Sit.BREATH, frac), style = T.Italic.copy(color = C.candle.copy(alpha = .75f), textAlign = TextAlign.Center))
            }
        }
        Gap(14)
        Text(mmss(left), style = T.Big.copy(fontSize = 26.sp, color = C.candle.copy(alpha = .8f)))
    }
}

private fun sitHint(home: Home, s: Sit, frac: Float): String {
    val w = home.w
    return when (s) {
        Sit.BREATH -> if (frac < .1f) w.t("Найди дыхание там, где оно ощущается яснее всего. Когда ум уходит — просто вернись.", "Find the breath where it is clearest. When the mind wanders, just come back.")
        else w.t("Вдох. Выдох. Снова сюда.", "In. Out. Back here again.")
        Sit.BODY -> listOf(
            w.t("Стопы. Их вес, тепло, пол под ними.", "Feet. Their weight, their warmth, the floor beneath."),
            w.t("Ноги. Колени. Бёдра.", "Legs. Knees. Hips."),
            w.t("Живот и спина. Как они движутся с дыханием.", "Belly and back. How they move with the breath."),
            w.t("Руки, плечи, шея.", "Hands, shoulders, neck."),
            w.t("Лицо: лоб, глаза, челюсть. Отпусти.", "Face: brow, eyes, jaw. Let go."),
            w.t("Всё тело целиком — одно поле ощущений.", "The whole body, one field of sensation."),
        ).let { it[min(it.lastIndex, (frac * it.size).toInt())] }
        Sit.NOTING -> w.t("Что бы ни возникло — мысль, звук, зуд — мягко назови: «думание», «слышание». И отпусти.", "Whatever arises — a thought, a sound, an itch — softly name it: thinking, hearing. Let it go.")
        Sit.OPEN -> w.t("Ничего не держи. Пусть всё приходит и уходит, как облака в небе.", "Hold nothing. Let everything come and go, like clouds across the sky.")
    }
}

/** A stick of incense burning down as the time passes, its smoke curling up. */
private fun incense(d: DrawScope, frac: Float, t: Float) = with(d) {
    val baseY = size.height * .92f
    val x = size.width / 2
    drawRoundRect(Color(0xFF4A3426), Offset(x - size.width * .2f, baseY), Size(size.width * .4f, size.height * .06f), CornerRadius(8f))
    val full = size.height * .55f
    val left = full * (1 - frac * .92f)
    drawLine(Color(0xFF7A5236), Offset(x, baseY), Offset(x, baseY - full * 1.04f), 3f)
    drawLine(Color(0xFF5C3A26), Offset(x, baseY), Offset(x, baseY - left), 7f, cap = StrokeCap.Round)
    val tip = Offset(x, baseY - left)
    drawCircle(Brush.radialGradient(listOf(C.ember, Color.Transparent), tip, 12f), 12f, tip)
    drawCircle(C.ember, 4f, tip)
    val smoke = Path().apply {
        moveTo(tip.x, tip.y)
        var y = tip.y
        var i = 0
        while (y > 0 && i < 60) {
            y -= 6f; i++
            lineTo(tip.x + sin(i * .25f + t * 1.4f) * (i * .6f) + sin(i * .11f - t * .7f) * 6f, y)
        }
    }
    drawPath(smoke, Brush.verticalGradient(listOf(Color.Transparent, C.candle.copy(alpha = .25f)), 0f, tip.y), style = Stroke(2.5f, cap = StrokeCap.Round))
}

/** A window with its sky slowly turning, for gazing outward. */
private fun window(d: DrawScope, frac: Float) = with(d) {
    val l = size.width * .1f; val tp = size.height * .08f; val ww = size.width * .8f; val hh = size.height * .8f
    drawRect(Brush.verticalGradient(listOf(Color(0xFF2B3F66), Color(0xFF8C6A6A).copy(alpha = .8f + frac * .2f), Color(0xFFE5A86A)), tp, tp + hh), Offset(l, tp), Size(ww, hh))
    drawCircle(C.candleHi.copy(alpha = .8f), ww * .06f, Offset(l + ww * .7f, tp + hh * (.3f + frac * .1f)))
    drawRect(C.nightCard, Offset(l, tp), Size(ww, hh), style = Stroke(12f))
    drawLine(C.nightCard, Offset(l + ww / 2, tp), Offset(l + ww / 2, tp + hh), 8f)
    drawLine(C.nightCard, Offset(l, tp + hh * .45f), Offset(l + ww, tp + hh * .45f), 8f)
}

private fun steps(d: DrawScope, beat: Int) = with(d) {
    for (i in 0 until 6) {
        val lit = i <= beat % 6
        val y = size.height * (.9f - i * .15f)
        val x = size.width / 2 + if (i % 2 == 0) -size.width * .1f else size.width * .1f
        drawOval(if (lit) C.candle.copy(alpha = .9f - (beat % 6 - i) * .12f) else C.nightLine, Offset(x - 12f, y - 22f), Size(24f, 40f))
    }
}

// ——— Beads ———

@Composable
private fun BeadsStep(home: Home, step: Step, onDone: (Done?) -> Unit) {
    val w = home.w
    val c = LocalContext.current.container
    val skin = Beads.entries.firstOrNull { it.name == step.mode } ?: Beads.PLAIN
    val total = if (step.n > 0) step.n else skin.count
    val every = step.every
    var count by remember { mutableIntStateOf(0) }
    val started = remember { System.currentTimeMillis() }
    val done by rememberUpdatedState(count >= total)
    fun move() {
        if (count >= total) return
        count++
        val heavy = every > 0 && count % every == 0
        c.sfx.play(app.horae.sound.Sfx.BEAD, if (heavy) 1f else .7f)
        c.sfx.feel(if (heavy) 70 else 18, if (heavy) 160 else 70)
    }
    DisposableEffect(Unit) { c.beadsInHand = true; onDispose { c.beadsInHand = false } }
    LaunchedEffect(Unit) { c.keys.collect { move() } }
    LaunchedEffect(done) {
        if (done) {
            c.beadsInHand = false
            c.sfx.stroke(Bell.HAND, .7f)
            onDone(Done(Kind.BEADS, ((System.currentTimeMillis() - started) / 1000).toInt(), count, skin.name, step.text))
        }
    }
    Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { move() }) {
        Frame(home, Kind.BEADS, w.beads(skin) + " · " + w.t("касание или кнопка громкости", "tap or a volume key"), bottom = {
            Btn(w.t("Закончить на $count", "Finish at $count"), Modifier.fillMaxWidth(), BtnKind.DARK) {
                c.beadsInHand = false
                onDone(Done(Kind.BEADS, ((System.currentTimeMillis() - started) / 1000).toInt(), count, skin.name, step.text))
            }
        }) {
            if (step.text.isNotBlank()) {
                Text(step.text, style = T.Big.copy(fontSize = 24.sp, lineHeight = 30.sp, textAlign = TextAlign.Center))
                Gap(18)
            }
            Canvas(Modifier.fillMaxWidth().height(300.dp)) { chain(this, skin, count, total, every) }
            Gap(8)
            Text("$count / $total", style = T.Big.copy(fontSize = 26.sp, color = C.candle.copy(alpha = .8f)))
        }
    }
}

/** The beads passing through the fingers: the current one in the middle, the counted ones dropping below. */
private fun chain(d: DrawScope, skin: Beads, count: Int, total: Int, every: Int) = with(d) {
    val cx = size.width / 2
    val gap = size.height / 9
    drawLine(C.nightLine, Offset(cx, 0f), Offset(cx, size.height), 3f)
    for (k in -4..4) {
        val idx = count + k
        if (idx < 0 || idx > total) continue
        val y = size.height / 2 - k * gap
        val heavy = every > 0 && idx > 0 && idx % every == 0
        val near = 1f - kotlin.math.abs(k) / 5f
        val color = when {
            heavy -> C.vermilion
            k < 0 -> C.inkSoft.copy(alpha = .9f)
            else -> when (skin) { Beads.MALA -> Color(0xFF9A6A3C); Beads.TASBIH -> Color(0xFF3F6E6A); Beads.ROSARY -> Color(0xFFD9CBB0); else -> Color(0xFF6B4A33) }
        }.copy(alpha = .35f + .65f * near)
        val r = gap * (if (k == 0) .42f else .3f) * (if (heavy) 1.25f else 1f)
        if (skin == Beads.LESTOVKA) {
            // The lestovka's leather steps rather than round beads.
            drawRoundRect(color, Offset(cx - r * 1.5f, y - r * .55f), Size(r * 3f, r * 1.1f), CornerRadius(r * .4f))
            drawRoundRect(Color.Black.copy(alpha = .25f), Offset(cx - r * 1.5f, y - r * .55f), Size(r * 3f, r * 1.1f), CornerRadius(r * .4f), style = Stroke(2f))
        } else {
            drawCircle(color, r, Offset(cx, y))
            drawCircle(Color.White.copy(alpha = .25f * near), r * .3f, Offset(cx - r * .35f, y - r * .35f))
        }
        if (k == 0) drawCircle(C.candle.copy(alpha = .5f), r * 1.35f, Offset(cx, y), style = Stroke(2f))
    }
}

// ——— Bows ———

@Composable
private fun BowsStep(home: Home, step: Step, onDone: (Done?) -> Unit) {
    val w = home.w
    val c = LocalContext.current.container
    val total = max(1, step.n)
    var count by remember { mutableIntStateOf(0) }
    val started = remember { System.currentTimeMillis() }
    fun bow() {
        if (count >= total) return
        count++
        c.sfx.feel(40, 90)
        if (count == total) {
            c.sfx.stroke(Bell.HAND, .6f)
            onDone(Done(Kind.BOWS, ((System.currentTimeMillis() - started) / 1000).toInt(), count, step.mode))
        }
    }
    DisposableEffect(Unit) { c.beadsInHand = true; onDispose { c.beadsInHand = false } }
    LaunchedEffect(Unit) { c.keys.collect { bow() } }
    Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { bow() }) {
        Frame(home, Kind.BOWS, if (step.mode == "GROUND") w.t("земные", "to the ground") else w.t("поясные", "from the waist"), bottom = {
            Btn(w.t("Закончить на $count", "Finish at $count"), Modifier.fillMaxWidth(), BtnKind.DARK) { onDone(Done(Kind.BOWS, ((System.currentTimeMillis() - started) / 1000).toInt(), count, step.mode)) }
        }) {
            Canvas(Modifier.size(200.dp)) {
                val s = size.minDimension
                val a = (count % 2) * 1f
                val hip = Offset(s * .5f, s * .62f)
                drawLine(C.candle, hip, Offset(s * .5f, s * .95f), 6f, cap = StrokeCap.Round)
                val ang = (-90f + a * 70f) * PI.toFloat() / 180
                val head = Offset(hip.x + cos(ang) * s * .3f, hip.y + sin(ang) * s * .3f)
                drawLine(C.candle, hip, head, 6f, cap = StrokeCap.Round)
                drawCircle(C.candle, s * .06f, Offset(head.x + cos(ang) * s * .06f, head.y + sin(ang) * s * .06f))
            }
            Gap(10)
            Text(w.t("Поклон — касание экрана", "A bow, a tap on the screen"), style = T.Body.copy(color = C.candle.copy(alpha = .6f)))
            Gap(10)
            Text("$count / $total", style = T.Big.copy(fontSize = 26.sp))
        }
    }
}

// ——— Reading ———

@Composable
private fun ReadStep(home: Home, step: Step, day: Long, salt: Long, onDone: (Done?) -> Unit) {
    val w = home.w
    val r = remember(step, day) { home.lib.reading(step, day, home.s.salt + salt) }
    val lectio = step.n == 1
    var word by remember { mutableStateOf("") }
    Frame(home, Kind.READ, if (lectio) w.t("lectio: медленно, дважды; коснись слова, которое отозвалось", "lectio: slowly, twice; touch the word that speaks") else null, scroll = true, bottom = {
        Btn(w.t("Прочитано", "Read"), Modifier.fillMaxWidth(), BtnKind.CANDLE) {
            onDone(if (r == null) null else Done(Kind.READ, 120, mode = r.ref(home.ru), text = r.text(home.ru), word = word))
        }
    }) {
        Gap(30)
        if (r == null) {
            Text(w.t("Своих текстов пока нет — их можно добавить в настройках этой практики.", "No texts of your own yet; add them in this practice's settings."), style = T.Italic.copy(color = C.candle))
        } else {
            if (r.ref(home.ru).isNotEmpty()) Text(r.ref(home.ru).uppercase(), style = T.Rubric.copy(color = C.ember))
            Gap(14)
            if (lectio) {
                Flow(gap = 6.dp) {
                    for (token in r.text(home.ru).split(' ')) {
                        val clean = token.trim(',', '.', ';', ':', '—', '«', '»', '!', '?', '"')
                        Text(
                            token, style = T.Big.copy(fontSize = 25.sp, lineHeight = 34.sp, color = if (clean == word && word.isNotEmpty()) C.vermilion.copy(red = 1f) else C.candleHi),
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { word = clean },
                        )
                    }
                }
            } else Text(r.text(home.ru), style = T.Big.copy(fontSize = 25.sp, lineHeight = 34.sp))
        }
    }
}

// ——— Writing: a question, thanks, the review ———

@Composable
private fun QuestionStep(home: Home, step: Step, day: Long, salt: Long, onDone: (Done?) -> Unit) {
    val w = home.w
    val q = remember(step, day) { home.lib.question(step, day, home.s.salt + salt, home.ru) }
    var a by remember { mutableStateOf("") }
    Frame(home, Kind.QUESTION, null, scroll = true, bottom = {
        Two(w.t("Без слов", "No words"), w.t("Вписать", "Write it"), { onDone(Done(Kind.QUESTION, 30, word = q)) }) {
            onDone(Done(Kind.QUESTION, 90, text = a.trim(), word = q))
        }
    }) {
        Gap(20)
        Text(q, style = T.Big.copy(fontSize = 26.sp, lineHeight = 33.sp))
        Gap(18)
        Field(a, { a = it }, w.t("Ответ ляжет на страницу…", "The answer goes onto the page…"), minLines = 4, dark = true)
    }
}

@Composable
private fun ThanksStep(home: Home, step: Step, onDone: (Done?) -> Unit) {
    val w = home.w
    val n = max(1, step.n)
    val lines = remember { mutableStateListOf(*Array(n) { "" }) }
    Frame(home, Kind.THANKS, null, scroll = true, bottom = {
        Btn(w.t("Вписать", "Write it"), Modifier.fillMaxWidth(), BtnKind.CANDLE) {
            val t = lines.map { it.trim() }.filter { it.isNotEmpty() }
            onDone(Done(Kind.THANKS, 30 * n, t.size, text = t.joinToString("\n")))
        }
    }) {
        Gap(20)
        Text(if (n == 1) w.t("За что сегодня спасибо?", "What are you thankful for today?") else w.t("За что сегодня спасибо? $n строки.", "What are you thankful for today? $n lines."), style = T.Big.copy(fontSize = 25.sp))
        Gap(14)
        for (i in 0 until n) {
            Field(lines[i], { lines[i] = it }, "${i + 1}.", dark = true)
            Gap(8)
        }
    }
}

@Composable
private fun ExamenStep(home: Home, step: Step, onDone: (Done?) -> Unit) {
    val w = home.w
    val e = Examen.entries.firstOrNull { it.name == step.mode } ?: Examen.IGNATIUS
    val parts = if (e == Examen.OWN) step.list else Reviews.steps(e, home.ru)
    var i by remember { mutableIntStateOf(0) }
    val notes = remember { mutableStateListOf(*Array(max(1, parts.size)) { "" }) }
    val started = remember { System.currentTimeMillis() }
    fun finish() = onDone(Done(Kind.EXAMEN, ((System.currentTimeMillis() - started) / 1000).toInt(), parts.size, e.name, notes.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")))
    if (parts.isEmpty()) { LaunchedEffect(Unit) { onDone(null) }; return }
    Frame(home, Kind.EXAMEN, w.examen(e) + " · ${i + 1}/${parts.size}", scroll = true, bottom = {
        Btn(if (i < parts.lastIndex) w.t("Дальше", "Next") else w.t("Вписать", "Write it"), Modifier.fillMaxWidth(), BtnKind.CANDLE) { if (i < parts.lastIndex) i++ else finish() }
    }) {
        Gap(24)
        Text(parts[i], style = T.Big.copy(fontSize = 25.sp, lineHeight = 32.sp))
        Gap(16)
        Field(notes[i], { notes[i] = it }, w.t("Можно записать — или просто побыть с этим", "Write, or just stay with it"), minLines = 3, dark = true)
    }
}

// ——— Own practice ———

@Composable
private fun CustomStep(home: Home, step: Step, onDone: (Done?) -> Unit) {
    val w = home.w
    val c = LocalContext.current.container
    if (step.list.isEmpty()) { LaunchedEffect(Unit) { onDone(null) }; return }
    var i by remember { mutableIntStateOf(0) }
    val text = Codec.customText(step.list[i])
    val secs = Codec.customSeconds(step.list[i])
    val started = remember { System.currentTimeMillis() }
    val t = key(i) { rememberElapsed(secs > 0) }
    fun next() {
        if (i < step.list.lastIndex) { i++; c.sfx.feel(30, 60) }
        else onDone(Done(Kind.CUSTOM, ((System.currentTimeMillis() - started) / 1000).toInt(), step.list.size, text = step.text))
    }
    LaunchedEffect(i, t >= secs * 1000L) { if (secs > 0 && t >= secs * 1000L) { c.sfx.stroke(Bell.HAND, .5f); next() } }
    Frame(home, Kind.CUSTOM, step.text.ifBlank { null }?.let { "$it · ${i + 1}/${step.list.size}" } ?: "${i + 1}/${step.list.size}", bottom = {
        Btn(w.t("Дальше", "Next"), Modifier.fillMaxWidth(), BtnKind.CANDLE) { next() }
    }) {
        Text(text, style = T.Big.copy(textAlign = TextAlign.Center))
        if (secs > 0) { Gap(16); Text(mmss(secs * 1000L - t), style = T.Big.copy(fontSize = 26.sp, color = C.candle.copy(alpha = .7f))) }
    }
}

@Composable
private fun <T> key(k: Any, block: @Composable () -> T): T = androidx.compose.runtime.key(k) { block() }
