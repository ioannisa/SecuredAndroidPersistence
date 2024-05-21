package eu.anifantakis.lib.securepersist

import android.content.Context
import eu.anifantakis.lib.securepersist.internal.DataStoreManager
import eu.anifantakis.lib.securepersist.internal.SharedPreferencesManager
import kotlin.reflect.KProperty

class PersistManager(context: Context, keyAlias: String) {

    private val encryptionManager = EncryptionManager(keyAlias)
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
     */
    suspend fun <T : Any> getDataStorePreference(key: String, defaultValue: T): T {
        return dataStoreManager.get(key, defaultValue)
    }

    /**
     * Encrypts and Saves a value to DataStore.
     *
     * @param key The key to store the value under.
     * @param value The value to store.
     */
    suspend fun <T> encryptDataStorePreference(key: String, value: T) {
        dataStoreManager.putEncrypted(key, value)
    }

    /**
     * Decrypts and Retrieves a value from DataStore.
     *
     * @param key The key to retrieve the value under.
     * @param defaultValue The default value to return if the key does not exist.
     */
    suspend fun <T> decryptDataStorePreference(key: String, defaultValue: T): T {
        return dataStoreManager.getEncrypted(key, defaultValue)
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
     * Uses Delegation to set and get encrypted SharedPreferences.  Key to the preference is the property name.
     *
     * @param defaultValue The default value to return if the key does not exist.
     */
    fun <T> preference(defaultValue: T): EncryptedPreference<T> = EncryptedPreference(this, defaultValue)

    /**
     * Uses Delegation to set and get encrypted SharedPreferences.
     *
     * @param key The key to store the value under.
     * @param defaultValue The default value to return if the key does not exist.
     */
    fun <T> preference(key: String, defaultValue: T): EncryptedPreference<T> = EncryptedPreference(this, defaultValue, key)
}