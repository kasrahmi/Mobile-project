package com.example.notable.view.wrapper

import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.notable.view.screen.LoginScreen
import com.example.notable.view.viewmodel.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreenWrapper(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onNavigateToHome()
        }
    }

    LoginScreen(
        onLogin = { username, password ->
            viewModel.login(username, password)
        },
        onRegisterClick = onNavigateToRegister
    )

    // Handle error states

    val context = LocalContext.current
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error toast or handle error display
            // For example, you can use a Snackbar or Toast to show the error
            // show in a toast
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }
}

