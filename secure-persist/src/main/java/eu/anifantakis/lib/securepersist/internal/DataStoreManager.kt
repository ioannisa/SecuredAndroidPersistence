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

/**
 * Manages encrypted and unencrypted preferences using Android's DataStore.
 *
 * This class provides methods to store and retrieve data from DataStore,
 * with support for encryption using an [IEncryptionManager]. It handles both primitive
 * and complex data types, allowing for seamless integration with your application's data storage needs.
 *
 * @property encryptionManager The encryption manager used for encrypting and decrypting data.
 */
class DataStoreManager(context: Context, private val encryptionManager: IEncryptionManager) {

    private val dataStore = context.dataStore
    private val gson = Gson()

    /**
     * Retrieves a value from DataStore, optionally decrypting it.
     *
     * @param key The key to retrieve the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @param encrypted Whether the value is stored encrypted. Defaults to `true`.
     * @return The retrieved value.
     */
    suspend fun <T> get(key: String, defaultValue: T, encrypted: Boolean = true): T {
        return if (encrypted) {
            getEncrypted(key, defaultValue)
        }
        else {
            getUnencrypted(key, defaultValue)
        }
    }

    /**
     * Retrieves a value from DataStore without using coroutines.
     * This is a **blocking** function.
     *
     * @param key The key to retrieve the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @param encrypted Whether the value is stored encrypted. Defaults to `true`.
     * @return The retrieved value.
     */
    fun <T> getDirect(key: String, defaultValue: T, encrypted: Boolean = true): T {
        return runBlocking {
            get(key, defaultValue, encrypted)
        }
    }

    /**
     * Saves a value to DataStore, optionally encrypting it.
     *
     * @param key The key to store the value under.
     * @param value The value to store.
     * @param encrypted Whether to encrypt the value before storing. Defaults to `true`.
     */
    suspend fun <T> put(key: String, value: T, encrypted: Boolean = true) {
        if (encrypted) {
            putEncrypted(key, value)
        } else {
            putUnencrypted(key, value)
        }
    }

    /**
     * Saves a value to DataStore without using coroutines.
     * This function is **non-blocking**.
     *
     * @param key The key to store the value under.
     * @param value The value to store.
     * @param encrypted Whether to encrypt the value before storing. Defaults to `true`.
     */
    fun <T> putDirect(key: String, value: T, encrypted: Boolean = true) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            put(key, value, encrypted)
        }
    }

    /**
     * Deletes a value from DataStore.
     *
     * @param key The key of the value to delete.
     */
    suspend fun delete(key: String) {
        val dataKey = stringPreferencesKey(key)
        dataStore.edit { preferences ->
            preferences.remove(dataKey)
        }
    }

    /**
     * Deletes a value from DataStore without using coroutines.
     * This function is **non-blocking**.
     *
     * @param key The key of the value to delete.
     */
    fun deleteDirect(key: String) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            delete(key)
        }
    }

    private suspend fun <T> putUnencrypted(key: String, value: T) {
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
    private suspend fun <T> getUnencrypted(key: String, defaultValue: T): T {
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
                        gson.fromJson(jsonString, (defaultValue as Any)::class.java) as T
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
