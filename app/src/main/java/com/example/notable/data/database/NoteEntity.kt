package com.example.notable.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: Int = 0,
    val title: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String,
    val creatorName: String,
    val creatorUsername: String,
    val isLocal: Boolean = false,
    val needsSync: Boolean = false
)
