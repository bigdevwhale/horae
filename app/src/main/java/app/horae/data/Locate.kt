package app.horae.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import app.horae.domain.Place
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Where the phone is, roughly: only to know when the sun rises here. Asked once, never tracked. */
object Locate {
    fun allowed(c: Context) = ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun here(c: Context): Place? {
        if (!allowed(c)) return null
        val lm = c.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER).filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        val last = try { providers.mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time } } catch (_: SecurityException) { null }
        val loc = last ?: if (Build.VERSION.SDK_INT >= 30 && providers.isNotEmpty()) withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine<Location?> { k ->
                try { lm.getCurrentLocation(providers.first(), null, c.mainExecutor) { k.resume(it) } } catch (_: SecurityException) { k.resume(null) }
            }
        } else null
        // Two decimals is a few kilometres: enough for the sun, not enough to find a door.
        return loc?.let { Place(Math.round(it.latitude * 100) / 100.0, Math.round(it.longitude * 100) / 100.0, "") }
    }
}
