package com.example.tujelly.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tujelly.data.local.BUTTON_STYLE_ICONS_ONLY
import com.example.tujelly.data.local.DEFAULT_TRAKT_CLIENT_ID
import com.example.tujelly.data.local.DEFAULT_TRAKT_CLIENT_SECRET
import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.remote.NetworkClientFactory
import com.example.tujelly.data.remote.jellyfin.JellyfinApiService
import com.example.tujelly.data.remote.jellyfin.JellyfinAuthRequest
import com.example.tujelly.data.remote.trakt.TraktApiService
import com.example.tujelly.data.remote.trakt.TraktDeviceCodeRequest
import com.example.tujelly.data.remote.trakt.TraktDeviceTokenRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URLEncoder

enum class SettingField(val title: String, val description: String, val isPassword: Boolean = false) {
    SERVER_URL("URL del Servidor Jellyfin", "Introduce la URL o IP de tu servidor (ej. https://tu-servidor-jellyfin.com)"),
    USERNAME("Usuario Jellyfin", "Nombre de tu cuenta en Jellyfin"),
    PASSWORD("Contraseña Jellyfin", "Contraseña de tu cuenta en Jellyfin", isPassword = true),
    TRAKT_CLIENT_ID("Trakt Client ID", "ID de aplicación creada en trakt.tv/oauth/applications"),
    TRAKT_CLIENT_SECRET("Trakt Client Secret", "Clave secreta opcional de tu aplicación Trakt", isPassword = true),
    TRAKT_MANUAL_TOKEN("Trakt Access Token Directo", "Pega directamente tu token personal de Trakt si ya lo tienes"),
    WATCH_REGION("Región de Catálogo", "Código de país de 2 letras (ej. ES para España, MX, US)")
}

