package eu.anifantakis.lib.securepersist.compose

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import eu.anifantakis.lib.securepersist.PersistManager
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistedMutableStateTest {

    private lateinit var context: Context
    private lateinit var persistManager: PersistManager


    data class User(val name: String, val age: Int)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        persistManager = PersistManager(context)
    }

    @After
    fun tearDown() {
        // Clean up persisted data
        with(persistManager) {
            delete("testKey")
            delete("testString")
            delete("testBooleanNew")
            delete("user")
            delete("testEncryptedPersistence")
            delete("dataStoreValueKey")
            delete("observedValue")
        }
    }

    @Test
    fun testInitializationWithDefaultValue() {
        persistManager.delete("testKey")

        val testValue by persistManager.sharedPrefs.mutableStateOf(
            defaultValue = 42,
            key = "testKey"
        )

        Assert.assertEquals(42, testValue)
    }

    @Test
    fun testPersistenceOfUpdatedValue() {
        var testValue by persistManager.sharedPrefs.mutableStateOf(
            defaultValue = 10,
            key = "testKey"
        )

        testValue = 100

        val testValueNew by persistManager.sharedPrefs.mutableStateOf(
            defaultValue = 20,
            key = "testKey"
        )

        Assert.assertEquals(100, testValueNew)
    }

    @Test
    fun testStringPersistence() {
        var testString by persistManager.sharedPrefs.mutableStateOf(
            defaultValue = "default1"
        )

        testString = "Updated Text"

        val testStringNew by persistManager.sharedPrefs.mutableStateOf(
            key = "testString",
            defaultValue = "default2"
        )

        Assert.assertEquals("Updated Text", testStringNew)
    }

    @Test
    fun testBooleanPersistence() {
        var testBoolean by persistManager.sharedPrefs.mutableStateOf(
            key = "testBooleanNew",
            defaultValue = false
        )

        testBoolean = true

        val testBooleanNew by persistManager.sharedPrefs.mutableStateOf(
            defaultValue = false
        )

        Assert.assertTrue(testBooleanNew)
    }

    @Test
    fun testCustomObjectPersistence() {
        val defaultUser = User("Default", 0)

        var user by persistManager.sharedPrefs.mutableStateOf(
            defaultValue = defaultUser
        )

        val newUser = User("Alice", 30)
        user = newUser

        val userNew by persistManager.sharedPrefs.mutableStateOf(
            key = "user",
            defaultValue = defaultUser,
        )

        Assert.assertEquals(newUser, userNew)
    }

    @Test
    fun testEncryptedPersistence() = runBlocking {
        var secretData by persistManager.dataStorePrefs.mutableStateOf(
            key="testEncryptedPersistence",
            defaultValue = "SecretData"
        )

        secretData = "EncryptedValue"

        // Because the above is non-blocking lets wait before we assert
        Thread.sleep(200L)

        val secretDataNew by persistManager.dataStorePrefs.mutableStateOf(
            key="testEncryptedPersistence",
            defaultValue = "SecretData"
        )

        Assert.assertEquals("EncryptedValue", secretDataNew)
    }

    @Test
    fun testUnencryptedDataStorePersistence() {
        var dataStoreValue by persistManager.dataStorePrefs.mutableStateOf(
            key="dataStoreValueKey",
            defaultValue = 999,
            encrypted = false,
        )

        dataStoreValue = 12345

        // Because the above is non-blocking lets wait before we assert
        Thread.sleep(200L)

        val dataStoreValueNew by persistManager.dataStorePrefs.mutableStateOf(
            key="dataStoreValueKey",
            defaultValue = 30,
            encrypted = false
        )

        Assert.assertEquals(12345, dataStoreValueNew)
    }

    @Test
    fun testStateObservation() {
        var observedValue by persistManager.sharedPrefs.mutableStateOf(
            defaultValue = 0
        )

        observedValue = 10

        Assert.assertEquals(10, observedValue)
    }
}
