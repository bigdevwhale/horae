package app.horae.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.horae.BuildConfig
import app.horae.container
import app.horae.data.Settings
import app.horae.notify.Bells
import app.horae.ui.Home
import app.horae.ui.components.Btn
import app.horae.ui.components.Card
import app.horae.ui.components.Chip
import app.horae.ui.components.CommunityLinks
import app.horae.ui.components.Flow
import app.horae.ui.components.Gap
import app.horae.ui.components.Kind
import app.horae.ui.components.Sheet
import app.horae.ui.components.Toggle
import app.horae.ui.theme.C
import app.horae.ui.theme.T

@Composable
fun SettingsSheet(home: Home, onClose: () -> Unit) {
    val w = home.w
    val ctx = LocalContext.current
    val c = ctx.container
    val st = home.settings
    fun save(f: (Settings) -> Settings) = c.write { settings.update(f); Bells.plan(ctx) }
    // Re-read the permission state after coming back from the system screens.
    var tick by remember { mutableIntStateOf(0) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val canNotify = remember(tick, home.now) { Bells.canNotify(ctx) }
    val canExact = remember(tick, home.now) { Bells.canExact(ctx) }

    Sheet(w.t("Настройки", "Settings"), onClose, w.t("Закрыть", "Close")) {
        Card {
            Text(w.t("Язык", "Language"), style = T.Heading)
            Flow(Modifier.padding(top = 8.dp)) {
                Chip("Русский", home.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru")) }
                Chip("English", !home.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
            }
        }
        Gap(10)
        Card {
            Text(w.t("Где солнце", "Where the sun is"), style = T.Heading)
            Text(w.t("Неравные часы считаются от здешних восхода и заката.", "The unequal hours are counted from the sunrise and sunset here."), style = T.Small)
            Gap(8)
            PlacePicker(w, st.place)
        }
        Gap(10)
        Card {
            Text(w.t("Колокола", "Bells"), style = T.Heading)
            Toggle(w.t("Звук", "Sound"), w.t("колокол часа и звуки практик; в беззвучном режиме колокол только вибрирует", "the hour's bell and the practices' sounds; on silent the bell only buzzes"), st.sound) { v -> save { it.copy(sound = v) } }
            Toggle(
                w.t("Звонить и в беззвучном режиме", "Ring even on silent"),
                w.t("колокол часа идёт через громкость будильника", "the hour's bell goes through the alarm volume"),
                st.loud,
            ) { v -> save { it.copy(loud = v) } }
            Toggle(w.t("Вибрация", "Vibration"), w.t("удары колокола, фазы дыхания, бусины", "bell strokes, breath phases, beads"), st.haptics) { v -> save { it.copy(haptics = v) } }
            if (!canNotify && Build.VERSION.SDK_INT >= 33) {
                Text(w.t("Уведомления выключены — колокол прозвонит, но час не откроется касанием.", "Notifications are off: the bell will ring, but no note will open the hour."), style = T.Small.copy(color = C.vermilion), modifier = Modifier.padding(top = 6.dp))
                Btn(w.t("Разрешить уведомления", "Allow notifications"), Modifier.fillMaxWidth().padding(top = 6.dp), Kind.GHOST) { ask.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }
            if (!canExact) {
                Text(w.t("Точные будильники выключены — колокол может опаздывать на несколько минут.", "Exact alarms are off, so the bell may be a few minutes late."), style = T.Small.copy(color = C.vermilion), modifier = Modifier.padding(top = 6.dp))
                Btn(w.t("Разрешить точное время", "Allow exact timing"), Modifier.fillMaxWidth().padding(top = 6.dp), Kind.GHOST) {
                    if (Build.VERSION.SDK_INT >= 31) runCatching { ctx.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", ctx.packageName, null))) }
                    tick++
                }
            }
            Text(
                w.t("На Xiaomi, Huawei и подобных стоит разрешить автозапуск и снять ограничения батареи — иначе система может заглушить колокол.", "On Xiaomi, Huawei and the like, allow autostart and lift battery limits, or the system may silence the bell."),
                style = T.Small, modifier = Modifier.padding(top = 8.dp),
            )
        }
        Gap(10)
        Card {
            Text(w.t("Великое молчание", "The great silence"), style = T.Heading)
            Toggle(
                w.t("Закрывать книгу на ночь", "Close the book for the night"),
                w.t("после последнего часа дня приложение молчит до первого часа завтра; нарушить можно долгим нажатием", "after the last hour of the day the app stays silent until the first hour tomorrow; a long press breaks it"),
                st.silence,
            ) { v -> save { it.copy(silence = v) } }
        }
        Gap(10)
        CommunityLinks(w)
        Gap(10)
        Text(
            w.t("Часослов ${BuildConfig.VERSION_NAME}. Всё хранится только в телефоне: ни аккаунтов, ни сети. Тексты чтений — собственные переводы из общественного достояния.", "Horae ${BuildConfig.VERSION_NAME}. Everything stays on the phone: no accounts, no network. The readings are our own renderings from the public domain."),
            style = T.Small, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
    }
}
