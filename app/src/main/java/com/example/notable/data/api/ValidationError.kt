package com.example.notable.data.api

data class ValidationError(
    val attr: String,
    val code: String,
    val detail: String
)