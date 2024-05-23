package eu.anifantakis.lib.securepersist

import android.content.Context
import eu.anifantakis.lib.securepersist.encryption.EncryptionManager
import eu.anifantakis.lib.securepersist.encryption.IEncryptionManager
import eu.anifantakis.lib.securepersist.internal.DataStoreManager
import eu.anifantakis.lib.securepersist.internal.SharedPreferencesManager
import kotlin.reflect.KProperty

class PersistManager(context: Context, keyAlias: String) {

    private val encryptionManager: IEncryptionManager = EncryptionManager.withKeyStore(keyAlias)
    private val sharedPreferencesManager = SharedPreferencesManager(context)
    private val dataStoreManager = DataStoreManager(context, encryptionManager)

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
     *
     * ```
     *
     * // Example usage:
     * val encryptedStore = PersistManager(context)
     *
     * encryptedStore.putDataStorePreference("key1", "exampleString")
     * encryptedStore.putDataStorePreference("key2", 123)
     * encryptedStore.putDataStorePreference("key3", true)
     * ```
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
     *
     * ```
     *
     * // Example usage:
     * val encryptedStore = PersistManager(context)
     *
     * val myStr: String = encryptedStore.getDataStorePreference("key1", "")
     * val myInt: Int = encryptedStore.getDataStorePreference("key2", 0)
     * val myBool: Boolean = encryptedStore.getDataStorePreference("key3", false)
     * ```
     */
    suspend fun <T : Any> getDataStorePreference(key: String, defaultValue: T): T {
        return dataStoreManager.get(key, defaultValue)
    }

    /**
     * Encrypts and Saves a value to DataStore.
     *
     * @param key The key to store the value under.
     * @param value The value to store.
     *
     * ```
     *
     * // Example usage:
     * val encryptedStore = PersistManager(context)
     *
     * encryptedStore.encryptDataStorePreference(eStringKey, "encryptedString")
     * encryptedStore.encryptDataStorePreference(eIntKey, 567)
     * encryptedStore.encryptDataStorePreference(eBooleanKey, true)
     * ```
     */
    suspend fun <T> encryptDataStorePreference(key: String, value: T) {
        dataStoreManager.putEncrypted(key, value)
    }

    /**
     * Decrypts and Retrieves a value from DataStore.
     *
     * @param key The key to retrieve the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @return The decrypted value.
     *
     * ```
     *
     * // Example usage:
     * val encryptedStore = PersistManager(context)
     *
     * val myStr: String = encryptedStore.decryptDataStorePreference("key1", "")
     * val myInt: Int = encryptedStore.decryptDataStorePreference("key2", 0)
     * val myBool: Boolean = encryptedStore.decryptDataStorePreference("key3", false)
     * ```
     */
    suspend fun <T> decryptDataStorePreference(key: String, defaultValue: T): T {
        return dataStoreManager.getEncrypted(key, defaultValue)
    }

    /**
     * Deletes a value from DataStore.
     *
     * @param key The key to delete the value under.
     *
     * ```
     *
     * // Example usage:
     * val encryptedStore = PersistManager(context)
     * encryptedStore.deleteDataStorePreference("key1")
     * ```
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
        private val key: String? = null
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val preferenceKey = key ?: property.name
            return persist.decryptSharedPreference(preferenceKey, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val preferenceKey = key ?: property.name
            persist.encryptSharedPreference(preferenceKey, value)
        }
    }

    /**
     * Uses Delegation to set and get encrypted SharedPreferences.
     * Key to the preference is automatically assigned by the variable name.
     *
     * @param defaultValue The default value to return if the key does not exist.
     * @return An EncryptedPreference instance.
     *
     * ```
     *
     * // Example usage:
     * val store = PersistManager(context)
     *
     * var myStr by store.preference("delegationString1")
     * var myInt by store.preference(11)
     * var myBool by store.preference(true)
     * ```
     */
    fun <T> preference(defaultValue: T): EncryptedPreference<T> = EncryptedPreference(this, defaultValue)

    /**
     * Uses Delegation to set and get encrypted SharedPreferences and sets the key to the property name.
     *
     * @param key The key to store the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @return An EncryptedPreference instance.
     *
     * ```
     *
     * // Example usage:
     * val store = PersistManager(context)
     *
     * var myStr by store.preference("myStr", "delegationString1")
     * var myInt by store.preference("myInt"", 11)
     * var myBool by store.preference("myBool"", true)
     * ```
     */
    fun <T> preference(key: String = "", defaultValue: T): EncryptedPreference<T> {
        return if (key.trim().isEmpty()) {
            EncryptedPreference(this, defaultValue, key)
        } else {
            EncryptedPreference(this, defaultValue)
        }
    }
}