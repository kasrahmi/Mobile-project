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

fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        creatorName = "", // Default values since Note doesn't have these
        creatorUsername = "",
        isLocal = false,
        needsSync = false
    )
}

fun NoteDto.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        creatorName = "", // These fields aren't in NoteDto, so using defaults
        creatorUsername = "",
        isLocal = false,
        needsSync = false
    )
}