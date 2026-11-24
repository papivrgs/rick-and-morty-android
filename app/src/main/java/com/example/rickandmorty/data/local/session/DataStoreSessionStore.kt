package com.example.rickandmorty.data.local.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "session",
)

class DataStoreSessionStore(
    context: Context,
) : SessionStore {
    private val dataStore = context.applicationContext.sessionDataStore

    override val userId: Flow<Long?> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences -> preferences[USER_ID] }

    override suspend fun setUserId(userId: Long) {
        dataStore.edit { preferences -> preferences[USER_ID] = userId }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(USER_ID) }
    }

    private companion object {
        val USER_ID = longPreferencesKey("authenticated_user_id")
    }
}
