package eu.anifantakis.lib.securepersist.internal

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import eu.anifantakis.lib.securepersist.encryption.IEncryptionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("encrypted_datastore")

internal class DataStoreManager(context: Context, private val encryptionManager: IEncryptionManager) {

    // Use the same DataStore instance across all calls
    private val dataStore = context.dataStore
    private val gson = Gson()

    /**
     * Stores a value in DataStore.
     *
     * @param key The key to store the value under.
     * @param value The value to store.
     */
    suspend fun <T> put(key: String, value: T) {
        val preferencesKey: Preferences.Key<Any> = when (value) {
            is Boolean -> booleanPreferencesKey(key)
            is Int -> intPreferencesKey(key)
            is Float -> floatPreferencesKey(key)
            is Long -> longPreferencesKey(key)
            is String -> stringPreferencesKey(key)
            is Double -> doublePreferencesKey(key)
            else -> stringPreferencesKey(key)
        } as Preferences.Key<Any>

        val storedValue: Any = when (value) {
            is Boolean, is Int, is Float, is Long, is String, is Double -> value
            else -> gson.toJson(value)
        }

        dataStore.edit { preferences ->
            preferences[preferencesKey] = storedValue
        }
    }

    /**
     * Retrieves a value from DataStore.
     *
     * @param key The key to retrieve the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @return The retrieved value.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> get(key: String, defaultValue: T): T {
        val preferencesKey: Preferences.Key<Any> = when (defaultValue) {
            is Boolean -> booleanPreferencesKey(key)
            is Int -> intPreferencesKey(key)
            is Float -> floatPreferencesKey(key)
            is Long -> longPreferencesKey(key)
            is String -> stringPreferencesKey(key)
            is Double -> doublePreferencesKey(key)
            else -> stringPreferencesKey(key)
        } as Preferences.Key<Any>

        val preferences = dataStore.data.map { preferences ->
            val storedValue = preferences[preferencesKey]
            when (defaultValue) {
                is Boolean -> storedValue as? Boolean ?: defaultValue
                is Int -> storedValue as? Int ?: defaultValue
                is Float -> storedValue as? Float ?: defaultValue
                is Long -> storedValue as? Long ?: defaultValue
                is String -> storedValue as? String ?: defaultValue
                is Double -> storedValue as? Double ?: defaultValue
                else -> {
                    val jsonString = storedValue as? String ?: return@map defaultValue
                    try {
                        gson.fromJson(jsonString, defaultValue::class.java) as T
                    } catch (e: Exception) {
                        defaultValue
                    }
                }
            }
        }.first()

        return preferences as T
    }

    /**
     * Encrypts and stores a value in DataStore.
     *
     * @param key The key to store the value under.
     * @param value The value to store.
     */
    suspend fun <T> putEncrypted(key: String, value: T) {
        val dataKey = stringPreferencesKey(key)
        val encryptedValue = encryptionManager.encryptValue(value)
        dataStore.edit { preferences ->
            preferences[dataKey] = encryptedValue
        }
    }

    /**
     * Retrieves and decrypts a value from DataStore.
     *
     * @param key The key to retrieve the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @return The decrypted value.
     */
    suspend fun <T> getEncrypted(key: String, defaultValue: T): T {
        val dataKey = stringPreferencesKey(key)
        val encryptedValue = dataStore.data.map { preferences ->
            preferences[dataKey]
        }.first() ?: return defaultValue
        return encryptionManager.decryptValue(encryptedValue, defaultValue)
    }

    /**
     * Deletes a value from DataStore.
     *
     * @param key The key to delete the value under.
     */
    suspend fun delete(key: String) {
        val dataKey = stringPreferencesKey(key)
        dataStore.edit { preferences ->
            preferences.remove(dataKey)
        }
    }
}
