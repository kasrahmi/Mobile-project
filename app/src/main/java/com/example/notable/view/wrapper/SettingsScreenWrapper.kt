package com.example.notable.view.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.notable.view.viewmodel.SettingsViewModel
import com.example.notable.view.screen.SettingsScreen

@Composable
fun SettingsScreenWrapper(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate to login when logged out
    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            navController.navigate("login") {
                popUpTo("main") { inclusive = true }
            }
        }
    }

    // Show logout dialog when needed
    if (uiState.showLogoutDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.hideLogoutDialog() },
            title = { androidx.compose.material3.Text("Confirm Logout") },
            text = { androidx.compose.material3.Text("Are you sure you want to logout?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.hideLogoutDialog()
                        viewModel.logout()
                    }
                ) {
                    androidx.compose.material3.Text("Logout")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.hideLogoutDialog() }
                ) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }

    SettingsScreen(
        userName = uiState.userName,
        userEmail = uiState.userEmail,
        appVersion = uiState.appVersion,
        profileImageUrl = null, // You can add this to your User model if needed
        onBackClick = {
            navController.popBackStack()
        },
        onChangePasswordClick = {
            navController.navigate("change_password")
        },
        onLogOutClick = {
            viewModel.showLogoutDialog()
        }
    )
}
