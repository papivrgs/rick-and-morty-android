package com.example.rickandmorty.domain.security

interface PasswordHasher {
    fun hash(password: CharArray): PasswordHash

    fun matches(
        password: CharArray,
        expectedHash: ByteArray,
        salt: ByteArray,
        iterations: Int,
    ): Boolean
}

data class PasswordHash(
    val hash: ByteArray,
    val salt: ByteArray,
    val iterations: Int,
)
