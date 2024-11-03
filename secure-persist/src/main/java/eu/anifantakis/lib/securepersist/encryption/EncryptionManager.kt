package eu.anifantakis.lib.securepersist.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
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

/**
 * [EncryptionManager] class handles encryption and decryption using the Android KeyStore system or an external key.
 *
 * It provides methods to encrypt and decrypt data, values, and files, either using a key stored in the Android KeyStore
 * or an externally provided [SecretKey]. It supports configurable encryption parameters.
 */
class EncryptionManager : IEncryptionManager {

    private val context: Context
    private val secretKey: SecretKey
    private val config: EncryptionConfig

    companion object {
        private val gson = Gson()

        /**
         * Generates a new external [SecretKey] with specified configuration.
         *
         * @param config The encryption configuration to use. Uses [EncryptionConfig.DEFAULT] if not specified.
         * @return The generated secret key.
         */
        fun generateExternalKey(config: EncryptionConfig = EncryptionConfig.DEFAULT): SecretKey {
            val keyGenerator = KeyGenerator.getInstance(config.keyAlgorithm)
            keyGenerator.init(config.keySize.bits)
            return keyGenerator.generateKey()
        }

        /**
         * Encrypts a value and encodes it to a Base64 string using an external key.
         *
         * @param value The value to encrypt.
         * @param secretKey The secret key to use for encryption.
         * @param config The encryption configuration to use.
         * @return The encrypted value as a Base64 encoded string.
         */
        fun <T> encryptValue(value: T, secretKey: SecretKey, config: EncryptionConfig = EncryptionConfig.DEFAULT): String {
            val stringValue = gson.toJson(value)
            val encryptedData = encryptData(stringValue, secretKey, config)
            return Base64.encodeToString(encryptedData, Base64.NO_WRAP)
        }

        /**
         * Decrypts a Base64 encoded string and returns the original value using an external key.
         *
         * @param encryptedValue The encrypted value as a Base64 encoded string.
         * @param defaultValue An instance of the default value (used for type inference).
         * @param secretKey The secret key to use for decryption.
         * @param config The encryption configuration to use.
         * @return The decrypted value.
         */
        fun <T> decryptValue(
            encryptedValue: String,
            defaultValue: T,
            secretKey: SecretKey,
            config: EncryptionConfig = EncryptionConfig.DEFAULT
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
         * Encrypts the given data using the provided secret key.
         *
         * @param data The plaintext data to encrypt.
         * @param secretKey The secret key to use for encryption.
         * @param config The encryption configuration to use.
         * @return The encrypted data as a byte array.
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
         * @param encryptedData The encrypted data as a byte array.
         * @param secretKey The secret key to use for decryption.
         * @param config The encryption configuration to use.
         * @return The decrypted plaintext data as a string.
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

        private fun combineIvAndEncryptedData(iv: ByteArray, encryptedData: ByteArray): ByteArray {
            val combined = ByteArray(iv.size + encryptedData.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
            return combined
        }

        fun encodeSecretKey(secretKey: SecretKey): String {
            val encodedKey = secretKey.encoded
            return Base64.encodeToString(encodedKey, Base64.NO_WRAP)
        }

        fun decodeSecretKey(encodedKey: String, config: EncryptionConfig = EncryptionConfig.DEFAULT): SecretKey {
            val decodedKey = Base64.decode(encodedKey, Base64.NO_WRAP)
            return SecretKeySpec(decodedKey, 0, decodedKey.size, config.keyAlgorithm)
        }
    }

    /**
     * Constructor for [EncryptionManager] using the Android KeyStore with custom configuration.
     *
     * @param context The application context.
     * @param keyAlias The alias for the encryption key in the KeyStore.
     * @param config The encryption configuration to use.
     */
    constructor(
        context: Context,
        keyAlias: String,
        config: EncryptionConfig = EncryptionConfig.DEFAULT
    ) {
        this.context = context
        this.config = config

        if (!config.useKeystore) {
            throw IllegalArgumentException("This constructor requires useKeystore=true in config")
        }

        val keyStore = KeyStore.getInstance(config.keystoreType).apply { load(null) }
        val keyFromStore = keyStore.getKey(keyAlias, null) as? SecretKey

        this.secretKey = if (keyFromStore != null) {
            keyFromStore
        } else {
            val keyGenerator = KeyGenerator.getInstance(config.keyAlgorithm, config.keystoreType)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(config.blockMode.mode)
                .setEncryptionPaddings(config.encryptionPadding)
                .setKeySize(config.keySize.bits)
                .setRandomizedEncryptionRequired(false)  // Allow caller-provided IV
                .build()
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
            keyStore.getKey(keyAlias, null) as SecretKey
        }
    }

    /**
     * Constructor for [EncryptionManager] using an external key with custom configuration.
     *
     * @param context The application context.
     * @param externalKey The external secret key for encryption and decryption.
     * @param config The encryption configuration to use.
     */
    constructor(
        context: Context,
        externalKey: SecretKey,
        config: EncryptionConfig = EncryptionConfig.DEFAULT
    ) {
        this.context = context
        this.secretKey = externalKey
        this.config = config
    }

    override fun encryptData(data: String): ByteArray {
        return Companion.encryptData(data, secretKey, config)
    }

    override fun decryptData(encryptedData: ByteArray): String {
        return Companion.decryptData(encryptedData, secretKey, config)
    }

    override fun <T> encryptValue(value: T): String {
        return Companion.encryptValue(value, secretKey, config)
    }

    override fun <T> decryptValue(encryptedValue: String, defaultValue: T): T {
        return Companion.decryptValue(encryptedValue, defaultValue, secretKey, config)
    }

    private fun encryptInputStream(inputStream: InputStream, encryptedFileName: String) {
        val fileContent: ByteArray = inputStream.readBytes()
        val base64Content = Base64.encodeToString(fileContent, Base64.NO_WRAP)
        val encryptedData: ByteArray = encryptData(base64Content)
        val encryptedFile = File(context.filesDir, encryptedFileName)
        FileOutputStream(encryptedFile).use { it.write(encryptedData) }
    }

    override fun encryptFile(inputFile: File, encryptedFileName: String) {
        val inputStream: InputStream = FileInputStream(inputFile)
        encryptInputStream(inputStream, encryptedFileName)
    }

    override fun decryptFile(encryptedFileName: String): ByteArray {
        val encryptedFile = File(context.filesDir, encryptedFileName)
        val encryptedData: ByteArray = encryptedFile.readBytes()
        val decryptedBase64String: String = decryptData(encryptedData)
        return Base64.decode(decryptedBase64String, Base64.NO_WRAP)
    }

    override fun getAttestationCertificateChain(alias: String): Array<Certificate> {
        val keyStore = KeyStore.getInstance(config.keystoreType).apply { load(null) }
        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalArgumentException("No key found under alias: $alias")
        return entry.certificateChain
    }

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