package eu.anifantakis.lib.securepersist

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import eu.anifantakis.lib.securepersist.encryption.EncryptionManager
import eu.anifantakis.lib.securepersist.encryption.IEncryptionManager
import eu.anifantakis.lib.securepersist.internal.DataStoreManager
import eu.anifantakis.lib.securepersist.internal.SharedPreferencesManager
import java.lang.reflect.Type
import kotlin.reflect.KProperty

class PersistManager(context: Context, keyAlias: String = "keyAlias") {

    private val encryptionManager: IEncryptionManager = EncryptionManager(context, keyAlias)
    val sharedPrefs = SharedPreferencesManager(context)
    val dataStorePrefs = DataStoreManager(context, encryptionManager)
    private val gson = Gson()

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
            val storedValue = persist.sharedPrefs.get(preferenceKey, "")

            return if (storedValue.isEmpty()) {
                defaultValue
            } else {
                when (defaultValue) {
                    is Boolean, is Int, is Float, is Long, is Double, is String -> {
                        persist.sharedPrefs.get(preferenceKey, defaultValue)
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
                    persist.sharedPrefs.put(preferenceKey, value)
                }
                else -> {
                    // Serialize object to JSON string
                    val jsonString = gson.toJson(value)
                    persist.sharedPrefs.put(preferenceKey, jsonString)
                }
            }
        }
    }

    /**
     * Uses delegation to set and get encrypted SharedPreferences with a specific key.
     *
     * @param key The key to store the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @return An EncryptedPreference instance.
     */
    inline fun <reified T> sharedPreferenceDelegate(defaultValue: T, key: String? = null): EncryptedPreference<T> {
        val type: Type = object : TypeToken<T>() {}.type
        return EncryptedPreference(this, defaultValue, key, type)
    }

    /**
     * Class to handle encrypted DataStore preferences using property delegation.
     */
    class EncryptedDataStore<T : Any>(
        private val persist: PersistManager,
        private val defaultValue: T,
        private val key: String? = null,
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val preferenceKey = key?.takeIf { it.isNotEmpty() } ?: property.name
            val storedValue = persist.dataStorePrefs.getDirect<T>(preferenceKey, defaultValue)
            return storedValue
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val preferenceKey = key?.takeIf { it.isNotEmpty() } ?: property.name
            persist.dataStorePrefs.putDirect(preferenceKey, value)
        }
    }

    /**
     * Uses delegation to set and get encrypted SharedPreferences with a specific key.
     *
     * @param key The key to store the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @return An EncryptedDataStore instance.
     */
    inline fun <reified T : Any> dataStoreDelegate(defaultValue: T, key: String? = null): EncryptedDataStore<T> {
        return EncryptedDataStore(this, defaultValue, key)
    }

}
