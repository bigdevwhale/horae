package app.horae.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.horae.R
import app.horae.domain.Hue

/** Parchment, iron-gall ink, and the scriptorium's pigments: vermilion, lapis, gold leaf, malachite. */
@Immutable
object C {
    val vellum = Color(0xFFF2E8D2)
    val vellumDeep = Color(0xFFE8DABB)
    val vellumEdge = Color(0xFFD9C59C)
    val ink = Color(0xFF2A1E16)
    val inkSoft = Color(0xFF5C4A3B)
    val faded = Color(0xFF8F7A62)
    val rule = Color(0x332A1E16)
    val card = Color(0x80FFFDF6)
    val vermilion = Color(0xFFB4351F)
    val vermilionDeep = Color(0xFF8E2716)
    val lapis = Color(0xFF2B4C8C)
    val lapisDeep = Color(0xFF1C3466)
    val gold = Color(0xFFC79A3A)
    val goldHi = Color(0xFFF3DE96)
    val goldLo = Color(0xFF8E6A22)
    val malachite = Color(0xFF3F6E4C)
    val violet = Color(0xFF5E3C78)
    val rose = Color(0xFFC07686)

    /** The night office: a room lit by one flame. */
    val night = Color(0xFF15100C)
    val nightCard = Color(0xFF231A13)
    val nightLine = Color(0xFF3A2C20)
    val candle = Color(0xFFF6D58A)
    val candleHi = Color(0xFFFFF4D0)
    val ember = Color(0xFFE08A3A)
    val scrim = Color(0xD9120D09)

    fun hue(h: Hue) = when (h) {
        Hue.GREEN -> malachite; Hue.VIOLET -> violet; Hue.WHITE -> gold; Hue.RED -> vermilion; Hue.ROSE -> rose
    }
}

val Garamond = FontFamily(
    Font(R.font.garamond_regular, FontWeight.Normal),
    Font(R.font.garamond_medium, FontWeight.Medium),
    Font(R.font.garamond_semibold, FontWeight.SemiBold),
    Font(R.font.garamond_italic, FontWeight.Normal, FontStyle.Italic),
)

object T {
    val Title = TextStyle(fontFamily = Garamond, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp, color = C.ink)
    val Heading = TextStyle(fontFamily = Garamond, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 26.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Garamond, fontSize = 18.sp, lineHeight = 24.sp, color = C.ink)
    val BodyM = Body.copy(fontWeight = FontWeight.Medium)
    val Italic = Body.copy(fontStyle = FontStyle.Italic)
    val Small = TextStyle(fontFamily = Garamond, fontSize = 15.sp, lineHeight = 20.sp, color = C.faded)
    /** Rubrics: the red small capitals that name things in a manuscript. */
    val Rubric = TextStyle(fontFamily = Garamond, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 1.6.sp, color = C.vermilion, fontFeatureSettings = "smcp, c2sc")
    val Button = TextStyle(fontFamily = Garamond, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 20.sp)
    val Big = TextStyle(fontFamily = Garamond, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 38.sp, color = C.candleHi)
    val Huge = TextStyle(fontFamily = Garamond, fontWeight = FontWeight.Medium, fontSize = 64.sp, lineHeight = 68.sp, color = C.candleHi)
}

@Composable
fun HoraeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.vermilion, onPrimary = Color.White, background = C.vellum, surface = C.vellum, onSurface = C.ink,
            onBackground = C.ink, outline = C.vellumEdge, error = C.vermilion,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
