package com.github.adriianh.melo.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.usecase.login.GetAccountProfilesUseCase
import com.github.adriianh.core.domain.usecase.login.SetActiveAccountProfileUseCase
import com.github.adriianh.core.domain.usecase.login.SetSessionCookiesUseCase
import com.github.adriianh.core.domain.usecase.login.VerifySessionUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoggedIn: Boolean = false,
    val isVerifying: Boolean = false,
    val isWaitingBrowserAuth: Boolean = false,
    val browserAuthStatus: String? = null,
    val accountName: String? = null,
    val profiles: List<AccountProfile> = emptyList(),
    val activePageId: String? = null,
    val isLoadingProfiles: Boolean = false,
    val isSwitchingProfile: Boolean = false,
    val profilesError: String? = null,
    val error: String? = null,
)

class LoginViewModel(
    private val getSettings: GetSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val setSessionCookies: SetSessionCookiesUseCase,
    private val verifySession: VerifySessionUseCase,
    private val getAccountProfiles: GetAccountProfilesUseCase,
    private val setActiveAccountProfile: SetActiveAccountProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = getSettings.getSnapshot()
            val cookies = settings.sessionCookies
            val loggedIn = !cookies.isNullOrBlank()
            _uiState.update { it.copy(activePageId = settings.accountPageId) }
            if (loggedIn) {
                setSessionCookies(cookies)
                setActiveAccountProfile(settings.accountPageId)
                _uiState.update { it.copy(isLoggedIn = true) }
            }
        }
    }

    fun saveSessionCookies(cookies: String) {
        val trimmed = cookies.trim()
        if (trimmed.isBlank() || _uiState.value.isVerifying) return

        _uiState.update { it.copy(isVerifying = true, error = null) }
        viewModelScope.launch {
            try {
                setSessionCookies(trimmed)
                val accountName = verifySession()
                if (accountName == null) {
                    setSessionCookies(null)
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            isLoggedIn = false,
                            error = "Las cookies no pudieron autenticar la sesión. Asegúrate de tener una sesión activa en music.youtube.com o inicia sesión con el navegador.",
                        )
                    }
                } else {
                    updateSettings { settings ->
                        settings.copy(
                            sessionCookies = trimmed,
                            accountPageId = null,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            isLoggedIn = true,
                            accountName = accountName,
                            activePageId = null,
                            profiles = emptyList(),
                            profilesError = null,
                            error = null,
                        )
                    }
                    loadProfiles()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setSessionCookies(null)
                _uiState.update {
                    it.copy(
                        isVerifying = false,
                        isLoggedIn = false,
                        error = e.message ?: "Verification failed",
                    )
                }
            }
        }
    }

    fun startAutomatedBrowserLogin() {
        if (_uiState.value.isWaitingBrowserAuth) return
        _uiState.update {
            it.copy(
                isWaitingBrowserAuth = true,
                browserAuthStatus = "Abriendo ventana de inicio de sesión seguro...",
            )
        }
        viewModelScope.launch {
            val captured = launchAutomatedBrowserLogin()
            if (captured.isNullOrBlank()) {
                _uiState.update { it.copy(error = BROWSER_LOGIN_FAILED) }
            }
            finishBrowserAuth(captured)
        }
    }

    fun startQuickImport() {
        if (_uiState.value.isWaitingBrowserAuth) return
        _uiState.update {
            it.copy(
                isWaitingBrowserAuth = true,
                browserAuthStatus =
                    "Buscando sesión en navegadores locales (Chrome, Edge, Brave, Firefox)...",
            )
        }
        viewModelScope.launch {
            val captured = importExistingBrowserCookies()
            if (!captured.isNullOrBlank()) {
                finishBrowserAuth(captured)
            } else {
                _uiState.update { it.copy(browserAuthStatus = "Iniciando ventana de navegador...") }
                val autoCaptured = launchAutomatedBrowserLogin()
                if (autoCaptured.isNullOrBlank()) {
                    _uiState.update { it.copy(error = BROWSER_IMPORT_FAILED) }
                }
                finishBrowserAuth(autoCaptured)
            }
        }
    }

    fun cancelWaitingBrowserAuth() {
        _uiState.update { it.copy(isWaitingBrowserAuth = false, browserAuthStatus = null) }
    }

    private fun finishBrowserAuth(cookies: String?) {
        _uiState.update { it.copy(isWaitingBrowserAuth = false, browserAuthStatus = null) }
        if (!cookies.isNullOrBlank()) {
            saveSessionCookies(cookies)
        }
    }

    fun loadProfiles() {
        val state = _uiState.value
        if (!state.isLoggedIn || state.isLoadingProfiles) return

        _uiState.update { it.copy(isLoadingProfiles = true, profilesError = null) }
        viewModelScope.launch {
            getAccountProfiles()
                .onSuccess { profiles ->
                    _uiState.update {
                        it.copy(
                            profiles = profiles,
                            isLoadingProfiles = false,
                            profilesError = null,
                        )
                    }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingProfiles = false,
                            profilesError =
                                throwable.message
                                    ?: "No se pudieron cargar los canales de la cuenta.",
                        )
                    }
                }
        }
    }

    fun selectProfile(pageId: String?) {
        val state = _uiState.value
        if (!state.isLoggedIn || state.isSwitchingProfile || state.activePageId == pageId) return

        _uiState.update { it.copy(isSwitchingProfile = true, profilesError = null) }
        viewModelScope.launch {
            try {
                setActiveAccountProfile(pageId)
                val accountName = verifySession()
                _uiState.update {
                    it.copy(
                        activePageId = pageId,
                        accountName = accountName ?: it.accountName,
                        isSwitchingProfile = false,
                        profilesError =
                            if (accountName == null) {
                                "No se pudo activar el canal seleccionado."
                            } else {
                                null
                            },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSwitchingProfile = false,
                        profilesError = e.message ?: "No se pudo cambiar de canal.",
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            setSessionCookies(null)
            updateSettings { it.copy(sessionCookies = null, accountPageId = null) }
            _uiState.update {
                it.copy(
                    isLoggedIn = false,
                    accountName = null,
                    profiles = emptyList(),
                    activePageId = null,
                    isLoadingProfiles = false,
                    isSwitchingProfile = false,
                    profilesError = null,
                    error = null,
                )
            }
        }
    }

    private companion object {
        const val BROWSER_LOGIN_FAILED =
            "No se pudo iniciar sesión con el navegador. Prueba a importar la sesión de un navegador local " +
                "o a ingresar las cookies manualmente."
        const val BROWSER_IMPORT_FAILED =
            "No se encontró una sesión activa en tus navegadores. Inicia sesión en music.youtube.com " +
                "o ingresa las cookies manualmente."
    }
}
