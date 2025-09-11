# Combined Kotlin Code

Source Directory: `/Users/parsa/AndroidStudioProjects/Notable/app/src/main/java/com/example/notable`

Date Generated: 1404/06/20 (2025/09/11)

---

## File: `MainActivity.kt`

```kotlin
package com.example.notable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.notable.ui.theme.NotableTheme
import com.example.notable.view.navigation.NotableNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotableTheme {
                NotableNavigation()
            }
        }
    }
}
```

---

## File: `NotableApplication.kt`

```kotlin
package com.example.notable

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NotableApplication : Application()
```

---

## File: `core/Constants.kt`

```kotlin
package com.example.notable.core

object Constants {
    const val BASE_URL = "http://10.0.2.2:8000/api/"  // For Android emulator
//     const val BASE_URL = "http://192.168.1.100:8000/"  // For physical device (replace with your IP)

    const val TOKEN_ENDPOINT = "auth/token/"
    const val REGISTER_ENDPOINT = "auth/register/"
    const val REFRESH_TOKEN_ENDPOINT = "auth/token/refresh/"
    const val USER_INFO_ENDPOINT = "auth/userinfo/"
    const val NOTES_ENDPOINT = "notes/"
    const val NOTES_FILTER_ENDPOINT = "notes/filter/"
    const val NOTES_BULK_ENDPOINT = "notes/bulk/"
}
```

---

## File: `data/NetworkManager.kt`

```kotlin
package com.example.notable.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
```

---

## File: `data/NotableApi.kt`

```kotlin
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

    //    TODO

    @POST("auth/change-password/")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<ChangePasswordResponse>

    @GET(Constants.NOTES_ENDPOINT)
    suspend fun getNotes(
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
```

---

## File: `data/TokenManager.kt`

```kotlin
package com.example.notable.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.notable.model.User
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val _tokenRefreshed = MutableLiveData<Boolean>()
    val tokenRefreshed: LiveData<Boolean> = _tokenRefreshed

    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
        private const val TOKEN_EXPIRY_KEY = "token_expiry"
        private const val ACCESS_TOKEN_LIFETIME_MS = 30 * 60 * 1000L // 30 minutes
        private const val REFRESH_BUFFER_MS = 5 * 60 * 1000L // Refresh 5 minutes before expiry
    }

    fun saveTokens(accessToken: String?, refreshToken: String?) {
        prefs.edit().apply {
            if (accessToken != null) {
                putString(ACCESS_TOKEN_KEY, accessToken)
                // Save expiry time (current time + 30 minutes)
                val expiryTime = System.currentTimeMillis() + ACCESS_TOKEN_LIFETIME_MS
                putLong(TOKEN_EXPIRY_KEY, expiryTime)
            }
            if (refreshToken != null) {
                putString(REFRESH_TOKEN_KEY, refreshToken)
            }
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString(ACCESS_TOKEN_KEY, null)
    fun getRefreshToken(): String? = prefs.getString(REFRESH_TOKEN_KEY, null)

    fun isTokenExpired(): Boolean {
        val expiryTime = prefs.getLong(TOKEN_EXPIRY_KEY, 0)
        return System.currentTimeMillis() >= expiryTime
    }

    fun isTokenExpiringSoon(): Boolean {
        val expiryTime = prefs.getLong(TOKEN_EXPIRY_KEY, 0)
        return System.currentTimeMillis() >= (expiryTime - REFRESH_BUFFER_MS)
    }

    fun clearTokens() {
        prefs.edit().clear().apply()
    }

    fun hasValidTokens(): Boolean {
        return !getAccessToken().isNullOrEmpty() && !isTokenExpired()
    }

    fun notifyTokenRefreshed() {
        _tokenRefreshed.value = true
    }
}
```

---

## File: `data/TokenRefreshInterceptor.kt`

```kotlin
package com.example.notable.data

import com.example.notable.data.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRefreshInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val api: NotableApi
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()

        // Check if this is a token refresh request (avoid infinite loop)
        if (originalRequest.url.encodedPath.contains("token/refresh")) {
            return chain.proceed(originalRequest)
        }

        // Check if token needs refresh
        if (tokenManager.isTokenExpiringSoon()) {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                try {
                    val refreshResponse = runBlocking {
                        api.refreshToken(RefreshTokenRequest(refreshToken))
                    }

                    if (refreshResponse.isSuccessful) {
                        val newToken = refreshResponse.body()?.access
                        if (newToken != null) {
                            tokenManager.saveTokens(newToken, refreshToken)
                            tokenManager.notifyTokenRefreshed()

                            // Update the request with new token
                            val newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Bearer $newToken")
                                .build()
                            return chain.proceed(newRequest)
                        }
                    }
                } catch (e: Exception) {
                    // Token refresh failed, continue with original request
                }
            }
        }

        return chain.proceed(originalRequest)
    }
}
```

---

## File: `data/database/NotableDatabase.kt`

```kotlin
package com.example.notable.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NotableDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
```

---

## File: `data/database/NoteDao.kt`

```kotlin
package com.example.notable.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Query("SELECT * FROM notes WHERE title LIKE :searchQuery OR description LIKE :searchQuery ORDER BY updatedAt DESC")
    fun searchNotes(searchQuery: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE needsSync = 1")
    suspend fun getNotesNeedingSync(): List<NoteEntity>
}
```

---

## File: `data/database/NoteEntity.kt`

```kotlin
package com.example.notable.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: Int = 0,
    val title: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String,
    val creatorName: String,
    val creatorUsername: String,
    val isLocal: Boolean = false,
    val needsSync: Boolean = false
)
```

---

## File: `data/dto/AuthDto.kt`

```kotlin
package com.example.notable.data.dto

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String
)

data class RegisterResponse(
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
)

data class TokenRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String
)

data class TokenResponse(
    @SerializedName("access")
    val access: String,
    @SerializedName("refresh")
    val refresh: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh")
    val refresh: String
)

data class AccessTokenResponse(
    @SerializedName("access")
    val access: String
)

data class UserDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("first_name")
    val first_name: String,
    @SerializedName("last_name")
    val last_name: String
)

data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String
)

data class ChangePasswordResponse(
    val detail: String
)
```

