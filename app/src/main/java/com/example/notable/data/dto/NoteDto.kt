package com.example.notable.data.dto

import com.google.gson.annotations.SerializedName

data class NoteDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("creator_name")
    val creatorName: String,
    @SerializedName("creator_username")
    val creatorUsername: String,
)

data class NotesResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("next")
    val next: String?,
    @SerializedName("previous")
    val previous: String?,
    @SerializedName("results")
    val results: List<NoteDto>
)

data class NoteResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class CreateNoteRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String
)

data class UpdateNoteRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String
)
