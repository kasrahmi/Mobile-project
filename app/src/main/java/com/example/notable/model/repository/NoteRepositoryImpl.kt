package com.example.notable.model.repository

import com.example.notable.data.database.NoteDao
import com.example.notable.data.TokenManager
import com.example.notable.data.mapper.*
import com.example.notable.data.NetworkManager
import com.example.notable.data.NotableApi
import com.example.notable.data.dto.CreateNoteRequest
import com.example.notable.data.dto.RefreshTokenRequest
import com.example.notable.data.dto.UpdateNoteRequest
import com.example.notable.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val api: NotableApi,
    private val noteDao: NoteDao,
    private val tokenManager: TokenManager,
    private val networkManager: NetworkManager
) : NoteRepository {

    override suspend fun getAllNotes(): Flow<List<Note>> {
        syncNotesIfNeeded()
        val userId = tokenManager.getCurrentUserId()
        return if (userId != null) {
            noteDao.getNotesByUser(userId).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            noteDao.getAllNotes().map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override suspend fun getNoteById(id: Int): Note? {
        return withContext(Dispatchers.IO) {
            noteDao.getNoteById(id)?.toDomain()
        }
    }

    override suspend fun createNote(title: String, description: String): Result<Note> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = tokenManager.getCurrentUserId()
                    ?: return@withContext Result.failure(Exception("User not authenticated"))

                if (networkManager.isNetworkAvailable()) {
                    val token = tokenManager.getAccessToken()
                        ?: return@withContext Result.failure(Exception("No access token"))

                    val request = CreateNoteRequest(title, description)
                    val response = api.createNote("Bearer $token", request)

                    if (response.isSuccessful) {
                        response.body()?.let { noteResponse ->
                            val note = noteResponse.toDomain()
                            noteDao.insertNote(note.toEntity(userId))
                            Result.success(note)
                        } ?: Result.failure(Exception("Empty response body"))
                    } else {
                        if (response.code() == 401) {
                            refreshTokenAndRetry { createNote(title, description) }
                        } else {
                            Result.failure(Exception("Failed to create note: ${response.message()}"))
                        }
                    }
                } else {
                    val localNote = Note(
                        id = 0,
                        title = title,
                        description = description,
                        createdAt = System.currentTimeMillis().toString(),
                        updatedAt = System.currentTimeMillis().toString()
                    )
                    val id = noteDao.insertNote(localNote.toEntity(userId))
                    val createdNote = localNote.copy(id = id.toInt())
                    Result.success(createdNote)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateNote(id: Int, title: String, description: String): Result<Note> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = tokenManager.getCurrentUserId()
                    ?: return@withContext Result.failure(Exception("User not authenticated"))

                if (networkManager.isNetworkAvailable()) {
                    val token = tokenManager.getAccessToken()
                        ?: return@withContext Result.failure(Exception("No access token"))

                    val request = UpdateNoteRequest(title, description)
                    val response = api.updateNote("Bearer $token", id, request)

                    if (response.isSuccessful) {
                        response.body()?.let { noteResponse ->
                            val note = noteResponse.toDomain()
                            noteDao.updateNote(note.toEntity(userId))
                            Result.success(note)
                        } ?: Result.failure(Exception("Empty response body"))
                    } else {
                        if (response.code() == 401) {
                            refreshTokenAndRetry { updateNote(id, title, description) }
                        } else {
                            Result.failure(Exception("Failed to update note: ${response.message()}"))
                        }
                    }
                } else {
                    val existingNote = noteDao.getNoteById(id)
                        ?: return@withContext Result.failure(Exception("Note not found"))

                    val updatedNote = existingNote.copy(
                        title = title,
                        description = description,
                        updatedAt = System.currentTimeMillis().toString(),
                        needsSync = true
                    )
                    noteDao.updateNote(updatedNote)
                    Result.success(updatedNote.toDomain())
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteNote(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (networkManager.isNetworkAvailable()) {
                    val token = tokenManager.getAccessToken()
                        ?: return@withContext Result.failure(Exception("No access token"))

                    val response = api.deleteNote("Bearer $token", id)

                    if (response.isSuccessful) {
                        noteDao.deleteNoteById(id)
                        Result.success(Unit)
                    } else {
                        if (response.code() == 401) {
                            refreshTokenAndRetry { deleteNote(id) }
                        } else {
                            Result.failure(Exception("Failed to delete note: ${response.message()}"))
                        }
                    }
                } else {
                    noteDao.deleteNoteById(id)
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun searchNotes(query: String): Flow<List<Note>> {
        val userId = tokenManager.getCurrentUserId()
        return if (userId != null) {
            noteDao.searchNotesByUser(userId, query).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            noteDao.searchNotes(query).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override suspend fun syncNotes() {
        if (!networkManager.isNetworkAvailable()) return

        try {
            val token = tokenManager.getAccessToken() ?: return
            val userId = tokenManager.getCurrentUserId() ?: return

            val response = api.getUserNotes(
                "Bearer $token",
                page = 0
            )

            if (response.isSuccessful) {
                response.body()?.let { notesResponse ->
                    // Clear existing notes for this user and insert new ones
                    noteDao.deleteNotesByUser(userId)

                    notesResponse.results.forEach { noteDto ->
                        noteDao.insertNote(noteDto.toEntity(userId))
                    }
                }
            }
        } catch (e: Exception) {
            // Handle sync error
        }
    }

    private suspend fun syncNotesIfNeeded() {
        if (networkManager.isNetworkAvailable()) {
            syncNotes()
        }
    }

    private suspend fun <T> refreshTokenAndRetry(action: suspend () -> Result<T>): Result<T> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
                ?: return Result.failure(Exception("No refresh token"))

            val request = RefreshTokenRequest(refreshToken)
            val response = api.refreshToken(request)

            if (response.isSuccessful) {
                response.body()?.let { accessTokenResponse ->
                    val currentRefreshToken = tokenManager.getRefreshToken() ?: ""
                    tokenManager.saveTokens(accessTokenResponse.access, currentRefreshToken)
                    action()
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                tokenManager.clearTokens()
                Result.failure(Exception("Refresh token expired"))
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            Result.failure(e)
        }
    }
}
