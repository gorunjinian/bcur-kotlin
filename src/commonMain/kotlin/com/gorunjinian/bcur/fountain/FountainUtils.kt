package com.gorunjinian.bcur.fountain

/**
 * Fountain Code utilities for Multipart UR (MUR).
 *
 * Ported from https://github.com/sparrowwallet/hummingbird
 */
object FountainUtils {

    fun chooseFragments(seqNum: Long, seqLen: Int, checksum: Long): List<Int> {
        if (seqNum <= seqLen) {
            return listOf((seqNum - 1).toInt())
        } else {
            val seed = ByteArray(8)
            seed[0] = ((seqNum shr 24) and 0xFF).toByte()
            seed[1] = ((seqNum shr 16) and 0xFF).toByte()
            seed[2] = ((seqNum shr 8) and 0xFF).toByte()
            seed[3] = (seqNum and 0xFF).toByte()
            seed[4] = ((checksum shr 24) and 0xFF).toByte()
            seed[5] = ((checksum shr 16) and 0xFF).toByte()
            seed[6] = ((checksum shr 8) and 0xFF).toByte()
            seed[7] = (checksum and 0xFF).toByte()

            val rng = Xoshiro256StarStar(seed)
            val degree = chooseDegree(seqLen, rng)
            val indexes = (0 until seqLen).toMutableList()
            val shuffled = shuffled(indexes, rng)
            return shuffled.take(degree)
        }
    }

    internal fun chooseDegree(seqLen: Int, rng: Xoshiro256StarStar): Int {
        val probabilities = (1..seqLen).map { 1.0 / it }
        val sampler = RandomSampler(probabilities)
        return sampler.next(rng) + 1
    }

    internal fun shuffled(indexes: List<Int>, rng: Xoshiro256StarStar): List<Int> {
        val remaining = indexes.toMutableList()
        val result = mutableListOf<Int>()

        while (remaining.isNotEmpty()) {
            val index = rng.nextInt(0, remaining.size)
            result.add(remaining.removeAt(index))
        }

        return result
    }
}
