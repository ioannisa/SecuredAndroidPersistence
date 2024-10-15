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

// Annotations
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SharedPref(val key: String = "")

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class DataStorePref(val key: String = "", val encrypted: Boolean = true)


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
     * Creates a SharedPreferences delegate that can be used to store and retrieve encrypted values.
     * This function provides a way to create preferences that are stored in SharedPreferences with enforced encryption.
     * Can be used both at class level and within function bodies.
     *
     * Usage:
     * 1. Using property name as key:
     *    val myKey by persistManager.sharedPreference("default value")
     *
     * 2. Explicitly specifying a key:
     *    val myVariable by persistManager.sharedPreference("default value", key = "customKey")
     *
     * Note:
     * - If no key is specified, the property name is used as the key.
     * - All values stored using this function are automatically encrypted using EncryptedSharedPreferences.
     * - Unlike DataStore preferences, there is no option to disable encryption for SharedPreferences.
     * - This function can be used for property declarations both at class level and within function bodies.
     *
     * @param defaultValue The default value to be used if no value is stored for the preference.
     * @param key The key to store the value under. Uses property name as default key if omitted.
     * @return An EncryptedPreference instance that handles the encrypted preference operations.
     */
    inline fun <reified T> sharedPreference(defaultValue: T, key: String? = null): EncryptedPreference<T> {
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
        private val encrypted: Boolean = true
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val preferenceKey = key?.takeIf { it.isNotEmpty() } ?: property.name
            val storedValue = persist.dataStorePrefs.getDirect<T>(preferenceKey, defaultValue, encrypted)
            return storedValue
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val preferenceKey = key?.takeIf { it.isNotEmpty() } ?: property.name
            persist.dataStorePrefs.putDirect(preferenceKey, value, encrypted)
        }
    }

    /**
     * Creates a DataStore preference delegate that can be used to store and retrieve encrypted values.
     * This function provides a way to create preferences that are stored in DataStore.
     * Can be used both at class level and within function bodies.
     *
     * Usage:
     * 1. Using property name as key (encrypted by default):
     *    val myKey by persistManager.dataStorePreference("default value")
     *
     * 2. Explicitly specifying a key:
     *    val myVariable by persistManager.dataStorePreference("default value", key = "customKey")
     *
     * 3. Explicitly specifying a key and disabling encryption:
     *    val myPlainTextVariable by persistManager.dataStorePreference("default value", key = "customKey", encrypted = false)
     *
     * Note:
     * - If no key is specified, the property name is used as the key.
     * - DataStore preferences are encrypted by default. Use 'encrypted = false' to store in plain text.
     * - This function can be used for property declarations both at class level and within function bodies.
     *
     * @param defaultValue The default value to be used if no value is stored for the preference.
     * @param key The key to store the value under. Uses property name as default key if omitted.
     * @param encrypted Whether to enable encryption for the DataStore preference (enabled by default).
     * @return An EncryptedDataStore instance that handles the preference operations.
     */
    inline fun <reified T : Any> dataStorePreference(defaultValue: T, key: String? = null, encrypted: Boolean = true): EncryptedDataStore<T> {
        return EncryptedDataStore(this, defaultValue, key, encrypted)
    }

    inner class PreferencesDelegate<T : Any>(
        private val persist: PersistManager,
        private val defaultValue: T,
        private val type: Type,
        private val gson: Gson = persist.gson
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val sharedPrefAnnotation = property.annotations.filterIsInstance<SharedPref>().firstOrNull()
            val dataStorePrefAnnotation = property.annotations.filterIsInstance<DataStorePref>().firstOrNull()
            checkRequiredAnnotations(sharedPrefAnnotation, dataStorePrefAnnotation)

            if (sharedPrefAnnotation != null) {
                val annotatedKey = sharedPrefAnnotation.key.trim()
                val preferenceKey = annotatedKey.trim().takeIf { it.isNotEmpty() } ?: property.name
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
     * Creates a preference delegate that can be used with either @SharedPref or @DataStorePref annotations.
     * This function provides a unified way to create preferences that can be stored either in SharedPreferences
     * or DataStore, based on the annotation used.
     * Can only be used for property declarations at the class level due to reflection restrictions.
     *
     * Usage:
     * 1. With SharedPreferences:
     *    a) Using property name as key:
     *       @SharedPref
     *       val myKey by persistManager.preference("default value")
     *
     *    b) Explicitly specifying a key:
     *       @SharedPref(key = "customKey")
     *       val myVariable by persistManager.preference("default value")
     *
     * 2. With DataStore:
     *    a) Using property name as key (encrypted by default):
     *       @DataStorePref
     *       val myKey by persistManager.preference("default value")
     *
     *    b) Explicitly specifying a key and disabling encryption:
     *       @DataStorePref(key = "customKey", encrypted = false)
     *       val myVariable by persistManager.preference("default value")
     *
     * Note:
     * - If no key is specified in the annotation, the property name is used as the key.
     * - DataStore preferences are encrypted by default. Use 'encrypted = false' to store in plain text.
     * - This function can only be used for property declarations at the class level due to reflection restrictions.
     * - For use within function bodies, use dataStorePreference() or sharedPreference() instead.
     *
     * @param defaultValue The default value to be used if no value is stored for the preference.
     * @return A PreferencesDelegate instance that handles the preference based on the annotation used.
     * @throws IllegalStateException if neither @SharedPref nor @DataStorePref annotation is used,
     *         or if both annotations are used simultaneously on the same property.
     */
    inline fun <reified T : Any> preference(defaultValue: T): PreferencesDelegate<T> {
        val type: Type = object : TypeToken<T>() {}.type
        return PreferencesDelegate(this, defaultValue, type)
    }
}
