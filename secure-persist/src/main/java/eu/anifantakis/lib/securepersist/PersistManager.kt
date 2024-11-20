package eu.anifantakis.lib.securepersist

import android.content.Context
import eu.anifantakis.lib.securepersist.encryption.EncryptionManager
import eu.anifantakis.lib.securepersist.encryption.IEncryptionManager
import eu.anifantakis.lib.securepersist.internal.DataStoreManager
import eu.anifantakis.lib.securepersist.internal.SharedPreferencesManager
import eu.anifantakis.lib.securepersist.internal.createGson
import java.io.File
import java.security.KeyStore
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
    private var encryptionManager: IEncryptionManager
    var sharedPrefs: SharedPreferencesManager
    var dataStorePrefs: DataStoreManager

    companion object {
        private fun cleanupAll(context: Context, keyAlias: String) {
            // Delete SharedPreferences file
            context.deleteSharedPreferences("encrypted_prefs_filename")

            // Delete DataStore file
            File(context.filesDir, "datastore/encrypted_datastore").deleteRecursively()

            // Clean KeyStore
            try {
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                if (keyStore.containsAlias(keyAlias)) {
                    keyStore.deleteEntry(keyAlias)
                }
                if (keyStore.containsAlias("_androidx_security_master_key_")) {
                    keyStore.deleteEntry("_androidx_security_master_key_")
                }
            } catch (e: Exception) {
                // Log but continue since we're in cleanup
            }
        }
    }

    init {
        try {
            encryptionManager = EncryptionManager(context, keyAlias, gson = gson)
            sharedPrefs = SharedPreferencesManager(context, gson)
            dataStorePrefs = DataStoreManager(context, encryptionManager, gson)
        } catch (e: Exception) {
            when {
                e is ClassCastException || e.cause is ClassCastException ||
                        e.toString().contains("VERIFICATION_FAILED") ||
                        e.toString().contains("KeyStoreException") ||
                        e.toString().contains("AEADBadTagException") -> {
                    // Full cleanup
                    cleanupAll(context, keyAlias)
                    // Try again
                    encryptionManager = EncryptionManager(context, keyAlias, gson = gson)
                    sharedPrefs = SharedPreferencesManager(context, gson)
                    dataStorePrefs = DataStoreManager(context, encryptionManager, gson)
                }
                else -> throw e
            }
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
}
