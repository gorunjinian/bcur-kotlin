package com.gorunjinian.bcur

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class CborItemTest {

    // --- Unsigned Integer round-trips ---

    @Test
    fun testUnsignedIntSmall() {
        val item = CborItem.UInt(23)
        val encoded = item.encode()
        assertEquals(1, encoded.size)
        assertEquals(23.toByte(), encoded[0])

        val decoded = CborItem.decode(encoded) as CborItem.UInt
        assertEquals(23UL, decoded.value)
    }

    @Test
    fun testUnsignedIntOneByte() {
        val item = CborItem.UInt(200)
        val encoded = item.encode()
        assertEquals(2, encoded.size)
        assertEquals(0x18.toByte(), encoded[0])

        val decoded = CborItem.decode(encoded) as CborItem.UInt
        assertEquals(200UL, decoded.value)
    }

    @Test
    fun testUnsignedIntTwoBytes() {
        val item = CborItem.UInt(1000)
        val encoded = item.encode()
        val decoded = CborItem.decode(encoded) as CborItem.UInt
        assertEquals(1000UL, decoded.value)
    }

    @Test
    fun testUnsignedIntFourBytes() {
        val item = CborItem.UInt(100000)
        val encoded = item.encode()
        val decoded = CborItem.decode(encoded) as CborItem.UInt
        assertEquals(100000UL, decoded.value)
    }

    // --- Byte String round-trips ---

    @Test
    fun testByteString() {
        val data = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val item = CborItem.Bytes(data)
        val encoded = item.encode()
        val decoded = CborItem.decode(encoded) as CborItem.Bytes
        assertContentEquals(data, decoded.value)
    }

    @Test
    fun testEmptyByteString() {
        val item = CborItem.Bytes(byteArrayOf())
        val encoded = item.encode()
        assertEquals(1, encoded.size)
        assertEquals(0x40.toByte(), encoded[0])
        val decoded = CborItem.decode(encoded) as CborItem.Bytes
        assertEquals(0, decoded.value.size)
    }

    // --- Text String round-trips ---

    @Test
    fun testTextString() {
        val item = CborItem.Text("hello")
        val encoded = item.encode()
        val decoded = CborItem.decode(encoded) as CborItem.Text
        assertEquals("hello", decoded.value)
    }

    @Test
    fun testEmptyTextString() {
        val item = CborItem.Text("")
        val encoded = item.encode()
        assertEquals(1, encoded.size)
        assertEquals(0x60.toByte(), encoded[0])
        val decoded = CborItem.decode(encoded) as CborItem.Text
        assertEquals("", decoded.value)
    }

    // --- Boolean round-trips ---

    @Test
    fun testBoolTrue() {
        val item = CborItem.Bool(true)
        val encoded = item.encode()
        assertEquals(1, encoded.size)
        assertEquals(0xF5.toByte(), encoded[0])
        val decoded = CborItem.decode(encoded) as CborItem.Bool
        assertTrue(decoded.value)
    }

    @Test
    fun testBoolFalse() {
        val item = CborItem.Bool(false)
        val encoded = item.encode()
        assertEquals(1, encoded.size)
        assertEquals(0xF4.toByte(), encoded[0])
        val decoded = CborItem.decode(encoded) as CborItem.Bool
        assertEquals(false, decoded.value)
    }

    // --- Array round-trips ---

    @Test
    fun testArray() {
        val item = CborItem.Array(
            CborItem.UInt(1),
            CborItem.UInt(2),
            CborItem.UInt(3)
        )
        val encoded = item.encode()
        val decoded = CborItem.decode(encoded) as CborItem.Array
        assertEquals(3, decoded.items.size)
        assertEquals(1UL, (decoded.items[0] as CborItem.UInt).value)
        assertEquals(2UL, (decoded.items[1] as CborItem.UInt).value)
        assertEquals(3UL, (decoded.items[2] as CborItem.UInt).value)
    }

    @Test
    fun testEmptyArray() {
        val item = CborItem.Array()
        val encoded = item.encode()
        assertEquals(1, encoded.size)
        assertEquals(0x80.toByte(), encoded[0])
        val decoded = CborItem.decode(encoded) as CborItem.Array
        assertEquals(0, decoded.items.size)
    }

    // --- Map round-trips ---

    @Test
    fun testMap() {
        val item = CborItem.Map()
        item.put(CborItem.UInt(1), CborItem.Text("one"))
        item.put(CborItem.UInt(2), CborItem.Bytes(byteArrayOf(0x02)))

        val encoded = item.encode()
        val decoded = CborItem.decode(encoded) as CborItem.Map

        assertEquals(2, decoded.entries.size)
        assertEquals("one", (decoded.get(1) as CborItem.Text).value)
        assertContentEquals(byteArrayOf(0x02), (decoded.get(2) as CborItem.Bytes).value)
    }

    // --- Tag round-trips ---

    @Test
    fun testSingleTag() {
        val item = CborItem.UInt(42)
        item.tagged(305UL)

        val encoded = item.encode()
        val decoded = CborItem.decode(encoded) as CborItem.UInt
        assertEquals(42UL, decoded.value)
        assertEquals(1, decoded.tags.size)
        assertEquals(305UL, decoded.tags[0])
    }

    @Test
    fun testMultipleTags() {
        // Simulate CryptoOutput tag chaining: tag(401) tag(404) tag(303) map{...}
        val map = CborItem.Map()
        map.put(CborItem.UInt(3), CborItem.Bytes(byteArrayOf(0x01)))

        map.tags.add(401UL)  // wsh (outermost)
        map.tags.add(404UL)  // wpkh
        map.tags.add(303UL)  // crypto-hdkey (innermost)

        val encoded = map.encode()
        val decoded = CborItem.decode(encoded) as CborItem.Map

        assertEquals(3, decoded.tags.size)
        assertEquals(401UL, decoded.tags[0])  // outermost
        assertEquals(404UL, decoded.tags[1])
        assertEquals(303UL, decoded.tags[2])  // innermost

        assertContentEquals(byteArrayOf(0x01), (decoded.get(3) as CborItem.Bytes).value)
    }

    // --- Known CBOR hex vectors (RFC 8949) ---

    @Test
    fun testKnownVectorZero() {
        // 0 encodes as 0x00
        val encoded = CborItem.UInt(0).encode()
        assertContentEquals(byteArrayOf(0x00), encoded)
    }

    @Test
    fun testKnownVectorTwentyThree() {
        // 23 encodes as 0x17
        val encoded = CborItem.UInt(23).encode()
        assertContentEquals(byteArrayOf(0x17), encoded)
    }

    @Test
    fun testKnownVectorTwentyFour() {
        // 24 encodes as 0x18 0x18
        val encoded = CborItem.UInt(24).encode()
        assertContentEquals(byteArrayOf(0x18, 0x18), encoded)
    }

    @Test
    fun testKnownVectorHundred() {
        // 100 encodes as 0x18 0x64
        val encoded = CborItem.UInt(100).encode()
        assertContentEquals(byteArrayOf(0x18, 0x64), encoded)
    }

    @Test
    fun testKnownVectorThousand() {
        // 1000 encodes as 0x19 0x03 0xe8
        val encoded = CborItem.UInt(1000).encode()
        assertContentEquals(byteArrayOf(0x19, 0x03, 0xe8.toByte()), encoded)
    }

    @Test
    fun testNullValue() {
        val encoded = CborItem.Null.encode()
        assertEquals(1, encoded.size)
        assertEquals(0xF6.toByte(), encoded[0])
    }

    // --- Nested structures ---

    @Test
    fun testNestedMapWithTaggedValues() {
        // Simulates a CryptoHDKey-like structure with tagged inner maps
        val map = CborItem.Map()
        map.put(CborItem.UInt(1), CborItem.Bool(true))  // isMaster
        map.put(CborItem.UInt(3), CborItem.Bytes(ByteArray(33)))  // keyData

        val innerMap = CborItem.Map()
        innerMap.put(CborItem.UInt(1), CborItem.UInt(0))  // coinType = BITCOIN
        innerMap.tagged(305UL)  // tag as crypto-coininfo
        map.put(CborItem.UInt(5), innerMap)  // useInfo

        val encoded = map.encode()
        val decoded = CborItem.decode(encoded) as CborItem.Map

        val isMaster = (decoded.get(1) as CborItem.Bool).value
        assertTrue(isMaster)

        val useInfo = decoded.get(5) as CborItem.Map
        assertEquals(1, useInfo.tags.size)
        assertEquals(305UL, useInfo.tags[0])
        assertEquals(0UL, (useInfo.get(1) as CborItem.UInt).value)
    }
}
