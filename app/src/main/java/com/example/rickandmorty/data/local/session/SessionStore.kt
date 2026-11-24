package com.example.rickandmorty.data.local.session

import kotlinx.coroutines.flow.Flow

interface SessionStore {
    val userId: Flow<Long?>

    suspend fun setUserId(userId: Long)

    suspend fun clear()
}
