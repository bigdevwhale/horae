package app.horae.ui.art

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import app.horae.domain.Kind
import app.horae.ui.theme.C
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The little creatures that live in a manuscript's margins. */
enum class Drollery { SNAIL, HARE, BIRD, FISH, CAT, DEMON }

/** Everything painted on the pages: initials, vines, the marks practices leave, drolleries, the dial, the flame. */
object Illum {
    private val brown = Color(0xFF8A5A2E)
    private val brownHi = Color(0xFFB98552)
    private val grey = Color(0xFF6E6862)
    private val white = Color(0xFFFFFBF0)

    /** The ground of an initial; [level] 0 is plain red ink, 1 lapis with red pen-work, 2 burnished gold. */
    fun initialGround(d: DrawScope, r: Rect, level: Int, tail: Boolean = true) = with(d) {
        val s = r.width
        when (level) {
            0 -> Unit
            1 -> {
                drawRect(C.lapis, r.topLeft, r.size)
                // Pen-work flourishes trailing from the square.
                val p = Path().apply {
                    moveTo(r.left, r.bottom); cubicTo(r.left - s * .25f, r.bottom + s * .2f, r.left - s * .1f, r.bottom + s * .6f, r.left - s * .25f, r.bottom + s * .9f)
                    moveTo(r.left + s * .2f, r.bottom); cubicTo(r.left + s * .2f, r.bottom + s * .3f, r.left, r.bottom + s * .4f, r.left + s * .1f, r.bottom + s * .6f)
                }
                drawPath(p, C.vermilion, style = Stroke(s * .025f, cap = StrokeCap.Round))
                for (i in 0..3) drawCircle(C.vermilion, s * .03f, Offset(r.left - s * .08f, r.top + s * (.2f + i * .22f)))
                drawRect(C.vermilion, r.topLeft, r.size, style = Stroke(s * .03f))
            }
            else -> {
                drawRect(C.lapis, r.topLeft, r.size)
                val inner = Rect(r.left + s * .08f, r.top + s * .08f, r.right - s * .08f, r.bottom - s * .08f)
                drawRect(Brush.linearGradient(listOf(C.goldHi, C.gold, C.goldLo, C.gold), inner.topLeft, inner.bottomRight), inner.topLeft, inner.size)
                // White dots of the illuminator's brush round the panel.
                val n = 7
                for (i in 0 until n) {
                    val t = (i + .5f) / n
                    for (o in listOf(Offset(r.left + s * t, r.top + s * .04f), Offset(r.left + s * t, r.bottom - s * .04f), Offset(r.left + s * .04f, r.top + s * t), Offset(r.right - s * .04f, r.top + s * t)))
                        drawCircle(white, s * .014f, o)
                }
                drawRect(C.goldLo, r.topLeft, r.size, style = Stroke(s * .02f))
                if (!tail) return@with
                // An ivy tail with gold bezants, growing down the margin.
                val tail = Path().apply { moveTo(r.left + s * .3f, r.bottom); cubicTo(r.left + s * .1f, r.bottom + s * .5f, r.left + s * .5f, r.bottom + s * .7f, r.left + s * .2f, r.bottom + s * 1.3f) }
                drawPath(tail, C.lapisDeep, style = Stroke(s * .03f, cap = StrokeCap.Round))
                leaf(this, Offset(r.left + s * .32f, r.bottom + s * .62f), s * .16f, 30f, C.malachite)
                leaf(this, Offset(r.left + s * .14f, r.bottom + s * .95f), s * .14f, 200f, C.vermilion)
                bezant(this, Offset(r.left + s * .45f, r.bottom + s * .38f), s * .05f)
                bezant(this, Offset(r.left + s * .05f, r.bottom + s * 1.15f), s * .05f)
            }
        }
    }

    fun bezant(d: DrawScope, at: Offset, r: Float) = with(d) {
        drawCircle(Brush.radialGradient(listOf(C.goldHi, C.gold, C.goldLo), center = at - Offset(r * .3f, r * .3f), radius = r * 1.4f), r, at)
        drawCircle(C.goldLo, r, at, style = Stroke(r * .18f))
    }

