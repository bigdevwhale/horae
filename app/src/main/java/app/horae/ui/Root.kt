package app.horae.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.horae.R
import app.horae.container
import app.horae.data.Settings
import app.horae.data.State
import app.horae.domain.Clock
import app.horae.domain.Library
import app.horae.domain.day
import app.horae.notify.Bells
import app.horae.ru
import app.horae.ui.book.BookScreen
import app.horae.ui.components.LocalToast
import app.horae.ui.components.ToastHost
import app.horae.ui.components.Toaster
import app.horae.ui.components.vellum
import app.horae.ui.office.OfficeScreen
import app.horae.ui.onboarding.Onboarding
import app.horae.ui.rule.RuleScreen
import app.horae.ui.settings.SettingsSheet
import app.horae.ui.theme.C
import app.horae.ui.theme.T
import app.horae.ui.today.TodayScreen
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

/** Everything a screen reads, at one moment. */
class Home(val s: State, val settings: Settings, val w: Words, val lib: Library, val now: Long) {
    val ru get() = w.ru
    val zone: ZoneId = ZoneId.systemDefault()
    val today: Long = now.day(zone)
    val date: LocalDate get() = LocalDate.ofEpochDay(today)
    val place get() = settings.place

    fun slots(date: LocalDate) = Clock.day(s.hours, date, zone, place)
    fun kept(day: Long, hourId: Long) = s.on(day).any { it.hourId == hourId }

    /** The page an open hour writes to: late night hours belong to yesterday's page. */
    fun open(): Clock.Slot? = Clock.open(s.hours, now, zone, place)

    /**
     * The great silence: after the last hour of a page is kept, the book stays closed until the first hour of
     * the next page rings. Returns the page it belongs to and when it ends, or null when there is none.
     */
    fun silence(): Pair<Long, Long>? {
        if (!settings.silence) return null
        for (d in listOf(today - 1, today)) {
            val date = LocalDate.ofEpochDay(d)
            val slots = slots(date).filter { it.at != null }
            val last = slots.lastOrNull() ?: continue
            val entry = s.on(d).firstOrNull { it.hourId == last.hour.id } ?: continue
            if (s.silenceBroken(d)) continue
            val next = slots(date.plusDays(1)).firstOrNull { it.at != null }?.at ?: Clock.sun(date.plusDays(1), zone, place).rise
            if (now >= entry.at && now < next) return d to next
        }
        return null
    }
}

enum class Tab { TODAY, BOOK, RULE }

sealed interface Overlay {
    data class Office(val hourId: Long, val day: Long, val brief: Boolean) : Overlay
    data object Settings : Overlay
}

@Composable
fun HoraeRoot(settings: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    val state by c.repo.state.collectAsStateWithLifecycle()
    val words = remember(ctx.ru) { Words(ctx.ru) }
    val toaster = remember { Toaster() }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(settings.sound, settings.haptics) { c.sfx.on = settings.sound; c.sfx.touch = settings.haptics }
    if (!settings.onboarded) {
        CompositionLocalProvider(LocalToast provides toaster) { Box(Modifier.fillMaxSize()) { Onboarding(words, settings); ToastHost(toaster) } }
        return
    }
    val life = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        life.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            c.repo.refresh()
            Bells.plan(ctx)
            while (true) { now = System.currentTimeMillis(); delay(20_000) }
        }
    }
    val s = state ?: run { Box(Modifier.fillMaxSize().vellum()); return }
    val home = Home(s, settings, words, c.library, now)

    var tab by remember { mutableStateOf(Tab.TODAY) }
    val overlays = remember { mutableStateListOf<Overlay>() }
    fun push(o: Overlay) = overlays.add(o)
    fun pop() { if (overlays.isNotEmpty()) overlays.removeAt(overlays.lastIndex) }
    BackHandler(enabled = overlays.isEmpty() && tab != Tab.TODAY) { tab = Tab.TODAY }

    // A tapped bell opens its hour straight away.
    val asked by c.openHour.collectAsStateWithLifecycle()
    LaunchedEffect(asked, s) {
        val id = asked ?: return@LaunchedEffect
        c.openHour.value = null
        val open = home.open()
        val day = if (open?.hour?.id == id) open.date.toEpochDay() else home.today
        if (s.hours.any { it.id == id } && overlays.none { it is Overlay.Office }) push(Overlay.Office(id, day, false))
    }

    CompositionLocalProvider(LocalToast provides toaster) {
        Box(Modifier.fillMaxSize().vellum()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        Tab.TODAY -> TodayScreen(home, onHour = { id, day, brief -> push(Overlay.Office(id, day, brief)) }, onSettings = { push(Overlay.Settings) })
                        Tab.BOOK -> BookScreen(home)
                        Tab.RULE -> RuleScreen(home)
                    }
                }
                TabBar(tab) { tab = it }
            }
            when (val o = overlays.lastOrNull()) {
                is Overlay.Office -> OfficeScreen(home, o.hourId, o.day, o.brief, onClose = { pop() })
                Overlay.Settings -> SettingsSheet(home, onClose = { pop() })
                null -> Unit
            }
            ToastHost(toaster)
        }
    }
}

@Composable
private fun TabBar(tab: Tab, onTab: (Tab) -> Unit) {
    val items = listOf(Tab.TODAY to R.string.tab_today, Tab.BOOK to R.string.tab_book, Tab.RULE to R.string.tab_rule)
    Column(Modifier.fillMaxWidth().background(C.vellumDeep)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.vellumEdge))
        Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
            for ((t, label) in items) {
                val sel = t == tab
                Box(Modifier.weight(1f).clickable { onTab(t) }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(label), style = T.Button.copy(color = if (sel) C.vermilion else C.faded))
                }
            }
        }
    }
}
