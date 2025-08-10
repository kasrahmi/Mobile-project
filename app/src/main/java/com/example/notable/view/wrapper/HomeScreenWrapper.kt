package com.example.notable.view.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notable.view.screen.EmptyHomeScreen
import com.example.notable.view.screen.HomeWithNotesScreen
import com.example.notable.view.viewmodel.NotesViewModel

@Composable
fun HomeScreenWrapper(
    onNavigateToNote: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAddNote: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.syncNotes()
    }

    if (uiState.notes.isEmpty() && !uiState.isLoading) {
        EmptyHomeScreen(
            onAddNoteClick = onNavigateToAddNote,
            onHomeClick = { /* Already on home */ },
            onSettingsClick = onNavigateToSettings
        )
    } else {
        HomeWithNotesScreen(
            notes = uiState.notes,
            searchQuery = searchQuery,
            onSearchChange = viewModel::updateSearchQuery,
            onNoteClick = onNavigateToNote,
            onAddNoteClick = onNavigateToAddNote,
            onHomeClick = { /* Already on home */ },
            onSettingsClick = onNavigateToSettings
        )
    }
}
