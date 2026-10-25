package app.horae.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import app.horae.domain.Bell
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * The book's sounds, synthesised once into small WAV files in the cache: bells of five kinds rung a given number
 * of strokes, a wooden bead click and a soft page. The same moments can be felt through the vibration motor.
 */
class Sfx(private val context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(6)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val ids = HashMap<String, Int>()
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private var player: MediaPlayer? = null
    @Volatile var on = true
    @Volatile var touch = true

    fun load() {
        for (name in listOf(BEAD, PAGE)) ids[name] = pool.load(Synth.file(context, name).absolutePath, 1)
        for (b in Bell.entries) if (b != Bell.NONE) ids[b.name] = pool.load(Synth.file(context, b, 1).absolutePath, 1)
    }

    /** One short sound: a bead, a page, or a single stroke of a bell. */
    fun play(name: String, vol: Float = 1f) {
        val id = ids[name] ?: return
        if (on && id != 0) pool.play(id, vol, vol, 1, 0, 1f)
    }

    fun stroke(b: Bell, vol: Float = .9f) { if (b != Bell.NONE) play(b.name, vol) }

    /** A whole ringing, as the hour would ring it. */
    fun ring(b: Bell, strokes: Int) {
        if (!on || b == Bell.NONE) return
        stop()
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(Synth.file(context, b, strokes).absolutePath)
                setOnCompletionListener { it.release(); if (player === it) player = null }
                prepare(); start()
            }
        }.getOrNull()
    }

    fun stop() { runCatching { player?.release() }; player = null }

    fun feel(ms: Long, amp: Int = 80) {
        if (!touch) return
        runCatching { if (vibrator?.hasVibrator() == true) vibrator.vibrate(VibrationEffect.createOneShot(ms, amp.coerceIn(1, 255))) }
    }

    companion object {
        const val BEAD = "bead"
        const val PAGE = "page"
    }
}

object Synth {
    const val RATE = 22050
    private const val VERSION = 1

    /** The cached ringing of [b] with [strokes] strokes, made on first use. */
    fun file(c: Context, b: Bell, strokes: Int): File {
        val n = strokes.coerceIn(1, 12)
        val f = File(File(c.cacheDir, "bells").apply { mkdirs() }, "${b.name.lowercase()}-$n-v$VERSION.wav")
        if (!f.exists()) Wav.write(f, ring(b, n))
        return f
    }

    fun file(c: Context, name: String): File {
        val f = File(File(c.cacheDir, "bells").apply { mkdirs() }, "$name-v$VERSION.wav")
        if (!f.exists()) Wav.write(f, if (name == Sfx.BEAD) bead() else page())
        return f
    }

    /** Partials of a stroke: frequency ratio, loudness, decay time in seconds. */
    private class Voice(val base: Double, val partials: List<Triple<Double, Double, Double>>, val length: Double, val gap: Double, val strike: Double)

    private fun voice(b: Bell) = when (b) {
        // A church bell: hum, prime, minor tierce, quint, nominal and a bright upper cluster.
        Bell.GREAT -> Voice(150.0, listOf(Triple(.5, .55, 5.5), Triple(1.0, .6, 4.0), Triple(1.19, .45, 3.0), Triple(1.5, .2, 2.5), Triple(2.0, .5, 2.2), Triple(2.52, .18, 1.2), Triple(3.0, .12, .9), Triple(4.07, .08, .5)), 6.0, 2.4, .5)
        Bell.SMALL -> Voice(392.0, listOf(Triple(.5, .35, 3.0), Triple(1.0, .6, 2.2), Triple(1.19, .4, 1.8), Triple(1.5, .2, 1.4), Triple(2.0, .45, 1.2), Triple(2.52, .15, .7), Triple(3.0, .1, .5)), 3.2, 1.5, .4)
        // A handbell: a clean fundamental and the twelfth.
        Bell.HAND -> Voice(784.0, listOf(Triple(1.0, .7, 1.4), Triple(3.0, .3, .7), Triple(2.0, .1, .5), Triple(4.2, .06, .25)), 1.8, .85, .25)
        // A singing bowl: few, inharmonic partials, each doubled a little apart so it beats.
        Bell.BOWL -> Voice(196.0, listOf(Triple(1.0, .7, 6.0), Triple(1.006, .4, 6.0), Triple(2.71, .35, 3.5), Triple(2.722, .2, 3.5), Triple(5.15, .12, 1.8)), 7.0, 4.5, .08)
        Bell.WOOD -> Voice(620.0, listOf(Triple(1.0, .8, .09), Triple(1.58, .4, .05), Triple(2.4, .2, .03)), .35, 1.1, .9)
        Bell.NONE -> Voice(0.0, emptyList(), .1, 1.0, 0.0)
    }

