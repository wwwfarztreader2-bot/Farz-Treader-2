package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserRole
import com.example.data.repository.FarnadRepository
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data object LoggedOut : SessionUiState
    data class LoggedIn(val role: UserRole) : SessionUiState
}

data class LoginFormState(
    val selectedRole: UserRole? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(private val repository: FarnadRepository) : ViewModel() {

    val sessionState: StateFlow<SessionUiState> = repository.sessionRoleFlow
        .onStart { repository.ensureInitialized() }
        .map { role ->
            if (role == null) SessionUiState.LoggedOut else SessionUiState.LoggedIn(role)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SessionUiState.Loading
        )

    val themeMode: StateFlow<ThemeMode> = repository.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ThemeMode.SYSTEM
        )

    private val _loginState = MutableStateFlow(LoginFormState())
    val loginState: StateFlow<LoginFormState> = _loginState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun toggleDarkMode(isCurrentlyDark: Boolean) {
        setThemeMode(if (isCurrentlyDark) ThemeMode.LIGHT else ThemeMode.DARK)
    }

    fun selectRole(role: UserRole?) {
        _loginState.value = LoginFormState(selectedRole = role)
    }

    fun clearLoginError() {
        _loginState.value = _loginState.value.copy(errorMessage = null)
    }

    fun attemptLogin(role: UserRole, password: String) {
        if (password.isBlank()) {
            _loginState.value = _loginState.value.copy(errorMessage = "Masukkan kata sandi terlebih dahulu")
            return
        }
        viewModelScope.launch {
            _loginState.value = _loginState.value.copy(isLoading = true, errorMessage = null)
            val ok = repository.login(role, password)
            if (ok) {
                _loginState.value = LoginFormState()
            } else {
                _loginState.value = _loginState.value.copy(
                    isLoading = false,
                    errorMessage = "Kata sandi ${role.label} salah. Silakan periksa kembali."
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _loginState.value = LoginFormState()
        }
    }

    class Factory(private val repository: FarnadRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
