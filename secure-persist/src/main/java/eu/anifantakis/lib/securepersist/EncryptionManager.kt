package eu.anifantakis.lib.securepersist

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class EncryptionManager(private val keyAlias: String) {

    companion object {
        private const val KEYSTORE_TYPE = "AndroidKeyStore"
        private const val CIPHER_TRANSFORMATION = "AES/CBC/PKCS7Padding"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply {
        load(null)
        if (!containsAlias(keyAlias)) {
            generateSecretKey(keyAlias)
        }
    }

    private fun generateSecretKey(alias: String) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_TYPE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    fun encryptData(data: String): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))

        // Combine IV and encrypted data
        val combined = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
        return combined
    }

    fun decryptData(encryptedData: ByteArray): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val secretKey = keyStore.getKey(keyAlias, null) as SecretKey

        // Extract IV from the beginning of the encrypted data
        val iv = ByteArray(16) // 16 bytes for AES
        System.arraycopy(encryptedData, 0, iv, 0, iv.size)

        val encryptedBytes = ByteArray(encryptedData.size - iv.size)
        System.arraycopy(encryptedData, iv.size, encryptedBytes, 0, encryptedBytes.size)

        val ivSpec = javax.crypto.spec.IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        return String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8)
    }

    /**
     * Encrypt values into a string
     *
     * @param value The value to encrypt
     *
     * Accepted types
     * Boolean, Int, Float, Long, String
     * Other types will throw an IllegalArgumentException
     */
    fun <T> encryptValue(value: T): String {
        val stringValue = when (value) {
            is Boolean, is Int, is Float, is Long, is String -> value.toString()
            else -> throw IllegalArgumentException("Unsupported type")
        }
        return Base64.encodeToString(encryptData(stringValue), Base64.DEFAULT)
    }

    /**
     * Decrypt a value using the provided default value if the decryption fails.
     *
     * @param encryptedValue The encrypted value to decrypt.
     * @param defaultValue The default value to return if decryption fails.
     *
     * Accepted types
     * Boolean, Int, Float, Long, String
     * Other types will throw an IllegalArgumentException
     */
    fun <T> decryptValue(encryptedValue: String, defaultValue: T): T {
        val decryptedString = decryptData(Base64.decode(encryptedValue, Base64.DEFAULT))
        return when (defaultValue) {
            is Boolean -> decryptedString.toBoolean() as T
            is Int -> decryptedString.toInt() as T
            is Float -> decryptedString.toFloat() as T
            is Long -> decryptedString.toLong() as T
            is String -> decryptedString as T
            else -> throw IllegalArgumentException("Unsupported type")
        }
    }
}