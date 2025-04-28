package com.example.notable.data.api
import retrofit2.Response
import retrofit2.http.*

interface SimpleNoteApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST("auth/token")
    suspend fun tokenCreate(@Body body: AuthTokenRequest): AuthTokenResponse

    @POST("auth/token/refresh")
    suspend fun tokenRefresh(@Body body: RefreshTokenRequest): RefreshTokenResponse

    @GET("auth/userinfo/")
    suspend fun getUserInfo(@Header("Authorization") authToken: String): UserInfoResponse

    @GET("notes/")
    suspend fun listNotes(@Header("Authorization") authToken: String,
                          @Query("page") page: Int? = null,
                          @Query("page_size") pageSize: Int? = null
    ): ListNotesResponse

    @POST("notes/")
    suspend fun createNote(@Header("Authorization") authToken: String,
                           @Body newNote: NoteCreateRequest
    ): CreateNoteResponse

    @GET("notes/{id}/")
    suspend fun getNote(@Header("Authorization") authToken: String,
                        @Path("id") id: Int
    ): GetNoteResponse

    @PUT("notes/{id}/")
    suspend fun updateNote(@Header("Authorization") authToken:
                               String, @Path("id") id: Int,
                           @Body updatedNote: NoteUpdateRequest
    ): NoteUpdateResponse

//    TODO: Notes Partial Update
//    @PATCH("notes/{id}/")
//    suspend fun partialUpdateNote(@Path("id") id: Int, @Body patchFields: Map<String, @JvmSuppressWildcards Any>): Note

    @DELETE("notes/{id}/")
    suspend fun deleteNote(@Header("Authorization") authToken: String,
                           @Path("id") id: Int
    ): Response<Unit>

    @GET("notes/filter_list/")
    suspend fun notesFilterList(
        @Header("Authorization") authToken: String,
        @Query("description")   description: String? = null,
        @Query("title")         title: String? = null,
        @Query("page")          page: Int?    = null,
        @Query("page_size")     pageSize: Int?= null,
        @Query("updated__gte")  updatedGte: String? = null,
        @Query("updated__lte")  updatedLte: String? = null
    ): FilterListResponse

    // TODO: bulk_create

}