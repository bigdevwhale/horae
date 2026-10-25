package app.horae.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import app.horae.container
import app.horae.data.Settings
import app.horae.domain.Preset
import app.horae.notify.Bells
import app.horae.ui.Words
import app.horae.ui.art.Drollery
import app.horae.ui.art.Illum
import app.horae.ui.components.Btn
import app.horae.ui.components.Card
import app.horae.ui.components.Chip
import app.horae.ui.components.Gap
import app.horae.ui.components.Kind
import app.horae.ui.components.vellum
import app.horae.ui.settings.PlacePicker
import app.horae.ui.theme.C
import app.horae.ui.theme.T

/** Three leaves: what the book is, where its sun rises, and which rule to start with. */
@Composable
fun Onboarding(w: Words, settings: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    var page by remember { mutableIntStateOf(0) }
    var preset by remember { mutableStateOf(Preset.BENEDICT) }
    BackHandler(enabled = page > 0) { page-- }
    fun finish() {
        val p = preset
        c.write {
            repo.preset(p, w.ru)
            this.settings.update { it.copy(onboarded = true) }
            Bells.plan(ctx)
        }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { finish() }

    Column(Modifier.fillMaxSize().vellum().statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 16.dp)) {
            when (page) {
                0 -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Chip("RU", w.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru")) }
                        Box(Modifier.size(6.dp))
                        Chip("EN", !w.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
                    }
                    Gap(10)
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(78.dp)) { Illum.initialGround(this, Rect(0f, 0f, size.width, size.height), 2, tail = false) }
                            Text(w.t("Ч", "H"), style = T.Title.copy(fontSize = 58.sp, lineHeight = 58.sp, color = C.lapisDeep))
                        }
                        Text(w.t("асослов", "orae"), style = T.Title.copy(fontSize = 46.sp, lineHeight = 50.sp, color = C.ink), modifier = Modifier.padding(top = 22.dp))
                    }
                    Gap(28)
                    Text(
                        w.t(
                            "Средневековая книга часов: день поделён на часы молитвы и тишины. Колокол зовёт — ты останавливаешься на несколько минут.",
                            "A medieval book of hours: the day divided into hours of prayer and quiet. A bell calls, and you stop for a few minutes.",
                        ), style = T.Body,
                    )
                    Gap(10)
                    Text(
                        w.t(
                            "Из чего состоит каждый час, решаешь ты: дыхание, медитация, чётки, чтение, вопрос, благодарность, разбор дня, поклоны, свеча, молчание — или своё.",
                            "What each hour holds is yours to choose: breathing, meditation, beads, a reading, a question, thanks, a review of the day, bows, a candle, silence, or your own.",
                        ), style = T.Body,
                    )
                    Gap(10)
                    Text(
                        w.t(
                            "Всё сделанное вписывается в рукопись. Чем вернее хранишь часы, тем богаче буквицы и поля; пропущенный час остаётся пустым местом, без упрёков.",
                            "Everything done is written into a manuscript. The more faithfully you keep the hours, the richer the initials and margins; a missed hour stays a blank, without reproach.",
                        ), style = T.Body,
                    )
                    Gap(20)
                    Canvas(Modifier.fillMaxWidth().height(70.dp)) {
                        val g = size.height - 4f
                        drawLine(C.inkSoft.copy(alpha = .5f), Offset(0f, g), Offset(size.width, g), 2f)
                        Illum.drollery(this, Drollery.HARE, Offset(size.width * .25f, g), size.height * .8f)
                        Illum.drollery(this, Drollery.SNAIL, Offset(size.width * .7f, g), size.height * .7f, flip = true)
                    }
                }
                1 -> {
                    Text(w.t("Неравные часы", "Unequal hours"), style = T.Title)
                    Gap(8)
                    Text(
                        w.t(
                            "Так считали в старину: от восхода до заката — двенадцать часов, от заката до восхода — тоже двенадцать. Летом час дня длится больше восьмидесяти минут, зимой — меньше пятидесяти. Часы идут вслед за солнцем.",
                            "This is how the old world counted: twelve hours from sunrise to sunset, twelve from sunset to sunrise. In summer a day hour runs past eighty minutes, in winter under fifty. The hours follow the sun.",
                        ), style = T.Body,
                    )
                    Gap(8)
                    Text(w.t("Для этого нужно знать, где ты.", "For that the book needs to know where you are."), style = T.Italic)
                    Gap(14)
                    var place by remember { mutableStateOf(settings.place) }
                    PlacePicker(w, place) { place = it }
                }
                else -> {
                    Text(w.t("С какого устава начать?", "Which rule to start with?"), style = T.Title)
                    Gap(6)
                    Text(w.t("Это только начало: число часов, время, колокола и каждую практику потом можно поменять во вкладке «Устав».", "Just a start: the number of hours, the times, the bells and every practice can be changed later on the Rule tab."), style = T.Small)
                    Gap(10)
                    for (p in Preset.entries) {
                        Card(Modifier.padding(vertical = 4.dp), bg = if (preset == p) C.goldHi.copy(alpha = .45f) else C.card, onClick = { preset = p }) {
                            Text(w.preset(p), style = T.Heading.copy(color = if (preset == p) C.vermilion else C.ink))
                            Text(w.presetSub(p), style = T.Small)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${page + 1} / 3", style = T.Small, modifier = Modifier.weight(1f))
            Btn(
                when (page) { 0 -> w.t("Открыть книгу", "Open the book"); 1 -> w.t("Дальше", "Next"); else -> w.t("Начать", "Begin") },
                kind = Kind.RED,
            ) {
                if (page < 2) page++
                else if (Build.VERSION.SDK_INT >= 33 && !Bells.canNotify(ctx)) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                else finish()
            }
        }
    }
}
