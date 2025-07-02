package com.example.notable.model

data class Note(
    val id: Int = 0,
    val title: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String,
    val creatorName: String,
    val creatorUsername: String,
    val isLocal: Boolean = false // For offline-first functionality
)
