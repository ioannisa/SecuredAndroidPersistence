package eu.anifantakis.lib.securepersist.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import eu.anifantakis.lib.securepersist.internal.createGson
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val keystoreType: String = "AndroidKeyStore"

/**
 * Manages encryption and decryption operations using the Android KeyStore system or external keys.
 *
 * This class provides secure encryption capabilities for various data types including primitive values,
 * complex objects, and files. It supports both the Android KeyStore system for key management and
 * external keys for custom key management scenarios.
 *
 * The encryption process uses either GCM or CBC block modes with configurable parameters through
 * [EncryptionConfig]. For complex objects, the class uses Gson for serialization before encryption
 * and deserialization after decryption.
 *
 * @property context The Android context used for file operations
 * @property secretKey The encryption key, either from Android KeyStore or externally provided
 * @property config Configuration for encryption parameters including algorithm, block mode, and key size
 * @property gson Gson instance for serializing/deserializing complex objects
 *
 * @see IEncryptionManager
 * @see EncryptionConfig
 */
class EncryptionManager : IEncryptionManager {

    private val context: Context
    private val secretKey: SecretKey
    private val config: EncryptionConfig
    private val gson: Gson


    companion object {
        /**
         * Generates a new external encryption key with the specified configuration.
         *
         * @param config Configuration parameters for key generation. Uses [EncryptionConfig.DEFAULT] if not specified.
         * @return A newly generated [SecretKey] that can be used for encryption/decryption.
         */
        fun generateExternalKey(config: EncryptionConfig = EncryptionConfig.DEFAULT): SecretKey {
            val keyGenerator = KeyGenerator.getInstance(config.keyAlgorithm)
            keyGenerator.init(config.keySize.bits)
            return keyGenerator.generateKey()
        }

        /**
         * Encrypts a value and encodes it to a Base64 string using an external key.
         *
         * @param T The type of the value to encrypt
         * @param value The value to encrypt
         * @param secretKey The secret key to use for encryption
         * @param config The encryption configuration parameters
         * @param gson Gson instance for serializing the value
         * @return The encrypted value as a Base64 encoded string
         */
        fun <T> encryptValue(
            value: T,
            secretKey: SecretKey,
            config: EncryptionConfig = EncryptionConfig.DEFAULT,
            gson: Gson = createGson()
        ): String {
            val stringValue = gson.toJson(value)
            val encryptedData = encryptData(stringValue, secretKey, config)
            return Base64.encodeToString(encryptedData, Base64.NO_WRAP)
        }

        /**
         * Decrypts a Base64 encoded string and returns the original value using an external key.
         *
         * @param T The type to deserialize the decrypted value into
         * @param encryptedValue The encrypted value as a Base64 encoded string
         * @param defaultValue Default value to return in case of decryption failure
         * @param secretKey The secret key to use for decryption
         * @param config The encryption configuration parameters
         * @param gson Gson instance for deserializing the value
         * @return The decrypted value of type T, or the default value if decryption fails
         */
        fun <T> decryptValue(
            encryptedValue: String,
            defaultValue: T,
            secretKey: SecretKey,
            config: EncryptionConfig = EncryptionConfig.DEFAULT,
            gson: Gson = createGson()
        ): T {
            return try {
                val encryptedData = Base64.decode(encryptedValue, Base64.NO_WRAP)
                val jsonString = decryptData(encryptedData, secretKey, config)
                gson.fromJson(jsonString, defaultValue!!::class.java) as T
            } catch (e: Exception) {
                Log.e("EncryptionManager", "Decryption failed", e)
                defaultValue
            }
        }

        /**
         * Encrypts the given string data using the provided secret key.
         *
         * @param data The string data to encrypt
         * @param secretKey The secret key to use for encryption
         * @param config The encryption configuration parameters
         * @return The encrypted data as a byte array, including the IV
         * @throws Exception if encryption fails
         */
        fun encryptData(
            data: String,
            secretKey: SecretKey,
            config: EncryptionConfig = EncryptionConfig.DEFAULT
        ): ByteArray {
            return try {
                val cipher = Cipher.getInstance(config.transformation)

                when (config.blockMode) {
                    BlockMode.GCM -> {
                        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                    }
                    BlockMode.CBC -> {
                        val iv = ByteArray(config.ivSize)
                        SecureRandom().nextBytes(iv)
                        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
                    }
                }

                val iv = cipher.iv
                val encryptedData = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
                combineIvAndEncryptedData(iv, encryptedData)
            } catch (e: Exception) {
                Log.e("EncryptionManager", "Encryption failed", e)
                throw e
            }
        }

        /**
         * Decrypts the given encrypted data using the provided secret key.
         *
         * @param encryptedData The encrypted data (including IV) as a byte array
         * @param secretKey The secret key to use for decryption
         * @param config The encryption configuration parameters
         * @return The decrypted data as a string
         * @throws Exception if decryption fails or the data is invalid
         */
        fun decryptData(
            encryptedData: ByteArray,
            secretKey: SecretKey,
            config: EncryptionConfig = EncryptionConfig.DEFAULT
        ): String {
            return try {
                val ivSize = config.ivSize

                if (encryptedData.size < ivSize) {
                    throw IllegalArgumentException("Encrypted data is too short to contain a valid IV")
                }

                val cipher = Cipher.getInstance(config.transformation)

                val iv = ByteArray(ivSize)
                System.arraycopy(encryptedData, 0, iv, 0, iv.size)

                val encryptedBytes = ByteArray(encryptedData.size - iv.size)
                System.arraycopy(encryptedData, iv.size, encryptedBytes, 0, encryptedBytes.size)

                when (config.blockMode) {
                    BlockMode.GCM -> {
                        val ivSpec = GCMParameterSpec(config.tagSize.bits, iv)
                        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
                    }
                    BlockMode.CBC -> {
                        val ivSpec = IvParameterSpec(iv)
                        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
                    }
                }

                String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8)
            } catch (e: Exception) {
                Log.e("EncryptionManager", "Decryption failed", e)
                throw e
            }
        }

