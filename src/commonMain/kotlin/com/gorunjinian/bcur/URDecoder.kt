package com.gorunjinian.bcur

import com.gorunjinian.bcur.fountain.FountainDecoder
import com.gorunjinian.bcur.fountain.FountainEncoder

/**
 * UR Decoder for Uniform Resources.
 *
 * Supports:
 * - Single-part UR decoding via static [decode] method
 * - Multi-part UR decoding via [receivePart] (streaming, fountain-coded)
 *
 * Ported from https://github.com/sparrowwallet/hummingbird
 */
class URDecoder {
    private val fountainDecoder = FountainDecoder()
    private var expectedType: String? = null

    var result: Result? = null
        private set

    val expectedPartCount: Int
        get() = fountainDecoder.expectedPartCount

    val receivedPartIndexes: Set<Int>
        get() = fountainDecoder.receivedPartIndexes

    val lastPartIndexes: Set<Int>?
        get() = fountainDecoder.lastPartIndexes

    val processedPartsCount: Int
        get() = fountainDecoder.processedPartsCount

    val estimatedPercentComplete: Double
        get() = fountainDecoder.estimatedPercentComplete

    /**
     * Receive a UR string part (single or multi-part).
     * Returns true if the part was accepted.
     *
     * For single-part URs, [result] is set immediately after one call.
     * For multi-part URs, call repeatedly until [result] is non-null.
     */
    fun receivePart(string: String): Boolean {
        try {
            if (result != null) return false

            val parsed = parse(string)
            if (!validatePart(parsed.type)) return false

            // Single-part UR
            if (parsed.components.size == 1) {
                val body = parsed.components[0]
                result = Result(ResultType.SUCCESS, decode(parsed.type, body), null)
                return true
            }

            // Multi-part UR: must have exactly 2 components (seq/fragment)
            if (parsed.components.size != 2) {
                throw UR.InvalidURException("Invalid path length")
            }

            val seq = parsed.components[0]
            val fragment = parsed.components[1]

            val matchResult = SEQUENCE_COMPONENT_REGEX.matchEntire(seq)
            if (matchResult != null) {
                val seqNum = matchResult.groupValues[1].toLong()
                val seqLen = matchResult.groupValues[2].toInt()

                val cbor = Bytewords.decode(fragment, Bytewords.Style.MINIMAL)
                val part = FountainEncoder.Part.fromCborBytes(cbor)

                if (seqNum != part.seqNum || seqLen != part.seqLen) {
                    return false
                }

                if (!fountainDecoder.receivePart(part)) {
                    return false
                }

                val decoderResult = fountainDecoder.result
                if (decoderResult != null) {
                    result = when (decoderResult.type) {
                        ResultType.SUCCESS -> Result(
                            ResultType.SUCCESS,
                            UR(parsed.type, decoderResult.data!!),
                            null
                        )
                        ResultType.FAILURE -> Result(
                            ResultType.FAILURE,
                            null,
                            decoderResult.error
                        )
                    }
                }

                return true
            } else {
                throw UR.InvalidURException("Invalid sequence component: $seq")
            }
        } catch (_: UR.InvalidURException) {
            return false
        } catch (_: IllegalArgumentException) {
            return false
        }
    }

    private fun validatePart(type: String): Boolean {
        if (expectedType == null) {
            if (!UR.isValidType(type)) return false
            expectedType = type
        } else {
            return expectedType == type
        }
        return true
    }

    class Result(
        val type: ResultType,
        val ur: UR?,
        val error: String?
    )

    companion object {
        private val SEQUENCE_COMPONENT_REGEX = Regex("(\\d+)-(\\d+)")

        fun decode(string: String): UR {
            val parsed = parse(string)
            if (parsed.components.isEmpty()) {
                throw UR.InvalidURException("Invalid path length")
            }
            return decode(parsed.type, parsed.components[0])
        }

        fun decode(type: String, body: String): UR {
            val cbor = Bytewords.decode(body, Bytewords.Style.MINIMAL)
            return UR(type, cbor)
        }

        internal fun parse(string: String): ParsedURString {
            val lowered = string.lowercase()

            if (!lowered.startsWith("ur:")) {
                throw UR.InvalidURException("Invalid scheme: must start with 'ur:'")
            }

            val path = lowered.substringAfter("ur:")
            val components = path.split("/")

            if (components.size <= 1) {
                throw UR.InvalidURException("Invalid path length")
            }

            val type = components[0]
            if (!UR.isValidType(type)) {
                throw UR.InvalidURException("Invalid type: $type")
            }

            return ParsedURString(type, components.subList(1, components.size))
        }
    }

    internal class ParsedURString(
        val type: String,
        val components: List<String>
    )
}
