package com.gorunjinian.bcur

import kotlin.test.Test
import kotlin.test.assertEquals

class SHA256Test {
    @Test
    fun testEmptyInput() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        val hash = SHA256.hash(ByteArray(0))
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", TestUtils.bytesToHex(hash))
    }

    @Test
    fun testShortInput() {
        // SHA-256("abc") = ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
        val hash = SHA256.hash("abc".encodeToByteArray())
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", TestUtils.bytesToHex(hash))
    }

    @Test
    fun test55ByteInput() {
        // Edge case: 55 bytes + 9 padding bytes = 64 (exact block boundary)
        // SHA-256 of 55 zero bytes
        val hash = SHA256.hash(ByteArray(55))
        assertEquals("02779466cdec163811d078815c633f21901413081449002f24aa3e80f0b88ef7", TestUtils.bytesToHex(hash))
    }

    @Test
    fun test56ByteInput() {
        // 56 bytes requires two blocks (56 + 1 + 8 = 65 > 64)
        val hash = SHA256.hash(ByteArray(56))
        assertEquals("d4817aa5497628e7c77e6b606107042bbba3130888c5f47a375e6179be789fbb", TestUtils.bytesToHex(hash))
    }

    @Test
    fun test119ByteInput() {
        // Another edge case: 119 + 9 = 128 (exact two-block boundary)
        val hash = SHA256.hash(ByteArray(119))
        assertEquals("f616b0d54e78571a9611f343c9f8e022e859e920381ab0e4d3da01e193a7bd7e", TestUtils.bytesToHex(hash))
    }
}
