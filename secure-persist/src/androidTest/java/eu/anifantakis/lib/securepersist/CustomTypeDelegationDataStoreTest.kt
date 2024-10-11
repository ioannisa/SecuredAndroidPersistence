package eu.anifantakis.lib.securepersist

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomTypeDelegationDataStoreTest {

    private lateinit var persistManager: PersistManager
    private lateinit var testClass: TestClass

    data class AuthInfo(
        val accessToken: String = "",
        val refreshToken: String = "",
        val expiresIn: Long = 0L
    )

    class TestClass(persistManager: PersistManager) {
        var authInfo by persistManager.secureDataStorePreferenceDelegate(AuthInfo())
    }

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        persistManager = PersistManager(context)
        testClass = TestClass(persistManager)

        // Clear existing preference before each test
        persistManager.dataStorePrefs.deleteDirect("authInfo")

        // because the above is async leave some time to thread for deletion to happen
        Thread.sleep(500L)

        persistManager.dataStorePrefs.putDirect(
            key = "authInfo",
            value = AuthInfo(
                accessToken = "token123",
                refreshToken = "refresh123",
                expiresIn = 3600L
            )
        )

        Thread.sleep(500L)
    }

    @Test
    fun testCustomTypeDataStoreDelegated() {
        val storedAuthInfo by persistManager.secureDataStorePreferenceDelegate(AuthInfo(), "authInfo")

        assertEquals("token123", storedAuthInfo.accessToken)
        assertEquals("refresh123", storedAuthInfo.refreshToken)
        assertEquals(3600L, storedAuthInfo.expiresIn)
    }

    @Test
    fun testCustomTypeDataStoreDelegatedChangingValue() {
        var storedAuthInfo by persistManager.secureDataStorePreferenceDelegate(AuthInfo(), "authInfo")
        storedAuthInfo = storedAuthInfo.copy(accessToken = "accessToken999")

        // Because the above is non-blocking lets wait before we assert
        Thread.sleep(500L)

        assertEquals("accessToken999", testClass.authInfo.accessToken)
        assertEquals("refresh123", testClass.authInfo.refreshToken)
        assertEquals(3600L, testClass.authInfo.expiresIn)
    }


    @Test
    fun testCustomTypeDataStore() {
        val storedAuthInfo = testClass.authInfo

        assertEquals("token123", storedAuthInfo.accessToken)
        assertEquals("refresh123", storedAuthInfo.refreshToken)
        assertEquals(3600L, storedAuthInfo.expiresIn)
    }

    @Test
    fun testCustomTypeDataStoreDelegation() {
        // if no key provided, SharedPreferences uses the variable name as key
        val authInfo by persistManager.secureDataStorePreferenceDelegate(AuthInfo())

        assertEquals("token123", authInfo.accessToken)
        assertEquals("refresh123", authInfo.refreshToken)
        assertEquals(3600L, authInfo.expiresIn)
    }

    @Test
    fun testCustomTypeDataStoreDelegationSetKey() {
        // if a key is provided, it will be used by SharedPreference as a key
        val storedAuthInfo by persistManager.secureDataStorePreferenceDelegate(AuthInfo(), "authInfo")

        assertEquals("token123", storedAuthInfo.accessToken)
        assertEquals("refresh123", storedAuthInfo.refreshToken)
        assertEquals(3600L, storedAuthInfo.expiresIn)
    }
}