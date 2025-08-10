package com.example.notable.view.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notable.model.User
import com.example.notable.model.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            authRepository.getUserInfo().fold(
                onSuccess = { user: User ->
                    _uiState.value = _uiState.value.copy(
                        user = user,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error: Throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChangingPassword = true, error = null)

            authRepository.changePassword(currentPassword, newPassword).fold(
                onSuccess = { _: Unit ->
                    _uiState.value = _uiState.value.copy(
                        isChangingPassword = false,
                        passwordChanged = true,
                        error = null
                    )
                },
                onFailure = { error: Throwable ->
                    _uiState.value = _uiState.value.copy(
                        isChangingPassword = false,
                        error = error.message ?: "An error occurred while changing password"
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(isLoggedOut = true)
        }
    }

    fun showLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = true)
    }

    fun hideLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearPasswordChanged() {
        _uiState.value = _uiState.value.copy(passwordChanged = false)
    }
}

data class SettingsUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isChangingPassword: Boolean = false,
    val isLoggedOut: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val error: String? = null,
    val passwordChanged: Boolean = false,
    val appVersion: String = "Notable v1.1"
) {
    val userName: String
        get() = user?.let { "${it.firstName} ${it.lastName}" } ?: "User Name"

    val userEmail: String
        get() = user?.email ?: "user@example.com"
}
