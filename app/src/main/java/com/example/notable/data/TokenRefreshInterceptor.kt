package com.example.notable.data

import android.util.Log
import com.example.notable.data.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TokenRefreshInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val apiProvider: Provider<NotableApi> // ✅ Use Provider to break circular dependency
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()

        Log.d("intercept", "1")

        // Check if this is a token refresh request (avoid infinite loop)
        if (originalRequest.url.encodedPath.contains("token/refresh")) {
            return chain.proceed(originalRequest)
        }

        // Add current token to all requests
        val currentToken = tokenManager.getAccessToken()
        val requestWithToken = if (currentToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        } else {
            originalRequest
        }

        // Check if token needs refresh BEFORE making the request
        if (tokenManager.isTokenExpiringSoon()) {

            Log.d("intercept", "2")
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                try {
                    val api = apiProvider.get() // ✅ Get API instance when needed
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

        return chain.proceed(requestWithToken)
    }
}
