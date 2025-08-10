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

    private val noteId: String? = savedStateHandle.get<String>("noteId")

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()

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
            } else {
                viewModelScope.launch {
                    val note = noteRepository.getNoteById(id.toInt())
                    note?.let {
                        _uiState.value = _uiState.value.copy(
                            title = it.title,
                            content = it.description,
                            lastEdited = formatLastEdited(it.updatedAt),
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(title = newTitle)
        autoSave()
    }

    fun updateContent(newContent: String) {
        _uiState.value = _uiState.value.copy(content = newContent)
        autoSave()
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
            delay(1000) // Auto-save after 1 second of no changes
            saveNote()
        }
    }

    fun saveNote() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.isNewNote) {
                noteRepository.createNote(state.title, state.content).fold(
                    onSuccess = { note ->
                        _uiState.value = _uiState.value.copy(
                            isNewNote = false,
                            lastEdited = formatLastEdited(note.updatedAt)
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
                )
            } else {
                noteId?.let { id ->
                    noteRepository.updateNote(id.toInt(), state.title, state.content).fold(
                        onSuccess = { note ->
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

    private fun formatLastEdited(timestamp: String): String {
        // Format timestamp to "Last edited on 19.30" format
        return "Last edited on ${timestamp.substring(11, 16)}"
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
