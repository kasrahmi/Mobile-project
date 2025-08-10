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
