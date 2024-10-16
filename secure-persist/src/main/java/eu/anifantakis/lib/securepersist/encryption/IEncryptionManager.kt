package eu.anifantakis.lib.securepersist.encryption

import java.io.File
import java.security.cert.Certificate

/**
 * Interface for encryption and decryption operations using secret keys.
 *
 * This interface defines the contract for an encryption manager that can encrypt and decrypt data,
 * values, and files using a secret key. Implementations can use different encryption strategies,
 * such as using the Android KeyStore or external keys.
 */
interface IEncryptionManager {

    /**
     * Encrypts the given plaintext data using the secret key.
     *
     * @param data The plaintext data to encrypt.
     * @return The encrypted data as a byte array.
     * @throws Exception If encryption fails.
     */
    fun encryptData(data: String): ByteArray

    /**
     * Decrypts the given encrypted data using the secret key.
     *
     * @param encryptedData The encrypted data as a byte array.
     * @return The decrypted plaintext data as a string.
     * @throws Exception If decryption fails.
     */
    fun decryptData(encryptedData: ByteArray): String

    /**
     * Encrypts a value and encodes it to a Base64 encoded string.
     *
     * @param value The value to encrypt.
     * @param T The type of the value to encrypt.
     * @return The encrypted value as a Base64 encoded string.
     * @throws Exception If encryption fails.
     */
    fun <T> encryptValue(value: T): String

    /**
     * Decrypts a Base64 encoded string and returns the original value.
     *
     * @param encryptedValue The encrypted value as a Base64 encoded string.
     * @param defaultValue An instance of the default value (used for type inference).
     * @param T The type of the value to decrypt.
     * @return The decrypted value, or [defaultValue] if decryption fails.
     */
    fun <T> decryptValue(encryptedValue: String, defaultValue: T): T

    /**
     * Encrypts a file from the file system and writes the encrypted data to a file in the app's private storage.
     *
     * This method reads the contents of [inputFile], encrypts it using the secret key,
     * and writes the encrypted data to a file named [encryptedFileName] in the app's private storage directory.
     *
     * @param inputFile The [File] object representing the file to encrypt.
     * @param encryptedFileName The name of the encrypted file to be created in the app's private storage.
     * @throws Exception If encryption fails or file operations fail.
     */
    fun encryptFile(inputFile: File, encryptedFileName: String)

    /**
     * Decrypts an encrypted file from the app's private storage and returns the decrypted content as a [ByteArray].
     *
     * This method reads the encrypted file named [encryptedFileName], decrypts the data using the secret key,
     * and returns the original file content.
     *
     * @param encryptedFileName The name of the encrypted file stored in the app's private storage.
     * @return The decrypted file content as a [ByteArray].
     * @throws Exception If decryption fails or file operations fail.
     */
    fun decryptFile(encryptedFileName: String): ByteArray

    /**
     * Retrieves the attestation certificate chain for the key.
     *
     * This method can be used to obtain the certificate chain associated with the encryption key,
     * which can be useful for key attestation and validation purposes.
     *
     * @param alias The alias of the key to get the attestation for. Defaults to `"keyAlias"`.
     * @return An array of [Certificate] objects representing the attestation certificate chain.
     * @throws Exception If the key is not found or retrieval fails.
     */
    fun getAttestationCertificateChain(alias: String = "keyAlias"): Array<Certificate>
}
