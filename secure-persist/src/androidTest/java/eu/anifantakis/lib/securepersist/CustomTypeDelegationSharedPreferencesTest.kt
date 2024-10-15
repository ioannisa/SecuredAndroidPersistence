package eu.anifantakis.lib.securepersist

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomTypeDelegationSharedPreferencesTest {

    private lateinit var persistManager: PersistManager
    private lateinit var testClass: TestClass

    data class AuthInfo(
        val accessToken: String = "",
        val refreshToken: String = "",
        val expiresIn: Long = 0L
    )

    class TestClass(persistManager: PersistManager) {
        // delegated without annotations (can be used also locally)
        var authInfo by persistManager.sharedPreference(AuthInfo())

        // delegated using annotations (due to reflection can only be used as class field)
        @SharedPref(key = "authInfo")
        var authInfo2 by persistManager.preference(defaultValue = AuthInfo())
    }

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        persistManager = PersistManager(context)
        testClass = TestClass(persistManager)

        // Clear existing preference before each test
        persistManager.sharedPrefs.delete("authInfo")
        persistManager.sharedPrefs.put(
            key = "authInfo",
            value = AuthInfo(
                accessToken = "token123",
                refreshToken = "refresh123",
                expiresIn = 3600L
            )
        )
    }

    @Test
    fun testCustomTypePreference() {
        val storedAuthInfo = testClass.authInfo

        val authInfo2 = testClass.authInfo2

        val localAuthInfo by persistManager.sharedPreference(key = "authInfo", defaultValue = AuthInfo())

        assertEquals("token123", localAuthInfo.accessToken)
        assertEquals("token123", authInfo2.accessToken)

        assertEquals("token123", storedAuthInfo.accessToken)
        assertEquals("refresh123", storedAuthInfo.refreshToken)
        assertEquals(3600L, storedAuthInfo.expiresIn)
    }

    @Test
    fun testCustomTypePreferenceDelegation() {
        // if no key provided, SharedPreferences uses the variable name as key
        val authInfo by persistManager.sharedPreference(AuthInfo())

        assertEquals("token123", authInfo.accessToken)
        assertEquals("refresh123", authInfo.refreshToken)
        assertEquals(3600L, authInfo.expiresIn)
    }

    @Test
    fun testCustomTypePreferenceDelegationSetKey() {
        // if a key is provided, it will be used by SharedPreference as a key
        val storedAuthInfo by persistManager.sharedPreference(AuthInfo(), "authInfo")

        assertEquals("token123", storedAuthInfo.accessToken)
        assertEquals("refresh123", storedAuthInfo.refreshToken)
        assertEquals(3600L, storedAuthInfo.expiresIn)
    }
}
