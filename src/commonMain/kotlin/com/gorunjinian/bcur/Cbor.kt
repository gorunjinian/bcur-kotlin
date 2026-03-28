package com.gorunjinian.bcur

/**
 * Minimal CBOR encode/decode utilities for BC-UR.
 *
 * Only implements the subset needed for UR:
 * - Major type 0: Unsigned integers
 * - Major type 2: Byte strings
 * - Major type 4: Arrays
 *
 * This avoids pulling in a full CBOR library (which would break KMP/watchOS).
 */
object Cbor {

    // --- Encoding ---

    fun encodeUnsignedInt(value: ULong): ByteArray {
        return when {
            value <= 23UL -> byteArrayOf(value.toByte())
            value <= 0xFFUL -> byteArrayOf(0x18, value.toByte())
            value <= 0xFFFFUL -> byteArrayOf(
                0x19,
                (value shr 8).toByte(),
                value.toByte()
            )
            value <= 0xFFFFFFFFUL -> byteArrayOf(
                0x1A.toByte(),
                (value shr 24).toByte(),
                (value shr 16).toByte(),
                (value shr 8).toByte(),
                value.toByte()
            )
            else -> byteArrayOf(
                0x1B.toByte(),
                (value shr 56).toByte(),
                (value shr 48).toByte(),
                (value shr 40).toByte(),
                (value shr 32).toByte(),
                (value shr 24).toByte(),
                (value shr 16).toByte(),
                (value shr 8).toByte(),
                value.toByte()
            )
        }
    }

    fun encodeByteString(data: ByteArray): ByteArray {
        val len = data.size.toULong()
        val header = when {
            len <= 23UL -> byteArrayOf((0x40 + len.toInt()).toByte())
            len <= 0xFFUL -> byteArrayOf(0x58, len.toByte())
            len <= 0xFFFFUL -> byteArrayOf(0x59, (len shr 8).toByte(), len.toByte())
            else -> byteArrayOf(
                0x5A.toByte(),
                (len shr 24).toByte(),
                (len shr 16).toByte(),
                (len shr 8).toByte(),
                len.toByte()
            )
        }
        return header + data
    }

    fun encodeArrayHeader(count: Int): ByteArray {
        val c = count.toULong()
        return when {
            c <= 23UL -> byteArrayOf((0x80 + c.toInt()).toByte())
            c <= 0xFFUL -> byteArrayOf(0x98.toByte(), c.toByte())
            c <= 0xFFFFUL -> byteArrayOf(0x99.toByte(), (c shr 8).toByte(), c.toByte())
            else -> byteArrayOf(
                0x9A.toByte(),
                (c shr 24).toByte(),
                (c shr 16).toByte(),
                (c shr 8).toByte(),
                c.toByte()
            )
        }
    }

    // --- Decoding ---

    fun decodeUnsignedInt(data: ByteArray, offset: Int): Pair<ULong, Int> {
        val initial = data[offset].toInt() and 0xFF
        val additionalInfo = initial and 0x1F

        return when {
            additionalInfo <= 23 -> additionalInfo.toULong() to (offset + 1)
            additionalInfo == 24 -> (data[offset + 1].toInt() and 0xFF).toULong() to (offset + 2)
            additionalInfo == 25 -> {
                val value = ((data[offset + 1].toInt() and 0xFF) shl 8) or
                           (data[offset + 2].toInt() and 0xFF)
                value.toULong() to (offset + 3)
            }
            additionalInfo == 26 -> {
                val value = ((data[offset + 1].toLong() and 0xFF) shl 24) or
                           ((data[offset + 2].toLong() and 0xFF) shl 16) or
                           ((data[offset + 3].toLong() and 0xFF) shl 8) or
                           (data[offset + 4].toLong() and 0xFF)
                value.toULong() to (offset + 5)
            }
            additionalInfo == 27 -> {
                val value = ((data[offset + 1].toLong() and 0xFF) shl 56) or
                           ((data[offset + 2].toLong() and 0xFF) shl 48) or
                           ((data[offset + 3].toLong() and 0xFF) shl 40) or
                           ((data[offset + 4].toLong() and 0xFF) shl 32) or
                           ((data[offset + 5].toLong() and 0xFF) shl 24) or
                           ((data[offset + 6].toLong() and 0xFF) shl 16) or
                           ((data[offset + 7].toLong() and 0xFF) shl 8) or
                           (data[offset + 8].toLong() and 0xFF)
                value.toULong() to (offset + 9)
            }
            else -> throw IllegalArgumentException("Unsupported CBOR unsigned int encoding: $additionalInfo")
        }
    }

    fun decodeByteString(data: ByteArray, offset: Int): Pair<ByteArray, Int> {
        val initial = data[offset].toInt() and 0xFF
        val additionalInfo = initial and 0x1F

        val (length, dataOffset) = when {
            additionalInfo <= 23 -> additionalInfo to (offset + 1)
            additionalInfo == 24 -> (data[offset + 1].toInt() and 0xFF) to (offset + 2)
            additionalInfo == 25 -> {
                val len = ((data[offset + 1].toInt() and 0xFF) shl 8) or
                         (data[offset + 2].toInt() and 0xFF)
                len to (offset + 3)
            }
            additionalInfo == 26 -> {
                val len = ((data[offset + 1].toInt() and 0xFF) shl 24) or
                         ((data[offset + 2].toInt() and 0xFF) shl 16) or
                         ((data[offset + 3].toInt() and 0xFF) shl 8) or
                         (data[offset + 4].toInt() and 0xFF)
                len to (offset + 5)
            }
            else -> throw IllegalArgumentException("Unsupported CBOR byte string length: $additionalInfo")
        }

        return data.copyOfRange(dataOffset, dataOffset + length) to (dataOffset + length)
    }

    fun decodeArrayHeader(data: ByteArray, offset: Int): Pair<Int, Int> {
        val initial = data[offset].toInt() and 0xFF
        val additionalInfo = initial and 0x1F

        return when {
            additionalInfo <= 23 -> additionalInfo to (offset + 1)
            additionalInfo == 24 -> (data[offset + 1].toInt() and 0xFF) to (offset + 2)
            additionalInfo == 25 -> {
                val count = ((data[offset + 1].toInt() and 0xFF) shl 8) or
                           (data[offset + 2].toInt() and 0xFF)
                count to (offset + 3)
            }
            else -> throw IllegalArgumentException("Unsupported CBOR array header: $additionalInfo")
        }
    }

    /**
     * Wrap raw bytes in a CBOR byte string (major type 2).
     * This is the CBOR encoding used by UR for raw data payloads.
     */
    fun wrapInByteString(data: ByteArray): ByteArray = encodeByteString(data)

    /**
     * Unwrap a CBOR byte string to get the raw bytes.
     */
    fun unwrapByteString(cbor: ByteArray): ByteArray {
        val (data, _) = decodeByteString(cbor, 0)
        return data
    }
}
