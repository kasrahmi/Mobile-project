package com.example.notable.view.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notable.view.screen.EmptyHomeScreen
import com.example.notable.view.screen.HomeWithNotesScreen
import com.example.notable.view.viewmodel.NotesViewModel

// This is what your HomeScreenWrapper should look like:
@Composable
fun HomeScreenWrapper(
    onNavigateToNote: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAddNote: () -> Unit
) {
    val notesViewModel: NotesViewModel = hiltViewModel()
    val uiState by notesViewModel.uiState.collectAsState()
    val searchQuery by notesViewModel.searchQuery.collectAsState()

    HomeWithNotesScreen(
        notes = uiState.notes,
        searchQuery = searchQuery,
        isSearchActive = uiState.isSearchActive,
        hasSearchResults = uiState.hasSearchResults,
        onSearchChange = { query -> notesViewModel.updateSearchQuery(query) },
        onClearSearch = { notesViewModel.clearSearch() },
        onNoteClick = onNavigateToNote,
        onAddNoteClick = onNavigateToAddNote,
        onHomeClick = { /* Already on home */ },
        onSettingsClick = onNavigateToSettings
    )
}

