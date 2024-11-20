package eu.anifantakis.lib.securepersist.internal

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import eu.anifantakis.lib.securepersist.EncryptedPreference
import eu.anifantakis.lib.securepersist.SecurePersistSolution
import java.lang.reflect.Type
import java.security.KeyStore
import kotlin.reflect.KProperty

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
class SharedPreferencesManager(private val context: Context, private val gson: Gson) : SecurePersistSolution {
    private val sharedPreferences: SharedPreferences

    companion object {
        private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"
        private const val PREFS_FILENAME = "encrypted_prefs_filename"
    }

    init {
        sharedPreferences = try {
            initializeEncryptedPreferences()
        } catch (e: Exception) {
            when {
                isKeystoreError(e) -> {
                    // Clean up the KeyStore entry and preferences file
                    cleanupKeyStore()
                    context.deleteSharedPreferences(PREFS_FILENAME)
                    // Try again
                    initializeEncryptedPreferences()
                }
                else -> throw e
            }
        }
    }

    private fun isKeystoreError(e: Exception): Boolean {
        return e.cause?.toString()?.contains("VERIFICATION_FAILED") == true ||
                e.toString().contains("KeyStoreException") ||
                e.cause?.toString()?.contains("KeyStoreException") == true ||
                e.toString().contains("AEADBadTagException") ||
                e.cause?.toString()?.contains("AEADBadTagException") == true
    }

    private fun cleanupKeyStore() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            // Remove the master key if exists
            if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MASTER_KEY_ALIAS)
            }
        } catch (e: Exception) {
            Log.e("SecurePersist", "KeyStore cleanup failed", e)
        }
    }

    private fun initializeEncryptedPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(false)
            .setUserAuthenticationRequired(false)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILENAME,
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

    /**
     * Creates a preference delegate for storing EncryptedSharedPreferences
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
     * @return An `EncryptedPreference`
     */
    inline fun <reified T : Any> preference(defaultValue: T, key: String? = null): EncryptedPreference<T> {
        val type: Type = object : TypeToken<T>() {}.type
        return EncryptedSharedPreference(defaultValue, key, type)
    }

    /**
     * Class to handle encrypted SharedPreferences using property delegation.
     *
     * @param T The type of the preference value.
     * @property defaultValue The default value for the preference.
     * @property key The key for the preference. If null or empty, the property name will be used.
     * @property type The TypeToken of the preference value.
     */
    inner class EncryptedSharedPreference<T> (
        private val defaultValue: T,
        private val key: String? = null,
        private val type: Type,
    ) : EncryptedPreference<T> {

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val preferenceKey = key?.takeIf { it.isNotEmpty() } ?: property.name

            return when (defaultValue) {
                is Boolean, is Int, is Float, is Long, is Double, is String -> {
                    get(preferenceKey, defaultValue)
                }
                else -> {
                    // For complex objects, check if exists first
                    val storedValue = get(preferenceKey, "")
                    if (storedValue.isEmpty()) {
                        defaultValue
                    } else {
                        // Deserialize JSON string to object using the provided type
                        gson.fromJson(storedValue, type) as T
                    }
                }
            }
        }

        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val preferenceKey = key?.takeIf { it.isNotEmpty() } ?: property.name
            when (value) {
                is Boolean, is Int, is Float, is Long, is Double, is String -> {
                    put(preferenceKey, value)
                }
                else -> {
                    // Serialize object to JSON string
                    val jsonString = gson.toJson(value)
                    put(preferenceKey, jsonString)
                }
            }
        }
    }
}