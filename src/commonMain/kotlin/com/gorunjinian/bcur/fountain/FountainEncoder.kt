package com.gorunjinian.bcur.fountain

import com.gorunjinian.bcur.CRC32
import com.gorunjinian.bcur.Cbor

/**
 * Fountain Encoder for Multipart UR (MUR).
 *
 * Generates a sequence of parts where:
 * - Parts 1 to seqLen are "pure" fragments (not mixed)
 * - Parts seqLen+1 and beyond are "mixed" fragments using XOR
 *
 * Ported from https://github.com/sparrowwallet/hummingbird
 */
class FountainEncoder(
    message: ByteArray,
    private val maxFragmentLen: Int,
    private val minFragmentLen: Int = 10,
    firstSeqNum: Long = 0
) {
    val messageLen: Int = message.size
    val checksum: Long = CRC32.compute(message)
    val fragmentLen: Int
    val fragments: List<ByteArray>
    val seqLen: Int

    var seqNum: Long = firstSeqNum
        private set
    var partIndexes: List<Int> = emptyList()
        private set

    init {
        fragmentLen = findNominalFragmentLength(messageLen, minFragmentLen, maxFragmentLen)
        fragments = partitionMessage(message, fragmentLen)
        seqLen = fragments.size
    }

    fun nextPart(): Part {
        seqNum += 1
        partIndexes = FountainUtils.chooseFragments(seqNum, seqLen, checksum)
        val mixed = mix(partIndexes)
        return Part(seqNum, seqLen, messageLen, checksum, mixed)
    }

    private fun mix(partIndexes: List<Int>): ByteArray {
        var result = ByteArray(fragmentLen)
        for (index in partIndexes) {
            result = xor(fragments[index], result)
        }
        return result
    }

    fun isComplete(): Boolean = seqNum >= seqLen

    fun isSinglePart(): Boolean = seqLen == 1

    companion object {
        fun xor(a: ByteArray, b: ByteArray): ByteArray {
            val result = ByteArray(a.size)
            for (i in result.indices) {
                result[i] = (a[i].toInt() xor b[i].toInt()).toByte()
            }
            return result
        }

        fun partitionMessage(message: ByteArray, fragmentLen: Int): List<ByteArray> {
            val fragmentCount = (message.size + fragmentLen - 1) / fragmentLen
            val fragments = mutableListOf<ByteArray>()

            var start = 0
            for (i in 0 until fragmentCount) {
                val fragment = ByteArray(fragmentLen)
                val end = minOf(start + fragmentLen, message.size)
                message.copyInto(fragment, 0, start, end)
                fragments.add(fragment)
                start += fragmentLen
            }

            return fragments
        }

        fun findNominalFragmentLength(messageLen: Int, minFragmentLen: Int, maxFragmentLen: Int): Int {
            val maxFragmentCount = maxOf(1, messageLen / minFragmentLen)
            var fragmentLen = 0

            for (fragmentCount in 1..maxFragmentCount) {
                fragmentLen = (messageLen + fragmentCount - 1) / fragmentCount
                if (fragmentLen <= maxFragmentLen) {
                    break
                }
            }

            return fragmentLen
        }
    }

    /**
     * Represents a single fountain-encoded part.
     */
    data class Part(
        val seqNum: Long,
        val seqLen: Int,
        val messageLen: Int,
        val checksum: Long,
        val data: ByteArray
    ) {
        fun toCborBytes(): ByteArray {
            return Cbor.encodeArrayHeader(5) +
                    Cbor.encodeUnsignedInt(seqNum.toULong()) +
                    Cbor.encodeUnsignedInt(seqLen.toULong()) +
                    Cbor.encodeUnsignedInt(messageLen.toULong()) +
                    Cbor.encodeUnsignedInt(checksum.toULong()) +
                    Cbor.encodeByteString(data)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Part
            return seqNum == other.seqNum &&
                   seqLen == other.seqLen &&
                   messageLen == other.messageLen &&
                   checksum == other.checksum &&
                   data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            var result = (seqNum xor (seqNum ushr 32)).toInt()
            result = 31 * result + seqLen
            result = 31 * result + messageLen
            result = 31 * result + (checksum xor (checksum ushr 32)).toInt()
            result = 31 * result + data.contentHashCode()
            return result
        }

        companion object {
            fun fromCborBytes(cborData: ByteArray): Part {
                var offset = 0

                val (count, off1) = Cbor.decodeArrayHeader(cborData, offset)
                require(count == 5) { "Expected CBOR array of 5 elements, got: $count" }
                offset = off1

                val (seqNum, off2) = Cbor.decodeUnsignedInt(cborData, offset)
                offset = off2

                val (seqLen, off3) = Cbor.decodeUnsignedInt(cborData, offset)
                offset = off3

                val (messageLen, off4) = Cbor.decodeUnsignedInt(cborData, offset)
                offset = off4

                val (checksum, off5) = Cbor.decodeUnsignedInt(cborData, offset)
                offset = off5

                val (data, _) = Cbor.decodeByteString(cborData, offset)

                return Part(seqNum.toLong(), seqLen.toInt(), messageLen.toInt(), checksum.toLong(), data)
            }
        }
    }
}
