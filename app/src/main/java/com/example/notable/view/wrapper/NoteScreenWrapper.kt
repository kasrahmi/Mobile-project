package com.example.notable.view.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notable.view.screen.DeleteDialog
import com.example.notable.view.screen.NoteEditingScreen
import com.example.notable.view.screen.NoteViewScreen
import com.example.notable.view.viewmodel.NoteDetailViewModel

@Composable
fun NoteScreenWrapper(
    noteId: String?,
    onNavigateBack: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()

) {
    val uiState by viewModel.uiState.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    LaunchedEffect(uiState.noteDeleted) {
        if (uiState.noteDeleted) {
            onNavigateBack()
        }
    }

    if (showDeleteDialog) {
        DeleteDialog(
            title = uiState.title,
            content = uiState.content,
            lastEdited = uiState.lastEdited,
            showDeleteDialog = showDeleteDialog,
            onBackClick = onNavigateBack,
            onDeleteConfirm = {
                viewModel.deleteNote()
                viewModel.hideDeleteDialog()
            },
            onDeleteCancel = {
                viewModel.hideDeleteDialog()
            }
        )
    } else {
        if (uiState.isEditing || uiState.isNewNote) {
            NoteEditingScreen(
                title = uiState.title,
                content = uiState.content,
                lastEdited = uiState.lastEdited,
                onBackClick = {
                    viewModel.saveNote()
                    onNavigateBack()
                },
                onTitleChange = viewModel::updateTitle,
                onContentChange = viewModel::updateContent,
                onDeleteClick = {
                    if (!uiState.isNewNote) {
                        viewModel.showDeleteDialog()
                    }
                }
            )
        } else {
            NoteViewScreen(
                title = uiState.title,
                content = uiState.content,
                lastEdited = uiState.lastEdited,
                onBackClick = onNavigateBack,
                onDeleteClick = viewModel::showDeleteDialog
            )
        }
    }
}