---

## File: `data/dto/NoteDto.kt`

```kotlin
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
```

---

## File: `data/mapper/NoteMapper.kt`

```kotlin
package com.example.notable.data.mapper

import com.example.notable.data.database.NoteEntity
import com.example.notable.data.dto.NoteDto
import com.example.notable.data.dto.NoteResponse
import com.example.notable.model.Note


fun NoteDto.toDomain(): Note {
    return Note(
        id = id,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun NoteResponse.toDomain(): Note {
    return Note(
        id = id,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun NoteEntity.toDomain(): Note {
    return Note(
        id = id,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        creatorName = "", // Default values since Note doesn't have these
        creatorUsername = "",
        isLocal = false,
        needsSync = false
    )
}

fun NoteDto.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        creatorName = "", // These fields aren't in NoteDto, so using defaults
        creatorUsername = "",
        isLocal = false,
        needsSync = false
    )
}
```

---

## File: `data/mapper/UserMapper.kt`

```kotlin
package com.example.notable.data.mapper

import com.example.notable.data.dto.UserDto
import com.example.notable.model.User

fun UserDto.toDomainModel(): User {
    return User(
        id = id,
        username = username,
        email = email,
        firstName = first_name,
        lastName = last_name
    )
}
```

---

## File: `di/DatabaseModule.kt`

```kotlin
package com.example.notable.di

import android.content.Context
import androidx.room.Room
import com.example.notable.data.database.NoteDao
import com.example.notable.data.database.NotableDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNotableDatabase(
        @ApplicationContext context: Context
    ): NotableDatabase {
        return Room.databaseBuilder(
            context,
            NotableDatabase::class.java,
            "notable_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: NotableDatabase): NoteDao {
        return database.noteDao()
    }
}
```

---

## File: `di/NetworkModule.kt`

```kotlin
package com.example.notable.di

import com.example.notable.core.Constants
import com.example.notable.data.NotableApi
import com.example.notable.data.TokenManager
import com.example.notable.data.TokenRefreshInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTokenRefreshInterceptor(
        tokenManager: TokenManager,
        api: NotableApi
    ): TokenRefreshInterceptor {
        return TokenRefreshInterceptor(tokenManager, api)
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideNotableApi(retrofit: Retrofit): NotableApi {
        return retrofit.create(NotableApi::class.java)
    }
}
```

---

## File: `di/RepositoryModule.kt`

```kotlin
package com.example.notable.di


import com.example.notable.model.repository.AuthRepositoryImpl
import com.example.notable.model.repository.NoteRepositoryImpl
import com.example.notable.model.repository.AuthRepository
import com.example.notable.model.repository.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        noteRepositoryImpl: NoteRepositoryImpl
    ): NoteRepository
}
```

---

## File: `model/Note.kt`

```kotlin
package com.example.notable.model

data class Note(
    val id: Int,
    val title: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String
)
```

---

## File: `model/User.kt`

```kotlin
package com.example.notable.model

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String
)
```

---

## File: `model/repository/AuthRepository.kt`

```kotlin
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
```

---

## File: `model/repository/AuthRepositoryImpl.kt`

```kotlin
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
```

---

## File: `model/repository/NoteRepository.kt`

```kotlin
package com.example.notable.model.repository

import com.example.notable.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    suspend fun getAllNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: Int): Note?
    suspend fun createNote(title: String, description: String): Result<Note>
    suspend fun updateNote(id: Int, title: String, description: String): Result<Note>
    suspend fun deleteNote(id: Int): Result<Unit>
    suspend fun searchNotes(query: String): Flow<List<Note>>
    suspend fun syncNotes()
}
```

---

## File: `model/repository/NoteRepositoryImpl.kt`

