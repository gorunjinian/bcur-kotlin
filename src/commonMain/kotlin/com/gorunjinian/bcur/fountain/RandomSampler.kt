package com.gorunjinian.bcur.fountain

/**
 * Random-number sampling using the Walker-Vose alias method,
 * as described by Keith Schwarz (2011)
 * http://www.keithschwarz.com/darts-dice-coins
 *
 * Ported from https://github.com/sparrowwallet/hummingbird
 */
class RandomSampler(probabilities: List<Double>) {
    private val aliases: IntArray
    private val probs: DoubleArray
    private val n: Int = probabilities.size

    init {
        val sum = probabilities.sum()
        val normalizedProbs = probabilities.map { it / sum * n }.toMutableList()

        aliases = IntArray(n)
        probs = DoubleArray(n)

        val small = mutableListOf<Int>()
        val large = mutableListOf<Int>()

        // Reverse iteration order to match hummingbird exactly
        for (i in n - 1 downTo 0) {
            if (normalizedProbs[i] < 1.0) {
                small.add(i)
            } else {
                large.add(i)
            }
        }

        while (small.isNotEmpty() && large.isNotEmpty()) {
            val a = small.removeAt(small.size - 1)
            val g = large.removeAt(large.size - 1)

            probs[a] = normalizedProbs[a]
            aliases[a] = g

            normalizedProbs[g] = (normalizedProbs[g] + normalizedProbs[a]) - 1.0

            if (normalizedProbs[g] < 1.0) {
                small.add(g)
            } else {
                large.add(g)
            }
        }

        while (large.isNotEmpty()) {
            probs[large.removeAt(large.size - 1)] = 1.0
        }

        while (small.isNotEmpty()) {
            probs[small.removeAt(small.size - 1)] = 1.0
        }
    }

    /**
     * Sample using the alias method.
     * Uses nextDouble()-based bucket selection to match hummingbird exactly.
     */
    fun next(rng: Xoshiro256StarStar): Int {
        val r1 = rng.nextDouble()
        val r2 = rng.nextDouble()
        val i = (n.toDouble() * r1).toInt()
        return if (r2 < probs[i]) i else aliases[i]
    }
}
