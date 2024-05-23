package eu.anifantakis.lib.securepersist.encryption

import java.security.cert.Certificate
import javax.crypto.SecretKey

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
     * Retrieves the attestation certificate chain for the key.
     *
     * @param alias The alias of the key to get the attestation for.
     * @return The attestation certificate chain.
     */
    fun getAttestationCertificateChain(alias: String = "keyAlias"): Array<Certificate>

    /**
     * Sets an external secret key for encryption and decryption.
     *
     * @param secretKey The external secret key to be used.
     */
    fun setExternalKey(secretKey: SecretKey)
}
