package app.horae

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The launcher icon drawn round, for the README (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArtLogo {
    @Test fun logo() {
        val c = ApplicationProvider.getApplicationContext<HoraeApp>()
        val size = 432
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.clipPath(Path().apply { addCircle(size / 2f, size / 2f, size / 2f * .98f, Path.Direction.CW) })
        // The adaptive layers are 108 units with the visible circle in the middle 72.
        val bleed = (size * 108f / 72f).toInt()
        val off = (size - bleed) / 2
        for (id in listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) {
            val d = ContextCompat.getDrawable(c, id)!!
            d.setBounds(off, off, off + bleed, off + bleed)
            d.draw(canvas)
        }
        val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }
        File(out, "logo.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        File("../docs").mkdirs()
        File("../docs/logo.png").outputStream().use { Bitmap.createScaledBitmap(bmp, 192, 192, true).compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
