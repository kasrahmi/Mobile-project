package com.example.notable.view.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.notable.view.wrapper.ChangePasswordScreenWrapper
import com.example.notable.view.wrapper.HomeScreenWrapper
import com.example.notable.view.wrapper.LoginScreenWrapper
import com.example.notable.view.wrapper.NoteScreenWrapper
import com.example.notable.view.wrapper.OnboardingScreenWrapper
import com.example.notable.view.wrapper.RegisterScreenWrapper
import com.example.notable.view.wrapper.SettingsScreenWrapper

@Composable
fun NotableNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = "onboarding"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreenWrapper(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreenWrapper(
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("register") {
            RegisterScreenWrapper(
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("home") {
            HomeScreenWrapper(
                onNavigateToNote = { noteId ->
                    navController.navigate("note/$noteId")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToAddNote = {
                    navController.navigate("note/new")
                }
            )
        }

        composable(
            "note/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            NoteScreenWrapper(
                noteId = noteId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("settings") {
            SettingsScreenWrapper(
                navController = navController
            )
        }

        composable("change_password") {
            ChangePasswordScreenWrapper(
                navController = navController
            )
        }
    }
}
