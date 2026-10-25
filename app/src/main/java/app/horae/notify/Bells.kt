package app.horae.notify

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.horae.MainActivity
import app.horae.R
import app.horae.container
import app.horae.domain.Bell
import app.horae.domain.Clock
import app.horae.domain.day
import app.horae.localized
import app.horae.ru
import app.horae.sound.Synth
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The bells: one alarm at a time, for the next hour that rings. When it fires, a short foreground service rings
 * the bell the hour's number of strokes and leaves a quiet notification that opens the hour.
 */
object Bells {
    const val CHANNEL = "hours_v1"
    const val EXTRA_HOUR = "hour"
    const val EXTRA_AT = "at"
    private const val ACTION = "app.horae.BELL"

    /** An alarm late by more than this (the phone was off) doesn't ring any more; the hour has passed. */
    private const val GRACE_MS = 15 * 60_000L

    fun canNotify(c: Context) = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    fun canExact(c: Context) = Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun channel(c: Context) {
        val l = c.localized()
        val ch = NotificationChannel(CHANNEL, if (l.ru) "Часы" else "Hours", NotificationManager.IMPORTANCE_DEFAULT).apply {
            // The bell itself is the sound; the notification stays quiet.
            setSound(null, null)
            enableVibration(false)
            description = if (l.ru) "Колокол на каждый час и тихая запись, открывающая его" else "A bell for each hour and a quiet note that opens it"
        }
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun pi(c: Context, hour: Long = 0, at: Long = 0) = PendingIntent.getBroadcast(
        c, 7, Intent(c, BellReceiver::class.java).setAction(ACTION).putExtra(EXTRA_HOUR, hour).putExtra(EXTRA_AT, at),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Plans the next bell; safe to call any time. */
    suspend fun plan(c: Context) {
        val app = c.applicationContext
        val st = app.container.settings.now()
        val s = app.container.repo.state.value ?: app.container.repo.refresh()
        val am = app.getSystemService(AlarmManager::class.java)
        val next = if (st.onboarded) Clock.next(s.hours, System.currentTimeMillis(), ZoneId.systemDefault(), st.place) else null
        if (next?.at == null) { am.cancel(pi(app)); return }
        val p = pi(app, next.hour.id, next.at)
        if (canExact(app)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at, p)
        else am.setWindow(AlarmManager.RTC_WAKEUP, next.at, 60_000L, p)
    }

    fun openIntent(c: Context, hour: Long): PendingIntent = PendingIntent.getActivity(
        c, hour.toInt(), Intent(c, MainActivity::class.java).putExtra(EXTRA_HOUR, hour).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun note(c: Context, hour: Long, name: String, ringing: Boolean): Notification {
        val ru = c.localized().ru
        return NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_horae)
            .setContentTitle(name)
            .setContentText(if (ringing) (if (ru) "Звонит колокол" else "The bell is ringing") else (if (ru) "Час открыт. Коснись, чтобы войти." else "The hour is open. Tap to enter."))
            .setContentIntent(openIntent(c, hour))
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
    }
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

class BellReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val hour = intent.getLongExtra(Bells.EXTRA_HOUR, 0)
        val at = intent.getLongExtra(Bells.EXTRA_AT, 0)
        val pending = goAsync()
        receiverScope.launch {
            try {
                val app = context.applicationContext
                val s = app.container.repo.refresh()
                val h = s.hours.firstOrNull { it.id == hour }
                // An hour already kept early on its page doesn't need calling.
                val day = at.day(ZoneId.systemDefault())
                val kept = h != null && (s.on(day) + s.on(day - 1)).any { it.hourId == h.id && it.at > at - 6 * 3_600_000L }
                if (h != null && h.on && !kept && h.bell != Bell.NONE && System.currentTimeMillis() - at < 15 * 60_000L) {
                    val svc = Intent(app, BellService::class.java).putExtra(Bells.EXTRA_HOUR, h.id)
                    // Starting a foreground service from an exact alarm is allowed; if the phone says no, the note still comes.
                    val started = runCatching { ContextCompat.startForegroundService(app, svc) }.isSuccess
                    if (!started && Bells.canNotify(app)) try { NotificationManagerCompat.from(app).notify(h.id.toInt() + 100, Bells.note(app, h.id, h.name, false)) } catch (_: SecurityException) {}
                }
                Bells.plan(app)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Rings one hour's bell, then leaves the quiet note behind and stops. */
class BellService : Service() {
    private var player: MediaPlayer? = null
    private val main = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hourId = intent?.getLongExtra(Bells.EXTRA_HOUR, 0) ?: 0
        val c = applicationContext.container
        val h = c.repo.state.value?.hours?.firstOrNull { it.id == hourId }
        if (h == null) { stopSelf(); return START_NOT_STICKY }
        val n = Bells.note(this, h.id, h.name, true)
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) startForeground(h.id.toInt() + 100, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            else startForeground(h.id.toInt() + 100, n)
        }.onFailure { stopSelf(); return START_NOT_STICKY }

        val strokes = Clock.strokes(h)
        val audio = getSystemService(AudioManager::class.java)
        val loud = audio.ringerMode == AudioManager.RINGER_MODE_NORMAL
        receiverScope.launch {
            val st = c.settings.now()
            if (st.haptics && audio.ringerMode != AudioManager.RINGER_MODE_SILENT) buzz(strokes, h.bell)
            if (st.sound && loud) {
                val f = Synth.file(this@BellService, h.bell, strokes)
                main.post {
                    player = runCatching {
                        MediaPlayer().apply {
                            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                            setDataSource(f.absolutePath)
                            setOnCompletionListener { finish(h.id, h.name) }
                            prepare(); start()
                        }
                    }.getOrNull()
                    if (player == null) finish(h.id, h.name)
                }
            } else main.postDelayed({ finish(h.id, h.name) }, 1500)
        }
        // Whatever happens, the service never outstays a ringing.
        main.postDelayed({ finish(h.id, h.name) }, 45_000)
        return START_NOT_STICKY
    }

    private fun buzz(strokes: Int, bell: Bell) {
        val v = getSystemService(Vibrator::class.java) ?: return
        val gap = when (bell) { Bell.GREAT -> 2400L; Bell.SMALL -> 1500L; Bell.BOWL -> 4500L; else -> 900L }
        val pattern = LongArray(strokes * 2) { if (it % 2 == 0) (if (it == 0) 0L else gap - 120) else 120L }
        runCatching { v.vibrate(VibrationEffect.createWaveform(pattern, -1)) }
    }

    private var done = false

    private fun finish(id: Long, name: String) {
        if (done) return
        done = true
        runCatching { player?.release() }
        player = null
        if (Bells.canNotify(this)) {
            stopForeground(STOP_FOREGROUND_DETACH)
            try { NotificationManagerCompat.from(this).notify(id.toInt() + 100, Bells.note(this, id, name, false)) } catch (_: SecurityException) {}
        } else stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        runCatching { player?.release() }
        super.onDestroy()
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
        "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try { Bells.plan(context.applicationContext) } finally { pending.finish() }
        }
    }
}
