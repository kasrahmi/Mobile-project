package com.example.notable.data.sync

import android.util.Log
import com.example.notable.data.NotableApi
import com.example.notable.data.TokenManager
import com.example.notable.data.database.NoteDao
import com.example.notable.data.database.SyncStatus
import com.example.notable.data.dto.CreateNoteRequest
import com.example.notable.data.dto.UpdateNoteRequest
import com.example.notable.data.mapper.toEntity
import com.example.notable.data.mapper.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    val api: NotableApi,
    private val noteDao: NoteDao,
    private val tokenManager: TokenManager
) {

    suspend fun syncAllNotes(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken()
                ?: return@withContext Result.failure(Exception("No access token"))

            val userId = tokenManager.getCurrentUserId()
                ?: return@withContext Result.failure(Exception("No user ID"))
            // Sync notes that need to be created on server
            syncNotesNeedingCreate(token, userId)

            // Sync notes that need to be updated on server
            syncNotesNeedingUpdate(token, userId)

            // Sync notes that need to be deleted on server
            syncNotesNeedingDelete(token, userId)

            // Fetch latest notes from server
            syncNotesFromServer(token, userId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncNotesNeedingCreate(token: String, userId: Int) {
        val notesToCreate = noteDao.getNotesNeedingCreate(userId)
        Log.d("CREATE", notesToCreate.size.toString())

        for (note in notesToCreate) {
            try {
                val request = CreateNoteRequest(note.title, note.description)
                val response = api.createNote("Bearer $token", request)

                if (response.isSuccessful) {
                    response.body()?.let { noteResponse ->
                        // Update local note with server ID and mark as synced
                        noteDao.updateSyncStatusWithServerId(
                            note.id,
                            SyncStatus.SYNCED,
                            noteResponse.id
                        )
                    }
                } else {
//                    noteDao.updateSyncStatus(note.id, SyncStatus.SYNC_ERROR)
                }
            } catch (e: Exception) {
//                noteDao.updateSyncStatus(note.id, SyncStatus.SYNC_ERROR)
            }
        }
    }

    private suspend fun syncNotesNeedingUpdate(token: String, userId: Int) {
        val notesToUpdate = noteDao.getNotesNeedingUpdate(userId)
        Log.d("UPDATE", notesToUpdate.size.toString())

        for (note in notesToUpdate) {
            try {
                val serverId = note.serverId ?: continue
                val request = UpdateNoteRequest(note.title, note.description)
                val response = api.updateNote("Bearer $token", serverId, request)

                if (response.isSuccessful) {
                    noteDao.updateSyncStatus(note.id, SyncStatus.SYNCED)
                } else {
//                    noteDao.updateSyncStatus(note.id, SyncStatus.SYNC_ERROR)
                }
            } catch (e: Exception) {
//                noteDao.updateSyncStatus(note.id, SyncStatus.SYNC_ERROR)
            }
        }
    }

    private suspend fun syncNotesNeedingDelete(token: String, userId: Int) {
        val notesToDelete = noteDao.getNotesNeedingDelete(userId)

        for (note in notesToDelete) {
            try {
                val serverId = note.serverId
                if (serverId != null) {
                    val response = api.deleteNote("Bearer $token", serverId)
                    if (response.isSuccessful) {
                        noteDao.hardDeleteNote(note.id)
                    } else {
//                        noteDao.updateSyncStatus(note.id, SyncStatus.SYNC_ERROR)
                    }
                } else {
                    // Local-only note, just delete it
                    noteDao.hardDeleteNote(note.id)
                }
            } catch (e: Exception) {
//                noteDao.updateSyncStatus(note.id, SyncStatus.SYNC_ERROR)
            }
        }
    }

    private suspend fun syncNotesFromServer(token: String, userId: Int) {
        try {
            val response = api.getUserNotes(
                "Bearer $token",
                page = 0
            )

            if (response.isSuccessful) {
                response.body()?.let { notesResponse ->
                    for (noteDto in notesResponse.results) {
                        val existingNote = noteDao.getNoteByServerId(noteDto.id)

                        if (existingNote == null) {
                            // New note from server
                            noteDao.insertNote(noteDto.toEntity(userId).copy(
                                serverId = noteDto.id,
                                syncStatus = SyncStatus.SYNCED.toString()
                            ))
                        } else if (existingNote.syncStatus.toString() == SyncStatus.SYNCED.toString()) {
                            // Update existing synced note
                            noteDao.updateNote(existingNote.copy(
                                title = noteDto.title,
                                description = noteDto.description,
                                updatedAt = noteDto.updatedAt,
                                syncStatus = SyncStatus.SYNCED.toString()
                            ))
                        }
                        // Don't overwrite notes with pending changes
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't fail the entire sync
        }
    }
}
