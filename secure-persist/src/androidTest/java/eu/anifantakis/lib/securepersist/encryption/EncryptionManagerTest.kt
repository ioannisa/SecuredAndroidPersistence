package eu.anifantakis.lib.securepersist.encryption

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import javax.crypto.SecretKey

@RunWith(AndroidJUnit4::class)
class EncryptionManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @After
    fun teardown() {
        // Clean up files
        val encryptedFile = File(context.filesDir, "encrypted_test_file")
        if (encryptedFile.exists()) encryptedFile.delete()

        val decryptedFile = File(context.filesDir, "decrypted_test.txt")
        if (decryptedFile.exists()) decryptedFile.delete()
    }

    @Test
    fun testKeyStoreEncryptionDecryption() {
        val keyAlias = "testKeyAlias"

        val encryptionManager = EncryptionManager(context, keyAlias)

        val originalText = "Hello, Secure World!"
        val encryptedData = encryptionManager.encryptData(originalText)
        val decryptedText = encryptionManager.decryptData(encryptedData)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testExternalKeyEncryptionDecryption() {
        val encryptionManager = EncryptionManager(context, EncryptionManager.generateExternalKey())

        val originalText = "Hello, Secure World!"
        val encryptedData = encryptionManager.encryptData(originalText)
        val decryptedText = encryptionManager.decryptData(encryptedData)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testBase64EncodedEncryptionDecryption() {
        val keyAlias = "testKeyAlias"
        val encryptionManager = EncryptionManager(context, keyAlias)

        val originalValue = "Hello, Secure World!"
        val encryptedValue = encryptionManager.encryptValue(originalValue)
        val decryptedValue = encryptionManager.decryptValue(encryptedValue, "")

        assertEquals(originalValue, decryptedValue)
    }

    @Test
    fun testExternalKeyBase64EncodedEncryptionDecryption() {
        val encryptionManager = EncryptionManager(context, EncryptionManager.generateExternalKey())

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
        val encryptionManager = EncryptionManager(context, externalKey)

        val originalText = "Hello, Secure World!"
        val encryptedData = encryptionManager.encryptData(originalText)
        val decryptedText = encryptionManager.decryptData(encryptedData)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testEncodeDecodeSecretKey() {
        val originalKey = EncryptionManager.generateExternalKey()
        val originalText = "Hello, Secure World!"
        val encryptedData = EncryptionManager.encryptData(originalText, originalKey)

        val encodedKey: String = EncryptionManager.encodeSecretKey(originalKey)
        val decodedKey: SecretKey = EncryptionManager.decodeSecretKey(encodedKey)

        val decryptedText = EncryptionManager.decryptData(encryptedData, decodedKey)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testEncryptAndDecryptFileFromFileSystem() {
        val testFileName = "test.txt"
        val encryptedFileName = "encrypted_test_file"
        val testFileContent = "Hello, Secure File!"

        val encryptionManager = EncryptionManager(context, "keyAlias")

        // Create test file with specific content
        val testFile = File(context.filesDir, testFileName)
        FileOutputStream(testFile).use { it.write(testFileContent.toByteArray()) }

        // Encrypt the file from file system
        encryptionManager.encryptFile(testFile, encryptedFileName)

        // Decrypt the file
        val decryptedContent: ByteArray = encryptionManager.decryptFile(encryptedFileName)
        val decryptedText = String(decryptedContent)

        // Compare the original and decrypted content
        assertEquals(
            "Decrypted content does not match the original content",
            testFileContent,
            decryptedText
        )

        // Clean up
        testFile.delete()
        val encryptedFile = File(context.filesDir, encryptedFileName)
        if (encryptedFile.exists()) encryptedFile.delete()
    }
}
