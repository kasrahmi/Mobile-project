package com.example.notable.model.repository

import com.example.notable.model.User

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<Unit>
    suspend fun register(
        username: String,
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<Unit>
    suspend fun refreshToken(): Result<String>
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
    suspend fun getUserInfo(): Result<User>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
}
