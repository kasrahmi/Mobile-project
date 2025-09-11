package com.example.notable.data

import com.example.notable.core.Constants
import com.example.notable.data.dto.AccessTokenResponse
import com.example.notable.data.dto.ChangePasswordRequest
import com.example.notable.data.dto.ChangePasswordResponse
import com.example.notable.data.dto.CreateNoteRequest
import com.example.notable.data.dto.NoteResponse
import com.example.notable.data.dto.NotesResponse
import com.example.notable.data.dto.RefreshTokenRequest
import com.example.notable.data.dto.RegisterRequest
import com.example.notable.data.dto.RegisterResponse
import com.example.notable.data.dto.TokenRequest
import com.example.notable.data.dto.TokenResponse
import com.example.notable.data.dto.UpdateNoteRequest
import com.example.notable.data.dto.UserDto
import retrofit2.Response
import retrofit2.http.*

interface NotableApi {

    @POST(Constants.TOKEN_ENDPOINT)
    suspend fun login(@Body request: TokenRequest): Response<TokenResponse>

    @POST(Constants.REGISTER_ENDPOINT)
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST(Constants.REFRESH_TOKEN_ENDPOINT)
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AccessTokenResponse>

    @GET(Constants.USER_INFO_ENDPOINT)
    suspend fun getUserInfo(@Header("Authorization") token: String): Response<UserDto>

    @POST("auth/change-password/")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<ChangePasswordResponse>

    @GET(Constants.NOTES_ENDPOINT)
    suspend fun getUserNotes(
        @Header("Authorization") token: String,
        @Query("page_size") page: Int
    ): Response<NotesResponse>

    @GET("${Constants.NOTES_ENDPOINT}{id}/")
    suspend fun getNoteById(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<NoteResponse>

    @POST(Constants.NOTES_ENDPOINT)
    suspend fun createNote(
        @Header("Authorization") token: String,
        @Body request: CreateNoteRequest
    ): Response<NoteResponse>

    @PUT("${Constants.NOTES_ENDPOINT}{id}/")
    suspend fun updateNote(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: UpdateNoteRequest
    ): Response<NoteResponse>

    @DELETE("${Constants.NOTES_ENDPOINT}{id}/")
    suspend fun deleteNote(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>
}
