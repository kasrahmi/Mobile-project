package com.example.notable.data.mapper

import com.example.notable.data.database.NoteEntity
import com.example.notable.data.database.SyncStatus
import com.example.notable.data.dto.NoteDto
import com.example.notable.data.dto.NoteResponse
import com.example.notable.model.Note

fun NoteDto.toDomain(): Note {
    return Note(
        id = id,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun NoteResponse.toDomain(): Note {
    return Note(
        id = id,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun NoteEntity.toDomain(): Note {
    return Note(
        id = id,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// In your mapper file (likely in data/mapper/ directory)
fun Note.toEntity(userId: Int): NoteEntity {
    return NoteEntity(
        id = this.id, // This will be the local Room ID
        serverId = null, // Will be set after server sync
        title = this.title,
        description = this.description,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        creatorName = "", // This should ideally be fetched from user info
        creatorUsername = "", // This should ideally be fetched from user info
        userId = userId, // Pass the userId here
        isLocal = true, // Default for new conversions
        needsSync = true, // Default for new conversions
        syncStatus = SyncStatus.PENDING_CREATE.toString() // Default for new local notes
    )
}


fun NoteDto.toEntity(userId: Int): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        creatorName = creatorName,
        creatorUsername = creatorUsername,
        userId = userId, // Add userId
        isLocal = false,
        needsSync = false
    )
}
