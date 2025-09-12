package com.example.notable.view.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notable.model.repository.AuthRepository
import com.example.notable.model.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var allNotes: List<com.example.notable.view.screen.NoteItem> = emptyList()

    init {
        syncNotes()
        loadNotes()
        observeSearchQuery()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            noteRepository.getAllNotes().collect { notes ->
                val noteItems = notes.map { note ->
                    com.example.notable.view.screen.NoteItem(
                        id = note.id.toString(),
                        title = note.title,
                        preview = note.description,
                        backgroundColor = getRandomNoteColor(note.id)
                    )
                }
                allNotes = noteItems

                // Only update displayed notes if not searching
                if (_searchQuery.value.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        notes = noteItems,
                        isLoading = false,
                        isSearchActive = false,
                        hasSearchResults = true
                    )
                } else {
                    // If we're searching, re-apply the search
                    performSearch(_searchQuery.value)
                }
            }
        }
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Debounce search queries
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            // Clear search - show all notes
            _uiState.value = _uiState.value.copy(
                notes = allNotes,
                isSearchActive = false,
                hasSearchResults = true
            )
        } else {
            // Active search
            val filteredNotes = allNotes.filter { note ->
                note.title.contains(query, ignoreCase = true) ||
                        note.preview.contains(query, ignoreCase = true)
            }

            _uiState.value = _uiState.value.copy(
                notes = filteredNotes,
                isSearchActive = true,
                hasSearchResults = filteredNotes.isNotEmpty()
            )
        }
    }

    // MISSING METHOD 1: Update search query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // MISSING METHOD 2: Clear search
    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun syncNotes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            noteRepository.syncNotes()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun getRandomNoteColor(noteId: Int): androidx.compose.ui.graphics.Color {
        val colors = listOf(
            Color(0xFFFFF9C4), // Light Yellow
            Color(0xFFFFE0B2), // Light Orange
            Color(0xFFE1F5FE), // Light Blue
            Color(0xFFF3E5F5), // Light Purple
            Color(0xFFE8F5E8), // Light Green
        )
        return if (noteId != -1) {
            colors[noteId.mod(colors.size)]
        } else {
            Color.Gray
        }
    }
}

data class NotesUiState(
    val notes: List<com.example.notable.view.screen.NoteItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSearchActive: Boolean = false,
    val hasSearchResults: Boolean = true,
    val noteColor: Color = Color.White, // Add this
)