```kotlin
package com.example.notable.model.repository

import com.example.notable.data.database.NoteDao
import com.example.notable.data.TokenManager
import com.example.notable.data.mapper.*
import com.example.notable.data.NetworkManager
import com.example.notable.data.NotableApi
import com.example.notable.data.dto.CreateNoteRequest
import com.example.notable.data.dto.RefreshTokenRequest
import com.example.notable.data.dto.UpdateNoteRequest
import com.example.notable.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val api: NotableApi,
    private val noteDao: NoteDao,
    private val tokenManager: TokenManager,
    private val networkManager: NetworkManager
) : NoteRepository {

    override suspend fun getAllNotes(): Flow<List<Note>> {
        // Always return local data first, then sync in background
        syncNotesIfNeeded()
        return noteDao.getAllNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getNoteById(id: Int): Note? {
        return withContext(Dispatchers.IO) {
            noteDao.getNoteById(id)?.toDomain()
        }
    }

    override suspend fun createNote(title: String, description: String): Result<Note> {
        return withContext(Dispatchers.IO) {
            try {
                if (networkManager.isNetworkAvailable()) {
                    // Try to create on server first
                    val token = tokenManager.getAccessToken()
                        ?: return@withContext Result.failure(Exception("No access token"))

                    val request = CreateNoteRequest(title, description)
                    val response = api.createNote("Bearer $token", request)

                    if (response.isSuccessful) {
                        response.body()?.let { noteResponse ->
                            val note = noteResponse.toDomain()
                            // Save to local database
                            noteDao.insertNote(note.toEntity())
                            Result.success(note)
                        } ?: Result.failure(Exception("Empty response body"))
                    } else {
                        if (response.code() == 401) {
                            refreshTokenAndRetry { createNote(title, description) }
                        } else {
                            Result.failure(Exception("Failed to create note: ${response.message()}"))
                        }
                    }
                } else {
                    // Create locally when offline
                    val localNote = Note(
                        id = 0, // Will be assigned by Room
                        title = title,
                        description = description,
                        createdAt = System.currentTimeMillis().toString(),
                        updatedAt = System.currentTimeMillis().toString()
                    )
                    val id = noteDao.insertNote(localNote.toEntity())
                    val createdNote = localNote.copy(id = id.toInt())
                    Result.success(createdNote)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateNote(id: Int, title: String, description: String): Result<Note> {
        return withContext(Dispatchers.IO) {
            try {
                if (networkManager.isNetworkAvailable()) {
                    val token = tokenManager.getAccessToken()
                        ?: return@withContext Result.failure(Exception("No access token"))

                    val request = UpdateNoteRequest(title, description)
                    val response = api.updateNote("Bearer $token", id, request)

                    if (response.isSuccessful) {
                        response.body()?.let { noteResponse ->
                            val note = noteResponse.toDomain()
                            // Update local database
                            noteDao.updateNote(note.toEntity())
                            Result.success(note)
                        } ?: Result.failure(Exception("Empty response body"))
                    } else {
                        if (response.code() == 401) {
                            refreshTokenAndRetry { updateNote(id, title, description) }
                        } else {
                            Result.failure(Exception("Failed to update note: ${response.message()}"))
                        }
                    }
                } else {
                    // Update locally when offline
                    val existingNote = noteDao.getNoteById(id)
                        ?: return@withContext Result.failure(Exception("Note not found"))

                    val updatedNote = existingNote.copy(
                        title = title,
                        description = description,
                        updatedAt = System.currentTimeMillis().toString()
                    )
                    noteDao.updateNote(updatedNote)
                    Result.success(updatedNote.toDomain())
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteNote(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (networkManager.isNetworkAvailable()) {
                    val token = tokenManager.getAccessToken()
                        ?: return@withContext Result.failure(Exception("No access token"))

                    val response = api.deleteNote("Bearer $token", id)

                    if (response.isSuccessful) {
                        // Delete from local database
                        noteDao.deleteNoteById(id)
                        Result.success(Unit)
                    } else {
                        if (response.code() == 401) {
                            refreshTokenAndRetry { deleteNote(id) }
                        } else {
                            Result.failure(Exception("Failed to delete note: ${response.message()}"))
                        }
                    }
                } else {
                    // Delete locally when offline
                    noteDao.deleteNoteById(id)
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes("%$query%").map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncNotes() {
        if (!networkManager.isNetworkAvailable()) return

        withContext(Dispatchers.IO) {
            try {
                val token = tokenManager.getAccessToken() ?: return@withContext

                var page = 1
                var hasMore = true

                while (hasMore) {
                    val response = api.getNotes("Bearer $token", page)

                    if (response.isSuccessful) {
                        response.body()?.let { notesResponse ->
                            // Save notes to local database
                            val noteEntities = notesResponse.results.map { it.toEntity() }
                            noteDao.insertNotes(noteEntities)

                            hasMore = notesResponse.next != null
                            page++
                        } ?: run { hasMore = false }
                    } else {
                        if (response.code() == 401) {
                            // For syncNotes, we need to handle the Unit return type
                            refreshTokenForSync()
                            return@withContext
                        } else {
                            hasMore = false
                        }
                    }
                }
            } catch (e: Exception) {
                // Sync failed, but continue with local data
            }
        }
    }

    private suspend fun syncNotesIfNeeded() {
        // Only sync if we have network and haven't synced recently
        if (networkManager.isNetworkAvailable()) {
            try {
                syncNotes()
            } catch (e: Exception) {
                // Ignore sync errors
            }
        }
    }

    private suspend fun <T> refreshTokenAndRetry(operation: suspend () -> Result<T>): Result<T> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
                ?: return Result.failure(Exception("No refresh token"))

            val response = api.refreshToken(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful) {
                response.body()?.let { accessTokenResponse ->
                    val currentRefreshToken = tokenManager.getRefreshToken() ?: ""
                    tokenManager.saveTokens(accessTokenResponse.access, currentRefreshToken)
                    operation()
                } ?: Result.failure(Exception("Failed to refresh token"))
            } else {
                // Refresh token is invalid
                tokenManager.clearTokens()
                Result.failure(Exception("Session expired"))
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            Result.failure(e)
        }
    }

    private suspend fun refreshTokenForSync() {
        try {
            val refreshToken = tokenManager.getRefreshToken() ?: return

            val response = api.refreshToken(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful) {
                response.body()?.let { accessTokenResponse ->
                    val currentRefreshToken = tokenManager.getRefreshToken() ?: ""
                    tokenManager.saveTokens(accessTokenResponse.access, currentRefreshToken)
                    // Retry sync after refreshing token
                    syncNotes()
                }
            } else {
                // Refresh token is invalid
                tokenManager.clearTokens()
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
        }
    }
}
```

---

## File: `ui/theme/Color.kt`

```kotlin
package com.example.notable.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
```

---

## File: `ui/theme/Theme.kt`

```kotlin
package com.example.notable.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun NotableTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

---

## File: `ui/theme/Type.kt`

```kotlin
package com.example.notable.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)
```

---

## File: `view/navigation/NotableNavigation.kt`

```kotlin
package com.example.notable.view.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.notable.view.wrapper.ChangePasswordScreenWrapper
import com.example.notable.view.wrapper.HomeScreenWrapper
import com.example.notable.view.wrapper.LoginScreenWrapper
import com.example.notable.view.wrapper.NoteScreenWrapper
import com.example.notable.view.wrapper.OnboardingScreenWrapper
import com.example.notable.view.wrapper.RegisterScreenWrapper
import com.example.notable.view.wrapper.SettingsScreenWrapper

