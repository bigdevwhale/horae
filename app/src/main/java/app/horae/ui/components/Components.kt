package app.horae.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.horae.ui.theme.C
import app.horae.ui.theme.T
import kotlin.random.Random
import kotlinx.coroutines.delay

/** Vellum: warm skin with faint flecks and a darker edge, the ground of every screen. */
fun Modifier.vellum(base: Color = C.vellum) = this.drawBehind {
    drawRect(base)
    val r = Random(17)
    val n = (size.width * size.height / 900f).toInt().coerceAtMost(2600)
    repeat(n) {
        val a = r.nextFloat() * .05f
        drawCircle(Color(0xFF6B4E2A).copy(alpha = a), r.nextFloat() * 1.6f + .4f, Offset(r.nextFloat() * size.width, r.nextFloat() * size.height))
    }
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0x22704C1E)), center = Offset(size.width / 2, size.height / 2), radius = maxOf(size.width, size.height) * .75f))
}

/** The dark of the night office. */
fun Modifier.night() = this.drawBehind {
    drawRect(C.night)
    drawRect(Brush.radialGradient(listOf(Color(0x33F6D58A), Color.Transparent), center = Offset(size.width / 2, size.height * .42f), radius = size.width * .9f))
}

enum class Kind { RED, GHOST, CANDLE, SOFT, DARK }

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, kind: Kind = Kind.RED, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val (bg, fg) = when (kind) {
        Kind.RED -> C.vermilion to C.vellum
        Kind.GHOST -> Color.Transparent to C.ink
        Kind.CANDLE -> C.candle to C.night
        Kind.SOFT -> C.card to C.ink
        Kind.DARK -> C.nightCard to C.candle
    }
    Box(
        modifier
            .clip(shape)
            .background(if (enabled) bg else C.vellumEdge.copy(alpha = .5f))
            .then(if (kind == Kind.GHOST) Modifier.border(1.dp, if (enabled) C.inkSoft else C.faded, shape) else Modifier)
            .then(if (kind == Kind.DARK) Modifier.border(1.dp, C.nightLine, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = T.Button.copy(color = if (enabled) fg else C.faded), textAlign = TextAlign.Center) }
}

@Composable
fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, dot: Color? = null, dark: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val (bgOn, fgOn, bgOff, fgOff, line) = if (dark) listOf(C.candle, C.night, C.nightCard, C.candle, C.nightLine) else listOf(C.ink, C.vellum, C.card, C.ink, C.vellumEdge)
    Row(
        modifier.clip(shape).background(if (on) bgOn else bgOff).border(1.dp, if (on) bgOn else line, shape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) { Box(Modifier.size(10.dp).clip(CircleShape).background(dot)); Spacer(Modifier.size(6.dp)) }
        Text(text, style = T.Body.copy(color = if (on) fgOn else fgOff))
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, bg: Color = C.card, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(bg).border(1.dp, C.vellumEdge.copy(alpha = .7f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 14.dp, vertical = 12.dp),
        content = content,
    )
}

/** A rubric over a group: red small capitals, as the scribes marked sections. */
@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = C.vermilion) =
    Text(text, style = T.Rubric.copy(color = color), modifier = modifier.padding(top = 18.dp, bottom = 6.dp))

@Composable
fun Gap(h: Int = 10) = Spacer(Modifier.height(h.dp))

@Composable
fun Flow(modifier: Modifier = Modifier, gap: Dp = 6.dp, content: @Composable () -> Unit) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) { content() }
}

@Composable
fun Field(
    value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, minLines: Int = 1, max: Int = 400,
    dark: Boolean = false, number: Boolean = false, style: TextStyle = T.Body,
) {
    val shape = RoundedCornerShape(10.dp)
    val fg = if (dark) C.candleHi else C.ink
    Box(modifier.fillMaxWidth().clip(shape).background(if (dark) C.nightCard else C.card).border(1.dp, if (dark) C.nightLine else C.vellumEdge, shape).padding(horizontal = 12.dp, vertical = 10.dp)) {
        if (value.isEmpty()) Text(hint, style = style.copy(color = (if (dark) C.candle else C.faded).copy(alpha = .6f)))
        BasicTextField(
            value, { onChange(it.take(max)) }, Modifier.fillMaxWidth(), textStyle = style.copy(color = fg), minLines = minLines, cursorBrush = SolidColor(if (dark) C.candle else C.vermilion),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = if (number) KeyboardType.Number else KeyboardType.Text),
        )
    }
}

/** − n + with a label, for counts and minutes. */
@Composable
fun Stepper(label: String, value: Int, range: IntRange, step: Int = 1, suffix: String = "", show: (Int) -> String = { "$it$suffix" }, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.Body, modifier = Modifier.weight(1f))
        RoundKey("−", value - step >= range.first) { onChange((value - step).coerceIn(range)) }
        Text(show(value), style = T.BodyM, textAlign = TextAlign.Center, modifier = Modifier.width(74.dp))
        RoundKey("+", value + step <= range.last) { onChange((value + step).coerceIn(range)) }
    }
}

@Composable
fun RoundKey(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(if (enabled) C.card else Color.Transparent).border(1.dp, C.vellumEdge, CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = T.Heading.copy(color = if (enabled) C.ink else C.vellumEdge)) }
}

@Composable
fun Toggle(label: String, sub: String? = null, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!on) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = T.Body)
            if (sub != null) Text(sub, style = T.Small)
        }
        Box(Modifier.width(46.dp).height(26.dp).clip(RoundedCornerShape(13.dp)).background(if (on) C.vermilion else C.vellumEdge), contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart) {
            Box(Modifier.padding(3.dp).size(20.dp).clip(CircleShape).background(C.vellum))
        }
    }
}

/** The title of a tab. */
@Composable
fun TopBar(title: String, sub: String? = null, end: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Title)
            if (sub != null) Text(sub, style = T.Small)
        }
        end()
    }
}

@Composable
fun TextKey(text: String, color: Color = C.vermilion, onClick: () -> Unit) =
    Text(text, style = T.Button.copy(color = color), modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(10.dp))

/** A full-screen sheet over the book, with a title and a way back. */
@Composable
fun Sheet(title: String, onClose: () -> Unit, closeLabel: String, end: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(C.scrim).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(8.dp).clip(RoundedCornerShape(16.dp)).vellum()) {
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = T.Heading, modifier = Modifier.weight(1f))
                end()
                TextKey(closeLabel, C.faded, onClose)
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), content = content)
        }
    }
}

/** Short messages at the bottom of the screen. */
class Toaster {
    var text by mutableStateOf<String?>(null)
    var seq by mutableIntStateOf(0)
    fun show(s: String) { text = s; seq++ }
}

val LocalToast = compositionLocalOf { Toaster() }

@Composable
fun BoxScope.ToastHost(t: Toaster) {
    LaunchedEffect(t.seq) { if (t.text != null) { delay(3200); t.text = null } }
    val s = t.text ?: return
    Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 18.dp, end = 18.dp, bottom = 84.dp)) {
        Text(s, style = T.Body.copy(color = C.vellum, textAlign = TextAlign.Center), modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(C.ink).clickable { t.text = null }.padding(horizontal = 16.dp, vertical = 11.dp))
    }
}
