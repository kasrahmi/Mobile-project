package com.example.notable.data.api

data class ErrorResponse(
    val type: String,
    val errors: List<ValidationError>
)
