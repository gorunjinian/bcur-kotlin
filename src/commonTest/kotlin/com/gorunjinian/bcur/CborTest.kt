package com.gorunjinian.bcur

import com.gorunjinian.bcur.fountain.FountainEncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CborTest {
    @Test
    fun testCborPartRoundTrip() {
        val part = FountainEncoder.Part(12, 8, 100, 0x12345678, byteArrayOf(1, 5, 3, 3, 5))
        val cbor = part.toCborBytes()
        val part2 = FountainEncoder.Part.fromCborBytes(cbor)
        assertTrue(cbor.contentEquals(part2.toCborBytes()))
    }

    @Test
    fun testCborByteStringEncoding() {
        val data = TestUtils.hexToBytes("00112233445566778899aabbccddeeff")
        val cbor = Cbor.wrapInByteString(data)
        assertEquals("5000112233445566778899aabbccddeeff", TestUtils.bytesToHex(cbor))
    }

    @Test
    fun testURWithCbor() {
        val data = TestUtils.hexToBytes("00112233445566778899aabbccddeeff")
        val cbor = Cbor.wrapInByteString(data)
        val ur = UR("bytes", cbor)
        val encoded = UREncoder.encode(ur)
        assertEquals("ur:bytes/gdaebycpeofygoiyktlonlpkrksfutwyzmwmfyeozs", encoded)
    }
}
