package com.example.rickandmorty.data.repository

import com.example.rickandmorty.data.local.session.SessionStore
import com.example.rickandmorty.data.local.user.UserDao
import com.example.rickandmorty.data.local.user.UserEntity
import com.example.rickandmorty.domain.model.AuthenticatedUser
import com.example.rickandmorty.domain.repository.AuthRepository
import com.example.rickandmorty.domain.repository.AuthResult
import com.example.rickandmorty.domain.repository.SessionState
import com.example.rickandmorty.domain.security.PasswordHasher
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val sessionStore: SessionStore,
    private val passwordHasher: PasswordHasher,
    private val hashingDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : AuthRepository {

    override val sessionState: Flow<SessionState> = sessionStore.userId
        .map { userId ->
            if (userId == null) {
                SessionState.Unauthenticated
            } else {
                val user = userDao.findById(userId)
                if (user == null) {
                    sessionStore.clear()
                    SessionState.Unauthenticated
                } else {
                    SessionState.Authenticated(user.toDomain())
                }
            }
        }
        .onStart { emit(SessionState.Loading) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(SessionState.Unauthenticated)
        }
        .distinctUntilChanged()

    override suspend fun login(username: String, password: String): AuthResult {
        return try {
            val user = userDao.findByNormalizedUsername(username.normalizedForLookup())
                ?: return AuthResult.InvalidCredentials
            val passwordChars = password.toCharArray()
            val matches = try {
                withContext(hashingDispatcher) {
                    passwordHasher.matches(
                        password = passwordChars,
                        expectedHash = user.passwordHash,
                        salt = user.passwordSalt,
                        iterations = user.passwordIterations,
                    )
                }
            } finally {
                passwordChars.fill('\u0000')
            }
            if (!matches) return AuthResult.InvalidCredentials

            sessionStore.setUserId(user.id)
            AuthResult.Success(user.toDomain())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            AuthResult.StorageError
        }
    }

    override suspend fun register(username: String, password: String): AuthResult {
        return try {
            val displayUsername = Normalizer.normalize(username.trim(), Normalizer.Form.NFKC)
            val normalizedUsername = displayUsername.normalizedForLookup()
            if (userDao.findByNormalizedUsername(normalizedUsername) != null) {
                return AuthResult.UsernameTaken
            }

            val passwordChars = password.toCharArray()
            val passwordHash = try {
                withContext(hashingDispatcher) { passwordHasher.hash(passwordChars) }
            } finally {
                passwordChars.fill('\u0000')
            }
            val userId = userDao.insert(
                UserEntity(
                    username = displayUsername,
                    normalizedUsername = normalizedUsername,
                    passwordHash = passwordHash.hash,
                    passwordSalt = passwordHash.salt,
                    passwordIterations = passwordHash.iterations,
                    createdAtEpochMillis = currentTimeMillis(),
                ),
            )
            if (userId == INSERT_CONFLICT) return AuthResult.UsernameTaken

            sessionStore.setUserId(userId)
            AuthResult.Success(AuthenticatedUser(id = userId, username = displayUsername))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            AuthResult.StorageError
        }
    }

    override suspend fun logout() {
        sessionStore.clear()
    }

    private fun UserEntity.toDomain() = AuthenticatedUser(
        id = id,
        username = username,
    )

    private fun String.normalizedForLookup(): String = Normalizer
        .normalize(trim(), Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)

    private companion object {
        const val INSERT_CONFLICT = -1L
    }
}
