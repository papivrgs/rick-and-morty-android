package com.example.rickandmorty.data.local.user

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.rickandmorty.data.local.database.RickAndMortyDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    private lateinit var database: RickAndMortyDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            RickAndMortyDatabase::class.java,
        ).build()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertsAndFindsAUser() = runTest {
        val id = userDao.insert(user(normalizedUsername = "rick"))

        val stored = userDao.findById(id)

        assertNotNull(stored)
        assertEquals("Rick", stored?.username)
        assertEquals("rick", stored?.normalizedUsername)
    }

    @Test
    fun rejectsADuplicateNormalizedUsername() = runTest {
        val first = userDao.insert(user(normalizedUsername = "morty"))
        val duplicate = userDao.insert(user(username = "MORTY", normalizedUsername = "morty"))

        assertEquals(1L, first)
        assertEquals(-1L, duplicate)
    }

    private fun user(
        username: String = "Rick",
        normalizedUsername: String,
    ) = UserEntity(
        username = username,
        normalizedUsername = normalizedUsername,
        passwordHash = byteArrayOf(1, 2),
        passwordSalt = byteArrayOf(3, 4),
        passwordIterations = 100,
        createdAtEpochMillis = 1L,
    )
}