@Composable
fun NotableNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = "onboarding"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreenWrapper(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreenWrapper(
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("register") {
            RegisterScreenWrapper(
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("home") {
            HomeScreenWrapper(
                onNavigateToNote = { noteId ->
                    navController.navigate("note/$noteId")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToAddNote = {
                    navController.navigate("note/new")
                }
            )
        }

        composable(
            "note/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            NoteScreenWrapper(
                noteId = noteId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("settings") {
            SettingsScreenWrapper(
                navController = navController
            )
        }

        composable("change_password") {
            ChangePasswordScreenWrapper(
                navController = navController
            )
        }
    }
}
```

---

## File: `view/screen/ChangePasswordScreen.kt`

```kotlin
package com.example.notable.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun ChangePasswordScreen(
    currentPassword: String = "",
    newPassword: String = "",
    retypePassword: String = "",
    onBackClick: () -> Unit = {},
    onCurrentPasswordChange: (String) -> Unit = {},
    onNewPasswordChange: (String) -> Unit = {},
    onRetypePasswordChange: (String) -> Unit = {},
    onSubmitClick: () -> Unit = {},
    isLoading: Boolean = false
) {
    var currentPass by remember { mutableStateOf(currentPassword) }
    var newPass by remember { mutableStateOf(newPassword) }
    var retypePass by remember { mutableStateOf(retypePassword) }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var retypePasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Status bar spacer
        Spacer(modifier = Modifier.height(24.dp))

        // Top bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onBackClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back",
                    color = Color(0xFF6366F1),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Change Password",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Instructions
        Text(
            text = "Please input your current password first",
            fontSize = 14.sp,
            color = Color(0xFF6366F1),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Current Password section
        Column {
            Text(
                text = "Current Password",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = currentPass,
                onValueChange = {
                    currentPass = it
                    onCurrentPasswordChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
//                        Icon(
//                            imageVector = if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
//                            contentDescription = if (currentPasswordVisible) "Hide password" else "Show password",
//                            tint = Color.Gray
//                        )
                    }
                },
                placeholder = {
                    Text(
                        text = "••••••••••",
                        color = Color.Gray
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // New password instruction
        Text(
            text = "Now, create your new password",
            fontSize = 14.sp,
            color = Color(0xFF6366F1),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // New Password section
        Column {
            Text(
                text = "New Password",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = newPass,
                onValueChange = {
                    newPass = it
                    onNewPasswordChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
//                        Icon(
//                            imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
//                            contentDescription = if (newPasswordVisible) "Hide password" else "Show password",
//                            tint = Color.Gray
//                        )
                    }
                },
                placeholder = {
                    Text(
                        text = "••••••••••",
                        color = Color.Gray
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                )
            )

            Text(
                text = "Password should contain a-z, A-Z, 0-9",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Retype Password section
        Column {
            Text(
                text = "Retype New Password",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = retypePass,
                onValueChange = {
                    retypePass = it
                    onRetypePasswordChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (retypePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { retypePasswordVisible = !retypePasswordVisible }) {
//                        Icon(
//                            imageVector = if (retypePasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
//                            contentDescription = if (retypePasswordVisible) "Hide password" else "Show password",
//                            tint = Color.Gray
//                        )
                    }
                },
                placeholder = {
                    Text(
                        text = "••••••••••",
                        color = Color.Gray
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                ),
                isError = newPass.isNotEmpty() && retypePass.isNotEmpty() && newPass != retypePass
            )

            if (newPass.isNotEmpty() && retypePass.isNotEmpty() && newPass != retypePass) {
                Text(
                    text = "Passwords do not match",
                    fontSize = 12.sp,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Submit button
        Button(
            onClick = onSubmitClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = currentPass.isNotEmpty() &&
                    newPass.isNotEmpty() &&
                    retypePass.isNotEmpty() &&
                    newPass == retypePass &&
                    !isLoading,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1),
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "Submit New Password",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Submit",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ChangePasswordScreenPreview() {
    ChangePasswordScreen()
}
```

---

## File: `view/screen/DeleteDialog.kt`

```kotlin
package com.example.notable.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun DeleteDialog(
    title: String = "New Product Ideas",
    content: String = "Create a mobile app UI Kit that provide a basic notes functionality but with some improvement.\n\nThere will be a choice to select what kind of notes that user needed, so the experience while taking notes can be unique based on the needs.",
    lastEdited: String = "Last edited on 19.30",
    showDeleteDialog: Boolean = true,
    onBackClick: () -> Unit = {},
    onDeleteConfirm: () -> Unit = {},
    onDeleteCancel: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content with overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (showDeleteDialog) 0.25f else 0f))
                .padding(16.dp)
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackClick() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back",
                    color = Color(0xFF6366F1),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title with light bulb icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Content
            Text(
                text = content,
                fontSize = 16.sp,
                color = Color.Gray,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Last edited
            Text(
                text = lastEdited,
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Delete Dialog
        if (showDeleteDialog) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Want to Delete this Note?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        IconButton(onClick = onDeleteCancel) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onDeleteConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Note",
                            color = Color.Red,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DeleteDialogPreview() {
    DeleteDialog()
}
```

---

## File: `view/screen/EmptyHomeScreen.kt`

```kotlin
package com.example.notable.view.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign

@Composable
fun EmptyHomeScreen(
    onAddNoteClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            Spacer(modifier = Modifier.height(48.dp))

            // Text content
            Text(
                text = "Start Your Journey",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Every big step start with small step.\nNotes your first idea and start\nyour journey!",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Curved arrow illustration (simplified)
            Canvas(
                modifier = Modifier
                    .size(100.dp, 80.dp)
                    .padding(bottom = 16.dp)
            ) {
                // Draw a simple curved arrow pointing to the FAB
                // This is a simplified representation
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.2f)
                        quadraticBezierTo(
                            size.width * 0.8f, size.height * 0.3f,
                            size.width * 0.9f, size.height * 0.8f
                        )
                    },
                    color = Color.Gray.copy(alpha = 0.5f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddNoteClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-80).dp),
            containerColor = Color(0xFF6366F1)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Note",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Bottom Navigation
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onHomeClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Home",
                    color = Color(0xFF6366F1),
                    fontSize = 12.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSettingsClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Settings",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EmptyHomeScreenPreview() {
    EmptyHomeScreen()
}
```

---

## File: `view/screen/HomeWithNotesScreen.kt`

```kotlin
package com.example.notable.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun HomeWithNotesScreen(
    notes: List<NoteItem> = listOf(
        NoteItem(
            id = "1",
            title = "New Product Idea Design",
            preview = "Create a mobile app UI Kit that provide a basic notes functionality but with some improvement. There will be a choice to select what kind of notes that user needed, so the experience while taking notes can be unique based on the needs.",
                    backgroundColor = Color(0xFFFFF9C4)
        ),
        NoteItem(
            id = "2",
            title = "New Product Idea Design",
            preview = "Create a mobile app UI Kit that provide a basic notes functionality but with some improvement. There will be a choice to select what kind of notes that user needed, so the experience while taking notes can be unique based on the needs.",
                    backgroundColor = Color(0xFFFFE0B2)
        )
    ),
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    hasSearchResults: Boolean = true,
    onSearchChange: (String) -> Unit = {},
    onClearSearch: () -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onAddNoteClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search bar - Always visible
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search...", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Gray
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 16.dp),
                shape = RoundedCornerShape(25.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                    focusedContainerColor = Color.Gray.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.Gray.copy(alpha = 0.1f)
                ),
                singleLine = true
            )

            // Content based on search state
            when {
                isSearchActive && !hasSearchResults -> {
                    // Show "No search results" message
                    NoSearchResultsContent(
                        searchQuery = searchQuery,
                        onClearSearch = onClearSearch
                    )
                }
                else -> {
                    // Show notes (filtered or all)
                    Column {
                        // Notes title
                        Text(
                            text = if (isSearchActive) "Search Results" else "Notes",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Notes grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(notes) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = { onNoteClick(note.id) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Space for bottom navigation
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddNoteClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-80).dp),
            containerColor = Color(0xFF6366F1)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Note",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Bottom Navigation
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onHomeClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Home",
                    color = Color(0xFF6366F1),
                    fontSize = 12.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSettingsClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Settings",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun NoSearchResultsContent(
    searchQuery: String,
    onClearSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No results found",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "No notes match $searchQuery",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onClearSearch,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1)
            )
        ) {
            Text("Clear Search", color = Color.White)
        }
    }
}

@Composable
fun NoteCard(
    note: NoteItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = note.backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = note.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.preview,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 6,
                lineHeight = 16.sp
            )
        }
    }
}

// Data class for note items
data class NoteItem(
    val id: String,
    val title: String,
    val preview: String,
    val backgroundColor: Color = Color.White
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeWithNotesScreenPreview() {
    HomeWithNotesScreen()
}
```

---

## File: `view/screen/LogOutDialog.kt`

```kotlin
package com.example.notable.view.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign

@Composable
fun LogOutDialog(
    userName: String = "Taha Hamifar",
    userEmail: String = "hamifar.taha@gmail.com",
    appVersion: String = "Taha Notes v1.1",
    showLogOutDialog: Boolean = true,
    profileImageUrl: String? = null,
    onBackClick: () -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
    onLogOutClick: () -> Unit = {},
    onLogOutConfirm: () -> Unit = {},
    onLogOutCancel: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main settings content with overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (showLogOutDialog) Color.Black.copy(alpha = 0.25f) else Color.White
                )
                .padding(16.dp)
        ) {
            // Status bar spacer
            Spacer(modifier = Modifier.height(24.dp))

            // Top bar with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { onBackClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back",
                        color = Color(0xFF6366F1),
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // User profile section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile image
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFFA726), Color(0xFFFF7043))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = userName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = userEmail,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Settings section
            Text(
                text = "APP SETTINGS",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Change Password option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChangePasswordClick() }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Change Password",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Change Password",
                    fontSize = 16.sp,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Navigate",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Log Out option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogOutClick() }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Log Out",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Log Out",
                    fontSize = 16.sp,
                    color = Color.Red
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // App version at bottom
            Text(
                text = appVersion,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // Log Out Dialog
        if (showLogOutDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Log Out",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Are you sure you want to log out from the application?",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onLogOutCancel,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(25.dp),
                                border = BorderStroke(1.dp, Color(0xFF6366F1)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF6366F1)
                                )
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = onLogOutConfirm,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(25.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1)
                                )
                            ) {
                                Text(
                                    text = "Yes",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LogOutDialogPreview() {
    LogOutDialog()
}
```

---

## File: `view/screen/LoginScreen.kt`

```kotlin
package com.example.notable.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit = { _, _ -> },
    onRegisterClick: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(Color.White)
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        // Title
        Text(
            text = "Let's Login",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = "And notes your idea",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Username
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Email Address") },
            placeholder = { Text("Example: johndoe@gmail.com", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            placeholder = { Text("••••••••", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Login Button
        Button(
            onClick = { onLogin(username, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = "Login",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Divider
        Text(
            text = "Or",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Register link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Don't have any account? ",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Text(
                text = "Register here",
                color = Color(0xFF6366F1),
                fontSize = 14.sp,
                modifier = Modifier.clickable { onRegisterClick() }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}
```

---

## File: `view/screen/NoteEditingScreen.kt`

```kotlin
package com.example.notable.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun NoteEditingScreen(
    title: String = "",
    content: String = "",
    lastEdited: String = "Last edited on 19.30",
    onBackClick: () -> Unit = {},
    onTitleChange: (String) -> Unit = {},
    onContentChange: (String) -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    var noteTitle by remember { mutableStateOf(title) }
    var noteContent by remember { mutableStateOf(content) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp)
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackClick() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back",
                    color = Color(0xFF6366F1),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title TextField
            TextField(
                value = noteTitle,
                onValueChange = {
                    noteTitle = it
                    onTitleChange(it)
                },
                placeholder = {
                    Text(
                        text = "Title",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content TextField
            TextField(
                value = noteContent,
                onValueChange = {
                    noteContent = it
                    onContentChange(it)
                },
                placeholder = {
                    Text(
                        text = "Feel Free to Write Here...",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    color = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            // Bottom section with last edited and delete button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lastEdited,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        // Floating delete button
        FloatingActionButton(
            onClick = onDeleteClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF6366F1)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NoteEditingScreenPreview() {
    NoteEditingScreen()
}
```

---

## File: `view/screen/NoteViewScreen.kt`

```kotlin
package com.example.notable.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun NoteViewScreen(
    title: String = "Title",
    content: String = "Feel Fre to Write Here...",
    lastEdited: String = "Last edited on 19.30",
    onBackClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp)
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackClick() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back",
                    color = Color(0xFF6366F1),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title with light bulb icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Content
            Text(
                text = content,
                fontSize = 16.sp,
                color = Color.Gray,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Last edited
            Text(
                text = lastEdited,
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Floating delete button
        FloatingActionButton(
            onClick = onDeleteClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF6366F1)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NoteViewScreenPreview() {
    NoteViewScreen()
}
```

---

## File: `view/screen/OnboardingScreen.kt`

```kotlin
package com.example.notable.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6366F1))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.weight(1f))

            // Text content
            Text(
                text = "Jot Down anything you want to achieve, today or in the future",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Get Started Button
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Let's Get Started",
                    color = Color(0xFF6366F1),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen()
}
```

---

## File: `view/screen/RegisterScreen.kt`

```kotlin
package com.example.notable.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit = {},
    onRegister: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onLoginClick: () -> Unit = {}
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var retypePassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(Color.White)
    ) {
        // Back to Login
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .clickable { onBackToLogin() }
//                .padding(vertical = 16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                imageVector = Icons.Default.ArrowBack,
//                contentDescription = "Back",
//                tint = Color(0xFF6366F1),
//                modifier = Modifier.size(20.dp)
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//            Text(
//                text = "Back to Login",
//                color = Color(0xFF6366F1),
//                fontSize = 16.sp
//            )
//        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "Register",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = "And start taking notes",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // First Name
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            placeholder = { Text("Example: Taha", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Last Name
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            placeholder = { Text("Example: Hamifar", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Username
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            placeholder = { Text("Example: @HamifarTaha", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email Address
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            placeholder = { Text("Example: hamifar.taha@gmail.com", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            placeholder = { Text("••••••••", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Retype Password
        OutlinedTextField(
            value = retypePassword,
            onValueChange = { retypePassword = it },
            label = { Text("Retype Password") },
            placeholder = { Text("••••••••", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Register Button
        Button(
            onClick = { onRegister(username, password, email, firstName, lastName) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = "Register",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Already have account
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Already have an account? ",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Text(
                text = "Login here",
                color = Color(0xFF6366F1),
                fontSize = 14.sp,
                modifier = Modifier.clickable { onLoginClick() }
            )
        }
    }
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen()
}
```

---

## File: `view/screen/SettingsScreen.kt`

```kotlin
package com.example.notable.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SettingsScreen(
    userName: String = "Taha Hamifar",
    userEmail: String = "hamifar.taha@gmail.com",
    appVersion: String = "Taha Notes v1.1",
    profileImageUrl: String? = null,
    onBackClick: () -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
    onLogOutClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Status bar spacer
        Spacer(modifier = Modifier.height(24.dp))

        // Top bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onBackClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back",
                    color = Color(0xFF6366F1),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // User profile section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFFA726), Color(0xFFFF7043))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUrl != null) {
                    // In a real app, you would use an image loading library like Coil
                    // AsyncImage(model = profileImageUrl, contentDescription = "Profile")
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = userEmail,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // App Settings section
        Text(
            text = "APP SETTINGS",
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Change Password option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChangePasswordClick() }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Change Password",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Change Password",
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Log Out option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogOutClick() }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Log Out",
                tint = Color.Red,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Log Out",
                fontSize = 16.sp,
                color = Color.Red
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // App version at bottom
        Text(
            text = appVersion,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
```

---

## File: `view/viewmodel/AuthViewModel.kt`

```kotlin
package com.example.notable.view.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notable.model.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            authRepository.login(username, password).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun register(
        username: String,
        password: String,
        email: String,
        firstName: String,
        lastName: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            authRepository.register(username, password, email, firstName, lastName).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        registrationSuccess = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState()
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val registrationSuccess: Boolean = false,
    val error: String? = null
)
```

---

## File: `view/viewmodel/NoteDetailViewModel.kt`

```kotlin
package com.example.notable.view.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notable.model.repository.AuthRepository
import com.example.notable.model.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // TODO
    private var noteId: String? = savedStateHandle.get<String>("noteId")

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()

    init {
        loadNote()
    }

    private fun loadNote() {
        noteId?.let { id ->
            if (id == "new") {
                _uiState.value = _uiState.value.copy(
                    isEditing = true,
                    isNewNote = true
                )
            } else {
                viewModelScope.launch {
                    val note = noteRepository.getNoteById(id.toInt())
                    note?.let {
                        _uiState.value = _uiState.value.copy(
                            title = it.title,
                            content = it.description,
                            lastEdited = formatLastEdited(it.updatedAt),
                            isLoading = false,
                            isEditing = true
                        )
                    }
                }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(title = newTitle)
        autoSave()
    }

    fun updateContent(newContent: String) {
        _uiState.value = _uiState.value.copy(content = newContent)
        autoSave()
    }

    fun toggleEditMode() {
        _uiState.value = _uiState.value.copy(isEditing = !_uiState.value.isEditing)
    }

    fun showDeleteDialog() {
        _showDeleteDialog.value = true
    }

    fun hideDeleteDialog() {
        _showDeleteDialog.value = false
    }

    fun deleteNote() {
        viewModelScope.launch {
            noteId?.let { id ->
                if (id != "new") {
                    noteRepository.deleteNote(id.toInt()).fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(noteDeleted = true)
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(error = error.message)
                        }
                    )
                }
            }
        }
    }

    private fun autoSave() {
        viewModelScope.launch {
            delay(50) // Auto-save after 1 second of no changes
            saveNote()
        }
    }

    fun saveNote() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.isNewNote) {
                noteRepository.createNote(state.title, state.content).fold(
                    onSuccess = { note ->
                        _uiState.value = _uiState.value.copy(
                            isNewNote = false,
                            lastEdited = formatLastEdited(note.updatedAt),
                            isEditing = true
                        )
                        savedStateHandle["noteId"] = note.id.toString() // Update noteId in SavedStateHandle
                        noteId = note.id.toString() // Update local noteId
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
                )
            } else {
                noteId?.let { id ->
                    noteRepository.updateNote(id.toInt(), state.title, state.content).fold(
                        onSuccess = { note ->
                            _uiState.value = _uiState.value.copy(
                                lastEdited = formatLastEdited(note.updatedAt)
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(error = error.message)
                        }
                    )
                }
            }
        }
    }

    private fun formatLastEdited(timestamp: String): String {
        // Format timestamp to "Last edited on 19.30" format
        return "Last edited on ${timestamp.substring(11, 16)}"
    }
}

data class NoteDetailUiState(
    val title: String = "",
    val content: String = "",
    val lastEdited: String = "",
    val isEditing: Boolean = false,
    val isNewNote: Boolean = false,
    val isLoading: Boolean = true,
    val noteDeleted: Boolean = false,
    val error: String? = null
)
```

---

## File: `view/viewmodel/NotesViewModel.kt`

```kotlin
package com.example.notable.view.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notable.model.repository.AuthRepository
import com.example.notable.model.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var allNotes: List<com.example.notable.view.screen.NoteItem> = emptyList()

    init {
        loadNotes()
        observeSearchQuery()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            noteRepository.getAllNotes().collect { notes ->
                val noteItems = notes.map { note ->
                    com.example.notable.view.screen.NoteItem(
                        id = note.id.toString(),
                        title = note.title,
                        preview = note.description,
                        backgroundColor = getRandomNoteColor()
                    )
                }
                allNotes = noteItems

                // Only update displayed notes if not searching
                if (_searchQuery.value.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        notes = noteItems,
                        isLoading = false,
                        isSearchActive = false,
                        hasSearchResults = true
                    )
                } else {
                    // If we're searching, re-apply the search
                    performSearch(_searchQuery.value)
                }
            }
        }
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Debounce search queries
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            // Clear search - show all notes
            _uiState.value = _uiState.value.copy(
                notes = allNotes,
                isSearchActive = false,
                hasSearchResults = true
            )
        } else {
            // Active search
            val filteredNotes = allNotes.filter { note ->
                note.title.contains(query, ignoreCase = true) ||
                        note.preview.contains(query, ignoreCase = true)
            }

            _uiState.value = _uiState.value.copy(
                notes = filteredNotes,
                isSearchActive = true,
                hasSearchResults = filteredNotes.isNotEmpty()
            )
        }
    }

    // MISSING METHOD 1: Update search query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // MISSING METHOD 2: Clear search
    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun syncNotes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            noteRepository.syncNotes()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun getRandomNoteColor(): androidx.compose.ui.graphics.Color {
        val colors = listOf(
            Color(0xFFFFF9C4), // Light Yellow
            Color(0xFFFFE0B2), // Light Orange
            Color(0xFFE1F5FE), // Light Blue
            Color(0xFFF3E5F5), // Light Purple
            Color(0xFFE8F5E8), // Light Green
        )
        return colors.random()
    }
}

data class NotesUiState(
    val notes: List<com.example.notable.view.screen.NoteItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSearchActive: Boolean = false,
    val hasSearchResults: Boolean = true
)
```

---

## File: `view/viewmodel/SettingsViewModel.kt`

```kotlin
package com.example.notable.view.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notable.model.User
import com.example.notable.model.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            authRepository.getUserInfo().fold(
                onSuccess = { user: User ->
                    _uiState.value = _uiState.value.copy(
                        user = user,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error: Throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChangingPassword = true, error = null)

            authRepository.changePassword(currentPassword, newPassword).fold(
                onSuccess = { _: Unit ->
                    _uiState.value = _uiState.value.copy(
                        isChangingPassword = false,
                        passwordChanged = true,
                        error = null
                    )
                },
                onFailure = { error: Throwable ->
                    _uiState.value = _uiState.value.copy(
                        isChangingPassword = false,
                        error = error.message ?: "An error occurred while changing password"
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(isLoggedOut = true)
        }
    }

    fun showLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = true)
    }

    fun hideLogoutDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearPasswordChanged() {
        _uiState.value = _uiState.value.copy(passwordChanged = false)
    }
}

data class SettingsUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isChangingPassword: Boolean = false,
    val isLoggedOut: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val error: String? = null,
    val passwordChanged: Boolean = false,
    val appVersion: String = "Notable v1.1"
) {
    val userName: String
        get() = user?.let { "${it.firstName} ${it.lastName}" } ?: "User Name"

    val userEmail: String
        get() = user?.email ?: "user@example.com"
}
```

---

## File: `view/wrapper/ChangePasswordScreenWrapper.kt`

```kotlin
package com.example.notable.view.wrapper

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.notable.view.viewmodel.SettingsViewModel
import com.example.notable.view.screen.ChangePasswordScreen

@Composable
fun ChangePasswordScreenWrapper(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var retypePassword by remember { mutableStateOf("") }

    // Navigate back when password is successfully changed
    LaunchedEffect(uiState.passwordChanged) {
        if (uiState.passwordChanged) {
            viewModel.clearPasswordChanged()
            navController.popBackStack()
        }
    }

    // Show error dialog if there's an error
    uiState.error?.let { error ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { androidx.compose.material3.Text("Error") },
            text = { androidx.compose.material3.Text(error) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.clearError() }
                ) {
                    androidx.compose.material3.Text("OK")
                }
            }
        )
    }

    ChangePasswordScreen(
        currentPassword = currentPassword,
        newPassword = newPassword,
        retypePassword = retypePassword,
        onBackClick = {
            navController.popBackStack()
        },
        onCurrentPasswordChange = { currentPassword = it },
        onNewPasswordChange = { newPassword = it },
        onRetypePasswordChange = { retypePassword = it },
        onSubmitClick = {
            if (newPassword == retypePassword && currentPassword.isNotEmpty()) {
                viewModel.changePassword(currentPassword, newPassword)
            }
        },
        isLoading = uiState.isChangingPassword
    )
}
```

---

## File: `view/wrapper/HomeScreenWrapper.kt`

```kotlin
package com.example.notable.view.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notable.view.screen.EmptyHomeScreen
import com.example.notable.view.screen.HomeWithNotesScreen
import com.example.notable.view.viewmodel.NotesViewModel

// This is what your HomeScreenWrapper should look like:
@Composable
fun HomeScreenWrapper(
    onNavigateToNote: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAddNote: () -> Unit
) {
    val notesViewModel: NotesViewModel = hiltViewModel()
    val uiState by notesViewModel.uiState.collectAsState()
    val searchQuery by notesViewModel.searchQuery.collectAsState()

    HomeWithNotesScreen(
        notes = uiState.notes,
        searchQuery = searchQuery,
        isSearchActive = uiState.isSearchActive,
        hasSearchResults = uiState.hasSearchResults,
        onSearchChange = { query -> notesViewModel.updateSearchQuery(query) },
        onClearSearch = { notesViewModel.clearSearch() },
        onNoteClick = onNavigateToNote,
        onAddNoteClick = onNavigateToAddNote,
        onHomeClick = { /* Already on home */ },
        onSettingsClick = onNavigateToSettings
    )
}
```

---

## File: `view/wrapper/LoginScreenWrapper.kt`

```kotlin
package com.example.notable.view.wrapper

import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.notable.view.screen.LoginScreen
import com.example.notable.view.viewmodel.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreenWrapper(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onNavigateToHome()
        }
    }

    LoginScreen(
        onLogin = { username, password ->
            viewModel.login(username, password)
        },
        onRegisterClick = onNavigateToRegister
    )

    // Handle error states

    val context = LocalContext.current
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error toast or handle error display
            // For example, you can use a Snackbar or Toast to show the error
            // show in a toast
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }
}
```

---

## File: `view/wrapper/NoteScreenWrapper.kt`

```kotlin
package com.example.notable.view.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notable.view.screen.DeleteDialog
import com.example.notable.view.screen.NoteEditingScreen
import com.example.notable.view.screen.NoteViewScreen
import com.example.notable.view.viewmodel.NoteDetailViewModel

@Composable
fun NoteScreenWrapper(
    noteId: String?,
    onNavigateBack: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()

) {
    val uiState by viewModel.uiState.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    LaunchedEffect(uiState.noteDeleted) {
        if (uiState.noteDeleted) {
            onNavigateBack()
        }
    }

    if (showDeleteDialog) {
        DeleteDialog(
            title = uiState.title,
            content = uiState.content,
            lastEdited = uiState.lastEdited,
            showDeleteDialog = showDeleteDialog,
            onBackClick = onNavigateBack,
            onDeleteConfirm = {
                viewModel.deleteNote()
                viewModel.hideDeleteDialog()
            },
            onDeleteCancel = {
                viewModel.hideDeleteDialog()
            }
        )
    } else {
        if (uiState.isEditing || uiState.isNewNote) {
            NoteEditingScreen(
                title = uiState.title,
                content = uiState.content,
                lastEdited = uiState.lastEdited,
                onBackClick = {
                    viewModel.saveNote()
                    onNavigateBack()
                },
                onTitleChange = viewModel::updateTitle,
                onContentChange = viewModel::updateContent,
                onDeleteClick = {
                    if (!uiState.isNewNote) {
                        viewModel.showDeleteDialog()
                    }
                }
            )
        } else {
            NoteViewScreen(
                title = uiState.title,
                content = uiState.content,
                lastEdited = uiState.lastEdited,
                onBackClick = onNavigateBack,
                onDeleteClick = viewModel::showDeleteDialog
            )
        }
    }
}
```

---

## File: `view/wrapper/OnboardingScreenWrapper.kt`

```kotlin
package com.example.notable.view.wrapper

import androidx.compose.runtime.Composable
import com.example.notable.view.screen.OnboardingScreen

@Composable
fun OnboardingScreenWrapper(
    onNavigateToLogin: () -> Unit
) {
    OnboardingScreen(
        onGetStarted = onNavigateToLogin
    )
}
```

---

## File: `view/wrapper/RegisterScreenWrapper.kt`

```kotlin
package com.example.notable.view.wrapper

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notable.view.screen.RegisterScreen
import com.example.notable.view.viewmodel.AuthViewModel

@Composable
fun RegisterScreenWrapper(
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.registrationSuccess) {
        if (uiState.registrationSuccess) {
            onNavigateToLogin()
        }
    }

    RegisterScreen(
        onBackToLogin = onNavigateToLogin,
        onRegister = { username, password, email, firstName, lastName ->
            viewModel.register(username, password, email, firstName, lastName)
        },
        onLoginClick = onNavigateToLogin
    )

    val context = LocalContext.current
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error toast or handle error display
            // For example, you can use a Snackbar or Toast to show the error
            // show in a toast
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }
}
```

---

## File: `view/wrapper/SettingsScreenWrapper.kt`

```kotlin
package com.example.notable.view.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.notable.view.viewmodel.SettingsViewModel
import com.example.notable.view.screen.SettingsScreen

@Composable
fun SettingsScreenWrapper(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate to login when logged out
    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            navController.navigate("login") {
                popUpTo("main") { inclusive = true }
            }
        }
    }

    // Show logout dialog when needed
    if (uiState.showLogoutDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.hideLogoutDialog() },
            title = { androidx.compose.material3.Text("Confirm Logout") },
            text = { androidx.compose.material3.Text("Are you sure you want to logout?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.hideLogoutDialog()
                        viewModel.logout()
                    }
                ) {
                    androidx.compose.material3.Text("Logout")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.hideLogoutDialog() }
                ) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }

    SettingsScreen(
        userName = uiState.userName,
        userEmail = uiState.userEmail,
        appVersion = uiState.appVersion,
        profileImageUrl = null, // You can add this to your User model if needed
        onBackClick = {
            navController.popBackStack()
        },
        onChangePasswordClick = {
            navController.navigate("change_password")
        },
        onLogOutClick = {
            viewModel.showLogoutDialog()
        }
    )
}
```

---

