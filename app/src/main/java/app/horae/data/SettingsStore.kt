package app.horae.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.horae.domain.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** Where the sun is reckoned; Moscow until the person says otherwise. */
    val place: Place = Cities.default,
    /** After the last hour of the day the book closes until the first one tomorrow. */
    val silence: Boolean = true,
    val sound: Boolean = true,
    /** The hour's bell rings through the alarm volume even when the phone is on silent. */
    val loud: Boolean = false,
    /** Beads, breath phases and bells felt in the hand. */
    val haptics: Boolean = true,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        place = if (this[LAT] != null && this[LON] != null) Place(this[LAT]!!, this[LON]!!, this[PLACE] ?: "") else Cities.default,
        silence = this[SILENCE] ?: true,
        sound = this[SOUND] ?: true,
        loud = this[LOUD] ?: false,
        haptics = this[HAPTICS] ?: true,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[LAT] = s.place.lat
            it[LON] = s.place.lon
            it[PLACE] = s.place.name
            it[SILENCE] = s.silence
            it[SOUND] = s.sound
            it[LOUD] = s.loud
            it[HAPTICS] = s.haptics
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val LAT = doublePreferencesKey("lat")
        val LON = doublePreferencesKey("lon")
        val PLACE = stringPreferencesKey("place")
        val SILENCE = booleanPreferencesKey("silence")
        val SOUND = booleanPreferencesKey("sound")
        val LOUD = booleanPreferencesKey("loud")
        val HAPTICS = booleanPreferencesKey("haptics")
    }
}
