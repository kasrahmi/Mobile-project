package com.example.notable.data.mapper

import com.example.notable.data.database.NoteEntity
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

fun Note.toEntity(userId: Int): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        creatorName = "",
        creatorUsername = "",
        userId = userId, // Add userId
        isLocal = false,
        needsSync = false
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
