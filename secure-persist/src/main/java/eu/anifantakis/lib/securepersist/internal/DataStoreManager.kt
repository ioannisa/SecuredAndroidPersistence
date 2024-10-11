package eu.anifantakis.lib.securepersist.internal

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import eu.anifantakis.lib.securepersist.encryption.IEncryptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore("encrypted_datastore")

class DataStoreManager(context: Context, private val encryptionManager: IEncryptionManager) {

    private val dataStore = context.dataStore
    private val gson = Gson()

    /**
     * Retrieves a value from DataStore.
     *
     * @param key The key to retrieve the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @param useEncryption Whether to use encryption for the value (default true)
     * @return The retrieved value.
     */
    suspend fun <T : Any> get(key: String, defaultValue: T, useEncryption: Boolean = true): T {
        return if (useEncryption) {
            getEncrypted(key, defaultValue)
        }
        else {
            get(key, defaultValue)
        }
    }

    /**
     * Retrieves a value from DataStore without using coroutines.
     * This is BLOCKING function
     *
     * @param key The key to retrieve the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @param useEncryption Whether to use encryption for the value (default true)
     * @return The decrypted value.
     */
    fun <T: Any> getDirect(key: String, defaultValue: T, useEncryption: Boolean = true): T {
        return runBlocking {
            get(key, defaultValue, useEncryption)
        }
    }

    /**
     * Saves a value to DataStore.
     *
     * @param key The key to store the value under.
     * @param value The value to store.
     * @param useEncryption Whether to use encryption for the value (default true)
     */
    suspend fun <T> put(key: String, value: T, useEncryption: Boolean = true) {
        if (useEncryption) {
            putEncrypted(key, value)
        } else {
            put(key, value)
        }
    }

    /**
     * Saves a value to DataStore without using coroutines.
     * This function is NON-BLOCKING
     *
     * @param key The key to store the value under.
     * @param value The value to store.
     * @param useEncryption Whether to use encryption for the value (default true)
     */
    fun <T> putDirect(key: String, value: T, useEncryption: Boolean = true) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            put(key, value, useEncryption)
        }
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

    /**
     * Deletes a value from DataStore without using coroutines.
     * This function is NON-BLOCKING
     *
     * @param key The key to delete the value under.
     */
    fun deleteDirect(key: String) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            delete(key)
        }
    }

    private suspend fun <T> put(key: String, value: T) {
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

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> get(key: String, defaultValue: T): T {
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

    private suspend fun <T> putEncrypted(key: String, value: T) {
        val dataKey = stringPreferencesKey(key)
        val encryptedValue = encryptionManager.encryptValue(value)
        dataStore.edit { preferences ->
            preferences[dataKey] = encryptedValue
        }
    }

    private suspend fun <T> getEncrypted(key: String, defaultValue: T): T {
        val dataKey = stringPreferencesKey(key)
        val encryptedValue = dataStore.data.map { preferences ->
            preferences[dataKey]
        }.first() ?: return defaultValue
        return encryptionManager.decryptValue(encryptedValue, defaultValue)
    }
}
