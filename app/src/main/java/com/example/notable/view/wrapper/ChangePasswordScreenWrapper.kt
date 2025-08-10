package com.example.notable.view.wrapper

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.notable.view.viewmodel.SettingsViewModel
import com.example.notable.view.screen.ChangePasswordScreen

@Composable
fun ChangePasswordScreenWrapper(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var retypePassword by remember { mutableStateOf("") }

    // Navigate back when password is successfully changed
    LaunchedEffect(uiState.passwordChanged) {
        if (uiState.passwordChanged) {
            viewModel.clearPasswordChanged()
            navController.popBackStack()
        }
    }

    // Show error dialog if there's an error
    uiState.error?.let { error ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { androidx.compose.material3.Text("Error") },
            text = { androidx.compose.material3.Text(error) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.clearError() }
                ) {
                    androidx.compose.material3.Text("OK")
                }
            }
        )
    }

    ChangePasswordScreen(
        currentPassword = currentPassword,
        newPassword = newPassword,
        retypePassword = retypePassword,
        onBackClick = {
            navController.popBackStack()
        },
        onCurrentPasswordChange = { currentPassword = it },
        onNewPasswordChange = { newPassword = it },
        onRetypePasswordChange = { retypePassword = it },
        onSubmitClick = {
            if (newPassword == retypePassword && currentPassword.isNotEmpty()) {
                viewModel.changePassword(currentPassword, newPassword)
            }
        },
        isLoading = uiState.isChangingPassword
    )
}
