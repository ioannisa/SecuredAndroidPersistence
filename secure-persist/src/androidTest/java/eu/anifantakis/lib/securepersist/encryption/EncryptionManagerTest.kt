package eu.anifantakis.lib.securepersist.encryption

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptionManagerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testKeyStoreEncryptionDecryption() {
        val keyAlias = "testKeyAlias"
        val encryptionManager = EncryptionManager.withKeyStore(keyAlias)

        val originalText = "Hello, Secure World!"
        val encryptedData = encryptionManager.encryptData(originalText)
        val decryptedText = encryptionManager.decryptData(encryptedData)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testExternalKeyEncryptionDecryption() {
        val encryptionManager = EncryptionManager.withExternalKey(EncryptionManager.generateExternalKey())

        val originalText = "Hello, Secure World!"
        val encryptedData = encryptionManager.encryptData(originalText)
        val decryptedText = encryptionManager.decryptData(encryptedData)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testSwitchToExternalKey() {
        val keyAlias = "testKeyAlias"
        val encryptionManager = EncryptionManager.withKeyStore(keyAlias)

        // Initially use KeyStore-based encryption
        val originalText = "Hello, Secure World!"
        var encryptedData = encryptionManager.encryptData(originalText)
        var decryptedText = encryptionManager.decryptData(encryptedData)

        assertEquals(originalText, decryptedText)

        // Switch to an external key
        val externalKey = EncryptionManager.generateExternalKey()
        encryptionManager.setExternalKey(externalKey)

        encryptedData = encryptionManager.encryptData(originalText)
        decryptedText = encryptionManager.decryptData(encryptedData)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testBase64EncodedEncryptionDecryption() {
        val keyAlias = "testKeyAlias"
        val encryptionManager = EncryptionManager.withKeyStore(keyAlias)

        val originalValue = "Hello, Secure World!"
        val encryptedValue = encryptionManager.encryptValue(originalValue)
        val decryptedValue = encryptionManager.decryptValue(encryptedValue, "")

        assertEquals(originalValue, decryptedValue)
    }

    @Test
    fun testExternalKeyBase64EncodedEncryptionDecryption() {
        val encryptionManager = EncryptionManager.withExternalKey(EncryptionManager.generateExternalKey())

        val originalValue = "Hello, Secure World!"
        val encryptedValue = encryptionManager.encryptValue(originalValue)
        val decryptedValue = encryptionManager.decryptValue(encryptedValue, "")

        assertEquals(originalValue, decryptedValue)
    }

    @Test
    fun testStaticExternalKeyEncryptionDecryption() {
        val externalKey = EncryptionManager.generateExternalKey()

        val originalText = "Hello, Secure World!"
        val encryptedData = EncryptionManager.encryptData(originalText, externalKey)
        val decryptedText = EncryptionManager.decryptData(encryptedData, externalKey)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testStaticExternalKeyBase64EncodedEncryptionDecryption() {
        val externalKey = EncryptionManager.generateExternalKey()

        val originalValue = "Hello, Secure World!"
        val encryptedValue = EncryptionManager.encryptValue(originalValue, externalKey)
        val decryptedValue = EncryptionManager.decryptValue(encryptedValue, "", externalKey)

        assertEquals(originalValue, decryptedValue)
    }

    @Test
    fun testFactoryConstructionWithExternalKey() {
        val externalKey = EncryptionManager.generateExternalKey()
        val encryptionManager = EncryptionManager.withExternalKey(externalKey)

        val originalText = "Hello, Secure World!"
        val encryptedData = encryptionManager.encryptData(originalText)
        val decryptedText = encryptionManager.decryptData(encryptedData)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testSetExternalKeyAfterConstruction() {
        val keyAlias = "testKeyAlias"
        val encryptionManager = EncryptionManager.withKeyStore(keyAlias)

        // Use KeyStore-based encryption
        val originalText = "Hello, Secure World!"
        var encryptedData = encryptionManager.encryptData(originalText)
        var decryptedText = encryptionManager.decryptData(encryptedData)

        assertEquals(originalText, decryptedText)

        // Set an external key after construction
        val externalKey = EncryptionManager.generateExternalKey()
        encryptionManager.setExternalKey(externalKey)

        encryptedData = encryptionManager.encryptData(originalText)
        decryptedText = encryptionManager.decryptData(encryptedData)

        assertEquals(originalText, decryptedText)
    }
}
