package com.example.notable.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: Int = 0,
    val serverId: Int? = null, // Server ID for syncing
    val title: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String,
    val creatorName: String,
    val creatorUsername: String,
    val userId: Int, // User who owns this note
    val isLocal: Boolean = false,
    val needsSync: Boolean = false,
    val syncStatus: String = SyncStatus.SYNCED.name, // Store as string
    val isDeleted: Boolean = false,
)


enum class SyncStatus {
    SYNCED,          // In sync with server
    PENDING_CREATE,  // Created offline, needs to be created on server
    PENDING_UPDATE,  // Updated offline, needs to be updated on server
    PENDING_DELETE,  // Deleted offline, needs to be deleted on server
    SYNC_ERROR       // Failed to sync
}