data class SettingsUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val traktToken: String = "",
    val traktClientId: String = DEFAULT_TRAKT_CLIENT_ID,
    val traktClientSecret: String = DEFAULT_TRAKT_CLIENT_SECRET,
    val tmdbApiKey: String = "",
    val watchRegion: String = "ES",
    val buttonStyle: String = BUTTON_STYLE_ICONS_ONLY,
    val accentColor: String = com.example.tujelly.data.local.ACCENT_CYAN,
    val indicatorTheme: String = com.example.tujelly.data.local.INDICATOR_THEME_COLOR,
    val platformLogoStyle: String = com.example.tujelly.data.local.PLATFORM_LOGO_COLOR,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val isSuccess: Boolean = false,
    val activeCategory: Int = 0,
    val serverName: String? = null,
    val serverVersion: String? = null,
    val isJellyfinConnected: Boolean = false,
    // Trakt QR Device Flow & Status
    val isTraktConnected: Boolean = false,
    val traktUsername: String? = null,
    val traktUserCode: String? = null,
    val traktQrUrl: String? = null,
    val isWaitingTraktAuth: Boolean = false,
    // Jellyfin Quick Connect Flow
    val jellyfinQuickConnectCode: String? = null,
    val jellyfinQrUrl: String? = null,
    val isWaitingJellyfinQuickConnect: Boolean = false,
    // TV Dialog Edit State
    val activeDialogField: SettingField? = null,
    val activeDialogValue: String = "",
    // GitHub App Updates
    val isCheckingUpdate: Boolean = false,
    val updateInfo: com.example.tujelly.data.remote.github.UpdateInfo? = null,
    val downloadProgress: Float? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val appUpdateManager = com.example.tujelly.data.remote.github.AppUpdateManager(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var traktPollJob: Job? = null
    private var jellyfinQuickConnectJob: Job? = null

    init {
        loadCurrentPreferences()
    }

    private fun loadCurrentPreferences() {
        viewModelScope.launch {
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            val isJfConnected = prefs.jellyfinAccessToken.isNotBlank() && prefs.jellyfinServerUrl.isNotBlank()

            _uiState.value = _uiState.value.copy(
                serverUrl = prefs.jellyfinServerUrl,
                username = prefs.jellyfinUsername,
                password = prefs.jellyfinPassword,
                traktToken = prefs.traktAccessToken,
                traktClientId = prefs.traktClientId,
                traktClientSecret = prefs.traktClientSecret,
                tmdbApiKey = prefs.tmdbApiKey,
                watchRegion = prefs.watchRegion,
                buttonStyle = prefs.buttonStyle,
                accentColor = prefs.accentColor,
                indicatorTheme = prefs.indicatorTheme,
                platformLogoStyle = prefs.platformLogoStyle,
                isJellyfinConnected = isJfConnected,
                isTraktConnected = prefs.traktAccessToken.isNotBlank()
            )
            if (prefs.traktAccessToken.isNotBlank()) {
                validateTraktToken(prefs.traktAccessToken, prefs.traktClientId)
            }
        }
    }

    fun selectCategory(categoryIndex: Int) {
        _uiState.value = _uiState.value.copy(activeCategory = categoryIndex)
    }

    fun updateButtonStyle(style: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateButtonStyle(style)
            _uiState.value = _uiState.value.copy(buttonStyle = style)
        }
    }

    fun updateAccentColor(accent: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateAccentColor(accent)
            _uiState.value = _uiState.value.copy(accentColor = accent)
        }
    }

    fun updateIndicatorTheme(theme: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateIndicatorTheme(theme)
            _uiState.value = _uiState.value.copy(indicatorTheme = theme)
        }
    }

    fun updatePlatformLogoStyle(style: String) {
        viewModelScope.launch {
            userPreferencesRepository.updatePlatformLogoStyle(style)
            _uiState.value = _uiState.value.copy(platformLogoStyle = style)
        }
    }

    fun validateTraktToken(
        token: String = _uiState.value.traktToken,
        clientId: String = _uiState.value.traktClientId
    ) {
        if (token.isBlank()) {
            _uiState.value = _uiState.value.copy(isTraktConnected = false, traktUsername = null)
            return
        }
        viewModelScope.launch {
            val database = com.example.tujelly.data.local.db.AppDatabase.getDatabase(getApplication())
            val repository = com.example.tujelly.data.repository.MediaRepository(database.jellyfinDao())
            val effectiveClientId = clientId.ifBlank { DEFAULT_TRAKT_CLIENT_ID }
            val result = repository.getTraktUserProfile(token, effectiveClientId)
            result.onSuccess { user ->
                val handle = user.username ?: user.name ?: "Conectado"
                _uiState.value = _uiState.value.copy(
                    isTraktConnected = true,
                    traktUsername = handle,
                    statusMessage = "Cuenta Trakt verificada: @$handle"
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isTraktConnected = true,
                    traktUsername = null,
                    statusMessage = "Trakt conectado (${e.localizedMessage ?: "Activo"})"
                )
            }
        }
    }

    fun openEditDialog(field: SettingField) {
        val currentVal = when (field) {
            SettingField.SERVER_URL -> _uiState.value.serverUrl
            SettingField.USERNAME -> _uiState.value.username
            SettingField.PASSWORD -> _uiState.value.password
            SettingField.TRAKT_CLIENT_ID -> _uiState.value.traktClientId
            SettingField.TRAKT_CLIENT_SECRET -> _uiState.value.traktClientSecret
            SettingField.TRAKT_MANUAL_TOKEN -> _uiState.value.traktToken
            SettingField.WATCH_REGION -> _uiState.value.watchRegion
        }
        _uiState.value = _uiState.value.copy(
            activeDialogField = field,
            activeDialogValue = currentVal
        )
    }

    fun onDialogValueChange(newValue: String) {
        _uiState.value = _uiState.value.copy(activeDialogValue = newValue)
    }

    fun commitDialogValue() {
        val field = _uiState.value.activeDialogField ?: return
        val value = _uiState.value.activeDialogValue.trim()
        when (field) {
            SettingField.SERVER_URL -> {
                _uiState.value = _uiState.value.copy(serverUrl = value)
                viewModelScope.launch {
                    userPreferencesRepository.updateJellyfinServerUrl(value)
                }
            }
            SettingField.USERNAME -> {
                _uiState.value = _uiState.value.copy(username = value)
                viewModelScope.launch {
                    userPreferencesRepository.updateJellyfinUsername(value)
                }
            }
            SettingField.PASSWORD -> {
                _uiState.value = _uiState.value.copy(password = value)
                viewModelScope.launch {
                    userPreferencesRepository.updateJellyfinPassword(value)
                }
            }
            SettingField.TRAKT_CLIENT_ID -> {
                _uiState.value = _uiState.value.copy(traktClientId = value)
                viewModelScope.launch {
                    userPreferencesRepository.updateTraktConfig(
                        token = _uiState.value.traktToken,
                        clientId = value,
                        clientSecret = _uiState.value.traktClientSecret
                    )
                }
            }
            SettingField.TRAKT_CLIENT_SECRET -> {
                _uiState.value = _uiState.value.copy(traktClientSecret = value)
                viewModelScope.launch {
                    userPreferencesRepository.updateTraktConfig(
                        token = _uiState.value.traktToken,
                        clientId = _uiState.value.traktClientId,
                        clientSecret = value
                    )
                }
            }
            SettingField.TRAKT_MANUAL_TOKEN -> {
                val isConnected = value.isNotBlank()
                _uiState.value = _uiState.value.copy(
                    traktToken = value,
                    isTraktConnected = isConnected,
                    statusMessage = if (isConnected) "Guardando y verificando Token de Trakt..." else "Token de Trakt eliminado."
                )
                viewModelScope.launch {
                    userPreferencesRepository.updateTraktConfig(
                        token = value,
                        clientId = _uiState.value.traktClientId,
                        clientSecret = _uiState.value.traktClientSecret
                    )
                    if (isConnected) {
                        validateTraktToken(value, _uiState.value.traktClientId)
                    }
                }
            }
            SettingField.WATCH_REGION -> {
                val region = value.ifBlank { "ES" }.uppercase()
                _uiState.value = _uiState.value.copy(watchRegion = region)
                viewModelScope.launch {
                    userPreferencesRepository.updateTmdbConfig(
                        apiKey = _uiState.value.tmdbApiKey,
                        region = region
                    )
                }
            }
        }
        _uiState.value = _uiState.value.copy(activeDialogField = null)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(activeDialogField = null)
    }

    fun onServerUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
        viewModelScope.launch {
            userPreferencesRepository.updateJellyfinServerUrl(url)
        }
    }

    fun disconnectTrakt() {
        traktPollJob?.cancel()
        viewModelScope.launch {
            userPreferencesRepository.updateTraktConfig("", _uiState.value.traktClientId, _uiState.value.traktClientSecret)
            _uiState.value = _uiState.value.copy(
                isTraktConnected = false,
                traktToken = "",
                traktUsername = null,
                traktUserCode = null,
                traktQrUrl = null,
                isWaitingTraktAuth = false,
                statusMessage = "Cuenta de Trakt desvinculada."
            )
        }
    }

    fun startTraktDeviceFlow() {
        traktPollJob?.cancel()
        traktPollJob = viewModelScope.launch {
            val clientId = _uiState.value.traktClientId.trim().ifBlank { DEFAULT_TRAKT_CLIENT_ID }
            val clientSecret = _uiState.value.traktClientSecret.trim().ifBlank { DEFAULT_TRAKT_CLIENT_SECRET }

            _uiState.value = _uiState.value.copy(
                isWaitingTraktAuth = true,
                statusMessage = "Solicitando código a Trakt.tv..."
            )
            try {
                val traktApi = NetworkClientFactory.createService("https://api.trakt.tv/", TraktApiService::class.java)
                val codeResponse = traktApi.getDeviceCode(
                    apiVersion = "2",
                    clientIdHeader = clientId,
                    request = TraktDeviceCodeRequest(clientId = clientId)
                )

                val verifyUrl = "https://trakt.tv/activate"
                val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=350x350&data=" + URLEncoder.encode(verifyUrl, "UTF-8")

                _uiState.value = _uiState.value.copy(
                    traktUserCode = codeResponse.userCode,
                    traktQrUrl = qrUrl,
                    statusMessage = "Escanea el código QR o entra en trakt.tv/activate"
                )

                var intervalMs = (codeResponse.interval.coerceAtLeast(5)) * 1000L
                val maxAttempts = (codeResponse.expiresIn / codeResponse.interval).coerceAtLeast(10)

                for (i in 0 until maxAttempts) {
                    delay(intervalMs)
                    try {
                        val tokenResponse = traktApi.pollDeviceToken(
                            apiVersion = "2",
                            clientIdHeader = clientId,
                            request = TraktDeviceTokenRequest(
                                code = codeResponse.deviceCode,
                                clientId = clientId,
                                clientSecret = clientSecret
                            )
                        )
                        if (tokenResponse.accessToken.isNotBlank()) {
                            userPreferencesRepository.updateTraktConfig(
                                token = tokenResponse.accessToken,
                                clientId = clientId,
                                clientSecret = clientSecret
                            )
                            _uiState.value = _uiState.value.copy(
                                isWaitingTraktAuth = false,
                                isTraktConnected = true,
                                traktToken = tokenResponse.accessToken,
                                statusMessage = "Trakt vinculado exitosamente."
                            )
                            validateTraktToken(tokenResponse.accessToken, clientId)
                            break
                        }
                    } catch (e: Throwable) {
                        if (e is retrofit2.HttpException) {
                            val code = e.code()
                            val errBody = runCatching { e.response()?.errorBody()?.string() ?: "" }.getOrDefault("")
                            if (code == 400) {
                                if (errBody.contains("slow_down")) {
                                    intervalMs += 2000L
                                } else if (errBody.contains("authorization_pending")) {
                                    continue
                                } else if (errBody.contains("expired_token") || errBody.contains("invalid_grant")) {
                                    _uiState.value = _uiState.value.copy(
                                        isWaitingTraktAuth = false,
                                        statusMessage = "Código de Trakt expirado."
                                    )
                                    break
                                }
                            } else if (code == 401 || code == 403) {
                                _uiState.value = _uiState.value.copy(
                                    isWaitingTraktAuth = false,
                                    statusMessage = "Credenciales Trakt no válidas."
                                )
                                break
                            } else if (code == 404 || code == 410 || code == 418) {
                                _uiState.value = _uiState.value.copy(
                                    isWaitingTraktAuth = false,
                                    statusMessage = "Proceso cancelado por Trakt."
                                )
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                val errorDetails = if (e is retrofit2.HttpException && e.code() == 401) {
                    "Client ID de Trakt no encontrado."
                } else {
                    "Error al vincular Trakt: ${e.localizedMessage}"
                }
                _uiState.value = _uiState.value.copy(
                    isWaitingTraktAuth = false,
                    statusMessage = errorDetails
                )
            }
        }
    }

    private fun formatNetworkError(e: Throwable, cleanUrl: String): String {
        return when (e) {
            is retrofit2.HttpException -> {
                when (e.code()) {
                    401 -> "Error 401: Usuario o contraseña incorrectos en Jellyfin."
                    403 -> "Error 403: Acceso denegado en Jellyfin."
                    404 -> "Error 404: Servidor Jellyfin no encontrado en $cleanUrl."
                    500, 502, 503 -> "Error ${e.code()}: Fallo interno del servidor."
                    else -> "Error HTTP ${e.code()}: ${e.message()}"
                }
            }
            is java.net.ConnectException -> "No se pudo conectar a $cleanUrl. Verifica que el servidor esté activo."
            is java.net.SocketTimeoutException -> "Tiempo de espera agotado al conectar a $cleanUrl."
            is java.net.UnknownHostException -> "No se encuentra el host '$cleanUrl'."
            is javax.net.ssl.SSLHandshakeException -> "Error de certificado SSL. Si estás en red local usa http://."
            else -> "Error de conexión: ${e.localizedMessage ?: "Fallo desconocido"}"
        }
    }

    fun testServerConnection() {
        val raw = _uiState.value.serverUrl
        val cleanUrl = NetworkClientFactory.normalizeUrl(raw)
        if (cleanUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(statusMessage = "Introduce primero la URL de tu servidor")
            return
        }
        _uiState.value = _uiState.value.copy(serverUrl = cleanUrl, isLoading = true, statusMessage = "Probando conexión con $cleanUrl...")

        viewModelScope.launch {
            val api = NetworkClientFactory.createService(cleanUrl, JellyfinApiService::class.java)
            try {
                val info = api.getPublicInfo()
                val serverName = info.effectiveServerName
                val version = info.effectiveVersion
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    serverName = serverName,
                    serverVersion = version,
                    statusMessage = "Servidor detectado: $serverName ($version)"
                )
            } catch (e: Exception) {
                val details = formatNetworkError(e, cleanUrl)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Error: $details"
                )
            }
        }
    }

    fun startJellyfinQuickConnect() {
        val raw = _uiState.value.serverUrl
        val cleanUrl = NetworkClientFactory.normalizeUrl(raw)
        if (cleanUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(statusMessage = "Introduce la URL de tu servidor Jellyfin")
            return
        }
        _uiState.value = _uiState.value.copy(serverUrl = cleanUrl)

        jellyfinQuickConnectJob?.cancel()
        jellyfinQuickConnectJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWaitingJellyfinQuickConnect = true,
                statusMessage = "Iniciando Quick Connect..."
            )

            val api = NetworkClientFactory.createService(cleanUrl, JellyfinApiService::class.java)
            val authHeader = "MediaBrowser Client=\"Tujelly\", Device=\"AndroidTV\", DeviceId=\"TujellyApp\", Version=\"1.0.0\""

            try {
                val initResult = api.initiateQuickConnect(authHeader = authHeader)

                val webQuickConnectUrl = "$cleanUrl/web/index.html#!/quickconnect.html"
                val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" + URLEncoder.encode(webQuickConnectUrl, "UTF-8")

                _uiState.value = _uiState.value.copy(
                    jellyfinQuickConnectCode = initResult.effectiveCode,
                    jellyfinQrUrl = qrUrl,
                    statusMessage = "Código Quick Connect: ${initResult.effectiveCode}"
                )

                for (i in 0 until 180) {
                    delay(3000L)
                    try {
                        val check = api.checkQuickConnect(authHeader = authHeader, secret = initResult.effectiveSecret)
                        if (check.isAuthed && !check.effectiveToken.isNullOrBlank()) {
                            val token = check.effectiveToken!!
                            val authedHeader = "$authHeader, Token=\"$token\""
                            val me = api.getCurrentUser(authedHeader)

                            // 1. Guardar credenciales inmediatamente en DataStore
                            userPreferencesRepository.updateJellyfinConfig(
                                serverUrl = cleanUrl,
                                userId = me.effectiveId,
                                accessToken = token,
                                username = me.effectiveName
                            )

                            _uiState.value = _uiState.value.copy(
                                isWaitingJellyfinQuickConnect = false,
                                username = me.effectiveName,
                                isJellyfinConnected = true,
                                statusMessage = "Autorizado como ${me.effectiveName}. Sincronizando catálogo..."
                            )

                            val database = com.example.tujelly.data.local.db.AppDatabase.getDatabase(getApplication())
                            val repository = com.example.tujelly.data.repository.MediaRepository(database.jellyfinDao(), userPreferencesRepository)
                            // 2. Sincronizar catálogo en segundo plano (no se cancela al salir de la pantalla)
                            repository.startBackgroundSync(
                                serverUrl = cleanUrl,
                                userId = me.effectiveId,
                                token = token,
                                forceFullSync = true
                            )
                            val count = repository.getLocalCount()

                            _uiState.value = _uiState.value.copy(
                                isSuccess = true,
                                isLoading = false,
                                statusMessage = "Sincronización iniciada en segundo plano ($count ítems en caché)"
                            )
                            break
                        }
                    } catch (_: Exception) {
                        // Esperando autorización
                    }
                }
            } catch (e: Exception) {
                val details = formatNetworkError(e, cleanUrl)
                _uiState.value = _uiState.value.copy(
                    isWaitingJellyfinQuickConnect = false,
                    statusMessage = "Error: $details"
                )
            }
        }
    }

    fun saveAndConnect() {
        viewModelScope.launch {
            val state = _uiState.value
            val cleanUrl = NetworkClientFactory.normalizeUrl(state.serverUrl)
            _uiState.value = state.copy(serverUrl = cleanUrl)

            val currentPrefs = userPreferencesRepository.userPreferencesFlow.first()
            val effectiveUsername = state.username.ifBlank { currentPrefs.jellyfinUsername }.trim()
            val effectivePassword = state.password.ifBlank { currentPrefs.jellyfinPassword }

            if (cleanUrl.isBlank() || effectiveUsername.isBlank()) {
                _uiState.value = state.copy(statusMessage = "Escribe la URL del servidor y tu usuario")
                return@launch
            }

            _uiState.value = state.copy(isLoading = true, statusMessage = "Conectando con $cleanUrl...")

            val api = NetworkClientFactory.createService(cleanUrl, JellyfinApiService::class.java)
            val authHeader = "MediaBrowser Client=\"Tujelly\", Device=\"AndroidTV\", DeviceId=\"TujellyApp\", Version=\"1.0.0\""

            try {
                val info = api.getPublicInfo()
                val serverName = info.effectiveServerName
                _uiState.value = state.copy(isLoading = true, statusMessage = "Servidor: $serverName. Autenticando...")
            } catch (_: Exception) {
                _uiState.value = state.copy(isLoading = true, statusMessage = "Autenticando '$effectiveUsername'...")
            }

            try {
                val response = api.authenticateByName(
                    authHeader = authHeader,
                    request = JellyfinAuthRequest(username = effectiveUsername, pw = effectivePassword)
                )

                val userId = response.effectiveUser.effectiveId
                val token = response.effectiveToken
                val authedUser = response.effectiveUser.effectiveName.ifBlank { effectiveUsername }

                // 1. Guardar credenciales inmediatamente en DataStore
                userPreferencesRepository.updateJellyfinConfig(
                    serverUrl = cleanUrl,
                    userId = userId,
                    accessToken = token,
                    username = authedUser,
                    password = effectivePassword
                )
                userPreferencesRepository.updateTraktConfig(state.traktToken, state.traktClientId, state.traktClientSecret)
                userPreferencesRepository.updateTmdbConfig(state.tmdbApiKey, state.watchRegion)

                val database = com.example.tujelly.data.local.db.AppDatabase.getDatabase(getApplication())
                val repository = com.example.tujelly.data.repository.MediaRepository(database.jellyfinDao(), userPreferencesRepository)

                // 2. Sincronizar catálogo en segundo plano en syncScope (no se cancela al salir de Ajustes)
                repository.startBackgroundSync(
                    serverUrl = cleanUrl,
                    userId = userId,
                    token = token,
                    forceFullSync = true
                )

                val count = repository.getLocalCount()
                _uiState.value = state.copy(
                    isLoading = false,
                    isSuccess = true,
                    username = authedUser,
                    isJellyfinConnected = true,
                    statusMessage = "Conectado como $authedUser. Sincronizando catálogo..."
                )
            } catch (e: Exception) {
                val details = formatNetworkError(e, cleanUrl)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Error: $details"
                )
            }
        }
    }

    fun forceSyncCatalog() {
        viewModelScope.launch {
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            if (prefs.jellyfinServerUrl.isBlank() || prefs.jellyfinUserId.isBlank() || prefs.jellyfinAccessToken.isBlank()) {
                _uiState.value = _uiState.value.copy(statusMessage = "Conecta primero con Jellyfin")
                return@launch
            }
            val database = com.example.tujelly.data.local.db.AppDatabase.getDatabase(getApplication())
            val repository = com.example.tujelly.data.repository.MediaRepository(database.jellyfinDao(), userPreferencesRepository)
            repository.startBackgroundSync(
                serverUrl = prefs.jellyfinServerUrl,
                userId = prefs.jellyfinUserId,
                token = prefs.jellyfinAccessToken,
                forceFullSync = true
            )
            _uiState.value = _uiState.value.copy(statusMessage = "Sincronización completa iniciada en segundo plano...")
        }
    }

    fun checkForAppUpdates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingUpdate = true, statusMessage = "Buscando actualizaciones en GitHub...")
            val result = appUpdateManager.checkForUpdates()
            result.onSuccess { info ->
                _uiState.value = _uiState.value.copy(
                    isCheckingUpdate = false,
                    updateInfo = info,
                    statusMessage = if (info.hasUpdate) "¡Nueva versión ${info.latestVersion} disponible!" else "Tujelly está al día (${info.currentVersion})"
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isCheckingUpdate = false,
                    statusMessage = "No se pudo comprobar: ${e.localizedMessage ?: "Error de conexión"}"
                )
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val apkUrl = _uiState.value.updateInfo?.apkUrl ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(statusMessage = "Descargando actualización de GitHub...")
            val result = appUpdateManager.downloadAndInstall(apkUrl) { progress ->
                _uiState.value = _uiState.value.copy(downloadProgress = progress)
            }
            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    downloadProgress = null,
                    statusMessage = "Error al descargar: ${e.localizedMessage}"
                )
            }
        }
    }
}
