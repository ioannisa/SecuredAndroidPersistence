package eu.anifantakis.lib.securepersist

import android.content.Context
import eu.anifantakis.lib.securepersist.encryption.EncryptionManager
import eu.anifantakis.lib.securepersist.encryption.IEncryptionManager
import eu.anifantakis.lib.securepersist.internal.DataStoreManager
import eu.anifantakis.lib.securepersist.internal.SharedPreferencesManager
import kotlin.reflect.KProperty

class PersistManager(context: Context, keyAlias: String) {

    private val encryptionManager: IEncryptionManager = EncryptionManager(keyAlias)
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
     * Example usage:
     *
     * val encryptedStore = PersistManager(context)
     *
     * encryptedStore.putDataStorePreference("key1", "exampleString")
     * encryptedStore.putDataStorePreference("key2", 123)
     * encryptedStore.putDataStorePreference("key3", true)
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
     * Example usage:
     *
     * val encryptedStore = PersistManager(context)
     *
     * val myStr: String = encryptedStore.getDataStorePreference("key1", "")
     * val myInt: Int = encryptedStore.getDataStorePreference("key2", 0)
     * val myBool: Boolean = encryptedStore.getDataStorePreference("key3", false)
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
     * Example usage:
     *
     * val encryptedStore = PersistManager(context)
     *
     * encryptedStore.encryptDataStorePreference(eStringKey, "encryptedString")
     * encryptedStore.encryptDataStorePreference(eIntKey, 567)
     * encryptedStore.encryptDataStorePreference(eBooleanKey, true)
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
     * Example usage:
     *
     * val encryptedStore = PersistManager(context)
     *
     * val myStr: String = encryptedStore.decryptDataStorePreference("key1", "")
     * val myInt: Int = encryptedStore.decryptDataStorePreference("key2", 0)
     * val myBool: Boolean = encryptedStore.decryptDataStorePreference("key3", false)
     */
    suspend fun <T> decryptDataStorePreference(key: String, defaultValue: T): T {
        return dataStoreManager.getEncrypted(key, defaultValue)
    }

    /**
     * Deletes a value from DataStore.
     *
     * @param key The key to delete the value under.
     *
     * Example usage:
     *
     * val encryptedStore = PersistManager(context)
     * encryptedStore.deleteDataStorePreference("key1")
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
     * Uses Delegation to set and get encrypted SharedPreferences. Key to the preference is the property name.
     *
     * @param defaultValue The default value to return if the key does not exist.
     * @return An EncryptedPreference instance.
     *
     * Example usage:
     *
     * val encryptedStore = PersistManager(context)
     *
     * var myStr by encryptedStore.preference("delegationString1")
     * var myInt by encryptedStore.preference(11)
     * var myBool by encryptedStore.preference(true)
     *
     * above the key will be same as the variable name
     */
    fun <T> preference(defaultValue: T): EncryptedPreference<T> = EncryptedPreference(this, defaultValue)

    /**
     * Uses Delegation to set and get encrypted SharedPreferences.
     *
     * @param key The key to store the value under.
     * @param defaultValue The default value to return if the key does not exist.
     * @return An EncryptedPreference instance.
     *
     * Example usage:
     *
     * val encryptedStore = PersistManager(context)
     *
     * var myStr by encryptedStore.preference("myStr", "delegationString1")
     * var myInt by encryptedStore.preference("myInt"", 11)
     * var myBool by encryptedStore.preference("myBool"", true)
     *
     * above the key is irrelevant from the variable name
     */
    fun <T> preference(key: String, defaultValue: T): EncryptedPreference<T> = EncryptedPreference(this, defaultValue, key)
}