    /** An ivy leaf: a heart with a point. */
    fun leaf(d: DrawScope, at: Offset, size: Float, angle: Float, color: Color) = with(d) {
        rotate(angle, at) {
            val p = Path().apply {
                moveTo(at.x, at.y)
                cubicTo(at.x - size * .7f, at.y - size * .2f, at.x - size * .6f, at.y - size * .9f, at.x, at.y - size)
                cubicTo(at.x + size * .6f, at.y - size * .9f, at.x + size * .7f, at.y - size * .2f, at.x, at.y)
                close()
            }
            drawPath(p, color)
            drawLine(white.copy(alpha = .6f), at, Offset(at.x, at.y - size * .8f), size * .06f)
        }
    }

    /**
     * A vine down the margin from [top] to [bottom] around x = [x]. [rich] from 0 to 1 decides how many leaves,
     * bezants and buds it carries; [seed] keeps each page's vine its own.
     */
    fun vine(d: DrawScope, x: Float, top: Float, bottom: Float, w: Float, rich: Float, seed: Int) = with(d) {
        if (bottom - top < w) return@with
        val r = Random(seed)
        val step = w * 2.2f
        val p = Path().apply { moveTo(x, top) }
        var y = top
        var side = 1
        val curls = mutableListOf<Pair<Offset, Int>>()
        while (y < bottom) {
            val ny = minOf(bottom, y + step)
            p.cubicTo(x + side * w * .6f, y + step * .3f, x + side * w * .6f, y + step * .7f, x, ny)
            curls += Offset(x + side * w * .35f, (y + ny) / 2) to side
            y = ny; side = -side
        }
        drawPath(p, C.inkSoft, style = Stroke(w * .07f, cap = StrokeCap.Round))
        for ((o, sd) in curls) {
            val roll = r.nextFloat()
            if (roll < .25f + rich * .6f) {
                // A tendril curling out with a leaf at its end.
                val end = Offset(o.x + sd * w * .9f, o.y - w * .3f)
                val t = Path().apply { moveTo(o.x, o.y); quadraticTo(o.x + sd * w * .6f, o.y + w * .3f, end.x, end.y) }
                drawPath(t, C.inkSoft, style = Stroke(w * .05f, cap = StrokeCap.Round))
                val colors = if (rich > .66f) listOf(C.lapis, C.vermilion, C.malachite, C.gold) else if (rich > .33f) listOf(C.lapis, C.vermilion, C.malachite) else listOf(C.vermilion, C.malachite)
                leaf(this, end, w * (.35f + r.nextFloat() * .15f), if (sd > 0) 60f + r.nextFloat() * 40 else -60f - r.nextFloat() * 40, colors[r.nextInt(colors.size)])
            }
            if (rich > .5f && r.nextFloat() < rich * .6f) bezant(this, Offset(o.x - sd * w * .45f, o.y + w * .2f), w * .09f)
            else if (r.nextFloat() < .5f) drawCircle(C.vermilion, w * .05f, Offset(o.x - sd * w * .4f, o.y + w * .25f))
        }
    }

