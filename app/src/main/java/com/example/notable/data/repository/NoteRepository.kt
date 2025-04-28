package com.example.notable.data.repository

import com.example.notable.data.api.ErrorResponse
import com.example.notable.data.api.RegisterRequest
import com.example.notable.data.api.RegisterResponse
import com.example.notable.data.api.SimpleNoteApi

import com.google.gson.Gson
import retrofit2.HttpException

class NoteRepository(private val apiService: SimpleNoteApi) {

    suspend fun register(
        username: String,
        password: String,
        email: String,
        firstName: String,
        lastName: String
    ): Result<RegisterResponse> {
        return try {
            val request = RegisterRequest(
                username = username,
                password = password,
                email = email,
                first_name = firstName,
                last_name = lastName
            )
            val response = apiService.register(request)
            Result.success(response)

        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val errorResponse = errorBody?.let {
                try {
                    Gson().fromJson(it, ErrorResponse::class.java)
                } catch (ex: Exception) {
                    null
                }
            }
            if (errorResponse != null) {
                Result.failure(RegistrationException(errorResponse))
            } else {
                Result.failure(e)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RegistrationException(val errorResponse: ErrorResponse) : Exception()

