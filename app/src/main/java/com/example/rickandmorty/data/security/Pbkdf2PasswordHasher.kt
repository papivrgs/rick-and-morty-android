package com.example.rickandmorty.data.security

import com.example.rickandmorty.domain.security.PasswordHash
import com.example.rickandmorty.domain.security.PasswordHasher
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class Pbkdf2PasswordHasher(
    private val secureRandom: SecureRandom = SecureRandom(),
    private val iterations: Int = DEFAULT_ITERATIONS,
) : PasswordHasher {

    override fun hash(password: CharArray): PasswordHash {
        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        return PasswordHash(
            hash = deriveKey(password, salt, iterations),
            salt = salt,
            iterations = iterations,
        )
    }

    override fun matches(
        password: CharArray,
        expectedHash: ByteArray,
        salt: ByteArray,
        iterations: Int,
    ): Boolean = MessageDigest.isEqual(
        deriveKey(password, salt, iterations),
        expectedHash,
    )

    private fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray {
        val specification = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(specification).encoded
        } finally {
            specification.clearPassword()
        }
    }

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val DEFAULT_ITERATIONS = 210_000
        const val SALT_BYTES = 16
        const val KEY_LENGTH_BITS = 256
    }
}
