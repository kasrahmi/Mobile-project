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
