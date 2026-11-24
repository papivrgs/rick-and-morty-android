package com.example.rickandmorty.data.security

import java.security.SecureRandom
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Pbkdf2PasswordHasherTest {
    private val hasher = Pbkdf2PasswordHasher(
        secureRandom = SecureRandom(),
        iterations = 10_000,
    )

    @Test
    fun `matches the original password and rejects another password`() {
        val passwordHash = hasher.hash("safe-password".toCharArray())

        assertTrue(
            hasher.matches(
                "safe-password".toCharArray(),
                passwordHash.hash,
                passwordHash.salt,
                passwordHash.iterations,
            ),
        )
        assertFalse(
            hasher.matches(
                "wrong-password".toCharArray(),
                passwordHash.hash,
                passwordHash.salt,
                passwordHash.iterations,
            ),
        )
    }

    @Test
    fun `uses an individual salt for every password`() {
        val first = hasher.hash("same-password".toCharArray())
        val second = hasher.hash("same-password".toCharArray())

        assertFalse(first.salt.contentEquals(second.salt))
        assertFalse(first.hash.contentEquals(second.hash))
    }

    @Test
    fun `does not store the password bytes as the derived hash`() {
        val password = "safe-password"
        val passwordHash = hasher.hash(password.toCharArray())

        assertFalse(passwordHash.hash.contentEquals(password.encodeToByteArray()))
    }
}
