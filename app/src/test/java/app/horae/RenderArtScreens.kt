package app.horae

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import app.horae.data.DemoSeed
import app.horae.domain.Kind
import app.horae.ui.Home
import app.horae.ui.Words
import app.horae.ui.book.BookScreen
import app.horae.ui.book.MonthLeaf
import app.horae.ui.book.Page
import app.horae.ui.components.LocalToast
import app.horae.ui.components.Toaster
import app.horae.ui.components.night
import app.horae.ui.components.vellum
import app.horae.ui.office.OfficeScreen
import app.horae.ui.office.StepView
import app.horae.ui.onboarding.Onboarding
import app.horae.ui.rule.HourEditor
import app.horae.ui.rule.RuleScreen
import app.horae.ui.settings.SettingsSheet
import app.horae.ui.theme.HoraeTheme
import app.horae.ui.today.TodayScreen
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The main screens over the demo book, drawn to PNGs (run with `-Prender`), English and Russian. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    private fun home(): Home = runBlocking {
        val app = ApplicationProvider.getApplicationContext<HoraeApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(c)
        val s = c.repo.refresh()
        val st = c.settings.now().copy(silence = false)
        Home(s, st, Words(lang == "ru"), c.library, System.currentTimeMillis())
    }

    private fun shot(name: String, advance: Long = 2_000, content: @Composable (Home) -> Unit) {
        val h = home()
        rule.mainClock.autoAdvance = false
        rule.setContent { HoraeTheme { CompositionLocalProvider(LocalToast provides Toaster()) { Box(Modifier.fillMaxSize().vellum()) { content(h) } } } }
        rule.mainClock.advanceTimeBy(advance)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun Home.hour(k: Kind) = s.hours.first { h -> h.steps.any { it.kind == k } }

    @Test fun welcome() = shot("0-welcome") { Onboarding(it.w, it.settings.copy(onboarded = false)) }
    @Test fun today() = shot("1-today") { TodayScreen(it, { _, _, _ -> }, {}) }
    @Test fun office() = shot("2-office") { h -> OfficeScreen(h, h.s.hours.first { it.steps.size >= 3 }.id, h.today, false) {} }
    @Test fun read() = shot("3-read") { h -> Box(Modifier.fillMaxSize().night().statusBarsPadding()) { val hr = h.hour(Kind.READ); StepView(h, hr, hr.steps.first { it.kind == Kind.READ }, h.today, 0) {} } }
    @Test fun breath() = shot("4-breath", 2_600) { h -> Box(Modifier.fillMaxSize().night().statusBarsPadding()) { val hr = h.hour(Kind.BREATH); StepView(h, hr, hr.steps.first { it.kind == Kind.BREATH }, h.today, 0) {} } }
    @Test fun page() = shot("5-page") { h -> Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(6.dp)) { Page(h, h.s.entries.keys.filter { it < h.today }.maxBy { h.s.on(it).size + if (it % 3 == 0L) 2 else 0 }) } }
    @Test fun book() = shot("6-book") { BookScreen(it) }
    @Test fun month() = shot("7-month") { h -> Column(Modifier.fillMaxSize().statusBarsPadding()) { MonthLeaf(h, LocalDate.ofEpochDay(h.today).withDayOfMonth(1)) } }
    @Test fun rule() = shot("8-rule") { RuleScreen(it) }
    @Test fun hour() = shot("9-hour") { h -> HourEditor(h, h.s.hours.first { it.steps.size >= 3 }) {} }
    @Test fun settings() = shot("10-settings") { SettingsSheet(it) {} }
}

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}
