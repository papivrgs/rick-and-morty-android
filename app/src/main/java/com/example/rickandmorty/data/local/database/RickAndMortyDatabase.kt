package com.example.rickandmorty.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.rickandmorty.data.local.user.UserDao
import com.example.rickandmorty.data.local.user.UserEntity

@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RickAndMortyDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
