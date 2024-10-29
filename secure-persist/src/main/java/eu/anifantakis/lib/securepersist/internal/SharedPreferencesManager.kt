package eu.anifantakis.lib.securepersist.internal

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson

/**
 * Manages encrypted shared preferences using Android's `EncryptedSharedPreferences`.
 *
 * This class provides methods to securely store, retrieve, and delete preferences using
 * `EncryptedSharedPreferences`. It handles both primitive data types and complex objects,
 * serializing complex objects to JSON strings using Gson.
 *
 * @constructor Creates an instance of [SharedPreferencesManager] with encrypted shared preferences.
 * @param context The application context.
 */
class SharedPreferencesManager(context: Context) {

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
     * Stores a value in `EncryptedSharedPreferences`.
     *
     * This method supports storing primitive types directly, and complex objects
     * by serializing them to JSON strings using Gson.
     *
     * @param key The key under which the value is stored.
     * @param value The value to store. Supported types are `Boolean`, `Int`, `Float`, `Long`, `String`, `Double`,
     *              and complex objects (which will be serialized to JSON).
     * @param T The type of the value.
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
     * Retrieves a value from `EncryptedSharedPreferences`.
     *
     * This method supports retrieving primitive types directly, and complex objects
     * by deserializing them from JSON strings using Gson.
     *
     * @param key The key under which the value is stored.
     * @param defaultValue The default value to return if the key does not exist or deserialization fails.
     *                      This parameter also determines the type of the returned value.
     * @param T The type of the value.
     * @return The retrieved value, or [defaultValue] if the key does not exist or deserialization fails.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, defaultValue: T): T {

        return when (defaultValue) {
            is Boolean -> sharedPreferences.getBoolean(key, defaultValue) as T
            is Int ->  sharedPreferences.getInt(key, defaultValue) as T
            is Float -> sharedPreferences.getFloat(key, defaultValue) as T
            is Long -> {
                if (defaultValue in Int.MIN_VALUE..Int.MAX_VALUE) {
                    val customDefaultValue = if (defaultValue<Int.MAX_VALUE-2) defaultValue.toInt()+1 else defaultValue.toInt()-1
                    val value = sharedPreferences.getInt(key, customDefaultValue) as T
                    if (value == customDefaultValue) {
                        sharedPreferences.getLong(key, defaultValue) as T
                    } else {
                        value
                    }
                } else {
                    sharedPreferences.getLong(key, defaultValue) as T
                }
            }
            is String ->  sharedPreferences.getString(key, defaultValue) as T
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
     * Deletes a value from `EncryptedSharedPreferences`.
     *
     * @param key The key under which the value is stored.
     */
    fun delete(key: String) {
        sharedPreferences.edit(commit = true) {
            remove(key)
        }
    }
}
