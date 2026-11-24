package com.example.rickandmorty.data.repository

import com.example.rickandmorty.data.local.session.SessionStore
import com.example.rickandmorty.data.local.user.UserDao
import com.example.rickandmorty.data.local.user.UserEntity
import com.example.rickandmorty.domain.repository.AuthResult
import com.example.rickandmorty.domain.repository.SessionState
import com.example.rickandmorty.domain.security.PasswordHash
import com.example.rickandmorty.domain.security.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {

    @Test
    fun `registers a user and persists its session`() = runTest {
        val userDao = FakeUserDao()
        val sessionStore = FakeSessionStore()
        val repository = repository(userDao, sessionStore)

        val result = repository.register("  Rick  ", "password")

        assertTrue(result is AuthResult.Success)
        assertEquals("Rick", (result as AuthResult.Success).user.username)
        assertEquals(1L, sessionStore.currentUserId)
        assertEquals("rick", userDao.users.single().normalizedUsername)
        assertEquals(FAKE_HASH.toList(), userDao.users.single().passwordHash.toList())
    }

    @Test
    fun `rejects duplicate usernames ignoring case`() = runTest {
        val userDao = FakeUserDao()
        val repository = repository(userDao, FakeSessionStore())
        repository.register("Morty", "password")

        val duplicate = repository.register("mOrTy", "another-password")

        assertEquals(AuthResult.UsernameTaken, duplicate)
        assertEquals(1, userDao.users.size)
    }

    @Test
    fun `logs in with valid credentials ignoring username case`() = runTest {
        val userDao = FakeUserDao()
        val sessionStore = FakeSessionStore()
        val repository = repository(userDao, sessionStore)
        repository.register("Summer", "password")
        sessionStore.clear()

        val result = repository.login("SUMMER", "password")

        assertTrue(result is AuthResult.Success)
        assertEquals(1L, sessionStore.currentUserId)
    }

    @Test
    fun `returns one generic error for unknown user and wrong password`() = runTest {
        val repository = repository(FakeUserDao(), FakeSessionStore())
        repository.register("Beth", "password")

        assertEquals(AuthResult.InvalidCredentials, repository.login("Unknown", "password"))
        assertEquals(AuthResult.InvalidCredentials, repository.login("Beth", "wrong"))
    }

    @Test
    fun `clears a session whose user no longer exists`() = runTest {
        val sessionStore = FakeSessionStore(initialUserId = 99L)
        val repository = repository(FakeUserDao(), sessionStore)

        val state = repository.sessionState
            .dropWhile { it is SessionState.Loading }
            .first()

        assertEquals(SessionState.Unauthenticated, state)
        assertNull(sessionStore.currentUserId)
    }

    @Test
    fun `logout clears the persisted session`() = runTest {
        val sessionStore = FakeSessionStore(initialUserId = 1L)
        val repository = repository(FakeUserDao(), sessionStore)

        repository.logout()

        assertNull(sessionStore.currentUserId)
    }

    private fun repository(
        userDao: FakeUserDao,
        sessionStore: FakeSessionStore,
    ) = AuthRepositoryImpl(
        userDao = userDao,
        sessionStore = sessionStore,
        passwordHasher = FakePasswordHasher(),
        hashingDispatcher = Dispatchers.Unconfined,
        currentTimeMillis = { 1234L },
    )

    private class FakeUserDao : UserDao {
        val users = mutableListOf<UserEntity>()
        private var nextId = 1L

        override suspend fun insert(user: UserEntity): Long {
            if (users.any { it.normalizedUsername == user.normalizedUsername }) return -1L
            val id = nextId++
            users += user.copy(id = id)
            return id
        }

        override suspend fun findByNormalizedUsername(normalizedUsername: String): UserEntity? =
            users.firstOrNull { it.normalizedUsername == normalizedUsername }

        override suspend fun findById(id: Long): UserEntity? = users.firstOrNull { it.id == id }

        override fun observeById(id: Long): Flow<UserEntity?> = flowOf(users.firstOrNull { it.id == id })
    }

    private class FakeSessionStore(initialUserId: Long? = null) : SessionStore {
        private val state = MutableStateFlow(initialUserId)
        val currentUserId: Long? get() = state.value

        override val userId: Flow<Long?> = state

        override suspend fun setUserId(userId: Long) {
            state.value = userId
        }

        override suspend fun clear() {
            state.value = null
        }
    }

    private class FakePasswordHasher : PasswordHasher {
        override fun hash(password: CharArray) = PasswordHash(
            hash = FAKE_HASH,
            salt = FAKE_SALT,
            iterations = 1,
        )

        override fun matches(
            password: CharArray,
            expectedHash: ByteArray,
            salt: ByteArray,
            iterations: Int,
        ): Boolean = password.concatToString() == "password"
    }

    private companion object {
        val FAKE_HASH = byteArrayOf(1, 2, 3)
        val FAKE_SALT = byteArrayOf(4, 5, 6)
    }
}
