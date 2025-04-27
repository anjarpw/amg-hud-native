package com.haskell.amghud

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "amghud_app_preferences")

interface GenericPreferencesRepository {
    suspend fun <T> set(keyName: String, value: T)
    suspend fun <T> get(keyName: String, defaultValue: T): Flow<T>
    suspend fun clear()
}

class DataStorePreferencesRepository(private val context: Context) : GenericPreferencesRepository {

    override suspend fun <T> set(keyName: String, value: T) {
        context.dataStore.edit { preferences ->
            val key = when (value) {
                is Int -> {
                    preferences[intPreferencesKey(keyName)] = value
                    intPreferencesKey(keyName)
                }

                is String -> {
                    preferences[stringPreferencesKey(keyName)] = value
                    stringPreferencesKey(keyName)
                }

                is Boolean -> {
                    preferences[booleanPreferencesKey(keyName)] = value
                    booleanPreferencesKey(keyName)
                }

                is Float -> {
                    preferences[floatPreferencesKey(keyName)] = value
                    floatPreferencesKey(keyName)
                }

                is Long -> {
                    preferences[longPreferencesKey(keyName)] = value
                    longPreferencesKey(keyName)
                }

                is Double -> {
                    preferences[doublePreferencesKey(keyName)] = value
                    doublePreferencesKey(keyName)
                }

                else -> throw IllegalArgumentException("Unsupported type: ${value?.let { it::class.java.name }}")
            } as Preferences.Key<*>
            // The assignment is now done within the when block
        }
    }

    override suspend fun <T> get(keyName: String, defaultValue: T): Flow<T> {
        return context.dataStore.data.map { preferences ->
            val key = when (defaultValue) {
                is Int -> intPreferencesKey(keyName)
                is String -> stringPreferencesKey(keyName)
                is Boolean -> booleanPreferencesKey(keyName)
                is Float -> floatPreferencesKey(keyName)
                is Long -> longPreferencesKey(keyName)
                is Double -> doublePreferencesKey(keyName)
                else -> throw IllegalArgumentException("Unsupported type: ${defaultValue?.let { it::class.java.name }}")
            } as Preferences.Key<T>
            preferences[key] ?: defaultValue
        }
    }

    override suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}