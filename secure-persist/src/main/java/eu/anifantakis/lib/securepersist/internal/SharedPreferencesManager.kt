package eu.anifantakis.lib.securepersist.internal

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlin.reflect.KProperty

internal class SharedPreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "encrypted_prefs_filename",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun <T> put(key: String, value: T) {
        sharedPreferences.edit {
            when (value) {
                is Boolean -> putBoolean(key, value).apply()
                is Int -> putInt(key, value).apply()
                is Float -> putFloat(key, value).apply()
                is Long -> putLong(key, value).apply()
                is String -> putString(key, value).apply()
                else -> throw UnsupportedOperationException()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is Boolean -> sharedPreferences.getBoolean(key, defaultValue) as T
            is Int -> sharedPreferences.getInt(key, defaultValue) as T
            is Float -> sharedPreferences.getFloat(key, defaultValue) as T
            is Long -> sharedPreferences.getLong(key, defaultValue) as T
            is String -> sharedPreferences.getString(key, defaultValue) as T
            else -> throw IllegalArgumentException("Unsupported type")
        }
    }

    fun delete(key: String) {
        sharedPreferences.edit {
            remove(key)
        }
    }

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

    fun <T> preference(defaultValue: T): EncryptedPreference<T> = EncryptedPreference(this, defaultValue)

    fun <T> preference(key: String, defaultValue: T): EncryptedPreference<T> = EncryptedPreference(this, defaultValue, key)
}