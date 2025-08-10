package com.example.notable.view.wrapper

import androidx.compose.runtime.Composable
import com.example.notable.view.screen.OnboardingScreen

@Composable
fun OnboardingScreenWrapper(
    onNavigateToLogin: () -> Unit
) {
    OnboardingScreen(
        onGetStarted = onNavigateToLogin
    )
}
