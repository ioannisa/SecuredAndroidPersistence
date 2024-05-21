package eu.anifantakis.lib.securepersist.internal

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import eu.anifantakis.lib.securepersist.EncryptionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class DataStoreManager(private val context: Context, private val encryptionManager: EncryptionManager) {

    private val Context.dataStore by preferencesDataStore("encrypted_datastore")

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> put(key: String, value: T) {
        val preferencesKey: Preferences.Key<T> = when (value) {
            is Boolean -> booleanPreferencesKey(key) as Preferences.Key<T>
            is Int -> intPreferencesKey(key) as Preferences.Key<T>
            is Float -> floatPreferencesKey(key) as Preferences.Key<T>
            is Long -> longPreferencesKey(key) as Preferences.Key<T>
            is String -> stringPreferencesKey(key) as Preferences.Key<T>
            else -> throw IllegalArgumentException("Unsupported type")
        }

        context.dataStore.edit { preferences ->
            preferences[preferencesKey] = value
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> get(key: String, defaultValue: T): T {
        val preferencesKey: Preferences.Key<*> = when (defaultValue) {
            is Boolean -> booleanPreferencesKey(key)
            is Int -> intPreferencesKey(key)
            is Float -> floatPreferencesKey(key)
            is Long -> longPreferencesKey(key)
            is String -> stringPreferencesKey(key)
            else -> throw IllegalArgumentException("Unsupported type")
        }

        val preferences = context.dataStore.data.map { preferences ->
            when (defaultValue) {
                is Boolean -> preferences[preferencesKey as Preferences.Key<Boolean>] ?: defaultValue
                is Int -> preferences[preferencesKey as Preferences.Key<Int>] ?: defaultValue
                is Float -> preferences[preferencesKey as Preferences.Key<Float>] ?: defaultValue
                is Long -> preferences[preferencesKey as Preferences.Key<Long>] ?: defaultValue
                is String -> preferences[preferencesKey as Preferences.Key<String>] ?: defaultValue
                else -> throw IllegalArgumentException("Unsupported type")
            }
        }.first()

        return preferences as T
    }

    suspend fun <T> putEncrypted(key: String, value: T) {
        val dataKey = stringPreferencesKey(key)
        val encryptedValue = encryptionManager.encryptValue(value)
        context.dataStore.edit { preferences ->
            preferences[dataKey] = encryptedValue
        }
    }

    suspend fun <T> getEncrypted(key: String, defaultValue: T): T {
        val dataKey = stringPreferencesKey(key)
        val encryptedValue = context.dataStore.data.map { preferences ->
            preferences[dataKey]
        }.first() ?: return defaultValue
        return encryptionManager.decryptValue(encryptedValue, defaultValue)
    }

    suspend fun delete(key: String) {
        val dataKey = stringPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences.remove(dataKey)
        }
    }
}