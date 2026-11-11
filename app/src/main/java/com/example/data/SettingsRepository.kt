package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "callbox_settings")

data class CallBoxSettings(
    val shizukuReady: Boolean = false,
    val autoRecordWhatsapp: Boolean = true,
    val autoRecordInstagram: Boolean = true,
    val autoRecordTelegram: Boolean = true,
    val autoRecordImo: Boolean = true,
    val autoRecordMeet: Boolean = true,
    val autoRecordPhone: Boolean = true,
    val audioBitrate: Int = 96000, // 64000, 96000, 128000
    val hasSeenDisclaimer: Boolean = false,
    val floatingOverlayEnabled: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val SHIZUKU_READY = booleanPreferencesKey("shizukuReady")
        val AUTO_RECORD_WHATSAPP = booleanPreferencesKey("autoRecord_whatsapp")
        val AUTO_RECORD_INSTAGRAM = booleanPreferencesKey("autoRecord_instagram")
        val AUTO_RECORD_TELEGRAM = booleanPreferencesKey("autoRecord_telegram")
        val AUTO_RECORD_IMO = booleanPreferencesKey("autoRecord_imo")
        val AUTO_RECORD_MEET = booleanPreferencesKey("autoRecord_meet")
        val AUTO_RECORD_PHONE = booleanPreferencesKey("autoRecord_phone")
        val AUDIO_BITRATE = intPreferencesKey("audioBitrate")
        val HAS_SEEN_DISCLAIMER = booleanPreferencesKey("has_seen_disclaimer")
        val FLOATING_OVERLAY_ENABLED = booleanPreferencesKey("floating_overlay_enabled")
    }

    val settingsFlow: Flow<CallBoxSettings> = context.dataStore.data.map { preferences ->
        CallBoxSettings(
            shizukuReady = preferences[PreferencesKeys.SHIZUKU_READY] ?: false,
            autoRecordWhatsapp = preferences[PreferencesKeys.AUTO_RECORD_WHATSAPP] ?: true,
            autoRecordInstagram = preferences[PreferencesKeys.AUTO_RECORD_INSTAGRAM] ?: true,
            autoRecordTelegram = preferences[PreferencesKeys.AUTO_RECORD_TELEGRAM] ?: true,
            autoRecordImo = preferences[PreferencesKeys.AUTO_RECORD_IMO] ?: true,
            autoRecordMeet = preferences[PreferencesKeys.AUTO_RECORD_MEET] ?: true,
            autoRecordPhone = preferences[PreferencesKeys.AUTO_RECORD_PHONE] ?: true,
            audioBitrate = preferences[PreferencesKeys.AUDIO_BITRATE] ?: 96000,
            hasSeenDisclaimer = preferences[PreferencesKeys.HAS_SEEN_DISCLAIMER] ?: false,
            floatingOverlayEnabled = preferences[PreferencesKeys.FLOATING_OVERLAY_ENABLED] ?: true
        )
    }

    suspend fun setShizukuReady(ready: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHIZUKU_READY] = ready
        }
    }

    suspend fun setAutoRecord(appKey: String, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            when (appKey) {
                "whatsapp" -> preferences[PreferencesKeys.AUTO_RECORD_WHATSAPP] = enabled
                "instagram" -> preferences[PreferencesKeys.AUTO_RECORD_INSTAGRAM] = enabled
                "telegram" -> preferences[PreferencesKeys.AUTO_RECORD_TELEGRAM] = enabled
                "imo" -> preferences[PreferencesKeys.AUTO_RECORD_IMO] = enabled
                "meet" -> preferences[PreferencesKeys.AUTO_RECORD_MEET] = enabled
                "phone" -> preferences[PreferencesKeys.AUTO_RECORD_PHONE] = enabled
            }
        }
    }

    suspend fun setAudioBitrate(bitrate: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_BITRATE] = bitrate
        }
    }

    suspend fun setHasSeenDisclaimer(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SEEN_DISCLAIMER] = seen
        }
    }

    suspend fun setFloatingOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FLOATING_OVERLAY_ENABLED] = enabled
        }
    }
}