        /**
         * Combines the initialization vector (IV) and encrypted data into a single byte array.
         *
         * @param iv The initialization vector
         * @param encryptedData The encrypted data
         * @return Combined byte array containing IV followed by encrypted data
         */
        private fun combineIvAndEncryptedData(iv: ByteArray, encryptedData: ByteArray): ByteArray {
            val combined = ByteArray(iv.size + encryptedData.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
            return combined
        }

        /**
         * Encodes a secret key to a Base64 string for storage or transmission.
         *
         * @param secretKey The secret key to encode
         * @return Base64 encoded string representation of the key
         */
        fun encodeSecretKey(secretKey: SecretKey): String {
            val encodedKey = secretKey.encoded
            return Base64.encodeToString(encodedKey, Base64.NO_WRAP)
        }

        /**
         * Decodes a Base64 encoded string back into a secret key.
         *
         * @param encodedKey The Base64 encoded key string
         * @param config Configuration parameters for key creation
         * @return The decoded secret key
         */
        fun decodeSecretKey(encodedKey: String, config: EncryptionConfig = EncryptionConfig.DEFAULT): SecretKey {
            val decodedKey = Base64.decode(encodedKey, Base64.NO_WRAP)
            return SecretKeySpec(decodedKey, 0, decodedKey.size, config.keyAlgorithm)
        }
    }

    /**
     * Creates an EncryptionManager instance using the Android KeyStore system.
     *
     * This constructor initializes the manager with a key from the Android KeyStore system. If the key
     * doesn't exist, it creates a new one with the specified parameters.
     *
     * @param context Android context for file operations
     * @param keyAlias Alias for the key in the Android KeyStore
     * @param config Encryption configuration parameters
     * @param gson Gson instance for serialization/deserialization
     */
    constructor(
        context: Context,
        keyAlias: String,
        config: EncryptionConfig = EncryptionConfig.DEFAULT,
        gson: Gson = createGson(),
    ) {
        this.context = context
        this.config = config
        this.gson = gson

        val keyStore = KeyStore.getInstance(keystoreType).apply { load(null) }
        val keyFromStore = keyStore.getKey(keyAlias, null) as? SecretKey

        this.secretKey = if (keyFromStore != null) {
            keyFromStore
        } else {
            val keyGenerator = KeyGenerator.getInstance(config.keyAlgorithm, keystoreType)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(config.blockMode.mode)
                .setEncryptionPaddings(config.encryptionPadding)
                .setKeySize(config.keySize.bits)
                .setRandomizedEncryptionRequired(false)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
            keyStore.getKey(keyAlias, null) as SecretKey
        }
    }

