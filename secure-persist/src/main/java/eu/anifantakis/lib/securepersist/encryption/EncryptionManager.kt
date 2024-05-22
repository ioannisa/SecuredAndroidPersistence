package eu.anifantakis.lib.securepersist.encryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.cert.Certificate
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * EncryptionManager class handles encryption and decryption using the Android KeyStore system.
 *
 * @param keyAlias The alias for the encryption key in the KeyStore.
 */
class EncryptionManager(private val keyAlias: String = "keyAlias") : IEncryptionManager {

    companion object {
        private const val KEYSTORE_TYPE = "AndroidKeyStore"
        private const val CIPHER_TRANSFORMATION = "AES/CBC/PKCS7Padding"
        private const val IV_SIZE = 16 // IV size for AES is 16 bytes
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply {
        load(null)
        if (!containsAlias(keyAlias)) {
            generateSecretKey(keyAlias)
        }
    }

    /**
     * Generates a new secret key and stores it in the KeyStore.
     *
     * @param alias The alias for the key.
     */
    private fun generateSecretKey(alias: String) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_TYPE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setAttestationChallenge("randomChallenge".toByteArray())
            .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    /**
     * Encrypts the given data using the secret key.
     *
     * @param data The plaintext data to encrypt.
     * @return The encrypted data as a byte array.
     */
    override fun encryptData(data: String): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))

        // Combine IV and encrypted data
        return combineIvAndEncryptedData(iv, encryptedData)
    }

    /**
     * Decrypts the given encrypted data using the secret key.
     *
     * @param encryptedData The encrypted data as a byte array.
     * @return The decrypted plaintext data as a string.
     */
    override fun decryptData(encryptedData: ByteArray): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val secretKey = keyStore.getKey(keyAlias, null) as SecretKey

        val iv = ByteArray(IV_SIZE)
        System.arraycopy(encryptedData, 0, iv, 0, iv.size)

        val encryptedBytes = ByteArray(encryptedData.size - iv.size)
        System.arraycopy(encryptedData, iv.size, encryptedBytes, 0, encryptedBytes.size)

        val ivSpec = javax.crypto.spec.IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        return String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8)
    }

    /**
     * Encrypts a value and encodes it to a Base64 string.
     *
     * @param value The value to encrypt.
     * @return The encrypted value as a Base64 string.
     */
    override fun <T> encryptValue(value: T): String {
        val stringValue = value.toString()
        return Base64.encodeToString(encryptData(stringValue), Base64.DEFAULT)
    }

    /**
     * Decrypts a Base64 encoded string and returns the original value.
     *
     * @param encryptedValue The encrypted value as a Base64 string.
     * @param defaultValue The default value to return if decryption fails.
     * @return The decrypted value.
     */
    override fun <T> decryptValue(encryptedValue: String, defaultValue: T): T {
        return try {
            val decryptedString = decryptData(Base64.decode(encryptedValue, Base64.DEFAULT))
            return when (defaultValue) {
                is Boolean -> decryptedString.toBoolean() as T
                is Int -> decryptedString.toInt() as T
                is Float -> decryptedString.toFloat() as T
                is Long -> decryptedString.toLong() as T
                is String -> decryptedString as T
                else -> throw IllegalArgumentException("Unsupported type")
            }
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * Retrieves the attestation certificate chain for the key.
     *
     * @param alias The alias of the key to get the attestation for.
     * @return The attestation certificate chain.
     */
    override fun getAttestationCertificateChain(alias: String): Array<Certificate> {
        val entry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
        return entry.certificateChain
    }

    /**
     * Combines the IV and encrypted data into a single byte array.
     *
     * @param iv The initialization vector.
     * @param encryptedData The encrypted data.
     * @return The combined IV and encrypted data.
     */
    private fun combineIvAndEncryptedData(iv: ByteArray, encryptedData: ByteArray): ByteArray {
        val combined = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
        return combined
    }
}