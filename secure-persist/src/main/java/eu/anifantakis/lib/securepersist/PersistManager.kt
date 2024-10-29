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

/**
 * Annotation to mark a property for storage in SharedPreferences.
 *
 * @property key The key to be used for the preference. If empty, the property name will be used.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SharedPref(val key: String = "")


/**
 * Annotation to mark a property for storage in DataStore.
 *
 * @property key The key to be used for the preference. If empty, the property name will be used.
 * @property encrypted Whether the preference should be stored encrypted. Defaults to true.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class DataStorePref(val key: String = "", val encrypted: Boolean = true)

/**
 * Interface for handling encrypted preferences using property delegation.
 *
 * @param T The type of the preference value.
 */
interface EncryptedPreference <T> {
    /**
     * Retrieves the value of the preference.
     *
     * @param thisRef The reference to the property owner.
     * @param property The property metadata.
     * @return The value of the preference.
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T

    /**
     * Sets the value of the preference.
     *
     * @param thisRef The reference to the property owner.
     * @param property The property metadata.
     * @param value The new value to set.
     */
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)

    fun checkRequiredAnnotations(sharedPrefAnnotation: SharedPref?, dataStorePrefAnnotation: DataStorePref?) {
        if (sharedPrefAnnotation != null || dataStorePrefAnnotation != null) {
            throw IllegalStateException("@SharedPref and @DataStorePref annotations cannot be used with 'preference' function. Did you mean to use 'annotatedPreference'?")
        }
    }
}

/**
 * Manager class for handling encrypted preferences using SharedPreferences and DataStore.
 *
 * Provides property delegation for convenient access and storage of preferences.
 *
 * @param context The application context.
 * @param keyAlias The alias for the encryption key.
 */
class PersistManager(context: Context, keyAlias: String = "keyAlias") {

    private val encryptionManager: IEncryptionManager = EncryptionManager(context, keyAlias)
    val sharedPrefs = SharedPreferencesManager(context)
    val dataStorePrefs = DataStoreManager(context, encryptionManager)
    private val gson = Gson()

