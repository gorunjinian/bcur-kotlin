package com.gorunjinian.bcur

import com.gorunjinian.bcur.fountain.FountainEncoder

/**
 * UR Encoder for Uniform Resources.
 *
 * Supports:
 * - Single-part UR: ur:<type>/<message>
 * - Multi-part UR: ur:<type>/<seqNum>-<seqLen>/<fragment>
 *
 * Ported from https://github.com/sparrowwallet/hummingbird
 */
class UREncoder(
    private val ur: UR,
    maxFragmentLen: Int = 100,
    minFragmentLen: Int = 10,
    firstSeqNum: Long = 0
) {
    private val fountainEncoder = FountainEncoder(
        ur.cborData,
        maxFragmentLen,
        minFragmentLen,
        firstSeqNum
    )

    fun isComplete(): Boolean = fountainEncoder.isComplete()

    fun isSinglePart(): Boolean = fountainEncoder.isSinglePart()

    fun nextPart(): String {
        val part = fountainEncoder.nextPart()
        return if (isSinglePart()) {
            encode(ur)
        } else {
            encodePart(ur.type, part)
        }
    }

    val seqNum: Long get() = fountainEncoder.seqNum
    val seqLen: Int get() = fountainEncoder.seqLen
    val partIndexes: List<Int> get() = fountainEncoder.partIndexes

    companion object {
        fun encode(ur: UR): String {
            val encoded = Bytewords.encode(ur.cborData, Bytewords.Style.MINIMAL)
            return "${UR.UR_PREFIX}:${ur.type}/$encoded"
        }

        private fun encodePart(type: String, part: FountainEncoder.Part): String {
            val seq = "${part.seqNum}-${part.seqLen}"
            val body = Bytewords.encode(part.toCborBytes(), Bytewords.Style.MINIMAL)
            return "${UR.UR_PREFIX}:$type/$seq/$body"
        }
    }
}
