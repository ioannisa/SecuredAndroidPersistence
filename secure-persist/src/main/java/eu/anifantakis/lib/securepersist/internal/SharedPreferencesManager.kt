package eu.anifantakis.lib.securepersist.internal

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import kotlin.reflect.KProperty

internal class SharedPreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences
    private val gson = Gson()

    init {
        // Create or retrieve the MasterKey for encryption
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Initialize EncryptedSharedPreferences
        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "encrypted_prefs_filename",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Stores a value in EncryptedSharedPreferences.
     *
     * @param key The key to store the value under.
     * @param value The value to store.
     */
    fun <T> put(key: String, value: T) {
        when (value) {
            is Boolean, is Int, is Float, is Long, is String -> {
                sharedPreferences.edit(commit = true) {
                    when (value) {
                        is Boolean -> putBoolean(key, value)
                        is Int -> putInt(key, value)
                        is Float -> putFloat(key, value)
                        is Long -> putLong(key, value)
                        is String -> putString(key, value)
                    }
                }
            }
            is Double -> {
                sharedPreferences.edit(commit = true) {
                    putString(key, value.toString())
                }
            }
            else -> {
                // Serialize complex types to JSON string
                val jsonString = gson.toJson(value)
                sharedPreferences.edit(commit = true) {
                    putString(key, jsonString)
                }
            }
        }
    }

    /**
     * Retrieves a value from EncryptedSharedPreferences.
     *
     * @param key The key to retrieve the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @return The retrieved value.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is Boolean -> sharedPreferences.getBoolean(key, defaultValue) as T
            is Int -> sharedPreferences.getInt(key, defaultValue) as T
            is Float -> sharedPreferences.getFloat(key, defaultValue) as T
            is Long -> sharedPreferences.getLong(key, defaultValue) as T
            is String -> sharedPreferences.getString(key, defaultValue) as T
            is Double -> {
                val stringValue = sharedPreferences.getString(key, null)
                stringValue?.toDouble() as T? ?: defaultValue
            }
            else -> {
                // Deserialize JSON string back to object
                val jsonString = sharedPreferences.getString(key, null) ?: return defaultValue
                try {
                    gson.fromJson(jsonString, defaultValue!!::class.java) as T
                } catch (e: Exception) {
                    defaultValue
                }
            }
        }
    }

    /**
     * Deletes a value from EncryptedSharedPreferences.
     *
     * @param key The key to delete the value under.
     */
    fun delete(key: String) {
        sharedPreferences.edit(commit = true) {
            remove(key)
        }
    }

    /**
     * Class to handle encrypted preferences using property delegation.
     */
    internal class EncryptedPreference<T>(
        private val sharedPreferencesManager: SharedPreferencesManager,
        private val defaultValue: T,
        private val key: String? = null
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val preferenceKey = key ?: property.name
            return sharedPreferencesManager.get(preferenceKey, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val preferenceKey = key ?: property.name
            sharedPreferencesManager.put(preferenceKey, value)
        }
    }

    /**
     * Creates an EncryptedPreference with the property name as the key.
     *
     * @param defaultValue The default value to return if the key does not exist.
     * @return An EncryptedPreference instance.
     */
    fun <T> preference(defaultValue: T): EncryptedPreference<T> = EncryptedPreference(this, defaultValue)

    /**
     * Creates an EncryptedPreference with a specific key.
     *
     * @param key The key to store the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @return An EncryptedPreference instance.
     */
    fun <T> preference(key: String, defaultValue: T): EncryptedPreference<T> = EncryptedPreference(this, defaultValue, key)
}