    /** The mark a kept practice leaves in the margin; [n] (cycles, beads, bows) or [minutes] set its length. */
    fun trace(d: DrawScope, kind: Kind, at: Offset, u: Float, n: Int, minutes: Int) = with(d) {
        when (kind) {
            Kind.BREATH -> {
                val waves = (if (n > 0) n / 3 else minutes).coerceIn(2, 8)
                val p = Path().apply { moveTo(at.x, at.y) }
                for (i in 1..waves * 8) {
                    val t = i / 8f
                    p.lineTo(at.x + sin(t * 2 * PI.toFloat()) * u * .35f, at.y + t * u * .55f)
                }
                drawPath(p, C.lapis, style = Stroke(u * .08f, cap = StrokeCap.Round))
            }
            Kind.BEADS -> {
                val k = (n / 10).coerceIn(3, 12)
                for (i in 0 until k) drawCircle(if (i % 5 == 4) C.vermilion else C.inkSoft, if (i % 5 == 4) u * .11f else u * .07f, Offset(at.x, at.y + i * u * .28f))
                drawLine(C.inkSoft.copy(alpha = .5f), at, Offset(at.x, at.y + (k - 1) * u * .28f), u * .025f)
            }
            Kind.SIT, Kind.METTA -> {
                val c = Offset(at.x, at.y + u * .45f)
                drawArc(if (kind == Kind.METTA) C.rose else C.vermilion, 110f, 310f, false, c - Offset(u * .4f, u * .4f), Size(u * .8f, u * .8f), style = Stroke(u * .1f, cap = StrokeCap.Round))
                bezant(this, c, u * .09f)
            }
            Kind.BOWS -> {
                val k = (n / 3).coerceIn(1, 6)
                for (i in 0 until k) {
                    val y = at.y + i * u * .22f
                    val p = Path().apply { moveTo(at.x - u * .22f, y); lineTo(at.x, y + u * .14f); lineTo(at.x + u * .22f, y) }
                    drawPath(p, C.vermilion, style = Stroke(u * .07f, cap = StrokeCap.Round))
                }
            }
            Kind.WALK -> {
                val k = minutes.coerceIn(2, 6)
                for (i in 0 until k) {
                    val sx = if (i % 2 == 0) -u * .12f else u * .12f
                    drawOval(C.inkSoft, Offset(at.x + sx - u * .06f, at.y + i * u * .3f), Size(u * .12f, u * .2f))
                }
            }
            Kind.GAZE -> candle(this, Offset(at.x, at.y + u * .8f), u * .7f, 0f)
            Kind.SILENCE -> {
                val h = u * (.5f + minutes.coerceAtMost(20) * .03f)
                val p = Path().apply { moveTo(at.x + u * .15f, at.y); lineTo(at.x - u * .05f, at.y); lineTo(at.x - u * .05f, at.y + h); lineTo(at.x + u * .15f, at.y + h) }
                drawPath(p, C.inkSoft, style = Stroke(u * .07f))
            }
            Kind.READ -> {
                val p = Path().apply {
                    moveTo(at.x, at.y + u * .1f); quadraticTo(at.x - u * .2f, at.y, at.x - u * .38f, at.y + u * .06f); lineTo(at.x - u * .38f, at.y + u * .42f)
                    quadraticTo(at.x - u * .2f, at.y + u * .36f, at.x, at.y + u * .46f); quadraticTo(at.x + u * .2f, at.y + u * .36f, at.x + u * .38f, at.y + u * .42f)
                    lineTo(at.x + u * .38f, at.y + u * .06f); quadraticTo(at.x + u * .2f, at.y, at.x, at.y + u * .1f); close()
                }
                drawPath(p, white)
                drawPath(p, C.inkSoft, style = Stroke(u * .05f))
                drawLine(C.inkSoft, Offset(at.x, at.y + u * .1f), Offset(at.x, at.y + u * .46f), u * .04f)
                drawLine(C.vermilion, Offset(at.x + u * .05f, at.y + u * .42f), Offset(at.x + u * .08f, at.y + u * .62f), u * .05f)
            }
            Kind.THANKS -> rosette(this, Offset(at.x, at.y + u * .3f), u * .28f, C.gold)
            Kind.EXAMEN -> rosette(this, Offset(at.x, at.y + u * .3f), u * .28f, C.lapis)
            Kind.QUESTION -> {
                drawCircle(C.vermilion, u * .09f, Offset(at.x, at.y + u * .2f))
                drawCircle(C.vermilion, u * .05f, Offset(at.x, at.y + u * .42f))
            }
            Kind.CUSTOM -> {
                val p = Path().apply { moveTo(at.x, at.y); lineTo(at.x + u * .2f, at.y + u * .25f); lineTo(at.x, at.y + u * .5f); lineTo(at.x - u * .2f, at.y + u * .25f); close() }
                drawPath(p, C.malachite)
            }
        }
    }

    fun rosette(d: DrawScope, c: Offset, r: Float, color: Color) = with(d) {
        for (i in 0 until 6) {
            val a = i * PI.toFloat() / 3
            drawCircle(color, r * .38f, Offset(c.x + cos(a) * r * .55f, c.y + sin(a) * r * .55f))
        }
        drawCircle(C.vermilion, r * .3f, c)
    }

