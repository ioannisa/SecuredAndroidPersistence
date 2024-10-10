package eu.anifantakis.lib.securepersist.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
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
 */
class EncryptionManager : IEncryptionManager {

    private val context: Context
    private val secretKey: SecretKey

    companion object {
        private const val KEYSTORE_TYPE = "AndroidKeyStore"

        private const val KEY_ALGORITHM: String = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE: String = KeyProperties.BLOCK_MODE_GCM
        private const val ENCRYPTION_PADDING: String = KeyProperties.ENCRYPTION_PADDING_NONE

        private const val KEY_SIZE: Int = 256
        private const val CIPHER_TRANSFORMATION = "$KEY_ALGORITHM/$BLOCK_MODE/$ENCRYPTION_PADDING"
        private const val IV_SIZE = 12 // IV size for GCM is 12 bytes
        private const val TAG_SIZE = 128 // Tag size for GCM is 128 bits

        /**
         * Generates a new external secret key.
         *
         * @return The generated secret key.
         */
        fun generateExternalKey(): SecretKey {
            val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM)
            keyGenerator.init(KEY_SIZE)
            return keyGenerator.generateKey()
        }

        /**
         * Encrypts a value and encodes it to a Base64 string using an external key.
         *
         * @param value The value to encrypt.
         * @param secretKey The secret key to use for encryption.
         * @return The encrypted value as a Base64 string.
         */
        fun <T> encryptValue(value: T, secretKey: SecretKey): String {
            val stringValue = value.toString()
            val encryptedData = encryptData(stringValue, secretKey)
            return Base64.encodeToString(encryptedData, Base64.NO_WRAP)
        }

        /**
         * Decrypts a Base64 encoded string and returns the original value using an external key.
         *
         * @param encryptedValue The encrypted value as a Base64 string.
         * @param defaultValue The default value to return if decryption fails.
         * @param secretKey The secret key to use for decryption.
         * @return The decrypted value.
         */
        fun <T> decryptValue(encryptedValue: String, defaultValue: T, secretKey: SecretKey): T {
            return try {
                val encryptedData = Base64.decode(encryptedValue, Base64.NO_WRAP)
                val decryptedString = decryptData(encryptedData, secretKey)
                when (defaultValue) {
                    is Boolean -> decryptedString.toBoolean() as T
                    is Int -> decryptedString.toInt() as T
                    is Float -> decryptedString.toFloat() as T
                    is Long -> decryptedString.toLong() as T
                    is String -> decryptedString as T
                    else -> throw IllegalArgumentException("Unsupported type")
                }
            } catch (e: Exception) {
                Log.e("EncryptionManager", "Decryption failed", e)
                defaultValue
            }
        }

        /**
         * Encrypts the given data using the provided secret key.
         *
         * @param data The plaintext data to encrypt.
         * @param secretKey The secret key to use for encryption.
         * @return The encrypted data as a byte array.
         */
        fun encryptData(data: String, secretKey: SecretKey): ByteArray {
            return try {
                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val encryptedData = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))

