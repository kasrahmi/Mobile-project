package com.example.notable.model.repository

import com.example.notable.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    suspend fun getAllNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: Int): Note?
    suspend fun createNote(title: String, description: String): Result<Note>
    suspend fun updateNote(id: Int, title: String, description: String): Result<Note>
    suspend fun deleteNote(id: Int): Result<Unit>
    suspend fun searchNotes(query: String): Flow<List<Note>>
    suspend fun syncNotes()
}
