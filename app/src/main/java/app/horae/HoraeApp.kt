package app.horae

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.horae.data.DemoSeed
import app.horae.data.HoraeDb
import app.horae.data.Repo
import app.horae.data.SettingsStore
import app.horae.domain.Library
import app.horae.notify.Bells
import app.horae.sound.Sfx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = HoraeDb.create(app)
    val repo = Repo(db)
    val settings = SettingsStore(app)
    val library: Library = Library.read(app.assets.open("readings.tsv"), app.assets.open("questions.tsv"))
    val sfx = Sfx(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** An hour to open, from a tapped bell notification. */
    val openHour = MutableStateFlow<Long?>(null)

    /** Volume keys, while the beads are in hand: each press moves one bead. */
    val keys = MutableSharedFlow<Unit>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    @Volatile var beadsInHand = false

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }
}

class HoraeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Bells.channel(this)
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(container)
            container.repo.refresh()
            container.sfx.load()
            Bells.plan(this@HoraeApp)
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as HoraeApp).container

/** Resources in the in-app language, which AppCompat doesn't localise outside activities before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

/** Is this context in Russian? */
val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
