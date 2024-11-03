package eu.anifantakis.lib.securepersist.encryption

import android.security.keystore.KeyProperties


/**
 * Supported key sizes for AES encryption.
 */
enum class KeySize(val bits: Int) {
    BITS_128(128),
    BITS_192(192),
    BITS_256(256);
}

/**
 * Supported authentication tag sizes for GCM mode.
 */
enum class TagSize(val bits: Int) {
    BITS_128(128),
    BITS_120(120),
    BITS_112(112),
    BITS_104(104),
    BITS_96(96);
}

/**
 * Supported block modes for encryption.
 */
enum class BlockMode(val mode: String, val ivSize: Int) {
    GCM(KeyProperties.BLOCK_MODE_GCM, 12),
    CBC(KeyProperties.BLOCK_MODE_CBC, 16);
}

/**
 * Configuration class for encryption settings.
 * Provides default values matching the Android's recommended security practices:
 * - AES encryption algorithm
 * - GCM block mode for authenticated encryption
 * - 256-bit key size
 * - Hardware-backed key storage when available
 *
 * @property keyAlgorithm The encryption algorithm to use. Defaults to AES.
 * @property blockMode The block cipher mode of operation. Defaults to GCM which provides authenticated encryption.
 *                     See [BlockMode] for available options.
 * @property encryptionPadding The padding scheme to use. For GCM mode, must be NONE as GCM handles padding internally.
 *                             For CBC mode, typically PKCS7 is used.
 * @property keySize The size of the encryption key in bits. See [KeySize] for available options.
 *                   Larger keys provide more security but may impact performance.
 * @property tagSize The size of the authentication tag when using GCM mode. See [TagSize] for available options.
 *                   Only applicable for GCM mode, ignored for other modes.
 */
data class EncryptionConfig(
    val keyAlgorithm: String = KeyProperties.KEY_ALGORITHM_AES,
    val blockMode: BlockMode = BlockMode.GCM,
    val encryptionPadding: String = KeyProperties.ENCRYPTION_PADDING_NONE,
    val keySize: KeySize = KeySize.BITS_256,
    val tagSize: TagSize = TagSize.BITS_128
) {
    val transformation: String
        get() = "$keyAlgorithm/${blockMode.mode}/$encryptionPadding"

    val ivSize: Int
        get() = blockMode.ivSize

    init {
        require(!(blockMode == BlockMode.GCM && encryptionPadding != KeyProperties.ENCRYPTION_PADDING_NONE)) {
            "GCM mode requires no padding"
        }
    }

    companion object {
        val DEFAULT = EncryptionConfig()
    }
}