    /**
     * Creates an EncryptionManager instance using an external encryption key.
     *
     * This constructor allows for using a custom encryption key instead of the Android KeyStore system.
     *
     * @param context Android context for file operations
     * @param externalKey External secret key to use for encryption/decryption
     * @param config Encryption configuration parameters
     * @param gson Gson instance for serialization/deserialization
     */
    constructor(
        context: Context,
        externalKey: SecretKey,
        config: EncryptionConfig = EncryptionConfig.DEFAULT,
        gson: Gson = createGson(),
    ) {
        this.context = context
        this.secretKey = externalKey
        this.config = config
        this.gson = gson
    }

    /**
     * Encrypts a string into a byte array using the manager's configuration.
     *
     * @param data The string to encrypt
     * @return Encrypted data as a byte array
     */
    override fun encryptData(data: String): ByteArray {
        return Companion.encryptData(data, secretKey, config)
    }

    /**
     * Decrypts a byte array back into the original string.
     *
     * @param encryptedData The encrypted data to decrypt
     * @return The decrypted string
     */
    override fun decryptData(encryptedData: ByteArray): String {
        return Companion.decryptData(encryptedData, secretKey, config)
    }

    /**
     * Encrypts a value of any type using the manager's configuration.
     *
     * @param T The type of the value to encrypt
     * @param value The value to encrypt
     * @return The encrypted value as a Base64 encoded string
     */
    override fun <T> encryptValue(value: T): String {
        return Companion.encryptValue(value, secretKey, config, gson)
    }

    /**
     * Decrypts a previously encrypted value.
     *
     * @param T The type to deserialize the decrypted value into
     * @param encryptedValue The encrypted value as a Base64 encoded string
     * @param defaultValue Default value to return if decryption fails
     * @return The decrypted value of type T, or the default value if decryption fails
     */
    override fun <T> decryptValue(encryptedValue: String, defaultValue: T): T {
        return Companion.decryptValue(encryptedValue, defaultValue, secretKey, config, gson)
    }

    /**
     * Encrypts a file and saves it with the specified filename.
     *
     * @param inputFile The file to encrypt
     * @param encryptedFileName Name for the encrypted output file
     */
    override fun encryptFile(inputFile: File, encryptedFileName: String) {
        val inputStream: InputStream = FileInputStream(inputFile)
        encryptInputStream(inputStream, encryptedFileName)
    }

    /**
     * Encrypts an input stream and saves it as a file.
     *
     * @param inputStream The input stream to encrypt
     * @param encryptedFileName Name for the encrypted output file
     */
    private fun encryptInputStream(inputStream: InputStream, encryptedFileName: String) {
        val fileContent: ByteArray = inputStream.readBytes()
        val base64Content = Base64.encodeToString(fileContent, Base64.NO_WRAP)
        val encryptedData: ByteArray = encryptData(base64Content)
        val encryptedFile = File(context.filesDir, encryptedFileName)
        FileOutputStream(encryptedFile).use { it.write(encryptedData) }
    }

    /**
     * Decrypts a previously encrypted file.
     *
     * @param encryptedFileName Name of the encrypted file
     * @return The decrypted file contents as a byte array
     */
    override fun decryptFile(encryptedFileName: String): ByteArray {
        val encryptedFile = File(context.filesDir, encryptedFileName)
        val encryptedData: ByteArray = encryptedFile.readBytes()
        val decryptedBase64String: String = decryptData(encryptedData)
        return Base64.decode(decryptedBase64String, Base64.NO_WRAP)
    }

    /**
     * Retrieves the attestation certificate chain for a key in the KeyStore.
     *
     * @param alias The alias of the key
     * @return Array of certificates in the attestation chain
     * @throws IllegalArgumentException if no key is found under the specified alias
     */
    override fun getAttestationCertificateChain(alias: String): Array<Certificate> {
        val keyStore = KeyStore.getInstance(keystoreType).apply { load(null) }
        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalArgumentException("No key found under alias: $alias")
        return entry.certificateChain
    }

    /**
     * Extension function to read a byte array from an InputStream.
     *
     * @return The contents of the InputStream as a byte array
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