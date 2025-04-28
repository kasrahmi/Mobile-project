package com.example.notable.data.api
import retrofit2.http.*

interface SimpleNoteApi {
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse
    @POST("api/auth/token")
    suspend fun tokenCreate(@Body body: AuthTokenRequest): AuthTokenResponse
    @POST("api/auth/token/refresh")
    suspend fun tokenRefresh(@Body body: RefreshTokenRequest): RefreshTokenResponse
    @GET("auth/userinfo/")
    suspend fun getUserInfo(@Header("Authorization") authToken: String): UserInfoResponse
}