package com.centelles.titan.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.centelles.titan.logic.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "game_state")

class GameRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val gameStateKey = stringPreferencesKey("state")

    val gameStateFlow: Flow<GameState?> = context.dataStore.data.map { preferences ->
        val stateJson = preferences[gameStateKey]
        if (stateJson != null) {
            try {
                json.decodeFromString<GameState>(stateJson)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun saveGameState(state: GameState) {
        context.dataStore.edit { preferences ->
            preferences[gameStateKey] = json.encodeToString(state)
        }
    }
}
