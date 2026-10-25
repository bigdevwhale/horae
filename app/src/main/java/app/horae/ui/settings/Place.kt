package app.horae.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.horae.container
import app.horae.data.Cities
import app.horae.data.Locate
import app.horae.domain.Place
import app.horae.notify.Bells
import app.horae.ui.Words
import app.horae.ui.components.Btn
import app.horae.ui.components.Field
import app.horae.ui.components.Gap
import app.horae.ui.components.Kind
import app.horae.ui.components.LocalToast
import app.horae.ui.theme.C
import app.horae.ui.theme.T
import kotlinx.coroutines.launch

/** Choosing where the sun is reckoned: by the phone's rough location once, or a city from the list. */
@Composable
fun PlacePicker(w: Words, current: Place, onPicked: (Place) -> Unit = {}) {
    val ctx = LocalContext.current
    val c = ctx.container
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    fun set(p: Place) {
        c.write { settings.update { it.copy(place = p) }; Bells.plan(ctx) }
        onPicked(p)
    }
    fun locate() = scope.launch {
        val p = Locate.here(ctx)
        if (p == null) toast.show(w.t("Не удалось определить — выбери город", "Couldn't tell where — pick a city")) else set(p)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) locate() else toast.show(w.t("Тогда — город из списка", "A city from the list, then")) }
    Column {
        Text(w.t("Сейчас: ", "Now: ") + Cities.name(current, w.ru), style = T.BodyM.copy(color = C.vermilion))
        Gap(6)
        Btn(w.t("Определить по телефону", "Use the phone's location"), Modifier.fillMaxWidth(), Kind.GHOST) {
            if (Locate.allowed(ctx)) locate() else ask.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        Text(w.t("Один раз и примерно, с точностью до пары километров, — только чтобы знать восход. Никуда не отправляется.", "Once and roughly, to a couple of kilometres, only to know the sunrise. Sent nowhere."), style = T.Small, modifier = Modifier.padding(top = 4.dp))
        Gap(10)
        Field(query, { query = it }, w.t("или найди город…", "or find a city…"))
        val list = Cities.all.filter { query.isBlank() || it.ru.contains(query, true) || it.en.contains(query, true) }.take(if (query.isBlank()) 12 else 30)
        for (city in list) {
            val on = current.name == city.place.name
            Text(
                if (w.ru) city.ru else city.en, style = T.Body.copy(color = if (on) C.vermilion else C.ink),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { set(city.place) }.padding(vertical = 7.dp, horizontal = 4.dp),
            )
        }
    }
}
