package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.UR

/**
 * Abstract base class for all BCR registry types.
 *
 * Subclasses implement [toCbor] for CBOR serialization and provide
 * a companion `fromCbor` for deserialization.
 */
abstract class RegistryItem : CborSerializable {
    abstract val registryType: RegistryType

    /** Encode this registry item as a UR. */
    fun toUR(): UR {
        val encoded = toCbor().encode()
        return UR(registryType.type, encoded)
    }

    companion object {
        /**
         * Convert a byte array (up to 4 bytes, big-endian) to a ULong.
         * Used for fingerprint fields in BCR registry types.
         */
        fun fingerprintToULong(bytes: ByteArray): ULong {
            val b = if (bytes.size > 4) bytes.copyOfRange(bytes.size - 4, bytes.size) else bytes
            var result = 0UL
            for (byte in b) {
                result = (result shl 8) or (byte.toInt() and 0xFF).toULong()
            }
            return result
        }

        /**
         * Convert a ULong to a byte array of [numBytes] length (big-endian).
         * Used for fingerprint fields in BCR registry types.
         */
        fun uLongToFingerprint(value: ULong, numBytes: Int = 4): ByteArray {
            val result = ByteArray(numBytes)
            for (i in numBytes - 1 downTo 0) {
                result[numBytes - 1 - i] = (value shr (i * 8)).toByte()
            }
            return result
        }
    }
}
