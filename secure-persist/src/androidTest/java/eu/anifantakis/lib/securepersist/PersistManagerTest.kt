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
    fun testPutEncryptedDelegatedSharedAnnotatedPreference() {
        val key = "sharedPrefKey"
        val value = "testValue"

        persistManager.sharedPrefs.put(key, value)
        val decryptedValue: String = persistManager.sharedPrefs.get(key, "")

        assertEquals(value, decryptedValue)
    }

    @Test
    fun testGetEncryptedDelegatedSharedAnnotatedPreferenceWithDefault() {
        val key = "nonExistentKey"
        val defaultValue = "default"

        val retrievedValue: String = persistManager.sharedPrefs.get(key, defaultValue)

        assertEquals(defaultValue, retrievedValue)
    }

    @Test
    fun testDeleteDelegatedSharedAnnotatedPreference() {
        val key = "deleteKey"
        val value = "toBeDeleted"

        persistManager.sharedPrefs.put(key, value)
        persistManager.sharedPrefs.delete(key)
        val retrievedValue: String = persistManager.sharedPrefs.get(key, "default")

        assertEquals("default", retrievedValue)
    }

    @Test
    fun anotherTestBlocking() = runBlocking {
        persistManager.sharedPrefs.put("key1", "secureValue")

        val retrievedValue: String = persistManager.sharedPrefs.get("key1", "")

        persistManager.sharedPrefs.delete("key1")

        assertEquals("secureValue", retrievedValue)
    }

    @Test
    fun testPutAndGetDataStoreAnnotatedPreference() = runBlocking {
        val key = "dataStoreKey"
        val value = "dataStoreValue"

        persistManager.dataStorePrefs.put(key, value)
        val retrievedValue: String = persistManager.dataStorePrefs.get(key, "")

        assertEquals(value, retrievedValue)
    }

    @Test
    fun testEncryptAndDecryptDataStoreAnnotatedPreference() = runBlocking {
        val key = "encryptedDataStoreKey"
        val value = "encryptedDataStoreValue"

        persistManager.dataStorePrefs.put(key, value)
        val retrievedValue: String = persistManager.dataStorePrefs.get(key, "")

        assertEquals(value, retrievedValue)
    }

    @Test
    fun testEncryptAndGetDataStoreAnnotatedPreferenceDirect() {
        val key = "encryptedDataStoreKey"
        val value = "encryptedDataStoreValue"

        // Encrypt & Store the primitive at the DataStore without exposing coroutines in non-blocking asynchronous way
        persistManager.dataStorePrefs.putDirect(key, value)

        // because the above is asynchronous let it finish by freezing main thread for some time
        Thread.sleep(500L)

        // Decrypt & Retrieve the primitive from DataStore in a blocking synchronous way without exposing coroutines
        val retrievedValue: String = persistManager.dataStorePrefs.getDirect(key, "")

        assertEquals(value, retrievedValue)
    }

    @Test
    fun testDeleteDataStoreAnnotatedPreference() = runBlocking {
        val key = "deleteDataStoreKey"
        val value = "toBeDeleted"

        persistManager.dataStorePrefs.put(key, value)
        persistManager.dataStorePrefs.delete(key)
        val retrievedValue: String = persistManager.dataStorePrefs.get(key, "default")

        assertEquals("default", retrievedValue)
    }

    @Test
    fun testDelegatedSharedAnnotatedPreferencePropertyDelegationPrimitive() {
        var myStr by persistManager.preference("defaultString")

        myStr = "newStringValue"
        assertEquals("newStringValue", myStr)
    }

    @Test
    fun testDataStoreAnnotatedPreferencePropertyDelegationPrimitive() = runBlocking {
        var myStr by persistManager.preference("dataStoreString", "key1")

        myStr = "newDataStoreStringValue"
        assertEquals("newDataStoreStringValue", myStr)
    }

    @Test
    fun testDelegatedSharedAnnotatedPreferencePropertyDelegationCustomType() {
        data class AuthInfo(
            val accessToken: String = "",
            val refreshToken: String = "",
            val userId: Int = 0
        )

        // create authInfo1 which is assigned to "authInfoKey" with Default Value AuthInfo()
        var authInfo1 by persistManager.preference(AuthInfo(), "authInfoKey")


        // update authInfo1 with new accessToken
        authInfo1 = authInfo1.copy(accessToken = "newAccessToken")

        // retrieve authInfo2 from "authInfoKey"
        val authInfo2 by persistManager.preference(AuthInfo(), "authInfoKey")

        // Assertions
        assertEquals(authInfo2.accessToken, "newAccessToken")
    }

    @Test
    fun testDelegatedSharedPreferencesObject() {
        // Define a custom object
        data class User(val id: Int, val name: String, val email: String)

        val user = User(1, "John Doe", "john.doe@example.com")

        // Store the object
        persistManager.sharedPrefs.put("user_key", user)

        // Retrieve the object
        val retrievedUser: User = persistManager.sharedPrefs.get("user_key", User(0, "", ""))

        // Assertions
        assertNotNull(retrievedUser)
        assertEquals(user.id, retrievedUser.id)
        assertEquals(user.name, retrievedUser.name)
        assertEquals(user.email, retrievedUser.email)
    }

    @Test
    fun testDataStoreObject() = runBlocking {
        // Define a custom object
        data class Settings(val notificationsEnabled: Boolean, val theme: String)

        val settings = Settings(true, "dark")

        // Store the object
        persistManager.dataStorePrefs.put("settings_key", settings)

        // Retrieve the object
        val retrievedSettings: Settings = persistManager.dataStorePrefs.get("settings_key", Settings(false, ""))

        // Assertions
        assertNotNull(retrievedSettings)
        assertEquals(settings.notificationsEnabled, retrievedSettings.notificationsEnabled)
        assertEquals(settings.theme, retrievedSettings.theme)
    }

    @Test
    fun testDataStoreObjectSync() {
        // Define a custom object
        data class Settings(val notificationsEnabled: Boolean, val theme: String)

        val settings = Settings(true, "dark")

        // Encrypt & Store the primitive at the DataStore without exposing coroutines in non-blocking asynchronous way
        persistManager.dataStorePrefs.putDirect("settings_key", settings)

        // because the above is asynchronous let it finish by freezing main thread for some time
        Thread.sleep(500L)

        // Decrypt & Retrieve the object from DataStore in a blocking synchronous way without exposing coroutines
        val retrievedSettings: Settings = persistManager.dataStorePrefs.getDirect("settings_key", Settings(false, ""))

        // Assertions
        assertNotNull(retrievedSettings)
        assertEquals(settings.notificationsEnabled, retrievedSettings.notificationsEnabled)
        assertEquals(settings.theme, retrievedSettings.theme)
    }

    @Test
    fun testCustomTypePreferenceDelegationIncrementsSharedPrefsDirect() {
        persistManager.delete("incrementsViaSharedPrefsDirect")
        persistManager.sharedPrefs.put("incrementsViaSharedPrefsDirect", 1000)
        persistManager.sharedPrefs.put("incrementsViaSharedPrefsDirect", 100)
        persistManager.sharedPrefs.put("incrementsViaSharedPrefsDirect", persistManager.sharedPrefs.get("incrementsViaSharedPrefsDirect", 0) +1)
        persistManager.sharedPrefs.put("incrementsViaSharedPrefsDirect", persistManager.sharedPrefs.get("incrementsViaSharedPrefsDirect", 0) +1)

        assertEquals(102, persistManager.sharedPrefs.get("incrementsViaSharedPrefsDirect", 0) )
    }

    @Test
    fun testCustomTypePreferenceDelegationIncrementsDataStoreDirect() = runBlocking{
        persistManager.delete("incrementsViaDataStoreDirect")
        persistManager.dataStorePrefs.put("incrementsViaDataStoreDirect", 1000, encrypted = false)
        persistManager.dataStorePrefs.put("incrementsViaDataStoreDirect", 100, encrypted = false)
        persistManager.dataStorePrefs.put("incrementsViaDataStoreDirect", persistManager.dataStorePrefs.get("incrementsViaDataStoreDirect", 0, encrypted = false) +1, encrypted = false)
        persistManager.dataStorePrefs.put("incrementsViaDataStoreDirect", persistManager.dataStorePrefs.get("incrementsViaDataStoreDirect", 0, encrypted = false) +1, encrypted = false)

        assertEquals(102, persistManager.dataStorePrefs.get("incrementsViaDataStoreDirect", 0, encrypted = false))
    }

    @Test
    fun testCustomTypePreferenceDelegationIncrementsDataStoreEncryptedDirect() = runBlocking{
        persistManager.delete("incrementsViaDataStoreEncryptedDirect")
        persistManager.dataStorePrefs.put("incrementsViaDataStoreEncryptedDirect", 1000)
        persistManager.dataStorePrefs.put("incrementsViaDataStoreEncryptedDirect", 100)
        persistManager.dataStorePrefs.put("incrementsViaDataStoreEncryptedDirect", persistManager.dataStorePrefs.get("incrementsViaDataStoreEncryptedDirect", 0) +1)
        persistManager.dataStorePrefs.put("incrementsViaDataStoreEncryptedDirect", persistManager.dataStorePrefs.get("incrementsViaDataStoreEncryptedDirect", 0) +1)

        assertEquals(102, persistManager.dataStorePrefs.get("incrementsViaDataStoreEncryptedDirect", 0))
    }

    @Test
    fun testStringsDirectSharedPref() {
        persistManager.delete("stringsDirectSP")
        val stringsDirectSP = persistManager.sharedPrefs.get("stringsDirectSP", "stringsDirectSP")
        assertEquals("stringsDirectSP", stringsDirectSP)

        persistManager.sharedPrefs.put("stringsDirectSP", "otherString")
        assertEquals("otherString", persistManager.sharedPrefs.get("stringsDirectSP", ""))

        val newValue = persistManager.sharedPrefs.get("stringsDirectSP", "stringsDirectSP")
        assertEquals("otherString", newValue)
    }

    @Test
    fun testStringsDirectSharedPrefDelegated() {
        persistManager.delete("stringsDirectSPDelegate")
        var stringsDirectSPDelegate by persistManager.preference("stringsDirect", storage = PersistManager.Storage.SHARED_PREFERENCES)
        stringsDirectSPDelegate = "otherString"
        stringsDirectSPDelegate += " plus1"
        stringsDirectSPDelegate += " plus2"

        var stringsDirectSP2 by persistManager.preference("stringsDirect2", key = "stringsDirectSPDelegate", storage = PersistManager.Storage.SHARED_PREFERENCES)

        assertEquals("otherString plus1 plus2", stringsDirectSPDelegate)
        assertEquals("otherString plus1 plus2", stringsDirectSP2)
    }


    @Test
    fun testCustomTypePreferenceDelegationIncrementsSharedPrefsLong() {
        persistManager.delete("incrementsViaSharedPrefsL")
        var incrementsViaSharedPrefsL by persistManager.preference(1000L, storage = PersistManager.Storage.SHARED_PREFERENCES)
        incrementsViaSharedPrefsL = 100L
        incrementsViaSharedPrefsL++
        incrementsViaSharedPrefsL++

        assertEquals(102L, incrementsViaSharedPrefsL)
    }

    @Test
    fun testCustomTypePreferenceDelegationIncrementsSharedPrefsInt() {
        persistManager.delete("incrementsViaSharedPrefs")
        var incrementsViaSharedPrefs by persistManager.preference(defaultValue = 1000, storage = PersistManager.Storage.SHARED_PREFERENCES)
        incrementsViaSharedPrefs = 100
        incrementsViaSharedPrefs++
        incrementsViaSharedPrefs++

        assertEquals(102, incrementsViaSharedPrefs)
    }

    @Test
    fun testCustomTypePreferenceDelegationIncrementsDataStore() {
        var incrementsViaDataStore by persistManager.preference(1000, storage = PersistManager.Storage.DATA_STORE)
        Thread.sleep(100L)
        incrementsViaDataStore = 100
        Thread.sleep(100L)
        incrementsViaDataStore++
        Thread.sleep(100L)
        incrementsViaDataStore++
        Thread.sleep(100L)

        assertEquals(102, incrementsViaDataStore)
    }

    @Test
    fun testCustomTypePreferenceDelegationIncrementsDataStoreEncrypted() {
        var incrementsViaDataStoreEncrypted by persistManager.preference(1000, storage = PersistManager.Storage.DATA_STORE_ENCRYPTED)
        Thread.sleep(100L)
        incrementsViaDataStoreEncrypted = 100
        Thread.sleep(100L)
        incrementsViaDataStoreEncrypted++
        Thread.sleep(100L)
        incrementsViaDataStoreEncrypted++
        Thread.sleep(100L)

        assertEquals(102, incrementsViaDataStoreEncrypted)
    }
}
