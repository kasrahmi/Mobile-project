package com.example.notable.view.wrapper

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notable.view.screen.RegisterScreen
import com.example.notable.view.viewmodel.AuthViewModel

@Composable
fun RegisterScreenWrapper(
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.registrationSuccess) {
        if (uiState.registrationSuccess) {
            onNavigateToLogin()
        }
    }

    RegisterScreen(
        onBackToLogin = onNavigateToLogin,
        onRegister = { username, password, email, firstName, lastName ->
            viewModel.register(username, password, email, firstName, lastName)
        },
        onLoginClick = onNavigateToLogin
    )

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
