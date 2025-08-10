package com.example.notable.model.repository

import com.example.notable.data.NotableApi
import com.example.notable.data.TokenManager
import com.example.notable.data.dto.ChangePasswordRequest
import com.example.notable.data.dto.TokenRequest
import com.example.notable.data.dto.RefreshTokenRequest
import com.example.notable.data.dto.RegisterRequest
import com.example.notable.data.mapper.toDomainModel
import com.example.notable.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: NotableApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TokenRequest(username, password)
                val response = api.login(request)

                if (response.isSuccessful) {
                    response.body()?.let { tokenResponse ->
                        tokenManager.saveTokens(tokenResponse.access, tokenResponse.refresh)
                        Result.success(Unit)
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    Result.failure(Exception("Login failed: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun register(
        username: String,
        password: String,
        email: String,
        firstName: String,
        lastName: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RegisterRequest(
                    username = username,
                    password = password,
                    email = email,
                    firstName = firstName,
                    lastName = lastName
                )
                val response = api.register(request)

                if (response.isSuccessful) {
                    response.body()?.let {
                        Result.success(Unit)
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    Result.failure(Exception("Registration failed: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getUserInfo(): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val token = tokenManager.getAccessToken()
                    ?: return@withContext Result.failure(Exception("No access token"))

                val response = api.getUserInfo("Bearer $token")

                if (response.isSuccessful) {
                    response.body()?.let { userDto ->
                        Result.success(userDto.toDomainModel())
                    } ?: Result.failure(Exception("Empty response body"))
                } else if (response.code() == 401) {
                    // Try to refresh token and retry
                    refreshToken().fold(
                        onSuccess = { newToken ->
                            val retryResponse = api.getUserInfo("Bearer $newToken")
                            if (retryResponse.isSuccessful) {
                                retryResponse.body()?.let { userDto ->
                                    Result.success(userDto.toDomainModel())
                                } ?: Result.failure(Exception("Empty response body"))
                            } else {
                                Result.failure(Exception("Failed to get user info: ${retryResponse.message()}"))
                            }
                        },
                        onFailure = { Result.failure(Exception("Failed to refresh token")) }
                    )
                } else {
                    Result.failure(Exception("Failed to get user info: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val token = tokenManager.getAccessToken()
                    ?: return@withContext Result.failure(Exception("No access token"))

                val request = ChangePasswordRequest(currentPassword, newPassword)
                val response = api.changePassword("Bearer $token", request)

                if (response.isSuccessful) {
                    Result.success(Unit)
                } else if (response.code() == 401) {
                    // Try to refresh token and retry
                    refreshToken().fold(
                        onSuccess = { newToken ->
                            val retryResponse = api.changePassword("Bearer $newToken", request)
                            if (retryResponse.isSuccessful) {
                                Result.success(Unit)
                            } else {
                                Result.failure(Exception("Failed to change password: ${retryResponse.message()}"))
                            }
                        },
                        onFailure = { Result.failure(Exception("Failed to refresh token")) }
                    )
                } else {
                    Result.failure(Exception("Failed to change password: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun refreshToken(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = tokenManager.getRefreshToken()
                    ?: return@withContext Result.failure(Exception("No refresh token"))

                val request = RefreshTokenRequest(refreshToken)
                val response = api.refreshToken(request)

                if (response.isSuccessful) {
                    response.body()?.let { accessTokenResponse ->
                        val currentRefreshToken = tokenManager.getRefreshToken() ?: ""
                        tokenManager.saveTokens(accessTokenResponse.access, currentRefreshToken)
                        Result.success(accessTokenResponse.access)
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    // Refresh token is invalid
                    tokenManager.clearTokens()
                    Result.failure(Exception("Refresh token expired"))
                }
            } catch (e: Exception) {
                tokenManager.clearTokens()
                Result.failure(e)
            }
        }
    }

    override suspend fun logout() {
        withContext(Dispatchers.IO) {
            tokenManager.clearTokens()
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return tokenManager.getAccessToken() != null
    }
}