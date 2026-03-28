package com.gorunjinian.bcur.fountain

import kotlin.test.Test
import kotlin.test.assertContentEquals

class Xoshiro256StarStarTest {
    @Test
    fun testRNG() {
        val rng = Xoshiro256StarStar("Wolf")
        val numbers = IntArray(100) { rng.nextInt(1, 10) }
        val expected = intArrayOf(
            6, 5, 8, 4, 10, 5, 7, 10, 4, 9, 10, 9, 7, 7, 1, 1, 2, 9, 9, 2,
            6, 4, 5, 7, 8, 5, 4, 2, 3, 8, 7, 4, 5, 1, 10, 9, 3, 10, 2, 6,
            8, 5, 7, 9, 3, 1, 5, 2, 7, 1, 4, 4, 4, 4, 9, 4, 5, 5, 6, 9,
            5, 1, 2, 8, 3, 3, 2, 8, 4, 3, 2, 1, 10, 8, 9, 3, 10, 8, 5, 5,
            6, 7, 10, 5, 8, 9, 4, 6, 4, 2, 10, 2, 1, 7, 9, 6, 7, 4, 2, 5
        )
        assertContentEquals(expected, numbers)
    }
}