    /** When each stroke falls; the wooden board is struck faster and faster, like the han of a zen hall. */
    private fun onsets(b: Bell, strokes: Int, v: Voice): List<Double> {
        if (b != Bell.WOOD) return List(strokes) { it * v.gap }
        val out = ArrayList<Double>()
        var t = 0.0
        for (i in 0 until strokes) {
            out += t
            t += maxOf(.18, v.gap * (1 - i / maxOf(1.0, strokes - 1.0)) + .18 * (i.toDouble() / strokes))
        }
        return out
    }

    fun ring(b: Bell, strokes: Int): ShortArray {
        val v = voice(b)
        val at = onsets(b, strokes, v)
        val total = at.last() + v.length
        val buf = DoubleArray((total * RATE).toInt())
        val r = Random(b.ordinal * 31 + strokes)
        for ((k, t0) in at.withIndex()) {
            val start = (t0 * RATE).toInt()
            val n = (v.length * RATE).toInt()
            // Each stroke a hair different, as a hand on a rope never pulls twice the same.
            val loud = 1.0 - .08 * r.nextDouble() - if (b == Bell.WOOD) .25 * (1 - k.toDouble() / strokes) else 0.0
            val detune = 1.0 + (r.nextDouble() - .5) * .002
            var lp = 0.0
            for (i in 0 until n) {
                if (start + i >= buf.size) break
                val t = i.toDouble() / RATE
                var s = 0.0
                for ((ratio, amp, decay) in v.partials) s += amp * sin(2 * PI * v.base * ratio * detune * t) * exp(-t / decay)
                val attack = if (b == Bell.BOWL) min(1.0, t / .04) else min(1.0, t / .002)
                val noise = r.nextDouble() * 2 - 1
                lp += (noise - lp) * .3
                s = s * attack + lp * v.strike * exp(-t / .012)
                buf[start + i] += s * loud
            }
        }
        val peak = buf.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-6)
        val fadeN = (RATE * .3).toInt()
        return ShortArray(buf.size) { i ->
            val fade = if (i > buf.size - fadeN) (buf.size - i).toDouble() / fadeN else 1.0
            (buf[i] / peak * .8 * fade * 32000).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    /** A wooden bead knocking on its neighbour. */
    fun bead(): ShortArray {
        val r = Random(5)
        val n = (RATE * .05).toInt()
        var lp = 0.0
        return ShortArray(n) { i ->
            val t = i.toDouble() / RATE
            lp += ((r.nextDouble() * 2 - 1) - lp) * .4
            val s = sin(2 * PI * 1800 * t) * exp(-t / .006) * .5 + lp * exp(-t / .004) * .6 + sin(2 * PI * 950 * t) * exp(-t / .01) * .3
            (s * 26000).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    /** A page turned: soft noise swelling and falling. */
    fun page(): ShortArray {
        val r = Random(9)
        val n = (RATE * .4).toInt()
        var hp = 0.0
        var prev = 0.0
        return ShortArray(n) { i ->
            val x = r.nextDouble() * 2 - 1
            hp = .9 * (hp + x - prev); prev = x
            (hp * sin(PI * i / n) * .25 * 32000).toInt().coerceIn(-32767, 32767).toShort()
        }
    }
}

object Wav {
    fun write(f: File, pcm: ShortArray) {
        val data = ByteArrayOutputStream()
        for (s in pcm) { data.write(s.toInt() and 0xFF); data.write((s.toInt() shr 8) and 0xFF) }
        val bytes = data.toByteArray()
        val tmp = File(f.parentFile, f.name + ".tmp")
        DataOutputStream(tmp.outputStream().buffered()).use { o ->
            fun le32(v: Int) { o.write(v and 0xFF); o.write((v shr 8) and 0xFF); o.write((v shr 16) and 0xFF); o.write((v shr 24) and 0xFF) }
            fun le16(v: Int) { o.write(v and 0xFF); o.write((v shr 8) and 0xFF) }
            o.writeBytes("RIFF"); le32(36 + bytes.size); o.writeBytes("WAVE")
            o.writeBytes("fmt "); le32(16); le16(1); le16(1); le32(Synth.RATE); le32(Synth.RATE * 2); le16(2); le16(16)
            o.writeBytes("data"); le32(bytes.size); o.write(bytes)
        }
        tmp.renameTo(f)
    }
}
