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
