package com.gorunjinian.bcur

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BytewordsTest {
    @Test
    fun testBytewordsStandard() {
        val input = TestUtils.hexToBytes("d9012ca20150c7098580125e2ab0981253468b2dbc5202d8641947da")
        val encoded = Bytewords.encode(input, Bytewords.Style.STANDARD)
        assertEquals(
            "tuna acid draw oboe acid good slot axis limp lava brag holy door puff monk brag guru frog luau drop roof grim also trip idle chef fuel twin tied draw grim ramp",
            encoded
        )
        val decoded = Bytewords.decode(encoded, Bytewords.Style.STANDARD)
        assertTrue(input.contentEquals(decoded))
    }

    @Test
    fun testBytewordsUri() {
        val input = TestUtils.hexToBytes("d9012ca20150c7098580125e2ab0981253468b2dbc5202d8641947da")
        val encoded = Bytewords.encode(input, Bytewords.Style.URI)
        assertEquals(
            "tuna-acid-draw-oboe-acid-good-slot-axis-limp-lava-brag-holy-door-puff-monk-brag-guru-frog-luau-drop-roof-grim-also-trip-idle-chef-fuel-twin-tied-draw-grim-ramp",
            encoded
        )
        val decoded = Bytewords.decode(encoded, Bytewords.Style.URI)
        assertTrue(input.contentEquals(decoded))
    }

    @Test
    fun testBytewordsMinimal() {
        val input = TestUtils.hexToBytes("d9012ca20150c7098580125e2ab0981253468b2dbc5202d8641947da")
        val encoded = Bytewords.encode(input, Bytewords.Style.MINIMAL)
        assertEquals("taaddwoeadgdstaslplabghydrpfmkbggufgludprfgmaotpiecffltntddwgmrp", encoded)
        val decoded = Bytewords.decode(encoded, Bytewords.Style.MINIMAL)
        assertTrue(input.contentEquals(decoded))
    }

    @Test
    fun testBytewordsRoundTrip() {
        val input = "Hello, world!".encodeToByteArray()
        val encoded = Bytewords.encode(input, Bytewords.Style.MINIMAL)
        val decoded = Bytewords.decode(encoded, Bytewords.Style.MINIMAL)
        assertTrue(input.contentEquals(decoded))
        assertEquals((input.size + 4) * 2, encoded.length)
    }
}
