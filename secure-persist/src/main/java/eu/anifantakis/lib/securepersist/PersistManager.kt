package eu.anifantakis.lib.securepersist

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import eu.anifantakis.lib.securepersist.encryption.EncryptionManager
import eu.anifantakis.lib.securepersist.encryption.IEncryptionManager
import eu.anifantakis.lib.securepersist.internal.DataStoreManager
import eu.anifantakis.lib.securepersist.internal.SharedPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Type
import kotlin.reflect.KProperty

class PersistManager(context: Context, keyAlias: String = "keyAlias") {

    private val encryptionManager: IEncryptionManager = EncryptionManager(context, keyAlias)
    internal val sharedPreferencesManager = SharedPreferencesManager(context)
    internal val dataStoreManager = DataStoreManager(context, encryptionManager)
    internal val gson = Gson()

    // Wrapper methods for SharedPreferencesManager

    /**
     * Encrypts and saves a value to SharedPreferences.
     *
     * @param key The key to store the value under.
     * @param value The value to store.
     */
    fun <T> encryptSharedPreference(key: String, value: T) {
        sharedPreferencesManager.put(key, value)
    }

    /**
     * Decrypts and retrieves a value from SharedPreferences.
     *
     * @param key The key to retrieve the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @return The decrypted value.
     */
    fun <T> decryptSharedPreference(key: String, defaultValue: T): T {
        return sharedPreferencesManager.get(key, defaultValue)
    }

    /**
     * Deletes a value from SharedPreferences.
     *
     * @param key The key to delete the value under.
     */
    fun deleteSharedPreference(key: String) {
        sharedPreferencesManager.delete(key)
    }

    // Wrapper methods for DataStoreManager

    /**
     * Saves a value to DataStore.
     *
     * @param key The key to store the value under.
     * @param value The value to store.
     */
    suspend fun <T> putDataStorePreference(key: String, value: T) {
        dataStoreManager.put(key, value)
    }

    /**
     * Retrieves a value from DataStore.
     *
     * @param key The key to retrieve the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @return The retrieved value.
     */
    suspend fun <T : Any> getDataStorePreference(key: String, defaultValue: T): T {
        return dataStoreManager.get(key, defaultValue)
    }

    /**
     * Encrypts and saves a value to DataStore.
     *
     * @param key The key to store the value under.
     * @param value The value to store.
     */
    suspend fun <T> encryptDataStorePreference(key: String, value: T) {
        dataStoreManager.putEncrypted(key, value)
    }

    /**
     * Decrypts and retrieves a value from DataStore.
     *
     * @param key The key to retrieve the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @return The decrypted value.
     */
    suspend fun <T> decryptDataStorePreference(key: String, defaultValue: T): T {
        return dataStoreManager.getEncrypted(key, defaultValue)
    }