                // Combine IV and encrypted data
                combineIvAndEncryptedData(iv, encryptedData)
            } catch (e: Exception) {
                Log.e("EncryptionManager", "Encryption failed", e)
                throw e
            }
        }

        /**
         * Decrypts the given encrypted data using the provided secret key.
         *
         * @param encryptedData The encrypted data as a byte array.
         * @param secretKey The secret key to use for decryption.
         * @return The decrypted plaintext data as a string.
         */
        fun decryptData(encryptedData: ByteArray, secretKey: SecretKey): String {
            return try {
                if (encryptedData.size < IV_SIZE) {
                    throw IllegalArgumentException("Encrypted data is too short to contain a valid IV")
                }

                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)

                val iv = ByteArray(IV_SIZE)
                System.arraycopy(encryptedData, 0, iv, 0, iv.size)

                val encryptedBytes = ByteArray(encryptedData.size - iv.size)
                System.arraycopy(encryptedData, iv.size, encryptedBytes, 0, encryptedBytes.size)

                val ivSpec = GCMParameterSpec(TAG_SIZE, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
                String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8)
            } catch (e: Exception) {
                // Log the exception for better debugging and tracing
                Log.e("EncryptionManager", "Decryption failed", e)
                throw e
            }
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

        /**
         * Encodes a SecretKey to a Base64 string for storage or transmission.
         *
         * @param secretKey The SecretKey to encode.
         * @return The Base64 encoded string representation of the SecretKey.
         */
        fun encodeSecretKey(secretKey: SecretKey): String {
            val encodedKey = secretKey.encoded
            return Base64.encodeToString(encodedKey, Base64.NO_WRAP)
        }

        /**
         * Decodes an encoded SecretKey that was stored as Base64 string from the "encodeSecretKey" function back to a SecretKey.
         *
         * @param encodedKey The Base64 encoded string representation of the SecretKey.
         * @return The decoded SecretKey.
         */
        fun decodeSecretKey(encodedKey: String): SecretKey {
            val decodedKey = Base64.decode(encodedKey, Base64.NO_WRAP)
            return SecretKeySpec(decodedKey, 0, decodedKey.size, KEY_ALGORITHM)
        }
    }



    /**
     * Constructor for EncryptionManager using the Android KeyStore.
     *
     * @param context The application context.
     * @param keyAlias The alias for the encryption key in the KeyStore.
     */
    constructor(context: Context, keyAlias: String) {
        this.context = context

        // Load or generate the secret key from the KeyStore using keyAlias
        val keyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply { load(null) }
        val keyFromStore = keyStore.getKey(keyAlias, null) as? SecretKey
        if (keyFromStore != null) {
            this.secretKey = keyFromStore
        } else {
            // Key not found, generate a new one
            val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM, KEYSTORE_TYPE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .setKeySize(KEY_SIZE)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
            // Now retrieve the generated key
            val generatedKey = keyStore.getKey(keyAlias, null) as SecretKey
            this.secretKey = generatedKey
        }
    }

    /**
     * Constructor for EncryptionManager using an external SecretKey.
     *
     * @param context The application context.
     * @param externalKey The external secret key for encryption and decryption.
     */
    constructor(context: Context, externalKey: SecretKey) {
        this.context = context
        this.secretKey = externalKey
    }

    /**
     * Encrypts the given data using the secret key.
     *
     * @param data The plaintext data to encrypt.
     * @return The encrypted data as a byte array.
     */
    override fun encryptData(data: String): ByteArray {
        return Companion.encryptData(data, secretKey)
    }

    /**
     * Decrypts the given encrypted data using the secret key.
     *
     * @param encryptedData The encrypted data as a byte array.
     * @return The decrypted plaintext data as a string.
     */
    override fun decryptData(encryptedData: ByteArray): String {
        return Companion.decryptData(encryptedData, secretKey)
    }

    /**
     * Encrypts a value and encodes it to a Base64 string.
     *
     * @param value The value to encrypt.
     * @return The encrypted value as a Base64 string.
     */
    override fun <T> encryptValue(value: T): String {
        return Companion.encryptValue(value, secretKey)
    }

    /**
     * Decrypts a Base64 encoded string and returns the original value.
     *
     * @param encryptedValue The encrypted value as a Base64 string.
     * @param defaultValue The default value to return if decryption fails.
     * @return The decrypted value.
     */
    override fun <T> decryptValue(encryptedValue: String, defaultValue: T): T {
        return Companion.decryptValue(encryptedValue, defaultValue, secretKey)
    }

    /**
     * Encrypts data from an [InputStream] and writes the encrypted data to a file in the app's private storage.
     *
     * This method reads all bytes from the provided [inputStream], encodes the data to Base64,
     * encrypts it using the encryption key, and writes the encrypted data to a file named [encryptedFileName]
     * in the app's private storage directory.
     *
     * @param inputStream The [InputStream] containing the data to encrypt.
     * @param encryptedFileName The name of the encrypted file to be created in the app's private storage.
     */
    private fun encryptInputStream(inputStream: InputStream, encryptedFileName: String) {
        val fileContent: ByteArray = inputStream.readBytes()
        val base64Content = Base64.encodeToString(fileContent, Base64.NO_WRAP)
        val encryptedData: ByteArray = encryptData(base64Content)
        val encryptedFile = File(context.filesDir, encryptedFileName)
        FileOutputStream(encryptedFile).use { it.write(encryptedData) }
    }

    /**
     * Encrypts a file from the file system and writes the encrypted data to a file in the app's private storage.
     *
     * This method reads the contents of [inputFile], encodes the data to Base64,
     * encrypts it using the encryption key, and writes the encrypted data to a file named [encryptedFileName]
     * in the app's private storage directory.
     *
     * @param inputFile The [File] object representing the file to encrypt.
     * @param encryptedFileName The name of the encrypted file to be created in the app's private storage.
     */
    override fun encryptFile(inputFile: File, encryptedFileName: String) {
        val inputStream: InputStream = FileInputStream(inputFile)
        encryptInputStream(inputStream, encryptedFileName)
    }

    /**
     * Decrypts an encrypted file from the app's private storage and returns the decrypted content as a [ByteArray].
     *
     * This method reads the encrypted file named [encryptedFileName], decrypts the data using the encryption key,
     * decodes the decrypted Base64 string back into bytes, and returns the original file content.
     *
     * @param encryptedFileName The name of the encrypted file stored in the app's private storage.
     * @return The decrypted file content as a [ByteArray].
     */
    override fun decryptFile(encryptedFileName: String): ByteArray {
        val encryptedFile = File(context.filesDir, encryptedFileName)
        val encryptedData: ByteArray = encryptedFile.readBytes()
        val decryptedBase64String: String = decryptData(encryptedData)
        return Base64.decode(decryptedBase64String, Base64.NO_WRAP)
    }

    /**
     * Retrieves the attestation certificate chain for the key.
     *
     * @param alias The alias of the key to get the attestation for.
     * @return The attestation certificate chain.
     */
    override fun getAttestationCertificateChain(alias: String): Array<Certificate> {
        val keyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply { load(null) }
        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalArgumentException("No key found under alias: $alias")
        return entry.certificateChain
    }

    /**
     * Reads InputStream as ByteArray.
     *
     * @return The ByteArray read from the InputStream.
     */
    private fun InputStream.readBytes(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val data = ByteArray(1024)
        var count: Int
        while (this.read(data).also { count = it } != -1) {
            buffer.write(data, 0, count)
        }
        return buffer.toByteArray()
    }
}
