package com.gorunjinian.bcur

import kotlin.test.Test
import kotlin.test.assertEquals

class CRC32Test {
    @Test
    fun testCRC32() {
        val data = "Hello, world!".encodeToByteArray()
        assertEquals(0xebe6c6e6L, CRC32.compute(data))
    }
}
