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

    init {
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
                        backgroundColor = getRandomNoteColor()
                    )
                }
                _uiState.value = _uiState.value.copy(
                    notes = noteItems,
                    isLoading = false
                )
            }
        }
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Debounce search queries
                .collect { query ->
                    if (query.isBlank()) {
                        loadNotes()
                    } else {
                        searchNotes(query)
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun searchNotes(query: String) {
        viewModelScope.launch {
            noteRepository.searchNotes(query).collect { notes ->
                val noteItems = notes.map { note ->
                    com.example.notable.view.screen.NoteItem(
                        id = note.id.toString(),
                        title = note.title,
                        preview = note.description,
                        backgroundColor = getRandomNoteColor()
                    )
                }
                _uiState.value = _uiState.value.copy(notes = noteItems)
            }
        }
    }

    fun syncNotes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            noteRepository.syncNotes()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun getRandomNoteColor(): androidx.compose.ui.graphics.Color {
        val colors = listOf(
            Color(0xFFFFF9C4), // Light Yellow
            Color(0xFFFFE0B2), // Light Orange
            Color(0xFFE1F5FE), // Light Blue
            Color(0xFFF3E5F5), // Light Purple
            Color(0xFFE8F5E8), // Light Green
        )
        return colors.random()
    }
}

data class NotesUiState(
    val notes: List<com.example.notable.view.screen.NoteItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
