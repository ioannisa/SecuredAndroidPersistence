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
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * EncryptionManager class handles encryption and decryption using the Android KeyStore system or an external key.
 *
 * @param keyAlias The alias for the encryption key in the KeyStore.
 */
class EncryptionManager : IEncryptionManager {

    private var keyAlias: String? = "keyAlias"
    private var externalKey: SecretKey? = null

    companion object {
        private const val KEYSTORE_TYPE = "AndroidKeyStore"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12 // IV size for GCM is 12 bytes
        private const val TAG_SIZE = 128 // Tag size for GCM is 128 bits

        /**
         * Factory method to create an EncryptionManager using the Android KeyStore.
         *
         * @param keyAlias The alias for the encryption key in the KeyStore.
         * @return The EncryptionManager instance.
         */
        fun withKeyStore(keyAlias: String): EncryptionManager {
            val manager = EncryptionManager()
            manager.keyAlias = keyAlias
            return manager
        }

        /**
         * Factory method to create an EncryptionManager using an external key.
         *
         * @param externalKey The external secret key for encryption and decryption.
         * @return The EncryptionManager instance.
         */
        fun withExternalKey(externalKey: SecretKey): EncryptionManager {
            val manager = EncryptionManager()
            manager.setExternalKey(externalKey)
            return manager
        }

        /**
         * Generates a new external secret key.
         *
         * @return The generated secret key.
         */
        fun generateExternalKey(): SecretKey {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            return keyGenerator.generateKey()
        }
    }

    private val keyStore: KeyStore? = keyAlias?.let {
        KeyStore.getInstance(KEYSTORE_TYPE).apply {
            load(null)
            if (!containsAlias(it)) {
                generateSecretKey(it)
            }
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
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256) // Using 256-bit key for strong encryption
            .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    /**
     * Sets an external secret key for encryption and decryption.
     *
     * @param secretKey The external secret key to be used.
     * @return The EncryptionManager instance.
     */
    fun withExternalKey(secretKey: SecretKey): EncryptionManager {
        setExternalKey(secretKey)
        return this
    }

    /**
     * Sets an external secret key for encryption and decryption.
     *
     * @param secretKey The external secret key to be used.
     */
    override fun setExternalKey(secretKey: SecretKey) {
        externalKey = secretKey
        keyAlias = null
    }

    /**
     * Encrypts the given data using the secret key.
     *
     * @param data The plaintext data to encrypt.
     * @param withKey The secret key to use for encryption. If null, the default key is used.
     * @return The encrypted data as a byte array.
     */
    override fun encryptData(data: String, withKey: SecretKey?): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val secretKey = withKey ?: getKey()
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
     * @param withKey The secret key to use for decryption. If null, the default key is used.
     * @return The decrypted plaintext data as a string.
     */
    override fun decryptData(encryptedData: ByteArray, withKey: SecretKey?): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val secretKey = withKey ?: getKey()

        val iv = ByteArray(IV_SIZE)
        System.arraycopy(encryptedData, 0, iv, 0, iv.size)

        val encryptedBytes = ByteArray(encryptedData.size - iv.size)
        System.arraycopy(encryptedData, iv.size, encryptedBytes, 0, encryptedBytes.size)

        val ivSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        return String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8)
    }

    /**
     * Encrypts a value and encodes it to a Base64 string.
     *
     * @param value The value to encrypt.
     * @param withKey The secret key to use for encryption. If null, the default key is used.
     * @return The encrypted value as a Base64 string.
     */
    override fun <T> encryptValue(value: T, withKey: SecretKey?): String {
        val stringValue = value.toString()
        val encryptedData = encryptData(stringValue, withKey)
        return Base64.encodeToString(encryptedData, Base64.DEFAULT)
    }

    /**
     * Decrypts a Base64 encoded string and returns the original value.
     *
     * @param encryptedValue The encrypted value as a Base64 string.
     * @param defaultValue The default value to return if decryption fails.
     * @param withKey The secret key to use for decryption. If null, the default key is used.
     * @return The decrypted value.
     */
    override fun <T> decryptValue(encryptedValue: String, defaultValue: T, withKey: SecretKey?): T {
        return try {
            val encryptedData = Base64.decode(encryptedValue, Base64.DEFAULT)
            val decryptedString = decryptData(encryptedData)
            when (defaultValue) {
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
        val entry = keyStore?.getEntry(alias, null) as KeyStore.PrivateKeyEntry
        return entry.certificateChain
    }

    private fun getKey(): SecretKey {
        return externalKey ?: keyStore!!.getKey(keyAlias, null) as SecretKey
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
