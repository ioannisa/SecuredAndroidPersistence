package eu.anifantakis.lib.securepersist.internal

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import eu.anifantakis.lib.securepersist.EncryptedPreference
import eu.anifantakis.lib.securepersist.SecurePersistSolution
import eu.anifantakis.lib.securepersist.encryption.IEncryptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty

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
class DataStoreManager(context: Context, private val encryptionManager: IEncryptionManager, private val gson: Gson) : SecurePersistSolution{

    private val dataStore = context.dataStore

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
            is Number -> {
                when {
                    value is Int || (value is Long && value in Int.MIN_VALUE..Int.MAX_VALUE) ->
                        intPreferencesKey(key)
                    value is Long -> longPreferencesKey(key)
                    value is Float -> floatPreferencesKey(key)
                    value is Double -> doublePreferencesKey(key)
                    else -> stringPreferencesKey(key)
                }
            }
            is String -> stringPreferencesKey(key)
            else -> stringPreferencesKey(key)
        } as Preferences.Key<Any>

        val storedValue: Any = when (value) {
            is Boolean -> value
            is Number -> {
                when {
                    value is Int || (value is Long && value in Int.MIN_VALUE..Int.MAX_VALUE) ->
                        value.toInt()
                    value is Long -> value
                    value is Float -> value
                    value is Double -> value
                    else -> value
                }
            }
            is String -> value
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
                is Int -> {
                    when (storedValue) {
                        is Int -> storedValue
                        is Long -> if (storedValue in Int.MIN_VALUE..Int.MAX_VALUE) storedValue.toInt() else defaultValue
                        else -> defaultValue
                    }
                }
                is Long -> {
                    when (storedValue) {
                        is Long -> storedValue
                        is Int -> storedValue.toLong()
                        else -> defaultValue
                    }
                }
                is Float -> storedValue as? Float ?: defaultValue
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

    /**
     * Creates a preference delegate for storing Encrypted DataStore Preferences
     *
     * **Usage:**
     *   ```kotlin
     *   val myPref by persistManager.sharedPrefs.preference("default value", key = "myKey")
     *   ```
     *
     * **Notes:**
     * - If `key` is null or empty, the property name will be used as the key.
     * - When using DataStore, you can specify whether the data should be encrypted by choosing the appropriate `StorageType`.
     *
     * @param defaultValue The default value to be used if no value is stored for the preference.
     * @param key The key for the preference. If null or empty, the property name will be used.
     * @return An `EncryptedSharedPreference`
     */
    inline fun <reified T : Any> preference(defaultValue: T, key: String? = null, encrypted: Boolean = true): EncryptedPreference<T> {
        return EncryptedDataStore(defaultValue, key, encrypted)
    }

    /**
     * Class to handle encrypted DataStore preferences using property delegation.
     *
     * @param T The type of the preference value.
     * @property defaultValue The default value for the preference.
     * @property key The key for the preference. If null or empty, the property name will be used.
     * @property encrypted Whether the preference should be stored encrypted. Defaults to true.
     */
    inner class EncryptedDataStore<T : Any>(
        private val defaultValue: T,
        private val key: String? = null,
        private val encrypted: Boolean = true
    ) : EncryptedPreference<T> {
        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {

            val preferenceKey = key?.takeIf { it.isNotEmpty() } ?: property.name
            val storedValue = getDirect<T>(preferenceKey, defaultValue, encrypted)
            return storedValue
        }

        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val preferenceKey = key?.takeIf { it.isNotEmpty() } ?: property.name
            putDirect(preferenceKey, value, encrypted)
        }
    }
}
