package com.example.notable.view.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notable.model.repository.AuthRepository
import com.example.notable.model.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // TODO
    private var noteId: String? = savedStateHandle.get<String>("noteId")

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()

    // Track original content to detect changes
    private var originalTitle: String = ""
    private var originalContent: String = ""
    private var hasUnsavedChanges: Boolean = false

    init {
        loadNote()
    }

    private fun loadNote() {
        noteId?.let { id ->
            if (id == "new") {
                _uiState.value = _uiState.value.copy(
                    isEditing = true,
                    isNewNote = true
                )
                // For new notes, original values are empty
                originalTitle = ""
                originalContent = ""
            } else {
                viewModelScope.launch {
                    val note = noteRepository.getNoteById(id.toInt())
                    note?.let {
                        // Store original values
                        originalTitle = it.title
                        originalContent = it.description

                        _uiState.value = _uiState.value.copy(
                            title = it.title,
                            content = it.description,
                            lastEdited = formatLastEdited(it.updatedAt),
                            isLoading = false,
                            isEditing = true
                        )
                    }
                }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(title = newTitle)
        checkForChanges()
        if (hasUnsavedChanges) {
            autoSave()
        }
    }

    fun updateContent(newContent: String) {
        _uiState.value = _uiState.value.copy(content = newContent)
        checkForChanges()
        if (hasUnsavedChanges) {
            autoSave()
        }
    }

    private fun checkForChanges() {
        val currentState = _uiState.value
        hasUnsavedChanges = currentState.title != originalTitle ||
                currentState.content != originalContent
    }

    fun toggleEditMode() {
        _uiState.value = _uiState.value.copy(isEditing = !_uiState.value.isEditing)
    }

    fun showDeleteDialog() {
        _showDeleteDialog.value = true
    }

    fun hideDeleteDialog() {
        _showDeleteDialog.value = false
    }

    fun deleteNote() {
        viewModelScope.launch {
            noteId?.let { id ->
                if (id != "new") {
                    noteRepository.deleteNote(id.toInt()).fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(noteDeleted = true)
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(error = error.message)
                        }
                    )
                }
            }
        }
    }

    private fun autoSave() {
        viewModelScope.launch {
            delay(50) // Auto-save after 50ms of no changes
            if (hasUnsavedChanges) { // Only save if there are actual changes
                saveNote()
            }
        }
    }

    fun saveNote() {
        // Only save if there are actual changes or it's a new note
        if (!hasUnsavedChanges && !_uiState.value.isNewNote) {
            return
        }

        viewModelScope.launch {
            val state = _uiState.value
            if (state.isNewNote) {
                // Always save new notes
                noteRepository.createNote(state.title, state.content).fold(
                    onSuccess = { note ->
                        // Update original values after successful save
                        originalTitle = state.title
                        originalContent = state.content
                        hasUnsavedChanges = false

                        _uiState.value = _uiState.value.copy(
                            isNewNote = false,
                            lastEdited = formatLastEdited(note.updatedAt),
                            isEditing = true
                        )
                        savedStateHandle["noteId"] = note.id.toString()
                        noteId = note.id.toString()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
                )
            } else {
                // Only update existing notes if there are changes
                noteId?.let { id ->
                    noteRepository.updateNote(id.toInt(), state.title, state.content).fold(
                        onSuccess = { note ->
                            // Update original values after successful save
                            originalTitle = state.title
                            originalContent = state.content
                            hasUnsavedChanges = false

                            _uiState.value = _uiState.value.copy(
                                lastEdited = formatLastEdited(note.updatedAt)
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(error = error.message)
                        }
                    )
                }
            }
        }
    }

    // Call this when navigating back to save only if needed
    fun saveOnExit() {
        if (hasUnsavedChanges || _uiState.value.isNewNote) {
            saveNote()
        }
    }

    // Public method to check if there are unsaved changes
    fun hasUnsavedChanges(): Boolean = hasUnsavedChanges

    private fun formatLastEdited(timestamp: String): String {
        return try {
            val instant = java.time.Instant.parse(timestamp)

            // Convert to local time
            val localDateTime = java.time.LocalDateTime.ofInstant(
                instant,
                java.time.ZoneId.systemDefault()
            )

            // Format as "Last edited on Sep 12, 2025 at 14:30"
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm")
            "Last edited on ${localDateTime.format(formatter)}"

        } catch (e: Exception) {
            // Fallback for invalid timestamps
            "Last edited on $timestamp"
        }
    }
}

data class NoteDetailUiState(
    val title: String = "",
    val content: String = "",
    val lastEdited: String = "",
    val isEditing: Boolean = false,
    val isNewNote: Boolean = false,
    val isLoading: Boolean = true,
    val noteDeleted: Boolean = false,
    val error: String? = null
)
