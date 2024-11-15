package eu.anifantakis.lib.securepersist

import android.content.Context
import eu.anifantakis.lib.securepersist.encryption.EncryptionManager
import eu.anifantakis.lib.securepersist.encryption.IEncryptionManager
import eu.anifantakis.lib.securepersist.internal.DataStoreManager
import eu.anifantakis.lib.securepersist.internal.SharedPreferencesManager
import eu.anifantakis.lib.securepersist.internal.createGson
import kotlin.reflect.KProperty

interface SecurePersistSolution

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

    private val gson = createGson()
    private val encryptionManager: IEncryptionManager = EncryptionManager(context, keyAlias, gson = gson)
    val sharedPrefs = SharedPreferencesManager(context, gson)
    val dataStorePrefs = DataStoreManager(context, encryptionManager, gson)


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
}
