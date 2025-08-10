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
        // Always return local data first, then sync in background
        syncNotesIfNeeded()
        return noteDao.getAllNotes().map { entities ->
            entities.map { it.toDomain() }
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
                if (networkManager.isNetworkAvailable()) {
                    // Try to create on server first
                    val token = tokenManager.getAccessToken()
                        ?: return@withContext Result.failure(Exception("No access token"))

                    val request = CreateNoteRequest(title, description)
                    val response = api.createNote("Bearer $token", request)

                    if (response.isSuccessful) {
                        response.body()?.let { noteResponse ->
                            val note = noteResponse.toDomain()
                            // Save to local database
                            noteDao.insertNote(note.toEntity())
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
                    // Create locally when offline
                    val localNote = Note(
                        id = 0, // Will be assigned by Room
                        title = title,
                        description = description,
                        createdAt = System.currentTimeMillis().toString(),
                        updatedAt = System.currentTimeMillis().toString()
                    )
                    val id = noteDao.insertNote(localNote.toEntity())
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
                if (networkManager.isNetworkAvailable()) {
                    val token = tokenManager.getAccessToken()
                        ?: return@withContext Result.failure(Exception("No access token"))

                    val request = UpdateNoteRequest(title, description)
                    val response = api.updateNote("Bearer $token", id, request)

                    if (response.isSuccessful) {
                        response.body()?.let { noteResponse ->
                            val note = noteResponse.toDomain()
                            // Update local database
                            noteDao.updateNote(note.toEntity())
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
                    // Update locally when offline
                    val existingNote = noteDao.getNoteById(id)
                        ?: return@withContext Result.failure(Exception("Note not found"))

                    val updatedNote = existingNote.copy(
                        title = title,
                        description = description,
                        updatedAt = System.currentTimeMillis().toString()
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
                        // Delete from local database
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
                    // Delete locally when offline
                    noteDao.deleteNoteById(id)
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes("%$query%").map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncNotes() {
        if (!networkManager.isNetworkAvailable()) return

        withContext(Dispatchers.IO) {
            try {
                val token = tokenManager.getAccessToken() ?: return@withContext

                var page = 1
                var hasMore = true

                while (hasMore) {
                    val response = api.getNotes("Bearer $token", page)

                    if (response.isSuccessful) {
                        response.body()?.let { notesResponse ->
                            // Save notes to local database
                            val noteEntities = notesResponse.results.map { it.toEntity() }
                            noteDao.insertNotes(noteEntities)

                            hasMore = notesResponse.next != null
                            page++
                        } ?: run { hasMore = false }
                    } else {
                        if (response.code() == 401) {
                            // For syncNotes, we need to handle the Unit return type
                            refreshTokenForSync()
                            return@withContext
                        } else {
                            hasMore = false
                        }
                    }
                }
            } catch (e: Exception) {
                // Sync failed, but continue with local data
            }
        }
    }

    private suspend fun syncNotesIfNeeded() {
        // Only sync if we have network and haven't synced recently
        if (networkManager.isNetworkAvailable()) {
            try {
                syncNotes()
            } catch (e: Exception) {
                // Ignore sync errors
            }
        }
    }

    private suspend fun <T> refreshTokenAndRetry(operation: suspend () -> Result<T>): Result<T> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
                ?: return Result.failure(Exception("No refresh token"))

            val response = api.refreshToken(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful) {
                response.body()?.let { accessTokenResponse ->
                    val currentRefreshToken = tokenManager.getRefreshToken() ?: ""
                    tokenManager.saveTokens(accessTokenResponse.access, currentRefreshToken)
                    operation()
                } ?: Result.failure(Exception("Failed to refresh token"))
            } else {
                // Refresh token is invalid
                tokenManager.clearTokens()
                Result.failure(Exception("Session expired"))
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            Result.failure(e)
        }
    }

    private suspend fun refreshTokenForSync() {
        try {
            val refreshToken = tokenManager.getRefreshToken() ?: return

            val response = api.refreshToken(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful) {
                response.body()?.let { accessTokenResponse ->
                    val currentRefreshToken = tokenManager.getRefreshToken() ?: ""
                    tokenManager.saveTokens(accessTokenResponse.access, currentRefreshToken)
                    // Retry sync after refreshing token
                    syncNotes()
                }
            } else {
                // Refresh token is invalid
                tokenManager.clearTokens()
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
        }
    }
}
