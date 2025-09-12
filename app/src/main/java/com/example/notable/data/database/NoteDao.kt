package com.example.notable.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getNotesByUser(userId: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id AND isDeleted = 0")
    suspend fun getNoteById(id: Int): NoteEntity?

    @Query("SELECT * FROM notes WHERE serverId = :serverId AND isDeleted = 0")
    suspend fun getNoteByServerId(serverId: Int): NoteEntity?

    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 0 AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    fun searchNotesByUser(userId: Int, query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET isDeleted = 1, syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteNote(id: Int, syncStatus: SyncStatus, updatedAt: String)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun hardDeleteNote(id: Int)

    @Query("DELETE FROM notes WHERE userId = :userId")
    suspend fun deleteNotesByUser(userId: Int)

    // Sync-related queries
    @Query("SELECT * FROM notes WHERE userId = :userId AND syncStatus != 'SYNCED'")
    suspend fun getNotesNeedingSync(userId: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE userId = :userId AND syncStatus = 'PENDING_CREATE'")
    suspend fun getNotesNeedingCreate(userId: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE userId = :userId AND syncStatus = 'PENDING_UPDATE'")
    suspend fun getNotesNeedingUpdate(userId: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE userId = :userId AND syncStatus = 'PENDING_DELETE'")
    suspend fun getNotesNeedingDelete(userId: Int): List<NoteEntity>

    @Query("UPDATE notes SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, status: SyncStatus)

    @Query("UPDATE notes SET syncStatus = :status, serverId = :serverId WHERE id = :id")
    suspend fun updateSyncStatusWithServerId(id: Int, status: SyncStatus, serverId: Int)

    // Get next available local ID (negative numbers for local-only notes)
    @Query("SELECT MIN(id) - 1 FROM notes WHERE id < 0")
    suspend fun getNextLocalId(): Int?
}
