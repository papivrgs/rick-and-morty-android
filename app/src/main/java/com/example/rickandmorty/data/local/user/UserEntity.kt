package com.example.rickandmorty.data.local.user

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["normalizedUsername"], unique = true)],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val normalizedUsername: String,
    val passwordHash: ByteArray,
    val passwordSalt: ByteArray,
    val passwordIterations: Int,
    val createdAtEpochMillis: Long,
)
