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
 * Provides default values matching the original implementation and allows custom configurations.
 */
data class EncryptionConfig(
    val keyAlgorithm: String = KeyProperties.KEY_ALGORITHM_AES,
    val blockMode: BlockMode = BlockMode.GCM,
    val encryptionPadding: String = KeyProperties.ENCRYPTION_PADDING_NONE,
    val keySize: KeySize = KeySize.BITS_256,
    val tagSize: TagSize = TagSize.BITS_128,
    val useKeystore: Boolean = true,
    val keystoreType: String = "AndroidKeyStore"
) {
    val transformation: String
        get() = "$keyAlgorithm/${blockMode.mode}/$encryptionPadding"

    // IV size is determined by block mode
    val ivSize: Int
        get() = blockMode.ivSize

    init {
        // Only need to validate that block mode matches padding
        // Other validations are handled by enum restrictions
        require(!(blockMode == BlockMode.GCM && encryptionPadding != KeyProperties.ENCRYPTION_PADDING_NONE)) {
            "GCM mode requires no padding"
        }
    }

    companion object {
        val DEFAULT = EncryptionConfig()
    }
}