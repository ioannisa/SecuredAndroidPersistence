package eu.anifantakis.lib.securepersist

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistManagerTest {

    private lateinit var context: Context
    private lateinit var persistManager: PersistManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        persistManager = PersistManager(context, "testKeyAlias")
    }

    @Test
    fun testEncryptSharedPreference() {
        val key = "sharedPrefKey"
        val value = "testValue"

        persistManager.encryptSharedPreference(key, value)
        val decryptedValue: String = persistManager.decryptSharedPreference(key, "")

        assertEquals(value, decryptedValue)
    }

    @Test
    fun testDecryptSharedPreferenceWithDefault() {
        val key = "nonExistentKey"
        val defaultValue = "default"

        val retrievedValue: String = persistManager.decryptSharedPreference(key, defaultValue)

        assertEquals(defaultValue, retrievedValue)
    }

    @Test
    fun testDeleteSharedPreference() {
        val key = "deleteKey"
        val value = "toBeDeleted"

        persistManager.encryptSharedPreference(key, value)
        persistManager.deleteSharedPreference(key)
        val retrievedValue: String = persistManager.decryptSharedPreference(key, "default")

        assertEquals("default", retrievedValue)
    }

    @Test
    fun testPutAndGetDataStorePreference() = runBlocking {
        val key = "dataStoreKey"
        val value = "dataStoreValue"

        persistManager.putDataStorePreference(key, value)
        val retrievedValue: String = persistManager.getDataStorePreference(key, "")

        assertEquals(value, retrievedValue)
    }

    @Test
    fun testEncryptAndDecryptDataStorePreference() = runBlocking {
        val key = "encryptedDataStoreKey"
        val value = "encryptedDataStoreValue"

        persistManager.encryptDataStorePreference(key, value)
        val retrievedValue: String = persistManager.decryptDataStorePreference(key, "")

        assertEquals(value, retrievedValue)
    }

    @Test
    fun testDeleteDataStorePreference() = runBlocking {
        val key = "deleteDataStoreKey"
        val value = "toBeDeleted"

        persistManager.putDataStorePreference(key, value)
        persistManager.deleteDataStorePreference(key)
        val retrievedValue: String = persistManager.getDataStorePreference(key, "default")

        assertEquals("default", retrievedValue)
    }

    @Test
    fun testSharedPreferencePropertyDelegation() {
        var myStr by persistManager.preference("defaultString")

        myStr = "newStringValue"
        assertEquals("newStringValue", myStr)
    }

    @Test
    fun testDataStorePreferencePropertyDelegation() = runBlocking {
        var myStr by persistManager.preference("dataStoreString", "defaultString")

        myStr = "newDataStoreStringValue"
        assertEquals("newDataStoreStringValue", myStr)
    }

    @Test
    fun testSharedPreferencesObject() {
        // Define a custom object
        data class User(val id: Int, val name: String, val email: String)

        val user = User(1, "John Doe", "john.doe@example.com")

        // Store the object
        persistManager.putObjectSharedPreference("user_key", user)

        // Retrieve the object
        val retrievedUser: User? = persistManager.getObjectSharedPreference("user_key")

        // Assertions
        assertNotNull(retrievedUser)
        assertEquals(user.id, retrievedUser?.id)
        assertEquals(user.name, retrievedUser?.name)
        assertEquals(user.email, retrievedUser?.email)
    }

    @Test
    fun testDataStoreObject() = runBlocking {
        // Define a custom object
        data class Settings(val notificationsEnabled: Boolean, val theme: String)

        val settings = Settings(true, "dark")

        // Store the object
        persistManager.putObjectDataStorePreference("settings_key", settings)

        // Retrieve the object
        val retrievedSettings: Settings? = persistManager.getObjectDataStorePreference("settings_key")

        // Assertions
        assertNotNull(retrievedSettings)
        assertEquals(settings.notificationsEnabled, retrievedSettings?.notificationsEnabled)
        assertEquals(settings.theme, retrievedSettings?.theme)
    }
}
