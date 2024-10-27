package eu.anifantakis.lib.securepersist.compose

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import eu.anifantakis.lib.securepersist.PersistManager
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatefulPersistManagerTest {

    private lateinit var context: Context
    private lateinit var persistManager: PersistManager
    private lateinit var statefulPersistManager: StatefulPersistManager

    data class User(val name: String, val age: Int)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        persistManager = PersistManager(context)
        statefulPersistManager = StatefulPersistManager(persistManager)
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

        var testValue by statefulPersistManager.persistedMutableState(
            defaultValue = 42,
            key = "testKey",
            storage = PersistManager.Storage.SHARED_PREFERENCES,
        )

        Assert.assertEquals(42, testValue)
    }

    @Test
    fun testPersistenceOfUpdatedValue() {
        var testValue by statefulPersistManager.persistedMutableState(
            defaultValue = 10,
            key = "testKey",
            storage = PersistManager.Storage.SHARED_PREFERENCES
        )

        testValue = 100

        var testValueNew by statefulPersistManager.persistedMutableState(
            defaultValue = 20,
            key = "testKey",
            storage = PersistManager.Storage.SHARED_PREFERENCES
        )

        Assert.assertEquals(100, testValueNew)
    }

    @Test
    fun testStringPersistence() {
        var testString by statefulPersistManager.persistedMutableState(
            defaultValue = "default1",
            storage = PersistManager.Storage.SHARED_PREFERENCES
        )

        testString = "Updated Text"

        var testStringNew by statefulPersistManager.persistedMutableState(
            key = "testString",
            defaultValue = "default2",
            storage = PersistManager.Storage.SHARED_PREFERENCES
        )

        Assert.assertEquals("Updated Text", testStringNew)
    }

    @Test
    fun testBooleanPersistence() {
        var testBoolean by statefulPersistManager.persistedMutableState(
            key = "testBooleanNew",
            defaultValue = false,
            storage = PersistManager.Storage.SHARED_PREFERENCES,
        )

        testBoolean = true

        var testBooleanNew by statefulPersistManager.persistedMutableState(
            defaultValue = false,
            storage = PersistManager.Storage.SHARED_PREFERENCES,
        )

        Assert.assertTrue(testBooleanNew)
    }

    @Test
    fun testCustomObjectPersistence() {
        val defaultUser = User("Default", 0)

        var user by statefulPersistManager.persistedMutableState(
            defaultValue = defaultUser,
            storage = PersistManager.Storage.SHARED_PREFERENCES,
        )

        val newUser = User("Alice", 30)
        user = newUser

        var userNew by statefulPersistManager.persistedMutableState(
            key = "user",
            defaultValue = defaultUser,
            storage = PersistManager.Storage.SHARED_PREFERENCES,
        )

        Assert.assertEquals(newUser, userNew)
    }

    @Test
    fun testEncryptedPersistence() = runBlocking {
        var secretData by statefulPersistManager.persistedMutableState(
            key="testEncryptedPersistence",
            defaultValue = "SecretData",
            storage = PersistManager.Storage.DATA_STORE_ENCRYPTED,
        )

        secretData = "EncryptedValue"

        // Because the above is non-blocking lets wait before we assert
        Thread.sleep(200L)

        var secretDataNew by statefulPersistManager.persistedMutableState(
            key="testEncryptedPersistence",
            defaultValue = "SecretData",
            storage = PersistManager.Storage.DATA_STORE_ENCRYPTED,
        )

        Assert.assertEquals("EncryptedValue", secretDataNew)
    }

    @Test
    fun testUnencryptedDataStorePersistence() {
        var dataStoreValue by statefulPersistManager.persistedMutableState(
            key="dataStoreValueKey",
            defaultValue = 999,
            storage = PersistManager.Storage.DATA_STORE,
        )

        dataStoreValue = 12345

        // Because the above is non-blocking lets wait before we assert
        Thread.sleep(200L)

        var dataStoreValueNew by statefulPersistManager.persistedMutableState(
            key="dataStoreValueKey",
            defaultValue = 30,
            storage = PersistManager.Storage.DATA_STORE,
        )

        Assert.assertEquals(12345, dataStoreValueNew)
    }

    @Test
    fun testStateObservation() {
        var observedValue by statefulPersistManager.persistedMutableState(
            defaultValue = 0,
            storage = PersistManager.Storage.SHARED_PREFERENCES,
        )

        observedValue = 10

        Assert.assertEquals(10, observedValue)
    }
}
