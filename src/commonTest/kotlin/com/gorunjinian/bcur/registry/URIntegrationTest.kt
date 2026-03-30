package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem
import com.gorunjinian.bcur.UR
import com.gorunjinian.bcur.registry.pathcomponent.IndexPathComponent
import com.gorunjinian.bcur.registry.pathcomponent.WildcardPathComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class URIntegrationTest {

    @Test
    fun testCryptoHDKeyToUR() {
        val key = ByteArray(33) { it.toByte() }
        val chainCode = ByteArray(32) { (it + 1).toByte() }
        val origin = CryptoKeypath(
            listOf(IndexPathComponent(84, true), IndexPathComponent(0, true), IndexPathComponent(0, true)),
            byteArrayOf(0x12, 0x34, 0x56, 0x78)
        )
        val hdKey = CryptoHDKey(false, key, chainCode, null, origin, null, null)

        val ur = hdKey.toUR()
        assertEquals("crypto-hdkey", ur.type)
        assertTrue(ur.cborData.isNotEmpty())

        // Decode back from CBOR bytes
        val decoded = CborItem.decode(ur.cborData)
        val restored = CryptoHDKey.fromCbor(decoded)
        assertContentEquals(key, restored.key)
        assertEquals("84'/0'/0'", restored.origin?.path)
    }

    @Test
    fun testCryptoOutputToUR() {
        val key = CryptoHDKey(false, ByteArray(33) { 0xAA.toByte() }, ByteArray(32) { 0xBB.toByte() }, null, null, null, null)
        val output = CryptoOutput(listOf(ScriptExpression.WITNESS_PUBLIC_KEY_HASH), key)

        val ur = output.toUR()
        assertEquals("crypto-output", ur.type)
        assertTrue(ur.cborData.isNotEmpty())
    }

    @Test
    fun testCryptoPSBTToUR() {
        val psbtData = ByteArray(50) { it.toByte() }
        val psbt = CryptoPSBT(psbtData)

        val ur = psbt.toUR()
        assertEquals("crypto-psbt", ur.type)

        val restored = CryptoPSBT.fromCbor(CborItem.decode(ur.cborData))
        assertContentEquals(psbtData, restored.psbt)
    }

    @Test
    fun testURHDKeyRetagging() {
        val key = ByteArray(33) { it.toByte() }
        val chainCode = ByteArray(32) { (it + 1).toByte() }
        val useInfo = URCoinInfo(CryptoCoinInfo.Type.BITCOIN, CryptoCoinInfo.Network.MAINNET)
        val origin = URKeypath(
            listOf(IndexPathComponent(84, true), IndexPathComponent(0, true), IndexPathComponent(0, true)),
            byteArrayOf(0x12, 0x34, 0x56, 0x78)
        )
        val children = URKeypath(
            listOf(IndexPathComponent(0, false), WildcardPathComponent(false)),
            null
        )

        val urHdKey = URHDKey(false, key, chainCode, useInfo, origin, children, null)

        // Verify toCbor re-tags with 40xxx tags
        val cborItem = urHdKey.toCbor() as CborItem.Map
        val useInfoItem = cborItem.get(CryptoHDKey.USE_INFO_KEY)!!
        assertTrue(useInfoItem.tags.contains(40305UL), "useInfo should be tagged 40305")

        val originItem = cborItem.get(CryptoHDKey.ORIGIN_KEY)!!
        assertTrue(originItem.tags.contains(40304UL), "origin should be tagged 40304")

        // Round-trip through encode/decode
        val encoded = cborItem.encode()
        val decoded = CborItem.decode(encoded)
        val restored = URHDKey.fromCbor(decoded)

        assertEquals(RegistryType.HDKEY, restored.registryType)
        assertContentEquals(key, restored.key)
        assertEquals("84'/0'/0'", restored.origin?.path)
    }

    @Test
    fun testUROutputDescriptorToUR() {
        val descriptor = UROutputDescriptor("wpkh([deadbeef/84h/0h/0h]xpub.../0/*)")
        val ur = descriptor.toUR()
        assertEquals("output-descriptor", ur.type)

        val restored = UROutputDescriptor.fromCbor(CborItem.decode(ur.cborData))
        assertEquals("wpkh([deadbeef/84h/0h/0h]xpub.../0/*)", restored.source)
    }

    @Test
    fun testCryptoAccountToUR() {
        val key = CryptoHDKey(false, ByteArray(33) { it.toByte() }, ByteArray(32) { 1 }, null, null, null, null)
        val output = CryptoOutput(listOf(ScriptExpression.WITNESS_PUBLIC_KEY_HASH), key)
        val fp = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val account = CryptoAccount(fp, listOf(output))

        val ur = account.toUR()
        assertEquals("crypto-account", ur.type)

        val restored = CryptoAccount.fromCbor(CborItem.decode(ur.cborData))
        assertContentEquals(fp, restored.masterFingerprint)
        assertEquals(1, restored.outputDescriptors.size)
    }

    @Test
    fun testFingerprintConversion() {
        val bytes = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val ulong = RegistryItem.fingerprintToULong(bytes)
        val restored = RegistryItem.uLongToFingerprint(ulong, 4)
        assertContentEquals(bytes, restored)
    }
}
