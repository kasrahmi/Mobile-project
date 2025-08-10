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