    /**
     * Class to handle encrypted SharedPreferences using property delegation.
     *
     * @param T The type of the preference value.
     * @property persist The PersistManager instance.
     * @property defaultValue The default value for the preference.
     * @property key The key for the preference. If null or empty, the property name will be used.
     * @property type The TypeToken of the preference value.
     */
    inner class EncryptedSharedPreference<T> (
        private val persist: PersistManager,
        private val defaultValue: T,
        private val key: String? = null,
        private val type: Type,
    ) : EncryptedPreference<T> {
        private val gson: Gson = persist.gson

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            checkRequiredAnnotations(
                property.annotations.filterIsInstance<SharedPref>().firstOrNull(),
                property.annotations.filterIsInstance<DataStorePref>().firstOrNull()
            )

            val preferenceKey = key?.takeIf { it.isNotEmpty() } ?: property.name

            return when (defaultValue) {
                is Boolean, is Int, is Float, is Long, is Double, is String -> {
                    persist.sharedPrefs.get(preferenceKey, defaultValue)
                }
                else -> {
                    // For complex objects, check if exists first
                    val storedValue = persist.sharedPrefs.get(preferenceKey, "")
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
            checkRequiredAnnotations(
                property.annotations.filterIsInstance<SharedPref>().firstOrNull(),
                property.annotations.filterIsInstance<DataStorePref>().firstOrNull()
            )

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
     * Class to handle encrypted DataStore preferences using property delegation.
     *
     * @param T The type of the preference value.
     * @property persist The PersistManager instance.
     * @property defaultValue The default value for the preference.
     * @property key The key for the preference. If null or empty, the property name will be used.
     * @property encrypted Whether the preference should be stored encrypted. Defaults to true.
     */
    inner class EncryptedDataStore<T : Any>(
        private val persist: PersistManager,
        private val defaultValue: T,
        private val key: String? = null,
        private val encrypted: Boolean = true
    ) : EncryptedPreference<T> {
        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            checkRequiredAnnotations(
                property.annotations.filterIsInstance<SharedPref>().firstOrNull(),
                property.annotations.filterIsInstance<DataStorePref>().firstOrNull()
            )

            val preferenceKey = key?.takeIf { it.isNotEmpty() } ?: property.name
            val storedValue = persist.dataStorePrefs.getDirect<T>(preferenceKey, defaultValue, encrypted)
            return storedValue
        }

        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            checkRequiredAnnotations(
                property.annotations.filterIsInstance<SharedPref>().firstOrNull(),
                property.annotations.filterIsInstance<DataStorePref>().firstOrNull()
            )

            val preferenceKey = key?.takeIf { it.isNotEmpty() } ?: property.name
            persist.dataStorePrefs.putDirect(preferenceKey, value, encrypted)
        }
    }

    /**
     * Delegate class to handle preferences using property annotations.
     *
     * This class allows properties annotated with @SharedPref or @DataStorePref to be delegated
     * and automatically stored in SharedPreferences or DataStore, respectively.
     *
     * @param T The type of the preference value.
     * @property persist The PersistManager instance.
     * @property defaultValue The default value for the preference.
     * @property type The TypeToken of the preference value.
     */
    inner class PreferencesDelegate<T : Any>(
        private val persist: PersistManager,
        private val defaultValue: T,
        private val type: Type,
    ) {
        private val gson: Gson = persist.gson

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val sharedPrefAnnotation = property.annotations.filterIsInstance<SharedPref>().firstOrNull()
            val dataStorePrefAnnotation = property.annotations.filterIsInstance<DataStorePref>().firstOrNull()
            checkRequiredAnnotations(sharedPrefAnnotation, dataStorePrefAnnotation)

            if (sharedPrefAnnotation != null) {
                val annotatedKey = sharedPrefAnnotation.key.trim()
                val preferenceKey = annotatedKey.trim().takeIf { it.isNotEmpty() } ?: property.name

                return when (defaultValue) {
                    is Boolean, is Int, is Float, is Long, is Double, is String -> {
                        persist.sharedPrefs.get(preferenceKey, defaultValue)
                    }
                    else -> {
                        // For complex objects, check if exists first
                        val storedValue = persist.sharedPrefs.get(preferenceKey, "")
                        if (storedValue.isEmpty()) {
                            defaultValue
                        } else {
                            // Deserialize JSON string to object using the provided type
                            gson.fromJson(storedValue, type) as T
                        }
                    }
                }
            }
            else if (dataStorePrefAnnotation != null) {
                val annotatedKey = dataStorePrefAnnotation.key.trim()
                val preferenceKey = annotatedKey.trim().takeIf { it.isNotEmpty() } ?: property.name

                val storedValue = persist.dataStorePrefs.getDirect<T>(preferenceKey, defaultValue, dataStorePrefAnnotation.encrypted)
                return storedValue
            }

            return defaultValue
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val sharedPrefAnnotation = property.annotations.filterIsInstance<SharedPref>().firstOrNull()
            val dataStorePrefAnnotation = property.annotations.filterIsInstance<DataStorePref>().firstOrNull()
            checkRequiredAnnotations(sharedPrefAnnotation, dataStorePrefAnnotation)

            if (sharedPrefAnnotation != null) {
                val annotatedKey = sharedPrefAnnotation.key.trim()
                val preferenceKey = annotatedKey.trim().takeIf { it.isNotEmpty() } ?: property.name

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

            if (dataStorePrefAnnotation != null) {
                val annotatedKey = dataStorePrefAnnotation.key.trim()
                val preferenceKey = annotatedKey.trim().takeIf { it.isNotEmpty() } ?: property.name

                persist.dataStorePrefs.putDirect(preferenceKey, value, dataStorePrefAnnotation.encrypted)
            }
        }

        private fun checkRequiredAnnotations(sharedPrefAnnotation: SharedPref?, dataStorePrefAnnotation: DataStorePref?) {
            if (sharedPrefAnnotation == null && dataStorePrefAnnotation == null) {
                throw IllegalStateException("preference must be annotated with either @SharedPreferences or @DataStorePref")
            }

            if (sharedPrefAnnotation != null && dataStorePrefAnnotation != null) {
                throw IllegalStateException("You cannot annotate a preference with both @SharedPreferences and @DataStorePref")
            }
        }
    }

    /**
     * Creates a preference delegate that handles properties annotated with @SharedPref or @DataStorePref.
     *
     * This function provides a unified way to create preferences that can be stored either in SharedPreferences
     * or DataStore, based on the annotation used on the property.
     *
     * **Important:**
     * - This function can only be used for property declarations at the class level due to reflection restrictions.
     * - For use within function bodies, use the `preference` function instead.
     *
     * **Usage:**
     *
     * 1. With SharedPreferences:
     *    - Using property name as key:
     *      ```kotlin
     *      @SharedPref
     *      val myKey by persistManager.annotatedPreference("default value")
     *      ```
     *    - Explicitly specifying a key:
     *      ```kotlin
     *      @SharedPref(key = "customKey")
     *      val myVariable by persistManager.annotatedPreference("default value")
     *      ```
     *
     * 2. With DataStore:
     *    - Using property name as key (encrypted by default):
     *      ```kotlin
     *      @DataStorePref
     *      val myKey by persistManager.annotatedPreference("default value")
     *      ```
     *    - Explicitly specifying a key and disabling encryption:
     *      ```kotlin
     *      @DataStorePref(key = "customKey", encrypted = false)
     *      val myVariable by persistManager.annotatedPreference("default value")
     *      ```
     *
     * **Notes:**
     * - If no key is specified in the annotation, the property name is used as the key.
     * - DataStore preferences are encrypted by default. Use `encrypted = false` to store in plain text.
     *
     * @param defaultValue The default value to be used if no value is stored for the preference.
     * @return A `PreferencesDelegate` instance that handles the preference based on the annotation used.
     * @throws IllegalStateException if neither @SharedPref nor @DataStorePref annotation is used,
     *         or if both annotations are used simultaneously on the same property.
     */
    inline fun <reified T : Any> annotatedPreference(defaultValue: T): PreferencesDelegate<T> {
        val type: Type = object : TypeToken<T>() {}.type
        return PreferencesDelegate(this, defaultValue, type)
    }

    /**
     * Creates a preference delegate for storing preferences without the need for property annotations.
     *
     * This function allows you to specify the storage type directly and is suitable for use within function bodies
     * or when annotations cannot be used.
     *
     * **Usage:**
     *
     * - Using SharedPreferences:
     *   ```kotlin
     *   val myPref by persistManager.preference("default value", key = "myKey", storage = StorageType.SHARED_PREFERENCES)
     *   ```
     *
     * - Using DataStore (encrypted):
     *   ```kotlin
     *   val myPref by persistManager.preference("default value", key = "myKey", storage = StorageType.ENCRYPTED_DATA_STORE)
     *   ```
     *
     * - Using DataStore (unencrypted):
     *   ```kotlin
     *   val myPref by persistManager.preference("default value", key = "myKey", storage = StorageType.DATA_STORE)
     *   ```
     *
     * **Notes:**
     * - If `key` is null or empty, the property name will be used as the key.
     * - When using DataStore, you can specify whether the data should be encrypted by choosing the appropriate `StorageType`.
     *
     * @param defaultValue The default value to be used if no value is stored for the preference.
     * @param key The key for the preference. If null or empty, the property name will be used.
     * @param storage The type of storage to use. Defaults to `StorageType.SHARED_PREFERENCES`.
     * @return An `EncryptedPreference` instance for the specified storage type.
     */
    inline fun <reified T : Any> preference(defaultValue: T, key: String? = null, storage: Storage = Storage.SHARED_PREFERENCES): EncryptedPreference<T> {
        val type: Type = object : TypeToken<T>() {}.type

        return if (storage == Storage.SHARED_PREFERENCES) {
            EncryptedSharedPreference(this, defaultValue, key, type)
        } else {
            EncryptedDataStore(this, defaultValue, key, encrypted = (storage == Storage.DATA_STORE_ENCRYPTED))
        }
    }

    /**
     * Deletes a preference by its key.
     *
     * The preference will be deleted from both SharedPreferences and DataStore.
     *
     * @param key The key of the preference to delete.
     */
    fun delete(key: String) {
        sharedPrefs.delete(key)
        dataStorePrefs.deleteDirect(key)
    }

    /**
     * Enum representing the types of storage available for preferences.
     */
    enum class Storage {
        /**
         * Store preference in SharedPreferences.
         */
        SHARED_PREFERENCES,
        /**
         * Store preference in encrypted DataStore.
         */
        DATA_STORE_ENCRYPTED,
        /**
         * Store preference in unencrypted DataStore.
         */
        DATA_STORE
    }
}
