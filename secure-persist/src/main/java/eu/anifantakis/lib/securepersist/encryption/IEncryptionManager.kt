package eu.anifantakis.lib.securepersist.encryption

import java.io.File
import java.security.cert.Certificate

/**
 * Interface for encryption and decryption using secret keys.
 */
interface IEncryptionManager {
    /**
     * Encrypts the given data using the secret key.
     *
     * @param data The plaintext data to encrypt.
     * @return The encrypted data as a byte array.
     */
    fun encryptData(data: String): ByteArray

    /**
     * Decrypts the given encrypted data using the secret key.
     *
     * @param encryptedData The encrypted data as a byte array.
     * @return The decrypted plaintext data as a string.
     */
    fun decryptData(encryptedData: ByteArray): String

    /**
     * Encrypts a value and encodes it to a Base64 string.
     *
     * @param value The value to encrypt.
     * @return The encrypted value as a Base64 string.
     */
    fun <T> encryptValue(value: T): String

    /**
     * Decrypts a Base64 encoded string and returns the original value.
     *
     * @param encryptedValue The encrypted value as a Base64 string.
     * @param defaultValue The default value to return if decryption fails.
     * @return The decrypted value.
     */
    fun <T> decryptValue(encryptedValue: String, defaultValue: T): T

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
    fun encryptFile(inputFile: File, encryptedFileName: String)

    /**
     * Decrypts an encrypted file from the app's private storage and returns the decrypted content as a [ByteArray].
     *
     * This method reads the encrypted file named [encryptedFileName], decrypts the data using the encryption key,
     * decodes the decrypted Base64 string back into bytes, and returns the original file content.
     *
     * @param encryptedFileName The name of the encrypted file stored in the app's private storage.
     * @return The decrypted file content as a [ByteArray].
     */
    fun decryptFile(encryptedFileName: String): ByteArray

    /**
     * Retrieves the attestation certificate chain for the key.
     *
     * @param alias The alias of the key to get the attestation for.
     * @return The attestation certificate chain.
     */
    fun getAttestationCertificateChain(alias: String = "keyAlias"): Array<Certificate>
}
