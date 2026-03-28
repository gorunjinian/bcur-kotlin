package com.gorunjinian.bcur.fountain

import com.gorunjinian.bcur.ResultType
import com.gorunjinian.bcur.TestUtils
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FountainDecoderTest {
    @Test
    fun testDecoder() {
        val message = TestUtils.makeMessage(32767, "Wolf")
        val encoder = FountainEncoder(message, 1000, 10, 0)
        val decoder = FountainDecoder()

        do {
            val part = encoder.nextPart()
            decoder.receivePart(part)
        } while (decoder.result == null)

        assertEquals(ResultType.SUCCESS, decoder.result!!.type)
        assertContentEquals(message, decoder.result!!.data)
    }

    @Test
    fun testDecoderHighFirstSeq() {
        val message = TestUtils.makeMessage(32767, "Wolf")
        val encoder = FountainEncoder(message, 1000, 10, 100)
        val decoder = FountainDecoder()

        do {
            val part = encoder.nextPart()
            decoder.receivePart(part)
        } while (decoder.result == null)

        assertEquals(ResultType.SUCCESS, decoder.result!!.type)
        assertContentEquals(message, decoder.result!!.data)
    }
}
