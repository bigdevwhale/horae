package app.horae

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.horae.data.Settings
import app.horae.notify.Bells
import app.horae.ui.HoraeRoot
import app.horae.ui.theme.HoraeTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(t, t), navigationBarStyle = SystemBarStyle.light(t, t))
        take(intent)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            // Wait for settings so the welcome pages don't flash for someone already here.
            settings?.let { s -> HoraeTheme { HoraeRoot(s) } }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    private fun take(i: Intent?) {
        val h = i?.getLongExtra(Bells.EXTRA_HOUR, 0) ?: 0
        if (h != 0L) { container.openHour.value = h; i?.removeExtra(Bells.EXTRA_HOUR) }
    }

    /** With the beads in hand, the volume keys move them, so the counting needs no look at the screen. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (container.beadsInHand && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            if (event?.repeatCount == 0) container.keys.tryEmit(Unit)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (container.beadsInHand && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) return true
        return super.onKeyUp(keyCode, event)
    }
}
