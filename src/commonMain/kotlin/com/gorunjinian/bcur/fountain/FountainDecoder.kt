package com.gorunjinian.bcur.fountain

import com.gorunjinian.bcur.CRC32
import com.gorunjinian.bcur.ResultType

/**
 * Fountain Decoder for Multipart UR (MUR).
 *
 * Reassembles a message from fountain-coded parts, supporting both
 * pure fragments and mixed (XOR) fragments for error recovery.
 *
 * Ported from https://github.com/sparrowwallet/hummingbird
 */
class FountainDecoder {
    private val _receivedPartIndexes: MutableSet<Int> = mutableSetOf()
    private val processedPartHashes: MutableSet<Int> = mutableSetOf()
    private val simpleParts: MutableMap<List<Int>, Part> = mutableMapOf()
    private var mixedParts: MutableMap<List<Int>, Part> = mutableMapOf()
    private val queuedParts: MutableList<Part> = mutableListOf()

    private var expectedPartIndexes: Set<Int>? = null
    private var expectedFragmentLen: Int = 0
    private var expectedMessageLen: Int = 0
    private var expectedChecksum: Long = 0

    val receivedPartIndexes: Set<Int> get() = _receivedPartIndexes

    var lastPartIndexes: Set<Int>? = null
        private set

    var processedPartsCount: Int = 0
        private set

    var result: Result? = null
        private set

    val expectedPartCount: Int
        get() = expectedPartIndexes?.size ?: 0

    val estimatedPercentComplete: Double
        get() {
            if (processedPartsCount == 0) return 0.0
            val estimatedInputParts = expectedPartCount * 1.75
            return minOf(0.99, processedPartsCount / estimatedInputParts)
        }

    fun receivePart(encoderPart: FountainEncoder.Part): Boolean {
        if (result != null) return false
        if (!validatePart(encoderPart)) return false

        val part = Part(encoderPart)
        lastPartIndexes = part.partIndexes.toSet()
        enqueue(part)

        while (result == null && queuedParts.isNotEmpty()) {
            processQueueItem()
        }

        if (processedPartHashes.add(encoderPart.hashCode())) {
            processedPartsCount++
        }

        return true
    }

    private fun enqueue(part: Part) {
        queuedParts.add(part)
    }

    private fun processQueueItem() {
        val part = queuedParts.removeAt(0)
        if (part.isSimple) {
            processSimplePart(part)
        } else {
            processMixedPart(part)
        }
    }

    private fun processSimplePart(part: Part) {
        val fragmentIndex = part.partIndexes[0]
        if (fragmentIndex in _receivedPartIndexes) return

        simpleParts[part.partIndexes] = part
        _receivedPartIndexes.add(fragmentIndex)

        if (_receivedPartIndexes == expectedPartIndexes) {
            val sortedParts = simpleParts.values.sortedBy { it.index }
            val fragments = sortedParts.map { it.data }
            val message = joinFragments(fragments, expectedMessageLen)

            val checksum = CRC32.compute(message)
            result = if (checksum == expectedChecksum) {
                Result(ResultType.SUCCESS, message, null)
            } else {
                Result(ResultType.FAILURE, null, "Invalid checksum")
            }
        } else {
            reduceMixed(part)
        }
    }

    private fun processMixedPart(part: Part) {
        if (part.partIndexes in mixedParts) return

        val allParts = mutableListOf<Part>()
        allParts.addAll(simpleParts.values)
        allParts.addAll(mixedParts.values)
        val reduced = allParts.fold(part) { acc, existing -> reducePart(acc, existing) }

        if (reduced.isSimple) {
            enqueue(reduced)
        } else {
            reduceMixed(reduced)
            mixedParts[reduced.partIndexes] = reduced
        }
    }

    private fun reduceMixed(by: Part) {
        val reducedParts = mixedParts.values.map { reducePart(it, by) }
        val newMixed = mutableMapOf<List<Int>, Part>()

        reducedParts.forEach { reducedPart ->
            if (reducedPart.isSimple) {
                enqueue(reducedPart)
            } else {
                newMixed[reducedPart.partIndexes] = reducedPart
            }
        }

        mixedParts = newMixed
    }

    private fun reducePart(a: Part, b: Part): Part {
        if (a.partIndexes.containsAll(b.partIndexes)) {
            val newIndexes = a.partIndexes.toMutableList()
            newIndexes.removeAll(b.partIndexes.toSet())
            val newData = FountainEncoder.xor(a.data, b.data)
            return Part(newIndexes, newData)
        }
        return a
    }

    private fun validatePart(part: FountainEncoder.Part): Boolean {
        if (expectedPartIndexes == null) {
            expectedPartIndexes = (0 until part.seqLen).toSet()
            expectedMessageLen = part.messageLen
            expectedChecksum = part.checksum
            expectedFragmentLen = part.data.size
            return true
        }

        return expectedPartCount == part.seqLen &&
                expectedMessageLen == part.messageLen &&
                expectedChecksum == part.checksum &&
                expectedFragmentLen == part.data.size
    }

    private class Part(
        val partIndexes: List<Int>,
        val data: ByteArray
    ) {
        val index: Int get() = partIndexes[0]
        val isSimple: Boolean get() = partIndexes.size == 1

        constructor(encoderPart: FountainEncoder.Part) : this(
            FountainUtils.chooseFragments(encoderPart.seqNum, encoderPart.seqLen, encoderPart.checksum),
            encoderPart.data
        )
    }

    class Result(
        val type: ResultType,
        val data: ByteArray?,
        val error: String?
    )

    companion object {
        fun joinFragments(fragments: List<ByteArray>, messageLen: Int): ByteArray {
            val totalSize = fragments.sumOf { it.size }
            val concatenated = ByteArray(totalSize)
            var offset = 0
            for (fragment in fragments) {
                fragment.copyInto(concatenated, offset)
                offset += fragment.size
            }
            return concatenated.copyOfRange(0, messageLen)
        }
    }
}