    /** A candle with its flame; [t] in seconds sways the flame. */
    fun candle(d: DrawScope, base: Offset, h: Float, t: Float, lit: Boolean = true) = with(d) {
        val w = h * .28f
        drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFE9DCC0), white, Color(0xFFD8C7A4)), base.x - w / 2, base.x + w / 2), Offset(base.x - w / 2, base.y - h * .6f), Size(w, h * .6f), CornerRadius(w * .1f))
        drawLine(C.ink, Offset(base.x, base.y - h * .6f), Offset(base.x, base.y - h * .67f), h * .02f)
        if (!lit) return@with
        val sway = sin(t * 2.3f) * .06f + sin(t * 5.1f) * .03f
        val fh = h * (.42f + sin(t * 3.7f) * .02f)
        val top = Offset(base.x + sway * fh, base.y - h * .62f - fh)
        val bottom = Offset(base.x, base.y - h * .63f)
        drawCircle(Brush.radialGradient(listOf(C.candle.copy(alpha = .35f), Color.Transparent), center = Offset(base.x, bottom.y - fh * .45f), radius = fh * 1.6f), fh * 1.6f, Offset(base.x, bottom.y - fh * .45f))
        val p = Path().apply {
            moveTo(bottom.x, bottom.y)
            cubicTo(bottom.x - fh * .3f, bottom.y - fh * .15f, top.x - fh * .18f, top.y + fh * .45f, top.x, top.y)
            cubicTo(top.x + fh * .18f, top.y + fh * .45f, bottom.x + fh * .3f, bottom.y - fh * .15f, bottom.x, bottom.y)
            close()
        }
        drawPath(p, Brush.verticalGradient(listOf(C.candleHi, C.candle, C.ember), top.y, bottom.y))
        drawOval(Color(0xFF6A8FD8).copy(alpha = .55f), Offset(bottom.x - fh * .07f, bottom.y - fh * .22f), Size(fh * .14f, fh * .2f))
    }

    /** One of the margin creatures, standing on [at] (its feet), [s] tall. */
    fun drollery(d: DrawScope, which: Drollery, at: Offset, s: Float, flip: Boolean = false) = with(d) {
        translate(at.x, at.y) {
            val k = if (flip) -1f else 1f
            fun o(x: Float, y: Float) = Offset(k * x * s, -y * s)
            when (which) {
                Drollery.SNAIL -> {
                    val body = Path().apply {
                        moveTo(o(-.55f, 0f).x, 0f); lineTo(o(.5f, 0f).x, 0f)
                        quadraticTo(o(.62f, .05f).x, o(.62f, .05f).y, o(.6f, .22f).x, o(.6f, .22f).y)
                        quadraticTo(o(.52f, .3f).x, o(.52f, .3f).y, o(.4f, .2f).x, o(.4f, .2f).y)
                        lineTo(o(-.5f, .12f).x, o(-.5f, .12f).y); close()
                    }
                    drawPath(body, Color(0xFFC9B48A)); drawPath(body, C.ink, style = Stroke(s * .025f))
                    drawLine(C.ink, o(.55f, .25f), o(.62f, .5f), s * .025f); drawLine(C.ink, o(.5f, .25f), o(.5f, .52f), s * .025f)
                    drawCircle(C.ink, s * .03f, o(.62f, .5f)); drawCircle(C.ink, s * .03f, o(.5f, .52f))
                    val c = o(-.1f, .42f)
                    drawCircle(C.lapis, s * .34f, c); drawCircle(C.ink, s * .34f, c, style = Stroke(s * .025f))
                    val sp = Path().apply {
                        for (i in 0..60) {
                            val a = i / 60f * 4.5f * PI.toFloat(); val r = s * .3f * (1 - i / 66f)
                            val p = Offset(c.x + k * cos(a) * r, c.y + sin(a) * r)
                            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                        }
                    }
                    drawPath(sp, C.goldHi, style = Stroke(s * .03f))
                }
                Drollery.HARE -> {
                    drawOval(brown, o(-.35f, .55f).let { Offset(minOf(it.x, it.x + k * .7f * s), it.y) }, Size(.7f * s, .5f * s))
                    drawCircle(brown, s * .18f, o(.32f, .72f))
                    for (dx in listOf(.26f, .38f)) {
                        rotate(k * (if (dx < .3f) -14f else 10f), o(dx, .85f)) { drawOval(brownHi, Offset(o(dx, .85f).x - s * .05f, o(dx, 1.25f).y), Size(s * .1f, s * .42f)) }
                    }
                    drawCircle(white, s * .07f, o(-.36f, .4f))
                    drawCircle(C.ink, s * .025f, o(.38f, .76f))
                    drawLine(brown, o(.15f, .1f), o(.2f, 0f), s * .08f, cap = StrokeCap.Round)
                    drawLine(brown, o(-.2f, .1f), o(-.25f, 0f), s * .08f, cap = StrokeCap.Round)
                    // The scribe's quill.
                    drawLine(C.ink, o(.3f, .45f), o(.62f, .95f), s * .025f)
                    drawLine(white, o(.5f, .78f), o(.64f, 1.0f), s * .06f, cap = StrokeCap.Round)
                }
                Drollery.BIRD -> {
                    val b = Path().apply {
                        moveTo(o(-.45f, .5f).x, o(-.45f, .5f).y)
                        quadraticTo(o(0f, .2f).x, o(0f, .2f).y, o(.35f, .5f).x, o(.35f, .5f).y)
                        quadraticTo(o(.4f, .78f).x, o(.4f, .78f).y, o(.2f, .78f).x, o(.2f, .78f).y)
                        quadraticTo(o(-.1f, .72f).x, o(-.1f, .72f).y, o(-.45f, .5f).x, o(-.45f, .5f).y); close()
                    }
                    drawPath(b, C.vermilion)
                    val wing = Path().apply { moveTo(o(-.15f, .62f).x, o(-.15f, .62f).y); quadraticTo(o(.05f, .5f).x, o(.05f, .5f).y, o(.18f, .62f).x, o(.18f, .62f).y); quadraticTo(o(0f, .7f).x, o(0f, .7f).y, o(-.15f, .62f).x, o(-.15f, .62f).y) }
                    drawPath(wing, C.lapis)
                    val tail = Path().apply { moveTo(o(-.4f, .52f).x, o(-.4f, .52f).y); lineTo(o(-.7f, .7f).x, o(-.7f, .7f).y); lineTo(o(-.66f, .5f).x, o(-.66f, .5f).y); close() }
                    drawPath(tail, C.lapis)
                    val beak = Path().apply { moveTo(o(.38f, .66f).x, o(.38f, .66f).y); lineTo(o(.52f, .62f).x, o(.52f, .62f).y); lineTo(o(.37f, .58f).x, o(.37f, .58f).y); close() }
                    drawPath(beak, C.gold)
                    drawCircle(C.ink, s * .03f, o(.28f, .68f))
                    drawLine(C.ink, o(0f, .32f), o(.02f, 0f), s * .025f); drawLine(C.ink, o(.1f, .34f), o(.1f, 0f), s * .025f)
                }
                Drollery.FISH -> {
                    val b = Path().apply {
                        moveTo(o(.5f, .4f).x, o(.5f, .4f).y)
                        quadraticTo(o(.1f, .75f).x, o(.1f, .75f).y, o(-.35f, .4f).x, o(-.35f, .4f).y)
                        quadraticTo(o(.1f, .05f).x, o(.1f, .05f).y, o(.5f, .4f).x, o(.5f, .4f).y); close()
                    }
                    drawPath(b, Brush.verticalGradient(listOf(Color(0xFF9DB4C9), C.lapis), -.75f * s, -.05f * s))
                    val tail = Path().apply { moveTo(o(-.3f, .4f).x, o(-.3f, .4f).y); lineTo(o(-.6f, .62f).x, o(-.6f, .62f).y); lineTo(o(-.55f, .4f).x, o(-.55f, .4f).y); lineTo(o(-.6f, .18f).x, o(-.6f, .18f).y); close() }
                    drawPath(tail, C.lapisDeep)
                    for (i in 0..2) drawArc(white.copy(alpha = .7f), -60f, 120f, false, o(-.05f + i * .14f, .5f), Size(s * .1f, s * .2f), style = Stroke(s * .02f))
                    drawCircle(white, s * .05f, o(.35f, .45f)); drawCircle(C.ink, s * .025f, o(.35f, .45f))
                    for (i in 0..2) drawCircle(C.lapis.copy(alpha = .5f), s * (.03f + i * .01f), o(.62f + i * .06f, .55f + i * .14f), style = Stroke(s * .015f))
                }
                Drollery.CAT -> {
                    val body = Path().apply {
                        moveTo(o(-.3f, 0f).x, 0f)
                        quadraticTo(o(-.38f, .5f).x, o(-.38f, .5f).y, o(-.05f, .62f).x, o(-.05f, .62f).y)
                        quadraticTo(o(.25f, .55f).x, o(.25f, .55f).y, o(.22f, 0f).x, 0f); close()
                    }
                    drawPath(body, grey)
                    drawCircle(grey, s * .2f, o(.0f, .75f))
                    for (ex in listOf(-.14f, .14f)) {
                        val ear = Path().apply { moveTo(o(ex - .06f, .85f).x, o(ex - .06f, .85f).y); lineTo(o(ex * 1.1f, 1.02f).x, o(ex * 1.1f, 1.02f).y); lineTo(o(ex + .06f, .88f).x, o(ex + .06f, .88f).y); close() }
                        drawPath(ear, grey)
                    }
                    drawCircle(C.goldHi, s * .035f, o(-.07f, .77f)); drawCircle(C.goldHi, s * .035f, o(.07f, .77f))
                    val tail = Path().apply { moveTo(o(.2f, .05f).x, o(.2f, .05f).y); cubicTo(o(.6f, 0f).x, o(.6f, 0f).y, o(.55f, .4f).x, o(.55f, .4f).y, o(.45f, .45f).x, o(.45f, .45f).y) }
                    drawPath(tail, grey, style = Stroke(s * .07f, cap = StrokeCap.Round))
                }
                Drollery.DEMON -> {
                    val dark = Color(0xFF2E3A2C)
                    for (side in listOf(-1f, 1f)) {
                        val wing = Path().apply {
                            moveTo(o(side * .1f, .6f).x, o(side * .1f, .6f).y)
                            lineTo(o(side * .55f, .95f).x, o(side * .55f, .95f).y)
                            lineTo(o(side * .45f, .7f).x, o(side * .45f, .7f).y)
                            lineTo(o(side * .5f, .55f).x, o(side * .5f, .55f).y)
                            lineTo(o(side * .3f, .5f).x, o(side * .3f, .5f).y); close()
                        }
                        drawPath(wing, C.violet)
                    }
                    drawOval(dark, Offset(o(-.18f, .65f).x.coerceAtMost(o(.18f, .65f).x), o(-.18f, .65f).y), Size(.36f * s, .5f * s))
                    drawCircle(dark, s * .15f, o(0f, .78f))
                    for (hx in listOf(-.1f, .1f)) {
                        val horn = Path().apply { moveTo(o(hx - .04f, .88f).x, o(hx - .04f, .88f).y); lineTo(o(hx * 1.8f, 1.05f).x, o(hx * 1.8f, 1.05f).y); lineTo(o(hx + .04f, .9f).x, o(hx + .04f, .9f).y); close() }
                        drawPath(horn, C.vellumEdge)
                    }
                    drawCircle(C.vermilion, s * .03f, o(-.05f, .8f)); drawCircle(C.vermilion, s * .03f, o(.05f, .8f))
                    drawLine(dark, o(-.08f, .18f), o(-.12f, 0f), s * .05f, cap = StrokeCap.Round)
                    drawLine(dark, o(.08f, .18f), o(.12f, 0f), s * .05f, cap = StrokeCap.Round)
                    val tail = Path().apply { moveTo(o(.1f, .25f).x, o(.1f, .25f).y); cubicTo(o(.5f, .1f).x, o(.5f, .1f).y, o(.55f, .4f).x, o(.55f, .4f).y, o(.4f, .45f).x, o(.4f, .45f).y) }
                    drawPath(tail, dark, style = Stroke(s * .03f, cap = StrokeCap.Round))
                    val tip = Path().apply { moveTo(o(.4f, .45f).x, o(.4f, .45f).y); lineTo(o(.33f, .52f).x, o(.33f, .52f).y); lineTo(o(.45f, .55f).x, o(.45f, .55f).y); close() }
                    drawPath(tip, dark)
                }
            }
        }
    }

    /**
     * The dial of unequal hours: the upper half is the day's twelve hours, the lower the night's, each the same
     * width on the dial however long it lasts on the clock. Marks show the rule's hours; the sun shows now.
     */
    fun dial(d: DrawScope, c: Offset, r: Float, nowPos: Double, marks: List<Pair<Double, Color>>, numerals: (Int, Offset) -> Unit) = with(d) {
        fun ang(pos: Double): Float {
            // Sunrise on the left, noon on top, sunset on the right, midnight at the bottom.
            val p = ((pos % 24) + 24) % 24
            return (PI + p / 12.0 * PI).toFloat()
        }
        fun pt(pos: Double, rr: Float) = Offset(c.x + cos(ang(pos)) * rr, c.y + sin(ang(pos)) * rr)
        drawArc(Brush.verticalGradient(listOf(C.goldHi.copy(alpha = .55f), C.vellumDeep), c.y - r, c.y), 180f, 180f, true, c - Offset(r, r), Size(r * 2, r * 2))
        drawArc(Brush.verticalGradient(listOf(C.lapisDeep.copy(alpha = .75f), C.lapisDeep), c.y, c.y + r), 0f, 180f, true, c - Offset(r, r), Size(r * 2, r * 2))
        drawCircle(C.ink, r, c, style = Stroke(r * .012f))
        drawLine(C.inkSoft, Offset(c.x - r, c.y), Offset(c.x + r, c.y), r * .01f)
        for (i in 0 until 24) {
            val inner = if (i % 6 == 0) r * .84f else r * .9f
            drawLine(if (i < 12) C.inkSoft else C.goldHi.copy(alpha = .7f), pt(i.toDouble(), inner), pt(i.toDouble(), r), r * .012f)
        }
        for (i in 1..12) numerals(i, pt(i - .5, r * .74f))
        val stars = Random(3)
        repeat(18) {
            val a = (stars.nextDouble() * .9 + .05) * PI
            val rr = r * (.15f + stars.nextFloat() * .55f)
            drawCircle(C.goldHi.copy(alpha = .4f + stars.nextFloat() * .4f), r * .008f + stars.nextFloat() * r * .008f, Offset(c.x + cos(a).toFloat() * rr, c.y + sin(a).toFloat() * rr))
        }
        for ((pos, color) in marks) {
            val o = pt(pos, r * 1.0f)
            drawCircle(C.vellum, r * .055f, o)
            drawCircle(color, r * .042f, o)
        }
        val sun = pt(nowPos, r * .5f)
        drawLine(C.ink.copy(alpha = .5f), c, pt(nowPos, r * .96f), r * .012f, cap = StrokeCap.Round)
        if (nowPos in 0.0..12.0) {
            for (i in 0 until 12) {
                val a = i * PI.toFloat() / 6
                drawLine(C.gold, Offset(sun.x + cos(a) * r * .07f, sun.y + sin(a) * r * .07f), Offset(sun.x + cos(a) * r * .11f, sun.y + sin(a) * r * .11f), r * .012f, cap = StrokeCap.Round)
            }
            drawCircle(Brush.radialGradient(listOf(C.goldHi, C.gold), center = sun, radius = r * .065f), r * .065f, sun)
        } else {
            drawCircle(C.goldHi, r * .06f, sun)
            drawCircle(C.lapisDeep, r * .055f, sun + Offset(r * .03f, -r * .015f))
        }
        drawCircle(C.ink, r * .02f, c)
    }

    /** A drop of red on the vellum, for the rule ticks. */
    fun tick(d: DrawScope, c: Offset, r: Float, on: Boolean) = with(d) {
        drawCircle(if (on) C.vermilion else C.vellumEdge, r, c, style = if (on) Fill else Stroke(r * .18f))
        if (on) {
            val p = Path().apply { moveTo(c.x - r * .45f, c.y); lineTo(c.x - r * .1f, c.y + r * .38f); lineTo(c.x + r * .5f, c.y - r * .38f) }
            drawPath(p, C.vellum, style = Stroke(r * .2f, cap = StrokeCap.Round))
        }
    }
}
