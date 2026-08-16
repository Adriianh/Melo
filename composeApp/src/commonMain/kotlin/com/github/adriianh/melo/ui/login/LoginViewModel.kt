package com.github.adriianh.melo.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val accountName: String? = null,
    val error: String? = null,
)

class LoginViewModel(
    private val getSettings: GetSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val setSessionCookies: SetSessionCookiesUseCase,
    private val verifySession: VerifySessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cookies = getSettings.getSnapshot().sessionCookies
            val loggedIn = !cookies.isNullOrBlank()
            if (loggedIn) {
                setSessionCookies(cookies)
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
                            error = "Those cookies did not authenticate. Copy them from an active music.youtube.com session and try again.",
                        )
                    }
                } else {
                    updateSettings { settings ->
                        settings.copy(sessionCookies = trimmed)
                    }
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            isLoggedIn = true,
                            accountName = accountName,
                            error = null,
                        )
                    }
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

    fun logout() {
        viewModelScope.launch {
            setSessionCookies(null)
            updateSettings { it.copy(sessionCookies = null) }
            _uiState.update { it.copy(isLoggedIn = false, accountName = null, error = null) }
        }
    }
}