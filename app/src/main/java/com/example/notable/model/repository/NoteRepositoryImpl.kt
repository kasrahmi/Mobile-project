package com.example.notable.model.repository

import android.util.Log
import com.example.notable.data.database.NoteDao
import com.example.notable.data.database.NoteEntity
import com.example.notable.data.database.SyncStatus
import com.example.notable.data.TokenManager
import com.example.notable.data.mapper.*
import com.example.notable.data.NetworkManager
import com.example.notable.data.NotableApi
import com.example.notable.data.dto.CreateNoteRequest
import com.example.notable.data.dto.UpdateNoteRequest
import com.example.notable.data.sync.SyncManager
import com.example.notable.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val api: NotableApi,
    private val noteDao: NoteDao,
    private val tokenManager: TokenManager,
    private val networkManager: NetworkManager,
    private val syncManager: SyncManager
) : NoteRepository {

    override suspend fun getAllNotes(): Flow<List<Note>> {
        // Always return local data first for immediate UI response
        val userId = getCurrentUserId() ?: 0

        // Trigger background sync if online
        if (networkManager.isNetworkAvailable()) {
            syncInBackground()
        }

        return noteDao.getNotesByUser(userId).map { entities ->
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
                val userId = getCurrentUserId()
                    ?: return@withContext Result.failure(Exception("User not authenticated"))

                val currentTime = java.time.Instant.now().toString() // TODO

                if (networkManager.isNetworkAvailable()) {
                    // Try to create on server immediately
                    val serverResult = createNoteOnServer(title, description, userId, currentTime)
                    if (serverResult.isSuccess) {
                        return@withContext serverResult
                    }
                    // If server creation fails, fall back to offline creation
                }

                // Create offline with temporary local ID
                val localId = noteDao.getNextLocalId() ?: -1
                val localNote = createLocalNote(
                    id = localId,
                    title = title,
                    description = description,
                    userId = userId,
                    currentTime = currentTime,
                    syncStatus = SyncStatus.PENDING_CREATE
                )

                val insertedId = noteDao.insertNote(localNote)
                val createdNote = localNote.copy(id = insertedId.toInt()).toDomain()

                // Try to sync in background
                Log.d("mmoooo", SyncStatus.PENDING_CREATE.toString())
                syncInBackground()

                Result.success(createdNote)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateNote(id: Int, title: String, description: String): Result<Note> {
        return withContext(Dispatchers.IO) {
            try {
                val existingNote = noteDao.getNoteById(id)
                    ?: return@withContext Result.failure(Exception("Note not found"))

                val currentTime = java.time.Instant.now().toString()

                if (networkManager.isNetworkAvailable() &&
                    existingNote.serverId != null &&
                    existingNote.syncStatus.toString() == SyncStatus.SYNCED.toString()) {
                    // Try to update on server immediately
                    val serverResult = updateNoteOnServer(
                        existingNote.serverId,
                        title,
                        description,
                        existingNote,
                        currentTime
                    )
                    if (serverResult.isSuccess) {
                        return@withContext serverResult
                    }
                }

                // Update offline
                val updatedNote = existingNote.copy(
                    title = title,
                    description = description,
                    updatedAt = currentTime,
                    syncStatus = if (existingNote.syncStatus.toString() == SyncStatus.SYNCED.toString())
                        SyncStatus.PENDING_UPDATE.toString() else existingNote.syncStatus
                )
                Log.d("up", updatedNote.syncStatus.toString())

                noteDao.updateNote(updatedNote)

                // Try to sync in background
                syncInBackground()

                Result.success(updatedNote.toDomain())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteNote(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val existingNote = noteDao.getNoteById(id)
                    ?: return@withContext Result.failure(Exception("Note not found"))

                if (networkManager.isNetworkAvailable() &&
                    existingNote.serverId != null &&
                    existingNote.syncStatus.toString() == SyncStatus.SYNCED.toString()) {
                    // Try to delete on server immediately
                    val serverResult = deleteNoteOnServer(existingNote.serverId, id)
                    if (serverResult.isSuccess) {
                        return@withContext serverResult
                    }
                }

                // Handle offline deletion
                if (existingNote.serverId == null) {
                    // Local-only note, hard delete immediately
                    noteDao.hardDeleteNote(id)
                } else {
                    // Server note, soft delete and mark for sync
                    val currentTime = java.time.Instant.now().toString()
                    noteDao.softDeleteNote(id, SyncStatus.PENDING_DELETE, currentTime)
                }

                // Try to sync in background
                syncInBackground()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun searchNotes(query: String): Flow<List<Note>> {
        val userId = getCurrentUserId() ?: 0
        return noteDao.searchNotesByUser(userId, query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncNotes() {
        if (networkManager.isNetworkAvailable()) {
            syncManager.syncAllNotes()
        }
    }

    // Private helper methods
    private suspend fun createNoteOnServer(
        title: String,
        description: String,
        userId: Int,
        currentTime: String
    ): Result<Note> {
        return try {
            val token = tokenManager.getAccessToken()
                ?: throw Exception("No access token")

            val request = CreateNoteRequest(title, description)
            val response = api.createNote("Bearer $token", request)

            if (response.isSuccessful) {
                response.body()?.let { noteResponse ->
                    val note = noteResponse.toDomain()
                    // Pass userId to toEntity() function
                    val noteEntity = note.toEntity(userId).copy(
                        syncStatus = SyncStatus.SYNCED.toString(),
                        serverId = noteResponse.id,
                        isLocal = false,
                        needsSync = false
                    )
                    noteDao.insertNote(noteEntity)
                    Result.success(note)
                } ?: Result.failure(Exception("Empty server response"))
            } else if (response.code() == 401) {
                // Try to refresh token and retry
                refreshTokenAndRetry { createNoteOnServer(title, description, userId, currentTime) }
            } else {
                Result.failure(Exception("Server error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateNoteOnServer(
        serverId: Int,
        title: String,
        description: String,
        localNote: NoteEntity,
        currentTime: String
    ): Result<Note> {
        return try {
            val token = tokenManager.getAccessToken()
                ?: throw Exception("No access token")

            val request = UpdateNoteRequest(title, description)
            val response = api.updateNote("Bearer $token", serverId, request)

            if (response.isSuccessful) {
                response.body()?.let { noteResponse ->
                    val updatedEntity = localNote.copy(
                        title = noteResponse.title,
                        description = noteResponse.description,
                        updatedAt = noteResponse.updatedAt,
                        syncStatus = SyncStatus.SYNCED.toString()
                    )
                    noteDao.updateNote(updatedEntity)
                    Result.success(updatedEntity.toDomain())
                } ?: Result.failure(Exception("Empty server response"))
            } else if (response.code() == 401) {
                // Try to refresh token and retry
                refreshTokenAndRetry {
                    updateNoteOnServer(serverId, title, description, localNote, currentTime)
                }
            } else {
                Result.failure(Exception("Server error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun deleteNoteOnServer(serverId: Int, localId: Int): Result<Unit> {
        return try {
            val token = tokenManager.getAccessToken()
                ?: throw Exception("No access token")

            val response = api.deleteNote("Bearer $token", serverId)

            if (response.isSuccessful) {
                noteDao.hardDeleteNote(localId)
                Result.success(Unit)
            } else if (response.code() == 401) {
                // Try to refresh token and retry
                refreshTokenAndRetry { deleteNoteOnServer(serverId, localId) }
            } else {
                Result.failure(Exception("Server error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createLocalNote(
        id: Int,
        title: String,
        description: String,
        userId: Int,
        currentTime: String,
        syncStatus: SyncStatus
    ) = NoteEntity(
        id = id,
        serverId = null,
        title = title,
        description = description,
        createdAt = currentTime,
        updatedAt = currentTime,
        creatorName = getCurrentUserName() ?: "",
        creatorUsername = getCurrentUsername() ?: "",
        userId = userId,
        isLocal = true,
        needsSync = true,
        syncStatus = syncStatus.toString()
    )

    private suspend fun <T> refreshTokenAndRetry(operation: suspend () -> Result<T>): Result<T> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
                ?: return Result.failure(Exception("No refresh token"))

            val refreshResponse = api.refreshToken(
                com.example.notable.data.dto.RefreshTokenRequest(refreshToken)
            )

            if (refreshResponse.isSuccessful) {
                refreshResponse.body()?.let { tokenResponse ->
                    tokenManager.saveTokens(tokenResponse.access, refreshToken)
                    // Retry the original operation
                    operation()
                } ?: Result.failure(Exception("Empty refresh token response"))
            } else {
                // Refresh failed, clear tokens
                tokenManager.clearTokens()
                Result.failure(Exception("Token refresh failed"))
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            Result.failure(e)
        }
    }

    private fun syncInBackground() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (networkManager.isNetworkAvailable()) {
                    syncManager.syncAllNotes()
                }
            } catch (e: Exception) {
                // Log error silently - don't crash the app
                // In a real app, you might want to use proper logging
            }
        }
    }

    // Helper methods to get current user info
    private fun getCurrentUserId(): Int? {
        // This would need to be implemented in TokenManager
        // For now, return a default or get from stored user info
        return tokenManager.getCurrentUserId()
    }

    private fun getCurrentUserName(): String? {
        return tokenManager.getCurrentUserName()
    }

    private fun getCurrentUsername(): String? {
        return tokenManager.getCurrentUsername()
    }
}
