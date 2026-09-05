package com.example.tujelly.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tujelly_settings")

const val DEFAULT_TMDB_API_KEY = "82dee22856e0d0ac5f767ec6fb845efc"
const val DEFAULT_TRAKT_CLIENT_ID = "9e65c7bb4a5b786a5b5df3e7371c3bcdfa00e26a49078ea20288d616fe83d84d"
const val DEFAULT_TRAKT_CLIENT_SECRET = "abc6fc95540499d0308e57bc27ecaf9adec7f9d40a8926f37941ff5799ba5ab3"

const val BUTTON_STYLE_ICONS_ONLY = "ICONS_ONLY"
const val BUTTON_STYLE_ICONS_AND_TEXT = "ICONS_AND_TEXT"
const val BUTTON_STYLE_TEXT_ONLY = "TEXT_ONLY"

const val ACCENT_CYAN = "CYAN"
const val ACCENT_AMBER = "AMBER"
const val ACCENT_WHITE = "WHITE"

const val INDICATOR_THEME_COLOR = "COLOR"
const val INDICATOR_THEME_MONOCHROME = "MONOCHROME"

const val PLATFORM_LOGO_COLOR = "COLOR"
const val PLATFORM_LOGO_MONOCHROME = "MONOCHROME"

data class UserPreferences(
    val jellyfinServerUrl: String = "",
    val jellyfinUserId: String = "",
    val jellyfinUsername: String = "",
    val jellyfinPassword: String = "",
    val jellyfinAccessToken: String = "",
    val traktAccessToken: String = "",
    val traktClientId: String = DEFAULT_TRAKT_CLIENT_ID,
    val traktClientSecret: String = DEFAULT_TRAKT_CLIENT_SECRET,
    val tmdbApiKey: String = DEFAULT_TMDB_API_KEY,
    val watchRegion: String = "ES",
    val buttonStyle: String = BUTTON_STYLE_ICONS_ONLY,
    val accentColor: String = ACCENT_CYAN,
    val indicatorTheme: String = INDICATOR_THEME_COLOR,
    val platformLogoStyle: String = PLATFORM_LOGO_COLOR,
    val jellyfinLastSync: String = ""
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val JELLYFIN_SERVER_URL = stringPreferencesKey("jellyfin_server_url")
        val JELLYFIN_USER_ID = stringPreferencesKey("jellyfin_user_id")
        val JELLYFIN_USERNAME = stringPreferencesKey("jellyfin_username")
        val JELLYFIN_PASSWORD = stringPreferencesKey("jellyfin_password")
        val JELLYFIN_ACCESS_TOKEN = stringPreferencesKey("jellyfin_access_token")
        val TRAKT_ACCESS_TOKEN = stringPreferencesKey("trakt_access_token")
        val TRAKT_CLIENT_ID = stringPreferencesKey("trakt_client_id")
        val TRAKT_CLIENT_SECRET = stringPreferencesKey("trakt_client_secret")
        val TMDB_API_KEY = stringPreferencesKey("tmdb_api_key")
        val WATCH_REGION = stringPreferencesKey("watch_region")
        val BUTTON_STYLE = stringPreferencesKey("button_style")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val INDICATOR_THEME = stringPreferencesKey("indicator_theme")
        val PLATFORM_LOGO_STYLE = stringPreferencesKey("platform_logo_style")
        val JELLYFIN_LAST_SYNC = stringPreferencesKey("jellyfin_last_sync")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val rawTmdb = prefs[Keys.TMDB_API_KEY] ?: ""
        val tmdbKey = if (rawTmdb.isBlank() || rawTmdb == "b673238641eb86a9f4787f0b5d564177") DEFAULT_TMDB_API_KEY else rawTmdb
        val rawTraktId = prefs[Keys.TRAKT_CLIENT_ID] ?: ""
        val traktId = if (rawTraktId.isBlank() || rawTraktId.startsWith("4376c989")) DEFAULT_TRAKT_CLIENT_ID else rawTraktId
        val rawTraktSecret = prefs[Keys.TRAKT_CLIENT_SECRET] ?: ""
        val traktSecret = if (rawTraktSecret.isBlank()) DEFAULT_TRAKT_CLIENT_SECRET else rawTraktSecret

        UserPreferences(
            jellyfinServerUrl = prefs[Keys.JELLYFIN_SERVER_URL] ?: "",
            jellyfinUserId = prefs[Keys.JELLYFIN_USER_ID] ?: "",
            jellyfinUsername = prefs[Keys.JELLYFIN_USERNAME] ?: "",
            jellyfinPassword = prefs[Keys.JELLYFIN_PASSWORD] ?: "",
            jellyfinAccessToken = prefs[Keys.JELLYFIN_ACCESS_TOKEN] ?: "",
            traktAccessToken = prefs[Keys.TRAKT_ACCESS_TOKEN] ?: "",
            traktClientId = traktId,
            traktClientSecret = traktSecret,
            tmdbApiKey = tmdbKey,
            watchRegion = prefs[Keys.WATCH_REGION] ?: "ES",
            buttonStyle = prefs[Keys.BUTTON_STYLE] ?: BUTTON_STYLE_ICONS_ONLY,
            accentColor = prefs[Keys.ACCENT_COLOR] ?: ACCENT_CYAN,
            indicatorTheme = prefs[Keys.INDICATOR_THEME] ?: INDICATOR_THEME_COLOR,
            platformLogoStyle = prefs[Keys.PLATFORM_LOGO_STYLE] ?: PLATFORM_LOGO_COLOR,
            jellyfinLastSync = prefs[Keys.JELLYFIN_LAST_SYNC] ?: ""
        )
    }

    suspend fun updateJellyfinServerUrl(serverUrl: String) {
        val cleanUrl = serverUrl.trimEnd('/')
        context.dataStore.edit { prefs ->
            prefs[Keys.JELLYFIN_SERVER_URL] = cleanUrl
        }
    }

    suspend fun updateJellyfinUsername(username: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.JELLYFIN_USERNAME] = username
        }
    }

    suspend fun updateJellyfinPassword(password: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.JELLYFIN_PASSWORD] = password
        }
    }

    suspend fun updateJellyfinConfig(serverUrl: String, userId: String, accessToken: String, username: String = "", password: String = "") {
        val cleanUrl = serverUrl.trimEnd('/')
        context.dataStore.edit { prefs ->
            prefs[Keys.JELLYFIN_SERVER_URL] = cleanUrl
            prefs[Keys.JELLYFIN_USER_ID] = userId
            prefs[Keys.JELLYFIN_ACCESS_TOKEN] = accessToken
            if (username.isNotBlank()) {
                prefs[Keys.JELLYFIN_USERNAME] = username
            }
            if (password.isNotBlank()) {
                prefs[Keys.JELLYFIN_PASSWORD] = password
            }
        }
    }

    suspend fun updateTraktConfig(token: String, clientId: String, clientSecret: String = "") {
        context.dataStore.edit { prefs ->
            prefs[Keys.TRAKT_ACCESS_TOKEN] = token
            prefs[Keys.TRAKT_CLIENT_ID] = clientId
            if (clientSecret.isNotBlank()) {
                prefs[Keys.TRAKT_CLIENT_SECRET] = clientSecret
            }
        }
    }

    suspend fun updateTmdbConfig(apiKey: String, region: String = "ES") {
        context.dataStore.edit { prefs ->
            prefs[Keys.TMDB_API_KEY] = apiKey
            prefs[Keys.WATCH_REGION] = region
        }
    }

    suspend fun updateButtonStyle(style: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BUTTON_STYLE] = style
        }
    }

    suspend fun updateAccentColor(color: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCENT_COLOR] = color
        }
    }

    suspend fun updateIndicatorTheme(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.INDICATOR_THEME] = theme
        }
    }

    suspend fun updatePlatformLogoStyle(style: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PLATFORM_LOGO_STYLE] = style
        }
    }

    suspend fun updateJellyfinLastSync(timestamp: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.JELLYFIN_LAST_SYNC] = timestamp
        }
    }

    suspend fun clearJellyfinLastSync() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.JELLYFIN_LAST_SYNC)
        }
    }
}