    /**
     * Encrypts and saves a value to DataStore without using coroutines.
     *
     * @param key The key to store the value under.
     * @param value The value to store.
     */
    fun <T> encryptDataStorePreferenceSync(key: String, value: T) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            encryptDataStorePreference(key, value)
        }
    }

    /**
     * Decrypts and retrieves a value from DataStore without using coroutines.
     *
     * @param key The key to retrieve the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @return The decrypted value.
     */
    fun <T> decryptDataStorePreferenceSync(key: String, defaultValue: T): T {
        return runBlocking {
            decryptDataStorePreference(key, defaultValue)
        }
    }

    /**
     * Deletes a value from DataStore.
     *
     * @param key The key to delete the value under.
     */
    suspend fun deleteDataStorePreference(key: String) {
        dataStoreManager.delete(key)
    }

    // Wrapper for the preference delegate

    /**
     * Class to handle encrypted preferences using property delegation.
     */
    class EncryptedPreference<T>(
        private val persist: PersistManager,
        private val defaultValue: T,
        private val key: String? = null,
        private val type: Type,
        private val gson: Gson = persist.gson
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val preferenceKey = key?.takeIf { it.isNotEmpty() } ?: property.name
            val storedValue = persist.decryptSharedPreference(preferenceKey, "")

            return if (storedValue.isEmpty()) {
                defaultValue
            } else {
                when (defaultValue) {
                    is Boolean, is Int, is Float, is Long, is Double, is String -> {
                        persist.decryptSharedPreference(preferenceKey, defaultValue)
                    }
                    else -> {
                        // Deserialize JSON string to object using the provided type
                        gson.fromJson(storedValue, type) as T
                    }
                }
            }
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val preferenceKey = key?.takeIf { it.isNotEmpty() } ?: property.name
            when (value) {
                is Boolean, is Int, is Float, is Long, is Double, is String -> {
                    persist.encryptSharedPreference(preferenceKey, value)
                }
                else -> {
                    // Serialize object to JSON string
                    val jsonString = gson.toJson(value)
                    persist.encryptSharedPreference(preferenceKey, jsonString)
                }
            }
        }
    }


    /**
     * Uses delegation to set and get encrypted SharedPreferences.
     * Key to the preference is automatically assigned by the variable name.
     *
     * @param defaultValue The default value to return if the key does not exist.
     * @return An EncryptedPreference instance.
     */
    inline fun <reified T> preference(defaultValue: T): EncryptedPreference<T> = preference(null, defaultValue)

    /**
     * Uses delegation to set and get encrypted SharedPreferences with a specific key.
     *
     * @param key The key to store the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @return An EncryptedPreference instance.
     */
    inline fun <reified T> preference(key: String? = null, defaultValue: T): EncryptedPreference<T> {
        val type: Type = object : TypeToken<T>() {}.type
        return EncryptedPreference(this, defaultValue, key, type)
    }

    // Methods for handling complex objects in SharedPreferences

    /**
     * Saves an object to SharedPreferences by serializing it to JSON.
     *
     * @param key The key under which the object will be stored.
     * @param value The object to be stored.
     */
    fun <T> putObjectSharedPreference(key: String, value: T) {
        try {
            val jsonString = gson.toJson(value)
            sharedPreferencesManager.put(key, jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Retrieves an object from SharedPreferences by deserializing the stored JSON string.
     *
     * @param key The key under which the object is stored.
     * @return The retrieved object, or null if not found or an error occurs.
     */
    internal inline fun <reified T> getObjectSharedPreference(key: String): T? {
        return try {
            val jsonString = sharedPreferencesManager.get(key, "")
            if (jsonString.isEmpty()) {
                null
            } else {
                gson.fromJson<T>(jsonString, object : TypeToken<T>() {}.type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Methods for handling complex objects in DataStore

    /**
     * Saves an object to DataStore by serializing it to JSON and encrypting the JSON string.
     *
     * @param key The key under which the object will be stored.
     * @param value The object to be stored.
     */
    suspend fun <T> putObjectDataStorePreference(key: String, value: T) {
        try {
            val jsonString = gson.toJson(value)
            val encryptedData = encryptionManager.encryptData(jsonString)
            val encryptedBase64 = Base64.encodeToString(encryptedData, Base64.NO_WRAP)
            dataStoreManager.put(key, encryptedBase64)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Retrieves an object from DataStore by decrypting and deserializing the stored JSON string.
     *
     * @param key The key under which the object is stored.
     * @return The retrieved object, or null if not found or an error occurs.
     */
    internal suspend inline fun <reified T> getObjectDataStorePreference(key: String): T? {
        return try {
            val encryptedBase64 = dataStoreManager.get(key, "")
            if (encryptedBase64.isEmpty()) {
                null
            } else {
                val encryptedData = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                val decryptedJson = encryptionManager.decryptData(encryptedData)
                gson.fromJson<T>(decryptedJson, object : TypeToken<T>() {}.type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves an object to DataStore by serializing it to JSON and encrypting the JSON string without exposing Coroutines
     *
     * @param key The key under which the object will be stored.
     * @param value The object to be stored.
     */
    fun <T> putObjectDataStorePreferenceSync(key: String, value: T) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val jsonString = gson.toJson(value)
                val encryptedData = encryptionManager.encryptData(jsonString)
                val encryptedBase64 = Base64.encodeToString(encryptedData, Base64.NO_WRAP)
                dataStoreManager.put(key, encryptedBase64)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Retrieves an object from DataStore by decrypting and deserializing the stored JSON string without exposing Coroutines.
     *
     * @param key The key under which the object is stored.
     * @return The retrieved object, or null if not found or an error occurs.
     */
    internal inline fun <reified T> getObjectDataStorePreferenceSync(key: String): T? {
        return runBlocking {
            try {
                val encryptedBase64 = dataStoreManager.get(key, "")
                if (encryptedBase64.isEmpty()) {
                    null
                } else {
                    val encryptedData = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                    val decryptedJson = encryptionManager.decryptData(encryptedData)
                    gson.fromJson<T>(decryptedJson, object : TypeToken<T>() {}.type)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
